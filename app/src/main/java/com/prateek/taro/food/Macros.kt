package com.prateek.taro.food

object Macros {
    const val PROTEIN_KCAL = 4.0
    const val CARB_KCAL = 4.0
    const val FAT_KCAL = 9.0
    val CARB_SHARE = 0.45..0.65
    val FAT_SHARE = 0.20..0.35

    fun grams(budget: Double, kcalPerGram: Double, share: Double): Double = budget * share / kcalPerGram
}
