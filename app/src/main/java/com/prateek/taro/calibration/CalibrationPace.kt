package com.prateek.taro.calibration

import androidx.annotation.StringRes
import com.prateek.taro.R

enum class CalibrationPace(
    val key: String,
    @StringRes val title: Int,
    @StringRes val hint: Int,
    val samples: Int,
) {
    SLOW("slow", R.string.calibration_pace_slow, R.string.calibration_pace_slow_hint, 5),
    USUAL("usual", R.string.calibration_pace_usual, R.string.calibration_pace_usual_hint, 5),
    BRISK("brisk", R.string.calibration_pace_brisk, R.string.calibration_pace_brisk_hint, 5),
    JOG("jog", R.string.calibration_pace_jog, R.string.calibration_pace_jog_hint, 4),
    ;

    companion object {
        fun fromKey(key: String?): CalibrationPace? = entries.firstOrNull { it.key == key }
    }
}

data class PaceResult(val stepLengthM: Double, val cadence: Double, val measured: Boolean)

data class CalibrationOverview(val paces: Map<CalibrationPace, PaceResult>, val variety: Boolean)
