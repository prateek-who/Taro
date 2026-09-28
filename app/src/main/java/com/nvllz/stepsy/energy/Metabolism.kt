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

object Metabolism {
    const val DIGESTION_SHARE = 0.10

    fun restingKcalPerDay(weightKg: Double, heightCm: Double, age: Int, sex: Sex): Double {
        val base = 10 * weightKg + 6.25 * heightCm - 5 * age
        return base + when (sex) {
            Sex.MALE -> 5.0
            Sex.FEMALE -> -161.0
            Sex.UNSPECIFIED -> -78.0
        }
    }

    fun restingSoFar(restingPerDay: Double, fractionOfDay: Double) = restingPerDay * fractionOfDay.coerceIn(0.0, 1.0)

    fun dailyNeed(restingPerDay: Double, averageActive: Double) = (restingPerDay + averageActive) / (1 - DIGESTION_SHARE)
}
