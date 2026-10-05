package com.prateek.taro.energy

data class TimedAmount(val at: Long, val amount: Double)

data class TimedSpan(val start: Long, val end: Long)

data class HourSlot(val start: Long, val steps: Int, val burn: Double, val eaten: Double, val sleepMinutes: Int)

data class DayTimeline(val start: Long, val end: Long, val hours: List<HourSlot>, val sleep: List<TimedSpan>)

object DayTimelines {
    private const val HOUR = 3_600_000L
    private const val MINUTE = 60_000L
    private const val DEFAULT_ACTIVITY_MINUTES = 30
    private const val DIGESTION_HOURS = 4

    private fun overlap(a: Long, b: Long, c: Long, d: Long) = (minOf(b, d) - maxOf(a, c)).coerceAtLeast(0)

    fun build(
        start: Long,
        end: Long,
        now: Long,
        body: Body,
        restingPerDay: Double?,
        minutes: List<Pair<Long, MinuteSample>>,
        food: List<TimedAmount>,
        activities: List<Pair<TimedSpan, Double>>,
        sleep: List<TimedSpan>,
        activityFactor: Double = 1.0,
        workoutFactor: Double = 1.0,
        digestion: List<TimedAmount>? = null,
        digestionShare: Double = Metabolism.DIGESTION_SHARE,
    ): DayTimeline {
        val count = ((end - start + HOUR - 1) / HOUR).toInt()
        val lastOpen = ((minOf(now, end - 1) - start) / HOUR).toInt().coerceIn(0, count - 1)
        val digested = DoubleArray(count)
        digestion?.forEach { meal ->
            val first = ((meal.at.coerceIn(start, end - 1) - start) / HOUR).toInt()
            val last = maxOf(first, minOf(first + DIGESTION_HOURS - 1, lastOpen))
            for (hour in first..last) digested[hour] += meal.amount / (last - first + 1)
        }
        val restingPerMs = (restingPerDay ?: 0.0) / (end - start)
        val hours = (0 until count).map { index ->
            val from = start + index * HOUR
            val to = minOf(from + HOUR, end)
            val elapsed = overlap(from, to, start, now)
            val asleep = sleep.sumOf { overlap(from, to, it.start, it.end) }
            val inHour = minutes.filter { it.first in from until to }
            val active = activityFactor * inHour.sumOf { EnergyModel.minuteKcal(body, it.second) }
            val logged = activities.sumOf { (span, kcal) ->
                val length = (span.end - span.start).coerceAtLeast(MINUTE)
                workoutFactor * kcal * overlap(from, to, span.start, span.start + length) / length
            }
            val asleepSoFar = sleep.sumOf { overlap(from, minOf(to, now), it.start, it.end) }
            val resting = restingPerMs * (elapsed - asleepSoFar * Metabolism.SLEEP_REDUCTION)
            HourSlot(
                start = from,
                steps = inHour.sumOf { it.second.steps },
                burn = when {
                    restingPerDay == null -> 0.0
                    digestion == null -> Metabolism.withDigestion(resting + active + logged, digestionShare)
                    else -> resting + active + logged + digested[index]
                },
                eaten = food.filter { it.at in from until to }.sumOf { it.amount },
                sleepMinutes = (asleep / MINUTE).toInt(),
            )
        }
        return DayTimeline(start, end, hours, sleep.filter { it.end > start && it.start < end })
    }

    fun activitySpan(loggedAt: Long, durationMinutes: Int?, dayStart: Long, dayEnd: Long): TimedSpan {
        val length = (durationMinutes ?: DEFAULT_ACTIVITY_MINUTES) * MINUTE
        val end = if (loggedAt in dayStart..dayEnd) loggedAt else dayStart + (dayEnd - dayStart) / 2 + length / 2
        return TimedSpan(maxOf(dayStart, end - length), end)
    }
}
