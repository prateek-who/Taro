package com.nvllz.stepsy.energy

import androidx.annotation.StringRes
import com.nvllz.stepsy.R
import kotlin.math.max

enum class ActivityType(@StringRes val label: Int, val met: Double) {
    STRENGTH(R.string.activity_strength, 3.5),
    CYCLING(R.string.activity_cycling, 6.8),
    SWIMMING(R.string.activity_swimming, 5.8),
    YOGA(R.string.activity_yoga, 2.5),
    ROWING(R.string.activity_rowing, 7.0),
    HIIT(R.string.activity_hiit, 8.0),
}

enum class Intensity(@StringRes val label: Int, val met: Double) {
    LIGHT(R.string.activity_light, 3.0),
    MODERATE(R.string.activity_moderate, 4.5),
    VIGOROUS(R.string.activity_vigorous, 8.0),
}

data class CustomActivity(val name: String, val met: Double)

object Activities {
    private const val FIELD = '|'
    private const val RECORD = '\n'

    fun cleanName(name: String): String = name.filterNot { it == FIELD || it == RECORD }.trim()

    fun encode(activities: List<CustomActivity>): String =
        activities.joinToString(RECORD.toString()) { "${cleanName(it.name)}$FIELD${it.met}" }

    fun decode(text: String?): List<CustomActivity> = text.orEmpty().split(RECORD).mapNotNull { line ->
        val name = line.substringBeforeLast(FIELD, "").trim()
        val met = line.substringAfterLast(FIELD, "").toDoubleOrNull()
        if (name.isEmpty() || met == null || met <= 0) null else CustomActivity(name, met)
    }

    fun withAdded(list: List<CustomActivity>, added: CustomActivity): List<CustomActivity> {
        val clean = added.copy(name = cleanName(added.name))
        return list.filterNot { it.name.equals(clean.name, ignoreCase = true) } + clean
    }

    fun estimateActiveKcal(met: Double, weightKg: Double, minutes: Int): Double =
        max(0.0, met - 1) * weightKg * minutes / 60.0
}
