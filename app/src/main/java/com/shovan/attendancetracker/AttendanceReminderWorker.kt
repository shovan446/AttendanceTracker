package com.shovan.attendancetracker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class AttendanceReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("attendance_tracker_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("reminder_enabled", false)) return Result.success()

        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.value // 1 = Monday .. 7 = Sunday
        val todayLong = today.toEpochDay()

        val db = AttendanceDatabase.getDatabase(applicationContext)
        val dao = db.attendanceDao()

        val scheduledSubjects = dao.getSubjectsForDayOnce(dayOfWeek)
        if (scheduledSubjects.isEmpty()) return Result.success()

        val loggedSubjectIds = dao.getRecordsForDateOnce(todayLong).map { it.subjectId }.toSet()
        val pendingCount = scheduledSubjects.count { it.id !in loggedSubjectIds }

        if (pendingCount > 0) {
            showReminderNotification(pendingCount)
        }

        return Result.success()
    }

    private fun showReminderNotification(pendingCount: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val channelId = "attendance_daily_reminder"
        val channel = NotificationChannel(
            channelId,
            "Daily Attendance Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminds you to log unrecorded classes at the end of the day"
        }

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val openAppIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            1001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = if (pendingCount == 1) {
            "You have 1 unlogged class scheduled for today."
        } else {
            "You have $pendingCount unlogged classes scheduled for today."
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Log Today's Attendance")
            .setContentText(bodyText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(1001, notification)
    }

    companion object {
        private const val WORK_NAME = "daily_attendance_reminder_work"

        fun schedule(context: Context, hour: Int, minute: Int) {
            val now = LocalDateTime.now()
            var target = now.with(LocalTime.of(hour, minute, 0))
            if (!target.isAfter(now)) {
                target = target.plusDays(1)
            }

            val initialDelayMinutes = Duration.between(now, target).toMinutes().coerceAtLeast(1)

            val request = PeriodicWorkRequestBuilder<AttendanceReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}