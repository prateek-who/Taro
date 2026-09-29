package com.nvllz.stepsy.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nvllz.stepsy.R
import com.nvllz.stepsy.ui.MainActivity
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object WeightReminderScheduler {
    const val EXTRA_LOG_WEIGHT = "com.nvllz.stepsy.LOG_WEIGHT"
    private const val WORK_NAME = "weight_reminder"
    private const val CHANNEL_ID = "com.nvllz.stepsy.WEIGHT_REMINDER"
    private const val NOTIFICATION_ID = 4210

    fun nextTrigger(nowMs: Long, reminder: WeightReminder, zone: TimeZone = TimeZone.getDefault()): Long {
        val next = Calendar.getInstance(zone).apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, reminder.minuteOfDay / 60)
            set(Calendar.MINUTE, reminder.minuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (reminder.weekly) {
            while (next.get(Calendar.DAY_OF_WEEK) != reminder.dayOfWeek || next.timeInMillis <= nowMs) {
                next.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else if (next.timeInMillis <= nowMs) {
            next.add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis
    }

    fun schedule(context: Context, replace: Boolean = true) {
        val workManager = WorkManager.getInstance(context)
        val reminder = AppPreferences.weightReminder
        if (!reminder.enabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val now = System.currentTimeMillis()
        val request = PeriodicWorkRequestBuilder<WeightReminderWorker>(if (reminder.weekly) 7L else 1L, TimeUnit.DAYS)
            .setInitialDelay(nextTrigger(now, reminder) - now, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            if (replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun showNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.weight_reminder_channel), NotificationManager.IMPORTANCE_DEFAULT)
        )
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_LOG_WEIGHT, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.weight_reminder_notification_title))
            .setContentText(context.getString(R.string.weight_reminder_notification_text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    fun dismissNotification(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
    }
}

class WeightReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val today = Util.todayDateString()
        val loggedToday = Database.getInstance(applicationContext).weightsSince(today).any { it.date == today }
        if (!loggedToday) WeightReminderScheduler.showNotification(applicationContext)
        return Result.success()
    }
}
