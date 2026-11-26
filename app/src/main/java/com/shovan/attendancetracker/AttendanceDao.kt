package com.shovan.attendancetracker

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableSlot(slot: TimetableSlot)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord)

    @Query("SELECT * FROM subjects")
    fun getAllSubjects(): Flow<List<Subject>>

    @Query("""
        SELECT DISTINCT subjects.* FROM subjects
        INNER JOIN timetable ON subjects.id = timetable.subjectId 
        WHERE timetable.dayOfWeek = :dayOfWeek
    """)
    fun getSubjectsForDay(dayOfWeek: Int): Flow<List<Subject>>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE subjectId = :subId AND status = 'PRESENT'")
    fun getPresentCount(subId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE subjectId = :subId AND status != 'CANCELLED'")
    fun getTotalCount(subId: Int): Flow<Int>

    @Query("SELECT * FROM attendance_records WHERE dateLong = :date")
    fun getRecordsForDate(date: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subId AND dateLong = :date")
    suspend fun getRecordForDate(subId: Int, date: Long): AttendanceRecord?

    @Update
    suspend fun updateSubject(subject: Subject)

    @Query("DELETE FROM timetable WHERE subjectId = :subId AND dayOfWeek = :day")
    suspend fun deleteTimetableSlot(subId: Int, day: Int)

    @Delete
    suspend fun deleteSubject(subject: Subject)

    @Query("DELETE FROM attendance_records WHERE subjectId = :subId AND dateLong = :date")
    suspend fun deleteRecordForDate(subId: Int, date: Long)

    @Query("SELECT * FROM subjects")
    suspend fun getAllSubjectsOnce(): List<Subject>

    @Query("SELECT * FROM timetable")
    suspend fun getAllTimetableSlotsOnce(): List<TimetableSlot>

    @Query("SELECT * FROM attendance_records")
    suspend fun getAllAttendanceRecordsOnce(): List<AttendanceRecord>

    @Query("DELETE FROM attendance_records")
    suspend fun clearAllAttendanceRecords()

    @Query("DELETE FROM timetable")
    suspend fun clearAllTimetableSlots()

    @Query("DELETE FROM subjects")
    suspend fun clearAllSubjects()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<Subject>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableSlots(slots: List<TimetableSlot>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecords(records: List<AttendanceRecord>)

    @Query("""
        SELECT DISTINCT subjects.* FROM subjects
        INNER JOIN timetable ON subjects.id = timetable.subjectId 
        WHERE timetable.dayOfWeek = :dayOfWeek
    """)
    suspend fun getSubjectsForDayOnce(dayOfWeek: Int): List<Subject>

    @Query("SELECT * FROM attendance_records WHERE dateLong = :date")
    suspend fun getRecordsForDateOnce(date: Long): List<AttendanceRecord>
}