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

data class EnergyHistory(val restingPerDay: Double, val averageActive: Double, val pastDays: List<DayBurn>, val factors: EnergyFactors) {
    val dailyNeed: Double get() = Metabolism.dailyNeed(restingPerDay, averageActive)
}

object DailyEnergy {
    private const val AVERAGE_DAYS = 14
    private const val CHART_DAYS = 7
    private const val HISTORY_DAYS = 365L
    private const val HOUR = 3_600_000L

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

    fun history(context: Context, factors: EnergyFactors): EnergyHistory? {
        val resting = restingPerDay() ?: return null
        val from = daysAgo(AVERAGE_DAYS)
        val yesterday = daysAgo(1)
        val database = Database.getInstance(context)
        val recordedDays = database.getEntries(from, yesterday).size
        val activeTotal = ActivityEnergy.range(context, from, yesterday).activeKcal
        val workoutTotal = database.activityKcal(from, yesterday)
        val pastDays = (CHART_DAYS - 1 downTo 1).map { offset ->
            val date = daysAgo(offset)
            pastDay(context, database, resting, date, database.getEntries(date, date).isNotEmpty(), factors)
        }
        return EnergyHistory(
            restingPerDay = resting * factors.resting,
            averageActive = if (recordedDays > 0) (factors.activity * activeTotal + factors.workout * workoutTotal) / recordedDays else 0.0,
            pastDays = pastDays,
            factors = factors,
        )
    }

    private fun pastDay(context: Context, database: Database, resting: Double, date: String, tracked: Boolean, factors: EnergyFactors) = pastDayBurn(
        date = date,
        restingPerDay = factors.resting * (resting - Metabolism.sleepSaving(resting, sleepMinutes(database, date))),
        tracked = tracked,
        active = factors.activity * ActivityEnergy.day(context, date).activeKcal,
        logged = factors.workout * database.activityKcal(date, date),
    )

    fun day(context: Context, history: EnergyHistory, date: String): DayBurn {
        val database = Database.getInstance(context)
        return pastDay(context, database, history.restingPerDay / history.factors.resting, date, database.getEntries(date, date).isNotEmpty(), history.factors)
    }

    fun timeline(context: Context, date: LocalDate, factors: EnergyFactors, now: Long = System.currentTimeMillis()): DayTimeline {
        val database = Database.getInstance(context)
        val day = date.toString()
        val start = Util.dayStartMillis(date)
        val end = Util.dayStartMillis(date.plusDays(1))
        return DayTimelines.build(
            start = start,
            end = end,
            now = now,
            body = ActivityEnergy.body(),
            restingPerDay = restingPerDay()?.times(factors.resting),
            minutes = database.getMinutes(day, day).map { it.minuteStart to MinuteSample(it.steps, it.ascentMeters.toDouble()) },
            food = database.foodOn(day).map { TimedAmount(it.loggedAt.coerceIn(start, end - 1), it.kcal) },
            activities = database.activitiesOn(day).map { DayTimelines.activitySpan(it.loggedAt, it.durationMinutes, start, end) to it.kcal },
            sleep = database.sleepsSince(date.minusDays(1).toString()).map { TimedSpan(it.startAt, it.endAt) },
            activityFactor = factors.activity,
            workoutFactor = factors.workout,
        )
    }

    fun today(context: Context, history: EnergyHistory, liveSteps: Int, nowMs: Long): DayBurn {
        val date = Util.todayDateString()
        val restingPerDay = history.restingPerDay
        return DayBurn(
            date = date,
            resting = (Metabolism.restingSoFar(restingPerDay, fractionOfDay(nowMs)) -
                Metabolism.sleepSaving(restingPerDay, sleepMinutes(Database.getInstance(context), date))).coerceAtLeast(0.0),
            active = history.factors.activity * ActivityEnergy.day(context, date, liveTodaySteps = liveSteps).activeKcal,
            logged = history.factors.workout * Database.getInstance(context).activityKcal(date, date),
        )
    }

    fun insights(context: Context): BodyInsights {
        val age = AppPreferences.age
        val sex = AppPreferences.sex
        val height = AppPreferences.height
        val database = Database.getInstance(context)
        val today = Util.logicalToday()
        val profileStart = today.minusDays(ActivityProfile.DAYS)
        val weights = database.weightsSince(today.minusDays(HISTORY_DAYS).toString())
        val start = listOfNotNull(weights.minOfOrNull { it.date }?.let(LocalDate::parse), profileStart).min()
        val from = start.toString()
        val to = today.toString()

        val body = ActivityEnergy.body()
        val steps = database.getEntries(from, to).associate { it.date to it.steps }
        val minutes = database.getMinutes(from, to).groupBy { it.date }
        val food = database.foodBetween(from, to).groupBy { it.date }
        val logged = database.activitiesBetween(from, to).groupBy({ it.date }) { it.kcal }
        val sleep = database.sleepsSince(from).associate { it.wakeDate to it.durationMinutes }
        val weightByDate = weights.associate { it.date to it.kg }
        val incomplete = AppPreferences.incompleteFoodDays

        val hourly = steps.keys.map(LocalDate::parse).filter { !it.isBefore(profileStart) && it.isBefore(today) }.map { date ->
            val dayStart = Util.dayStartMillis(date)
            val size = ((Util.dayStartMillis(date.plusDays(1)) - dayStart + HOUR - 1) / HOUR).toInt()
            val hours = DoubleArray(size)
            minutes[date.toString()].orEmpty().forEach { minute ->
                val index = ((minute.minuteStart - dayStart) / HOUR).toInt()
                if (index in hours.indices) hours[index] += EnergyModel.minuteKcal(body, MinuteSample(minute.steps, minute.ascentMeters.toDouble()))
            }
            HourlyActive(date, hours)
        }

        val model = if (age == null || sex == null) null else EnergyFilter.run(
            days = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }.map { date ->
                val key = date.toString()
                val logs = food[key].orEmpty()
                FilterDay(
                    date = date,
                    intake = logs.takeIf { it.isNotEmpty() && key !in incomplete }?.sumOf { it.kcal },
                    carbs = logs.takeIf { list -> list.isNotEmpty() && list.all { it.carbs != null } }?.sumOf { it.carbs ?: 0.0 },
                    sleepMinutes = sleep[key] ?: 0L,
                    active = steps[key]?.let { daySteps ->
                        EnergyModel.dayKcal(body, minutes[key].orEmpty().map { MinuteSample(it.steps, it.ascentMeters.toDouble()) }, daySteps)
                    },
                    logged = logged[key]?.sum() ?: 0.0,
                    weight = weightByDate[key],
                )
            }.toList(),
            restingAt = { kg -> Metabolism.restingKcalPerDay(kg, height, age, sex) },
        )
        return BodyInsights(model, hourly)
    }
}

data class BodyInsights(val model: BodyModel?, val hourly: List<HourlyActive>)
