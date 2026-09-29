package com.prateek.taro.energy

import android.content.Context
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Database
import com.prateek.taro.util.Util
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
            pastDayBurn(
                date = date,
                restingPerDay = resting - Metabolism.sleepSaving(resting, sleepMinutes(database, date)),
                tracked = database.getEntries(date, date).isNotEmpty(),
                active = ActivityEnergy.day(context, date).activeKcal,
                logged = database.activityKcal(date, date),
            )
        }
        return EnergyHistory(
            restingPerDay = resting,
            averageActive = if (recordedDays > 0) activeTotal / recordedDays else 0.0,
            pastDays = pastDays,
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
