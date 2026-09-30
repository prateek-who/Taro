package com.prateek.taro.sleep

data class ScreenState(val at: Long, val on: Boolean)

data class SleepWindow(val start: Long, val end: Long) {
    val durationMs: Long get() = end - start
}

object SleepEstimator {
    const val MIN_SLEEP_MS = 3 * 60 * 60 * 1000L

    const val MAX_SLEEP_MS = 14 * 60 * 60 * 1000L

    fun isPlausibleNight(start: Long, end: Long) = end - start in MIN_SLEEP_MS..MAX_SLEEP_MS
    const val MAX_WAKE_GAP_MS = 10 * 60 * 1000L
    const val MAX_AWAKE_SHARE = 0.15
    const val ACTIVE_STEPS_PER_MINUTE = 15
    private const val MINUTE_MS = 60_000L

    fun estimate(
        windowStart: Long,
        windowEnd: Long,
        screen: List<ScreenState>,
        stepsPerMinute: Map<Long, Int>,
    ): SleepWindow? {
        val active = mutableListOf<SleepWindow>()
        val known = screen.lastOrNull { it.at <= windowStart }
        val start = if (known != null) windowStart else screen.minOfOrNull { it.at } ?: return null

        var screenOn = known?.on ?: false
        var onSince = start
        screen.filter { it.at in (start + 1)..windowEnd }.sortedBy { it.at }.forEach { event ->
            if (screenOn && !event.on) active += SleepWindow(onSince, event.at)
            if (!screenOn && event.on) onSince = event.at
            screenOn = event.on
        }
        if (screenOn) active += SleepWindow(onSince, windowEnd)

        stepsPerMinute
            .filter { (minute, steps) -> steps >= ACTIVE_STEPS_PER_MINUTE && minute in windowStart until windowEnd }
            .keys
            .forEach { active += SleepWindow(it, it + MINUTE_MS) }

        val idle = mutableListOf<SleepWindow>()
        var cursor = start
        active.sortedBy { it.start }.forEach { busy ->
            if (busy.start > cursor) idle += SleepWindow(cursor, busy.start)
            cursor = maxOf(cursor, busy.end)
        }
        if (cursor < windowEnd) idle += SleepWindow(cursor, windowEnd)

        val merged = mutableListOf<Pair<SleepWindow, Long>>()
        idle.forEach { block ->
            val last = merged.lastOrNull()
            if (last != null && block.start - last.first.end < MAX_WAKE_GAP_MS) {
                val awake = last.second + (block.start - last.first.end)
                merged[merged.lastIndex] = SleepWindow(last.first.start, block.end) to awake
            } else {
                merged += block to 0L
            }
        }

        return merged
            .filter { (window, awake) -> isPlausibleNight(window.start, window.end) && awake <= window.durationMs * MAX_AWAKE_SHARE }
            .maxByOrNull { it.first.durationMs }
            ?.first
    }
}
