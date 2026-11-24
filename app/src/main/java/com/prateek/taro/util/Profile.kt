package com.prateek.taro.util

import com.prateek.taro.energy.DietGoal
import com.prateek.taro.energy.Metabolism
import kotlin.math.roundToInt

enum class Aim(val key: String, val goal: DietGoal?) {
    LOSE("lose", DietGoal.CUT),
    HOLD("hold", DietGoal.MAINTAIN),
    BUILD("build", DietGoal.BULK),
    MOVE("move", null),
    SLEEP("sleep", null),
    ;

    val paced: Boolean get() = this == LOSE || this == BUILD

    fun calorieGoal(pace: Pace): CalorieGoal? = goal?.let { CalorieGoal(it, if (paced) pace.dailyKcal else 0) }

    companion object {
        fun fromKey(key: String?): Aim? = entries.firstOrNull { it.key == key }
    }
}

enum class Pace(val kgPerWeek: Double) {
    GENTLE(0.25),
    STEADY(0.5),
    FAST(0.75),
    ;

    val dailyKcal: Int get() = (kgPerWeek * Metabolism.KCAL_PER_KG / 7).roundToInt()
}

enum class Voice(val key: String) {
    PLAIN("plain"),
    WARM("warm"),
    DRY("dry"),
    ;

    companion object {
        fun fromKey(key: String?): Voice = entries.firstOrNull { it.key == key } ?: PLAIN
    }
}

data class UsualSleep(val bedMinute: Int, val wakeMinute: Int) {
    val minutes: Int get() = (wakeMinute - bedMinute + DAY) % DAY

    private companion object {
        const val DAY = 24 * 60
    }
}
