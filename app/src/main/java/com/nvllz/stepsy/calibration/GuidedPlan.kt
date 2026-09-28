package com.nvllz.stepsy.calibration

import androidx.annotation.StringRes
import com.nvllz.stepsy.R

enum class GuidedPhase(
    @StringRes val title: Int,
    @StringRes val hint: Int,
    val seconds: Int,
) {
    USUAL(R.string.calibration_phase_usual_title, R.string.calibration_phase_usual_hint, 180),
    RELAXED(R.string.calibration_phase_relaxed_title, R.string.calibration_phase_relaxed_hint, 120),
    BRISK(R.string.calibration_phase_brisk_title, R.string.calibration_phase_brisk_hint, 120),
}

data class GuidedProgress(val phase: GuidedPhase?, val index: Int, val secondsLeft: Int)

object GuidedPlan {
    val phases = GuidedPhase.entries

    fun progress(elapsedS: Long, skipsAtS: List<Long>): GuidedProgress {
        val skips = skipsAtS.sorted()
        var nextSkip = 0
        var start = 0L
        phases.forEachIndexed { index, phase ->
            while (nextSkip < skips.size && skips[nextSkip] < start) nextSkip++
            val natural = start + phase.seconds
            val skip = skips.getOrNull(nextSkip)?.takeIf { it < natural }
            val end = skip ?: natural
            if (skip != null) nextSkip++
            if (elapsedS < end) return GuidedProgress(phase, index, (end - elapsedS).toInt())
            start = end
        }
        return GuidedProgress(null, phases.size, 0)
    }
}
