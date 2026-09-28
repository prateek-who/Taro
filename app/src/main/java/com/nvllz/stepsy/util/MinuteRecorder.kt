package com.nvllz.stepsy.util

import kotlin.math.max

class MinuteRecorder(
    private val maxGapMs: Long = 10 * MINUTE_MS,
    private val maxCadence: Int = 250,
) {
    private val pending = sortedMapOf<Long, Int>()

    fun record(steps: Int, fromMs: Long?, toMs: Long) {
        if (steps <= 0) return
        val start = fromMs?.takeIf { toMs - it in 1..maxGapMs }
        if (start == null) {
            if (steps <= maxCadence) add(minuteOf(toMs), steps)
            return
        }
        val gap = toMs - start

        val minutesCovered = max(1.0, gap / MINUTE_MS.toDouble())
        if (steps / minutesCovered > maxCadence) return

        var assigned = 0
        var cursor: Long = start
        while (cursor < toMs) {
            val bucketEnd = minOf(minuteOf(cursor) + MINUTE_MS, toMs)
            val share = ((bucketEnd - start).toDouble() / gap * steps).toInt() - assigned
            if (share > 0) add(minuteOf(cursor), share)
            assigned += share
            cursor = bucketEnd
        }
        if (assigned < steps) add(minuteOf(toMs - 1), steps - assigned)
    }

    fun drain(): Map<Long, Int> = pending.toMap().also { pending.clear() }

    private fun add(minute: Long, steps: Int) {
        pending[minute] = (pending[minute] ?: 0) + steps
    }

    companion object {
        const val MINUTE_MS = 60_000L

        fun minuteOf(timestampMs: Long) = timestampMs - timestampMs % MINUTE_MS
    }
}
