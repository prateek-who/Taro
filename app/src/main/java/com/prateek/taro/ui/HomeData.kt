package com.prateek.taro.ui

import android.content.Context
import com.prateek.taro.data.SleepSession
import com.prateek.taro.energy.DayBurn
import com.prateek.taro.energy.DayTimelines
import com.prateek.taro.energy.TimedSpan
import com.prateek.taro.sleep.SleepDates
import com.prateek.taro.sleep.SleepRepository
import com.prateek.taro.util.Database
import com.prateek.taro.util.Util
import com.prateek.taro.util.Walks
import java.time.LocalDate

data class DayGlance(
    val start: Long,
    val end: Long,
    val sleep: List<TimedSpan>,
    val walks: List<TimedSpan>,
    val workouts: List<TimedSpan>,
    val meals: List<Long>,
    val walkMinutes: Int,
)

data class SleepGlance(val last: SleepSession?, val week: List<Double?>, val days: List<LocalDate>)

data class WeekGlance(val days: List<Pair<Double, Double?>>, val net: Double?, val loggedDays: Int)

private const val MEAL_MERGE_MS = 30 * 60_000L
const val WALK_MERGE_MS = 15 * 60_000L
private const val USUAL_NIGHTS = 14L
private const val LOGGED_SHARE = 0.5

fun dayGlance(context: Context, date: LocalDate): DayGlance {
    val database = Database.getInstance(context)
    val day = date.toString()
    val start = Util.dayStartMillis(date)
    val end = Util.dayStartMillis(date.plusDays(1))
    val meals = database.foodOn(day).map { it.loggedAt.coerceIn(start, end - 1) }.sorted()
        .fold(listOf<Long>()) { kept, at -> if (kept.isNotEmpty() && at - kept.last() < MEAL_MERGE_MS) kept else kept + at }
    val walks = Walks.all(database.getMinutes(day, day).map { it.minuteStart to it.steps })
    return DayGlance(
        start = start,
        end = end,
        sleep = database.sleepsSince(date.minusDays(1).toString()).filter { it.endAt > start && it.startAt < end }.map { TimedSpan(it.startAt, it.endAt) },
        walks = Walks.merged(walks, WALK_MERGE_MS).map { TimedSpan(it.start, it.end) },
        workouts = database.activitiesOn(day).map { DayTimelines.activitySpan(it.loggedAt, it.durationMinutes, start, end) },
        meals = meals,
        walkMinutes = walks.sumOf { it.minutes },
    )
}

fun sleepGlance(context: Context): SleepGlance {
    val today = SleepDates.today()
    val nights = SleepRepository.sessions(context, USUAL_NIGHTS).associateBy { LocalDate.parse(it.wakeDate) }
    val days = (6L downTo 0L).map { today.minusDays(it) }
    return SleepGlance(last = nights[today], week = days.map { nights[it]?.durationMinutes?.div(60.0) }, days = days)
}

fun weekGlance(context: Context, burns: List<DayBurn>, incomplete: Set<String>, restingPerDay: Double): WeekGlance {
    val today = Util.todayDateString()
    val eaten = Database.getInstance(context).foodBetween(burns.first().date, today).groupBy { it.date }.mapValues { (_, logs) -> logs.sumOf { it.kcal } }
    val logged = burns.filter { it.date != today && it.date !in incomplete && (eaten[it.date] ?: 0.0) >= restingPerDay * LOGGED_SHARE }
    return WeekGlance(
        days = burns.map { it.total to eaten[it.date] },
        net = logged.takeIf { it.isNotEmpty() }?.sumOf { (eaten[it.date] ?: 0.0) - it.total },
        loggedDays = logged.size,
    )
}
