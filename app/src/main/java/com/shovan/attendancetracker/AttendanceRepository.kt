package com.shovan.attendancetracker

import kotlinx.coroutines.flow.Flow

class AttendanceRepository(private val attendanceDao: AttendanceDao) {

    // Get all subjects directly from DB
    val allSubjects: Flow<List<Subject>> = attendanceDao.getAllSubjects()

    // Add a new subject
    suspend fun addSubject(name: String, minPercentage: Int) {
        val newSubject = Subject(name = name, minAttendancePercentage = minPercentage)
        attendanceDao.insertSubject(newSubject)
    }

    // Delete a subject
    suspend fun deleteSubject(subject: Subject) {
        attendanceDao.deleteSubject(subject)
    }
}