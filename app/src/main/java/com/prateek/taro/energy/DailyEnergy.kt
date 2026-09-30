package com.prateek.taro.energy

import android.content.Context
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Database
import com.prateek.taro.util.Util
import java.time.LocalDate
import java.util.Calendar

data class DayBurn(val date: String, val resting: Double, val active: Double, val logged: Double = 0.0) {
    val digestion: Double get() = Metabolism.withDigestion(resting + active + logged) - (resting + active + logged)
    val total: Double get() = resting + active + logged + digestion
}

data class EnergyHistory(val restingPerDay: Double, val averageActive: Double, val pastDays: List<DayBurn>) {
    val dailyNeed: Double get() = Metabolism.dailyNeed(restingPerDay, averageActive)
}

object DailyEnergy {
    private const val AVERAGE_DAYS = 14
    private const val CHART_DAYS = 7

    fun pastDayBurn(date: String, restingPerDay: Double, tracked: Boolean, active: Double, logged: Double): DayBurn =
        if (tracked || logged > 0) DayBurn(date, restingPerDay, active, logged) else DayBurn(date, 0.0, 0.0, 0.0)

    private fun sleepMinutes(database: Database, date: String): Long = database.sleepOn(date)?.durationMinutes ?: 0L

    fun restingPerDay(): Double? {
        val age = AppPreferences.age ?: return null
        val sex = AppPreferences.sex ?: return null
        return Metabolism.restingKcalPerDay(AppPreferences.weight, AppPreferences.height, age, sex)
    }

    fun fractionOfDay(nowMs: Long): Double = Util.fractionOfToday(nowMs)

    private fun daysAgo(days: Int): String = Util.calendarToDateString(
        Util.todayCalendar().apply { add(Calendar.DAY_OF_YEAR, -days) }
    )

    fun history(context: Context): EnergyHistory? {
        val resting = restingPerDay() ?: return null
        val from = daysAgo(AVERAGE_DAYS)
        val yesterday = daysAgo(1)
        val database = Database.getInstance(context)
        val recordedDays = database.getEntries(from, yesterday).size
        val activeTotal = ActivityEnergy.range(context, from, yesterday).activeKcal + database.activityKcal(from, yesterday)
        val pastDays = (CHART_DAYS - 1 downTo 1).map { offset ->
            val date = daysAgo(offset)
            pastDay(context, database, resting, date, database.getEntries(date, date).isNotEmpty())
        }
        return EnergyHistory(
            restingPerDay = resting,
            averageActive = if (recordedDays > 0) activeTotal / recordedDays else 0.0,
            pastDays = pastDays,
        )
    }

    private fun pastDay(context: Context, database: Database, resting: Double, date: String, tracked: Boolean) = pastDayBurn(
        date = date,
        restingPerDay = resting - Metabolism.sleepSaving(resting, sleepMinutes(database, date)),
        tracked = tracked,
        active = ActivityEnergy.day(context, date).activeKcal,
        logged = database.activityKcal(date, date),
    )

    fun measured(context: Context, restingPerDay: Double, weights: List<TrendPoint>): Measured? {
        val database = Database.getInstance(context)
        val from = daysAgo(MeasuredNeed.WINDOW_DAYS)
        val to = daysAgo(1)
        val days = database.foodBetween(from, to)
            .groupBy { it.date }
            .mapValues { (_, logs) -> logs.sumOf { it.kcal } }
            .filterValues { it >= MeasuredNeed.MIN_INTAKE }
            .map { (date, intake) -> BalanceDay(LocalDate.parse(date), intake, pastDay(context, database, restingPerDay, date, true).total) }
        return MeasuredNeed.estimate(days, weights, LocalDate.parse(from), LocalDate.parse(to))
    }

    fun timeline(context: Context, date: LocalDate, now: Long = System.currentTimeMillis()): DayTimeline {
        val database = Database.getInstance(context)
        val day = date.toString()
        val start = Util.dayStartMillis(date)
        val end = Util.dayStartMillis(date.plusDays(1))
        return DayTimelines.build(
            start = start,
            end = end,
            now = now,
            body = ActivityEnergy.body(),
            restingPerDay = restingPerDay(),
            minutes = database.getMinutes(day, day).map { it.minuteStart to MinuteSample(it.steps, it.ascentMeters.toDouble()) },
            food = database.foodOn(day).map { TimedAmount(it.loggedAt.coerceIn(start, end - 1), it.kcal) },
            activities = database.activitiesOn(day).map { DayTimelines.activitySpan(it.loggedAt, it.durationMinutes, start, end) to it.kcal },
            sleep = database.sleepsSince(date.minusDays(1).toString()).map { TimedSpan(it.startAt, it.endAt) },
        )
    }

    fun today(context: Context, restingPerDay: Double, liveSteps: Int, nowMs: Long): DayBurn {
        val date = Util.todayDateString()
        return DayBurn(
            date = date,
            resting = (Metabolism.restingSoFar(restingPerDay, fractionOfDay(nowMs)) -
                Metabolism.sleepSaving(restingPerDay, sleepMinutes(Database.getInstance(context), date))).coerceAtLeast(0.0),
            active = ActivityEnergy.day(context, date, liveTodaySteps = liveSteps).activeKcal,
            logged = Database.getInstance(context).activityKcal(date, date),
        )
    }
}
