package com.prateek.taro.util

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
import com.prateek.taro.R
import com.prateek.taro.energy.DailyEnergy
import com.prateek.taro.energy.EnergyFilter
import com.prateek.taro.ui.MainActivity
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object FoodReminderScheduler {
    const val EXTRA_LOG_FOOD = "com.prateek.taro.LOG_FOOD"
    private const val WORK_NAME = "food_reminder"
    private const val CHANNEL_ID = "com.prateek.taro.FOOD_REMINDER"
    private const val NOTIFICATION_ID = 4220
    private const val FALLBACK_RESTING = 1_500.0

    fun schedule(context: Context, replace: Boolean = false) {
        val workManager = WorkManager.getInstance(context)
        if (!AppPreferences.foodReminderEnabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val minutes = AppPreferences.foodReminderMinutes
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, minutes / 60)
            set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val request = PeriodicWorkRequestBuilder<FoodReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(next.timeInMillis - now.timeInMillis, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            if (replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun check(context: Context) {
        val eaten = Database.getInstance(context).foodOn(Util.todayDateString()).sumOf { it.kcal }
        if (EnergyFilter.isLogged(eaten, DailyEnergy.restingPerDay() ?: FALLBACK_RESTING)) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.food_reminder_channel), NotificationManager.IMPORTANCE_DEFAULT)
        )
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_LOG_FOOD, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val text = if (eaten <= 0) {
            context.getString(R.string.food_reminder_nothing)
        } else {
            context.getString(R.string.food_reminder_little, Util.formatSteps(eaten.roundToInt()))
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.food_reminder_title))
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) manager.notify(NOTIFICATION_ID, notification)
    }
}

class FoodReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        FoodReminderScheduler.check(applicationContext)
        return Result.success()
    }
}
