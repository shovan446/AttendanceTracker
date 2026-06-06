package com.shovan.attendancetracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Subject::class, TimetableSlot::class, AttendanceRecord::class],
    version = 3, // Upgraded version because schema changed (added manual counts)
    exportSchema = false
)
abstract class AttendanceDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AttendanceDatabase? = null

        fun getDatabase(context: Context): AttendanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AttendanceDatabase::class.java,
                    "attendance_manager_db"
                )
                    .fallbackToDestructiveMigration() // Automatically wipes the old simple DB version to apply the new schema cleanly
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}