package com.shovan.attendancetracker

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val minAttendancePercentage: Int = 75,
    val initialPresent: Int = 0,
    val initialTotal: Int = 0
)

// NEW: Stores which subjects happen on which days
@Entity(
    tableName = "timetable",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TimetableSlot(
    @PrimaryKey(autoGenerate = true) val slotId: Int = 0,
    val subjectId: Int,
    val dayOfWeek: Int, // 1 = Monday, 2 = Tuesday ... 7 = Sunday
    val timeString: String // e.g., "10:00 AM" or "04:00 PM"
)

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val recordId: Int = 0,
    val subjectId: Int,
    val dateLong: Long, // Epoch timestamp of the specific calendar day
    val status: String  // "PRESENT", "ABSENT", "CANCELLED"
)