package com.shovan.attendancetracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import org.json.JSONArray
import org.json.JSONObject

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AttendanceDatabase.getDatabase(application).attendanceDao()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    val allSubjects: StateFlow<List<Subject>> = dao.getAllSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val subjectsWithStatsState: StateFlow<List<SubjectWithStats>> = dao.getAllSubjects()
        .flatMapLatest { subjects ->
            if (subjects.isEmpty()) return@flatMapLatest flowOf(emptyList())
            val flows = subjects.map { subject ->
                combine(dao.getPresentCount(subject.id), dao.getTotalCount(subject.id)) { present, total ->
                    SubjectWithStats(subject, total, present)
                }
            }
            combine(flows) { it.toList() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val scheduledSubjectsForSelectedDate: StateFlow<List<Subject>> = _selectedDate
        .flatMapLatest { date -> dao.getSubjectsForDay(date.dayOfWeek.value) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val attendanceRecordsForSelectedDate: StateFlow<Map<Int, AttendanceRecord>> = _selectedDate
        .flatMapLatest { date -> dao.getRecordsForDate(dateToLong(date)) }
        .map { records -> records.associateBy { it.subjectId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun addSubject(name: String, minPercentage: Int) {
        viewModelScope.launch { dao.insertSubject(Subject(name = name, minAttendancePercentage = minPercentage)) }
    }

    fun updateInitialAttendance(subject: Subject, present: Int, total: Int) {
        viewModelScope.launch {
            dao.updateSubject(subject.copy(initialPresent = present, initialTotal = total))
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch { dao.deleteSubject(subject) }
    }

    fun assignSubjectToDay(subjectId: Int, dayOfWeek: Int) {
        viewModelScope.launch {
            dao.insertTimetableSlot(TimetableSlot(subjectId = subjectId, dayOfWeek = dayOfWeek, timeString = "Scheduled"))
        }
    }

    fun logAttendance(subjectId: Int, date: LocalDate, status: String) {
        viewModelScope.launch {
            val dateLong = dateToLong(date)
            val existing = dao.getRecordForDate(subjectId, dateLong)
            if (existing != null) {
                dao.insertRecord(existing.copy(status = status))
            } else {
                dao.insertRecord(AttendanceRecord(subjectId = subjectId, dateLong = dateLong, status = status))
            }
        }
    }

    fun markAllForSelectedDate(status: String) {
        val subjects = scheduledSubjectsForSelectedDate.value
        val date = _selectedDate.value
        if (subjects.isEmpty()) return

        subjects.forEach { subject ->
            logAttendance(subject.id, date, status)
        }
    }

    fun clearAllForSelectedDate() {
        val subjects = scheduledSubjectsForSelectedDate.value
        val date = _selectedDate.value
        if (subjects.isEmpty()) return

        subjects.forEach { subject ->
            removeAttendance(subject.id, date)
        }
    }


    fun removeAttendance(subjectId: Int, date: LocalDate) {
        viewModelScope.launch {
            dao.deleteRecordForDate(subjectId, dateToLong(date))
        }
    }

    fun dateToLong(date: LocalDate): Long {
        return date.atStartOfDay().toEpochSecond(ZoneOffset.UTC)
    }
    
    // For Timetable view selection
    private val _selectedDayOfWeek = MutableStateFlow(LocalDate.now().dayOfWeek.value)
    val selectedDayOfWeek: StateFlow<Int> = _selectedDayOfWeek
    
    fun selectDay(day: Int) {
        _selectedDayOfWeek.value = day
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val timetableSubjectsForSelectedDay: StateFlow<List<Subject>> = _selectedDayOfWeek
        .flatMapLatest { day -> dao.getSubjectsForDay(day) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeSubjectFromDay(subjectId: Int, dayOfWeek: Int) {
        viewModelScope.launch {
            dao.deleteTimetableSlot(subjectId, dayOfWeek)
        }
    }

    suspend fun exportBackupJson(): String {
        val subjects = dao.getAllSubjectsOnce()
        val slots = dao.getAllTimetableSlotsOnce()
        val records = dao.getAllAttendanceRecordsOnce()

        val root = JSONObject().apply {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())

            put("subjects", JSONArray().apply {
                subjects.forEach { s ->
                    put(JSONObject().apply {
                        put("id", s.id)
                        put("name", s.name)
                        put("minAttendancePercentage", s.minAttendancePercentage)
                        put("initialPresent", s.initialPresent)
                        put("initialTotal", s.initialTotal)
                    })
                }
            })

            put("timetableSlots", JSONArray().apply {
                slots.forEach { slot ->
                    put(JSONObject().apply {
                        put("subjectId", slot.subjectId)
                        put("dayOfWeek", slot.dayOfWeek)
                        put("timeString", slot.timeString)
                    })
                }
            })

            put("attendanceRecords", JSONArray().apply {
                records.forEach { r ->
                    put(JSONObject().apply {
                        put("subjectId", r.subjectId)
                        put("dateLong", r.dateLong)
                        put("status", r.status)
                    })
                }
            })
        }
        return root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Result<String> {
        return try {
            val root = JSONObject(jsonString)
            val subjectsArr = root.getJSONArray("subjects")
            val slotsArr = root.getJSONArray("timetableSlots")
            val recordsArr = root.getJSONArray("attendanceRecords")

            val subjects = (0 until subjectsArr.length()).map { i ->
                val obj = subjectsArr.getJSONObject(i)
                Subject(
                    id = obj.getInt("id"),
                    name = obj.getString("name"),
                    minAttendancePercentage = obj.getInt("minAttendancePercentage"),
                    initialPresent = obj.optInt("initialPresent", 0),
                    initialTotal = obj.optInt("initialTotal", 0)
                )
            }

            val slots = (0 until slotsArr.length()).map { i ->
                val obj = slotsArr.getJSONObject(i)
                TimetableSlot(
                    subjectId = obj.getInt("subjectId"),
                    dayOfWeek = obj.getInt("dayOfWeek"),
                    timeString = obj.optString("timeString", "")
                )
            }

            val records = (0 until recordsArr.length()).map { i ->
                val obj = recordsArr.getJSONObject(i)
                AttendanceRecord(
                    subjectId = obj.getInt("subjectId"),
                    dateLong = obj.getLong("dateLong"),
                    status = obj.getString("status")
                )
            }

            // Clear child tables first to respect foreign keys, then restore
            dao.clearAllAttendanceRecords()
            dao.clearAllTimetableSlots()
            dao.clearAllSubjects()

            dao.insertSubjects(subjects)
            dao.insertTimetableSlots(slots)
            dao.insertAttendanceRecords(records)

            Result.success("Restored ${subjects.size} subjects & ${records.size} records")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
