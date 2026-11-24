package com.prateek.taro.ui

import java.util.Locale
import java.time.temporal.WeekFields
import android.content.Context
import androidx.annotation.StringRes
import com.prateek.taro.R
import com.prateek.taro.achievements.AchievementData
import com.prateek.taro.energy.DailyEnergy
import com.prateek.taro.energy.TrendPoint
import com.prateek.taro.energy.WeightJournal
import com.prateek.taro.report.ReportData
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Database
import com.prateek.taro.util.Util
import java.time.LocalDate

enum class TrendRange(val days: Int, @StringRes val label: Int, @StringRes val span: Int) {
    MONTH(28, R.string.trends_range_month, R.string.trends_span_month),
    QUARTER(91, R.string.trends_range_quarter, R.string.trends_span_quarter),
    YEAR(365, R.string.trends_range_year, R.string.trends_span_year),
}

data class Bests(
    val steps: Int?,
    val goalDays: Int,
    val nightMinutes: Long?,
    val walkMinutes: Int,
    val climbM: Double,
    val workouts: Int,
    val score: Int?,
    val weekScore: Int?,
)

data class TrendSummary(
    val range: TrendRange,
    val steps: Int?,
    val stepsBefore: Int?,
    val stepsSeries: List<Double?>,
    val goal: Int,
    val sleep: Long?,
    val sleepBefore: Long?,
    val sleepSeries: List<Double?>,
    val balance: Double?,
    val loggedDays: Int,
    val days: Int,
    val balanceBars: List<Pair<Double, Double?>>,
    val protein: Double?,
    val proteinTarget: Double,
    val proteinDays: Int,
    val movingKcal: Double?,
    val movingKm: Double?,
    val movingSeries: List<Double?>,
    val weightChange: Double?,
    val weights: List<TrendPoint>,
    val bests: Bests,
)

object TrendsData {
    private const val SERIES_POINTS = 28
    private const val LINE_POINTS = 14
    private const val BALANCE_BARS = 4
    private const val MIN_LOGGED = 3
    private const val MIN_NIGHTS = 3

    private fun <T> buckets(values: List<T>, count: Int): List<List<T>> {
        if (values.size <= count) return values.map { listOf(it) }
        return (0 until count).map { index -> values.subList(index * values.size / count, (index + 1) * values.size / count) }
    }

    private fun series(values: List<Double?>, points: Int = SERIES_POINTS): List<Double?> =
        buckets(values, points).map { bucket -> bucket.filterNotNull().takeIf { it.isNotEmpty() }?.average() }

    fun load(context: Context, range: TrendRange, today: LocalDate = Util.logicalToday()): TrendSummary {
        val database = Database.getInstance(context)
        val end = today.minusDays(1)
        val start = today.minusDays(range.days.toLong())
        val before = start.minusDays(range.days.toLong())
        val days = ReportData.days(context, start, end, today)
        val tracked = days.filter { it.steps > 0 }
        val logged = days.filter { it.fullyLogged && it.eaten != null && it.burned > 0 }
        val proteinTarget = AppPreferences.weight * AppPreferences.proteinPerKg

        val stepsBefore = database.getEntries(before.toString(), start.minusDays(1).toString()).map { it.steps }.filter { it > 0 }
        val sessions = database.sleepsSince(before.toString()).filterNot { it.nap }
        val nights = sessions.filter { !LocalDate.parse(it.wakeDate).isBefore(start) && !LocalDate.parse(it.wakeDate).isAfter(end) }
        val nightsBefore = sessions.filter { LocalDate.parse(it.wakeDate).isBefore(start) }
        val byWake = nights.associate { it.wakeDate to it.durationMinutes }

        val minutes = database.getMinutes(start.toString(), end.toString()).groupBy { it.date }
        val scores = ReportData.scores(context, start, end, minutes).mapValues { it.value.score }
        val weeks = WeekFields.of(Locale.getDefault())
        val tissue = DailyEnergy.insights(context).model?.states?.associate { it.date to it.tissue }.orEmpty()
        val points = WeightJournal.points(context)
        fun weightOn(date: LocalDate): Double? =
            tissue.filterKeys { !it.isAfter(date) }.maxByOrNull { it.key }?.value ?: points.lastOrNull { !it.date.isAfter(date) }?.trend
        val weightChange = if (points.any { !it.date.isBefore(start) }) {
            val from = weightOn(start.minusDays(1)) ?: points.firstOrNull { !it.date.isBefore(start) }?.trend
            val to = weightOn(end)
            if (from != null && to != null) to - from else null
        } else {
            null
        }

        return TrendSummary(
            range = range,
            steps = tracked.takeIf { it.isNotEmpty() }?.map { it.steps }?.average()?.toInt(),
            stepsBefore = stepsBefore.takeIf { it.isNotEmpty() }?.average()?.toInt(),
            stepsSeries = series(days.map { day -> day.steps.takeIf { it > 0 }?.toDouble() }),
            goal = AppPreferences.goalHistory.on(today),
            sleep = nights.takeIf { it.size >= MIN_NIGHTS }?.map { it.durationMinutes }?.average()?.toLong(),
            sleepBefore = nightsBefore.takeIf { it.size >= MIN_NIGHTS }?.map { it.durationMinutes }?.average()?.toLong(),
            sleepSeries = series(days.map { day -> byWake[day.date.toString()]?.div(60.0) }, LINE_POINTS),
            balance = logged.takeIf { it.size >= MIN_LOGGED }?.map { (it.eaten ?: 0.0) - it.burned }?.average(),
            loggedDays = logged.size,
            days = days.size,
            balanceBars = buckets(days, BALANCE_BARS).map { chunk ->
                val full = chunk.filter { it.fullyLogged && it.eaten != null && it.burned > 0 }
                val burned = (full.ifEmpty { chunk.filter { it.burned > 0 } }).map { it.burned }.takeIf { it.isNotEmpty() }?.average() ?: 0.0
                burned to full.takeIf { it.isNotEmpty() }?.map { it.eaten ?: 0.0 }?.average()
            },
            protein = days.filter { it.eaten != null }.takeIf { it.size >= MIN_LOGGED }?.map { it.protein }?.average(),
            proteinTarget = proteinTarget,
            proteinDays = days.count { it.eaten != null && proteinTarget > 0 && it.protein >= proteinTarget },
            movingKcal = tracked.takeIf { it.isNotEmpty() }?.map { it.activeKcal }?.average(),
            movingKm = tracked.takeIf { it.isNotEmpty() }?.map { it.distanceM / 1000 }?.average(),
            movingSeries = series(days.map { day -> day.activeKcal.takeIf { day.steps > 0 } }, LINE_POINTS),
            weightChange = weightChange,
            weights = points,
            bests = Bests(
                steps = tracked.maxOfOrNull { it.steps },
                goalDays = days.count { it.goal > 0 && it.steps >= it.goal },
                nightMinutes = nights.maxOfOrNull { it.durationMinutes },
                walkMinutes = minutes.values.maxOfOrNull { day -> AchievementData.longestWalk(day.map { it.minuteStart to it.steps }) } ?: 0,
                climbM = minutes.values.maxOfOrNull { day -> day.sumOf { it.ascentMeters.toDouble() } } ?: 0.0,
                workouts = database.activitiesBetween(start.toString(), end.toString()).size,
                score = scores.values.maxOrNull(),
                weekScore = scores.entries.groupBy { it.key.with(weeks.dayOfWeek(), 1) }.values.maxOfOrNull { week -> week.sumOf { it.value } },
            ),
        )
    }
}
