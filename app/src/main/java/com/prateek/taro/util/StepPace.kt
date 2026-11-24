package com.prateek.taro.util

import java.time.DayOfWeek
import java.time.LocalDate

data class StepPace(
    val today: List<Pair<Double, Double>>,
    val usual: DoubleArray?,
    val now: Double?,
    val total: Int,
    val usualByNow: Int?,
    val projected: Int?,
    val course: List<Pair<Double, Double>>,
    val goalHour: Double?,
    val weekend: Boolean,
) {
    val usualTotal: Int? get() = usual?.last()?.toInt()
}

object StepPaces {
    const val DAYS = 28L
    const val HOURS = 24
    private const val MIN_DAYS = 5
    private const val HOUR_MS = 3_600_000L
    private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    fun hourly(minutes: List<Pair<Long, Int>>, dayStart: Long): DoubleArray {
        val hours = DoubleArray(HOURS)
        minutes.forEach { (at, steps) -> hours[((at - dayStart) / HOUR_MS).toInt().coerceIn(0, HOURS - 1)] += steps.toDouble() }
        return hours
    }

    private fun cumulative(hours: DoubleArray): DoubleArray {
        val out = DoubleArray(HOURS + 1)
        hours.forEachIndexed { index, value -> out[index + 1] = out[index] + value }
        return out
    }

    fun at(curve: DoubleArray, hour: Double): Double {
        val clamped = hour.coerceIn(0.0, HOURS.toDouble())
        val index = clamped.toInt().coerceAtMost(HOURS - 1)
        return curve[index] + (curve[index + 1] - curve[index]) * (clamped - index)
    }

    fun reachedAt(points: List<Pair<Double, Double>>, value: Double): Double? = crossing(points, value)

    private fun crossing(points: List<Pair<Double, Double>>, goal: Double): Double? =
        points.zipWithNext().firstOrNull { (a, b) -> a.second < goal && b.second >= goal }?.let { (a, b) ->
            a.first + (b.first - a.first) * (goal - a.second) / (b.second - a.second)
        }

    fun build(date: LocalDate, hours: DoubleArray, total: Int, past: Map<LocalDate, DoubleArray>, now: Double?, goal: Int): StepPace {
        val weekend = date.dayOfWeek in WEEKEND
        val tracked = past.filter { (day, values) -> day.isBefore(date) && values.sum() > 0 }
        val matching = tracked.filterKeys { (it.dayOfWeek in WEEKEND) == weekend }
        val pool = (if (matching.size >= MIN_DAYS) matching else tracked).values.takeIf { it.size >= MIN_DAYS }
        val usual = pool?.let { days -> cumulative(DoubleArray(HOURS) { hour -> days.sumOf { it[hour] } / days.size }) }

        val curve = cumulative(hours)
        val end = now ?: HOURS.toDouble()
        val scale = if (curve.last() > 0 && total > curve.last()) total / curve.last() else 1.0
        val today = (0..end.toInt().coerceAtMost(HOURS)).map { it.toDouble() to curve[it] * scale } +
            listOfNotNull((end to maxOf(total.toDouble(), curve.last())).takeIf { end % 1 > 0 })
        val usualByNow = usual?.let { at(it, end) }
        val course = if (now != null && usual != null && usualByNow != null) {
            val from = today.last().second
            listOf(now to from) + (now.toInt() + 1..HOURS).map { it.toDouble() to from + usual[it] - usualByNow }
        } else {
            emptyList()
        }
        val goalHour = goal.takeIf { it > 0 }?.let { crossing(today + course.drop(1), it.toDouble()) }
        return StepPace(
            today = today,
            usual = usual,
            now = now,
            total = total,
            usualByNow = usualByNow?.toInt(),
            projected = course.lastOrNull()?.second?.toInt(),
            course = course,
            goalHour = goalHour,
            weekend = weekend,
        )
    }
}
