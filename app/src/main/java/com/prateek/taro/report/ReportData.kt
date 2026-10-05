package com.prateek.taro.report

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
import com.prateek.taro.achievements.AchievementData
import com.prateek.taro.energy.ActivityEnergy
import com.prateek.taro.energy.DailyEnergy
import com.prateek.taro.energy.EnergyFilter
import com.prateek.taro.energy.WeightJournal
import com.prateek.taro.sleep.Night
import com.prateek.taro.sleep.SleepInsights
import com.prateek.taro.ui.MainActivity
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Database
import com.prateek.taro.util.Util
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object ReportData {
    private const val TOP_FOODS = 3
    private const val BRISK_CADENCE = 100

    fun firstDayOfWeek(): DayOfWeek = DayOfWeek.of(((AppPreferences.firstDayOfWeek + 5) % 7) + 1)

    fun latestComplete(kind: ReportKind, today: LocalDate = Util.logicalToday()): Period =
        Period.containing(kind, today, firstDayOfWeek()).previous()

    fun stepsIn(context: Context, period: Period): Int =
        Database.getInstance(context).getEntries(period.start.toString(), period.end.toString()).sumOf { it.steps }

    fun load(context: Context, period: Period): PeriodReport? {
        val today = Util.logicalToday()
        if (period.start.isAfter(today)) return null
        val end = minOf(period.end, today)
        val database = Database.getInstance(context)
        val factors = AppPreferences.energyFactors
        val history = DailyEnergy.history(context, factors)
        val goals = AppPreferences.goalHistory
        val incomplete = AppPreferences.incompleteFoodDays
        val steps = database.getEntries(period.start.toString(), end.toString()).associate { it.date to it.steps }
        val foods = database.foodBetween(period.start.toString(), end.toString()).groupBy { it.date }
        val restingKcal = history?.restingPerDay ?: 0.0

        val dates = generateSequence(period.start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
        val days = dates.map { date ->
            val key = date.toString()
            val activity = ActivityEnergy.day(context, key)
            val logs = foods[key].orEmpty()
            val eaten = logs.takeIf { it.isNotEmpty() }?.sumOf { it.kcal }
            DayInput(
                date = date,
                steps = steps[key] ?: 0,
                distanceM = activity.distanceM,
                activeKcal = factors.activity * activity.activeKcal + factors.workout * database.activityKcal(key, key),
                burned = history?.let { DailyEnergy.day(context, it, key).total } ?: 0.0,
                eaten = eaten,
                protein = logs.sumOf { it.protein },
                fullyLogged = eaten != null && key !in incomplete && EnergyFilter.isLogged(eaten, restingKcal),
                goal = goals.on(date),
                complete = date.isBefore(today),
            )
        }

        val previous = period.previous().let { before ->
            val previousEnd = before.start.plusDays((dates.size - 1).toLong()).let { minOf(it, before.end) }
            val totals = ActivityEnergy.range(context, before.start.toString(), previousEnd.toString())
            Totals(totals.steps, totals.distanceM, totals.activeKcal * factors.activity).takeIf { totals.steps > 0 }
        }

        val tissue = DailyEnergy.insights(context).model?.states?.associate { it.date to it.tissue }.orEmpty()
        val trend = WeightJournal.points(context)
        fun weightOn(date: LocalDate): Double? =
            tissue.filterKeys { !it.isAfter(date) }.maxByOrNull { it.key }?.value
                ?: trend.lastOrNull { !it.date.isAfter(date) }?.trend
        val weightStart = weightOn(period.start.minusDays(1)) ?: trend.firstOrNull { period.contains(it.date) }?.trend
        val weightEnd = weightOn(end)?.takeIf { trend.any { point -> period.contains(point.date) } }

        val sessions = database.sleepsSince(period.start.toString()).filter { !LocalDate.parse(it.wakeDate).isAfter(end) }
        val nights = sessions.filterNot { it.nap }.map { Night(LocalDate.parse(it.wakeDate), it.startAt, it.endAt) }

        val minutes = database.getMinutes(period.start.toString(), end.toString())
        val hours = IntArray(24)
        val calendar = Calendar.getInstance()
        minutes.forEach {
            calendar.timeInMillis = it.minuteStart
            hours[calendar.get(Calendar.HOUR_OF_DAY)] += it.steps
        }

        val earned = runCatching { AchievementData.evaluate(context) }.getOrDefault(emptyList())
            .filter { result -> result.progress.earnedOn?.let { !it.isBefore(period.start) && !it.isAfter(end) } == true }
            .map { it.def.title }

        return Reports.build(
            ReportInputs(
                period = period,
                days = days,
                previous = previous,
                weightStart = weightStart,
                weightEnd = weightEnd,
                nightMinutes = nights.map { it.minutes },
                bedtimeSpread = SleepInsights.bedtimeSpreadMinutes(nights),
                napMinutes = sessions.filter { it.nap }.map { it.durationMinutes },
                topFoods = foods.values.flatten().groupingBy { it.name }.eachCount().entries
                    .sortedByDescending { it.value }.take(TOP_FOODS).map { it.key },
                badges = earned,
                proteinTarget = AppPreferences.weight * AppPreferences.proteinPerKg,
                hourSteps = hours.toList(),
                briskMinutes = minutes.count { it.steps >= BRISK_CADENCE },
                longestWalkMin = minutes.groupBy { it.date }.values.maxOfOrNull { day ->
                    AchievementData.longestWalk(day.map { it.minuteStart to it.steps })
                } ?: 0,
            )
        )
    }
}

object ReportScheduler {
    const val EXTRA_REPORT = "com.prateek.taro.REPORT"
    private const val WORK_NAME = "period_reports"
    private const val CHANNEL_ID = "com.prateek.taro.REPORTS"
    private const val HOUR = 9

    fun schedule(context: Context, replace: Boolean = false) {
        val workManager = WorkManager.getInstance(context)
        if (!AppPreferences.reportNotifications) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val request = PeriodicWorkRequestBuilder<ReportWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(next.timeInMillis - now.timeInMillis, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            if (replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun notify(context: Context, period: Period, report: PeriodReport) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.report_channel), NotificationManager.IMPORTANCE_DEFAULT)
        )
        val id = period.key.hashCode()
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_REPORT, period.key)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val parts = listOfNotNull(
            context.getString(R.string.report_notification_steps, Util.formatSteps(report.steps)),
            "%.1f %s".format(Locale.getDefault(), Util.metersToDistance(report.distanceM), Util.distanceUnit()),
            report.actualKg?.takeIf { abs(it) >= 0.05 }?.let { Util.formatWeightChange(it) },
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(if (period.kind == ReportKind.WEEK) R.string.report_notification_week else R.string.report_notification_month))
            .setContentText(parts.joinToString(" · "))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) manager.notify(id, notification)
    }
}

class ReportWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val today = Util.logicalToday()
        val due = buildList {
            if (today.dayOfWeek == ReportData.firstDayOfWeek()) add(ReportData.latestComplete(ReportKind.WEEK, today))
            if (today.dayOfMonth == 1) add(ReportData.latestComplete(ReportKind.MONTH, today))
        }
        due.forEach { period ->
            if (ReportData.stepsIn(applicationContext, period) <= 0) return@forEach
            ReportData.load(applicationContext, period)?.let { ReportScheduler.notify(applicationContext, period, it) }
        }
        return Result.success()
    }
}

