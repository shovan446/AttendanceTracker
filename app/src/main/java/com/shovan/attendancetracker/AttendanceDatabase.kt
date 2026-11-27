package com.shovan.attendancetracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Subject::class, TimetableSlot::class, AttendanceRecord::class],
    version = 4, // Upgraded version because schema changed (added manual counts)
    exportSchema = false
)
abstract class AttendanceDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao

    companion object {

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN slotId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_timetable_subjectId` ON `timetable` (`subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_records_subjectId` ON `attendance_records` (`subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_records_slotId` ON `attendance_records` (`slotId`)")
            }
        }
        @Volatile
        private var INSTANCE: AttendanceDatabase? = null

        fun getDatabase(context: Context): AttendanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AttendanceDatabase::class.java,
                    "attendance_manager_db"
                )
                    .addMigrations(MIGRATION_3_4) // Automatically wipes the old simple DB version to apply the new schema cleanly
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}