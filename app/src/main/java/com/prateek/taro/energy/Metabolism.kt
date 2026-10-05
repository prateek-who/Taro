package com.prateek.taro.energy

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
    val DIGESTION_RANGE = 0.05..0.15
    private const val PROTEIN_COST = 0.25
    private const val CARB_COST = 0.075
    private const val FAT_COST = 0.02
    private const val UNKNOWN_COST = 0.06
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

    fun withDigestion(burn: Double, share: Double = DIGESTION_SHARE) = burn / (1 - share)

    fun digestion(kcal: Double, protein: Double, carbs: Double?, fat: Double?): Double {
        if (carbs == null && fat == null && protein <= 0) return kcal * DIGESTION_SHARE
        val proteinKcal = minOf(protein * 4, kcal)
        val carbKcal = (carbs ?: 0.0) * 4
        val fatKcal = (fat ?: 0.0) * 9
        val rest = (kcal - proteinKcal - carbKcal - fatKcal).coerceAtLeast(0.0)
        return (PROTEIN_COST * proteinKcal + CARB_COST * carbKcal + FAT_COST * fatKcal + UNKNOWN_COST * rest)
            .coerceIn(FAT_COST * kcal, PROTEIN_COST * kcal)
    }

    fun dailyNeed(restingPerDay: Double, averageActive: Double, share: Double = DIGESTION_SHARE) =
        withDigestion(restingPerDay + averageActive, share)

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
