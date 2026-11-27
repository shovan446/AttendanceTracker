package com.shovan.attendancetracker

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val minAttendancePercentage: Int = 75,
    val initialPresent: Int = 0,
    val initialTotal: Int = 0
)

// Stores which subjects happen on which days (supports multiple slots per subject per day)
@Entity(
    tableName = "timetable",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"])]
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
    ],
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["slotId"])
    ]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val recordId: Int = 0,
    val subjectId: Int,
    val slotId: Int = 0, // Links directly to TimetableSlot.slotId for multiple classes per day
    val dateLong: Long,  // Epoch timestamp of the specific calendar day
    val status: String   // "PRESENT", "ABSENT", "CANCELLED"
)

// Relation model used by AttendanceDao to fetch each individual scheduled slot along with its Subject
data class ScheduledSlotWithSubject(
    @Embedded val slot: TimetableSlot,
    @Relation(
        parentColumn = "subjectId",
        entityColumn = "id"
    )
    val subject: Subject
)