package com.prateek.taro.util

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.ColorRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.prateek.taro.R
import com.prateek.taro.ui.MainActivity
import com.prateek.taro.ui.components.SPREE_NAMES
import com.prateek.taro.ui.components.TIER_NAMES
import com.prateek.taro.ui.components.tierMultiple
import com.prateek.taro.ui.stepStreak
import com.prateek.taro.ui.stepsByDay
import com.prateek.taro.ui.stepsDay
import java.util.Calendar
import java.util.Date
import kotlin.concurrent.thread
import kotlin.math.ceil

data class PaceInfo(val cadence: Double?, val goalAt: Long?, val at: Long)

object StepMoments {
    const val EXTRA_OPEN_STEPS = "com.prateek.taro.OPEN_STEPS"
    private const val MOMENT_CHANNEL_ID = "daily_goal_notifications"
    private const val NUDGE_CHANNEL_ID = "pace_nudges"
    private const val TIER_NOTIFICATION_ID = 1001
    private const val NUDGE_NOTIFICATION_ID = 1002
    private const val SPREE_NOTIFICATION_ID = 1003
    private const val HOUR_MS = 3_600_000L
    private const val PACE_REFRESH_MS = 15 * 60_000L
    private const val NUDGE_EVERY_MS = 30 * 60_000L
    private const val NUDGE_FROM_HOUR = 12
    private const val NUDGE_UNTIL_HOUR = 21
    private const val NUDGE_BEHIND_SHARE = 0.15
    private const val NUDGE_MIN_BEHIND = 500
    private const val NUDGE_MAX_MINUTES = 90
    private const val NUDGE_ROUND_MINUTES = 5
    private const val FALLBACK_CADENCE = 100.0

    @ColorRes
    private val TIER_COLORS = listOf(R.color.colorGoal, R.color.colorMint, R.color.colorSpecial, R.color.colorFlame, R.color.colorRed, R.color.colorPurple, R.color.colorPurple)

    private var notified: Pair<String, Int>? = null
    private var lastNudgeCheck = 0L
    @Volatile private var pace: Pair<String, PaceInfo>? = null
    @Volatile private var paceLoading = false

    fun createNotificationChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(MOMENT_CHANNEL_ID, context.getString(R.string.moments_channel), NotificationManager.IMPORTANCE_HIGH)
                .apply { description = context.getString(R.string.moments_channel_desc) }
        )
        manager.createNotificationChannel(
            NotificationChannel(NUDGE_CHANNEL_ID, context.getString(R.string.nudges_channel), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.nudges_channel_desc) }
        )
    }

    fun tierColor(context: Context, index: Int): Int = ContextCompat.getColor(context, TIER_COLORS.getOrElse(index) { R.color.colorGoal })

    fun paceInfo(context: Context, steps: Int, goal: Int): PaceInfo? {
        val today = Util.todayDateString()
        val cached = pace?.takeIf { it.first == today }?.second
        if ((cached == null || System.currentTimeMillis() - cached.at > PACE_REFRESH_MS) && !paceLoading) {
            paceLoading = true
            thread {
                runCatching {
                    val date = Util.logicalToday()
                    val day = stepsDay(context, date, steps, goal)
                    val goalAt = day.pace.goalHour?.let { Util.dayStartMillis(date) + (it * HOUR_MS).toLong() }
                    pace = today to PaceInfo(day.cadence, goalAt, System.currentTimeMillis())
                }
                paceLoading = false
            }
        }
        return cached
    }

    fun onSteps(context: Context, steps: Int, goal: Int) {
        if (goal <= 0) return
        val today = Util.todayDateString()
        val tier = StepTiers.reached(steps, goal)
        val known = notified?.takeIf { it.first == today }?.second
            ?: (AppPreferences.tierNotified?.split(":")?.takeIf { it.firstOrNull() == today }?.getOrNull(1)?.toIntOrNull() ?: -1)
                .also { notified = today to it }
        if (tier > known) {
            notified = today to tier
            AppPreferences.tierNotified = "$today:$tier"
            thread { runCatching { announce(context, tier, known, steps, goal) } }
        }
        if (tier < 0) maybeNudge(context, steps, goal)
    }

    private fun announce(context: Context, tier: Int, previous: Int, steps: Int, goal: Int) {
        if (appVisible(context)) return
        if (AppPreferences.dailyGoalNotification && (tier == 0 || AppPreferences.tierMomentsAll)) {
            val name = context.getString(TIER_NAMES[tier])
            val text = when {
                tier == 0 -> context.getString(R.string.moment_goal_text, Util.formatSteps(goal), Util.formatSteps(StepTiers.threshold(1, goal) - steps), context.getString(TIER_NAMES[1]))
                tier < StepTiers.LAST -> context.getString(R.string.moment_tier_text, tierMultiple(tier), Util.formatSteps(StepTiers.threshold(tier + 1, goal) - steps), context.getString(TIER_NAMES[tier + 1]))
                else -> context.getString(R.string.moment_tier_top, tierMultiple(tier))
            }
            post(context, MOMENT_CHANNEL_ID, TIER_NOTIFICATION_ID, context.getString(R.string.moment_title, name, Util.formatSteps(steps)), text, tierColor(context, tier), high = true)
        }
        if (previous < 0 && AppPreferences.spreeMoments) {
            val days = stepsByDay(context)
            val goals = AppPreferences.goalHistory
            val choices = AppPreferences.streakChoices
            val preferRest = AppPreferences.streakPreferRest
            val now = stepStreak(days, steps, goals, choices, preferRest).current
            val before = stepStreak(days, 0, goals, choices, preferRest).current
            val spree = Sprees.reached(now)
            if (spree >= 0 && spree > Sprees.reached(before)) {
                val title = context.getString(SPREE_NAMES[spree]) + " · " + context.resources.getQuantityString(R.plurals.spree_days, now, now)
                val text = if (spree < Sprees.LAST) {
                    val left = Sprees.DAYS[spree + 1] - now
                    context.resources.getQuantityString(R.plurals.spree_next, left, left, context.getString(SPREE_NAMES[spree + 1])) + "."
                } else {
                    context.getString(R.string.tier_top)
                }
                post(context, MOMENT_CHANNEL_ID, SPREE_NOTIFICATION_ID, title, text, tierColor(context, spree + 1), high = true)
            }
        }
    }

    private fun maybeNudge(context: Context, steps: Int, goal: Int) {
        val now = System.currentTimeMillis()
        if (now - lastNudgeCheck < NUDGE_EVERY_MS) return
        lastNudgeCheck = now
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour < NUDGE_FROM_HOUR || hour >= NUDGE_UNTIL_HOUR) return
        thread {
            runCatching {
                if (!AppPreferences.encouragingNotifications) return@thread
                val today = Util.todayDateString()
                if (AppPreferences.paceNudged == today) return@thread
                val day = stepsDay(context, Util.logicalToday(), steps, goal)
                val usual = day.pace.usualByNow ?: return@thread
                val behind = usual - steps
                if (behind < maxOf(NUDGE_MIN_BEHIND.toDouble(), usual * NUDGE_BEHIND_SHARE)) return@thread
                val projected = day.pace.projected ?: return@thread
                if (projected >= goal) return@thread
                val minutes = (ceil((goal - projected) / (day.cadence ?: FALLBACK_CADENCE) / NUDGE_ROUND_MINUTES) * NUDGE_ROUND_MINUTES).toInt()
                if (minutes > NUDGE_MAX_MINUTES || appVisible(context)) return@thread
                AppPreferences.paceNudged = today
                val kind = context.getString(if (day.pace.weekend) R.string.pace_weekend else R.string.pace_weekday)
                post(
                    context,
                    NUDGE_CHANNEL_ID,
                    NUDGE_NOTIFICATION_ID,
                    context.getString(R.string.pace_behind, Util.formatSteps(behind), kind),
                    context.getString(R.string.nudge_text, minutes, Util.formatSteps(goal)),
                    tierColor(context, 0),
                    high = false,
                    action = R.string.nudge_open,
                )
            }
        }
    }

    fun timeText(context: Context, millis: Long): String = android.text.format.DateFormat.getTimeFormat(context).format(Date(millis)).lowercase()

    private fun appVisible(context: Context): Boolean {
        val manager = context.getSystemService(ActivityManager::class.java) ?: return false
        return manager.runningAppProcesses.orEmpty().any { it.processName == context.packageName && it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND }
    }

    private fun post(context: Context, channel: String, id: Int, title: String, text: String, color: Int, high: Boolean, action: Int? = null) {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN_STEPS, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(color)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(if (high) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .apply { action?.let { addAction(R.drawable.ic_notification, context.getString(it), pending) } }
            .build()
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id, notification)
        }
    }
}
