package com.nvllz.stepsy.energy

enum class Sex(val key: String) {
    MALE("male"),
    FEMALE("female"),
    UNSPECIFIED("unspecified"),
    ;

    companion object {
        fun fromKey(key: String?): Sex? = entries.firstOrNull { it.key == key }
    }
}

enum class DietGoal(val key: String) {
    CUT("cut"),
    MAINTAIN("maintain"),
    BULK("bulk"),
    ;

    companion object {
        fun fromKey(key: String?): DietGoal? = entries.firstOrNull { it.key == key }
    }
}

object Metabolism {
    const val KCAL_PER_KG = 7_700.0

    const val DIGESTION_SHARE = 0.10
    const val SLEEP_REDUCTION = 0.05

    fun restingKcalPerDay(weightKg: Double, heightCm: Double, age: Int, sex: Sex): Double {
        val base = 10 * weightKg + 6.25 * heightCm - 5 * age
        return base + when (sex) {
            Sex.MALE -> 5.0
            Sex.FEMALE -> -161.0
            Sex.UNSPECIFIED -> -78.0
        }
    }

    fun restingSoFar(restingPerDay: Double, fractionOfDay: Double) = restingPerDay * fractionOfDay.coerceIn(0.0, 1.0)

    fun sleepSaving(restingPerDay: Double, sleepMinutes: Long): Double =
        restingPerDay / 1440.0 * sleepMinutes.coerceIn(0, 1440) * SLEEP_REDUCTION

    fun withDigestion(burn: Double) = burn / (1 - DIGESTION_SHARE)

    fun dailyNeed(restingPerDay: Double, averageActive: Double) = withDigestion(restingPerDay + averageActive)

    fun calorieTarget(dailyNeed: Double, goal: DietGoal, adjustment: Int): Double = when (goal) {
        DietGoal.CUT -> dailyNeed - adjustment
        DietGoal.MAINTAIN -> dailyNeed
        DietGoal.BULK -> dailyNeed + adjustment
    }

    fun weeklyChangeKg(goal: DietGoal, adjustment: Int): Double = when (goal) {
        DietGoal.CUT -> -adjustment * 7 / KCAL_PER_KG
        DietGoal.MAINTAIN -> 0.0
        DietGoal.BULK -> adjustment * 7 / KCAL_PER_KG
    }
}
