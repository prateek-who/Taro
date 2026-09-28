package com.nvllz.stepsy.energy

import android.content.Context
import com.nvllz.stepsy.util.AppPreferences
import com.nvllz.stepsy.util.Database
import com.nvllz.stepsy.util.Util
import java.util.Calendar

data class DayBurn(val date: String, val resting: Double, val active: Double, val logged: Double = 0.0) {
    val total: Double get() = resting + active + logged
}

data class EnergyHistory(val restingPerDay: Double, val averageActive: Double, val pastDays: List<DayBurn>) {
    val dailyNeed: Double get() = Metabolism.dailyNeed(restingPerDay, averageActive)
}

object DailyEnergy {
    private const val AVERAGE_DAYS = 14
    private const val CHART_DAYS = 7

    fun restingPerDay(): Double? {
        val age = AppPreferences.age ?: return null
        val sex = AppPreferences.sex ?: return null
        return Metabolism.restingKcalPerDay(AppPreferences.weight, AppPreferences.height, age, sex)
    }

    fun fractionOfDay(nowMs: Long): Double {
        val calendar = Calendar.getInstance().apply { timeInMillis = nowMs }
        val minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        return minutes / 1440.0
    }

    private fun daysAgo(days: Int): String = Util.calendarToDateString(
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) }
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
            DayBurn(date, resting, ActivityEnergy.day(context, date).activeKcal, database.activityKcal(date, date))
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
            resting = Metabolism.restingSoFar(restingPerDay, fractionOfDay(nowMs)),
            active = ActivityEnergy.day(context, date, liveTodaySteps = liveSteps).activeKcal,
            logged = Database.getInstance(context).activityKcal(date, date),
        )
    }
}
