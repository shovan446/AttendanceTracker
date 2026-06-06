package com.shovan.attendancetracker

import kotlin.math.ceil

data class SubjectWithStats(
    val subject: Subject,
    val totalRecords: Int,
    val presentRecords: Int
) {
    val totalClasses: Int get() = totalRecords + subject.initialTotal
    val presentClasses: Int get() = presentRecords + subject.initialPresent
    
    val attendancePercentage: Double 
        get() = if (totalClasses > 0) (presentClasses.toDouble() / totalClasses) * 100 else 100.0

    val statusMessage: String
        get() {
            val target = subject.minAttendancePercentage / 100.0

            if (totalClasses == 0) return "No classes tracked yet."

            val currentPercentage = attendancePercentage

            return if (currentPercentage >= subject.minAttendancePercentage) {
                // Safe to bunk calculation
                var safeToBunk = 0
                var simulatedTotal = totalClasses
                while (true) {
                    val simulatedPercentage = (presentClasses.toDouble() / (simulatedTotal + 1)) * 100
                    if (simulatedPercentage >= subject.minAttendancePercentage) {
                        safeToBunk++
                        simulatedTotal++
                    } else {
                        break
                    }
                }
                if (safeToBunk > 0) "You can safely bunk the next $safeToBunk class(es)."
                else "You are right on the line! Don't miss the next class."
            } else {
                // Must attend calculation
                val required = ceil((target * totalClasses - presentClasses) / (1 - target)).toInt()
                "You must attend the next $required class(es) to recover."
            }
        }
}
