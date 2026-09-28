package com.nvllz.stepsy.energy

import com.nvllz.stepsy.util.MinuteRecorder
import kotlin.math.exp
import kotlin.math.min

class AscentTracker(
    private val smoothingSeconds: Double = 15.0,
    private val thresholdM: Double = 1.0,
) {
    private var smoothed: Double? = null
    private var lastTimeMs = 0L
    private var reference = 0.0
    private val pending = sortedMapOf<Long, Double>()

    fun addAltitude(timeMs: Long, altitudeM: Double) {
        val previous = smoothed
        if (previous == null || timeMs <= lastTimeMs) {
            smoothed = altitudeM
            reference = altitudeM
            lastTimeMs = timeMs
            return
        }
        val alpha = 1 - exp(-(timeMs - lastTimeMs) / 1000.0 / smoothingSeconds)
        val current = previous + alpha * (altitudeM - previous)
        smoothed = current
        lastTimeMs = timeMs

        when {
            current - reference >= thresholdM -> {
                val minute = MinuteRecorder.minuteOf(timeMs)
                pending[minute] = (pending[minute] ?: 0.0) + (current - reference)
                reference = current
            }
            current < reference -> reference = current
        }
    }

    fun drain(): Map<Long, Double> = pending.toMap().also { pending.clear() }

    companion object {
        const val MAX_RISE_PER_STEP_M = 0.2

        fun credited(ascentM: Double, steps: Int): Double = if (steps <= 0) 0.0 else min(ascentM, steps * MAX_RISE_PER_STEP_M)
    }
}
