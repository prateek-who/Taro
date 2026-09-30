package com.prateek.taro.energy

data class TimedAmount(val at: Long, val amount: Double)

data class TimedSpan(val start: Long, val end: Long)

data class HourSlot(val start: Long, val steps: Int, val burn: Double, val eaten: Double, val sleepMinutes: Int)

data class DayTimeline(val start: Long, val end: Long, val hours: List<HourSlot>, val sleep: List<TimedSpan>)

object DayTimelines {
    private const val HOUR = 3_600_000L
    private const val MINUTE = 60_000L
    private const val DEFAULT_ACTIVITY_MINUTES = 30

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
    ): DayTimeline {
        val count = ((end - start + HOUR - 1) / HOUR).toInt()
        val restingPerMs = (restingPerDay ?: 0.0) / (end - start)
        val hours = (0 until count).map { index ->
            val from = start + index * HOUR
            val to = minOf(from + HOUR, end)
            val elapsed = overlap(from, to, start, now)
            val asleep = sleep.sumOf { overlap(from, to, it.start, it.end) }
            val inHour = minutes.filter { it.first in from until to }
            val active = inHour.sumOf { EnergyModel.minuteKcal(body, it.second) }
            val logged = activities.sumOf { (span, kcal) ->
                val length = (span.end - span.start).coerceAtLeast(MINUTE)
                kcal * overlap(from, to, span.start, span.start + length) / length
            }
            val asleepSoFar = sleep.sumOf { overlap(from, minOf(to, now), it.start, it.end) }
            val resting = restingPerMs * (elapsed - asleepSoFar * Metabolism.SLEEP_REDUCTION)
            HourSlot(
                start = from,
                steps = inHour.sumOf { it.second.steps },
                burn = if (restingPerDay == null) 0.0 else Metabolism.withDigestion(resting + active + logged),
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
