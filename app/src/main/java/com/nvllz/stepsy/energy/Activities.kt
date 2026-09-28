package com.nvllz.stepsy.energy

import androidx.annotation.StringRes
import com.nvllz.stepsy.R
import kotlin.math.max

enum class ActivityType(@StringRes val label: Int, val met: Double?) {
    STRENGTH(R.string.activity_strength, 3.5),
    CYCLING(R.string.activity_cycling, 6.8),
    SWIMMING(R.string.activity_swimming, 5.8),
    YOGA(R.string.activity_yoga, 2.5),
    ROWING(R.string.activity_rowing, 7.0),
    HIIT(R.string.activity_hiit, 8.0),
    OTHER(R.string.activity_other, null),
}

object Activities {
    fun estimateActiveKcal(met: Double, weightKg: Double, minutes: Int): Double =
        max(0.0, met - 1) * weightKg * minutes / 60.0
}
