package com.shovan.attendancetracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset

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
}
