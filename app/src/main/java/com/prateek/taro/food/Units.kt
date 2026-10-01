package com.prateek.taro.food

import java.util.Locale

enum class FoodUnit(val label: String, val volume: Boolean, val size: Double) {
    GRAM("g", false, 1.0),
    ML("ml", true, 1.0),
    OUNCE("oz", false, 28.35),
    TSP("tsp", true, 5.0),
    TBSP("tbsp", true, 15.0),
    CUP("cup", true, 240.0),
}

object Units {
    const val GRAMS = "g"
    const val ML = "ml"

    private val measure = Regex("""^([0-9.]+) (cup|tbsp|tablespoon|tsp|teaspoon|fl oz)\b(.*)$""", RegexOption.IGNORE_CASE)
    private val millilitres = mapOf("cup" to 236.6, "tbsp" to 14.8, "tablespoon" to 14.8, "tsp" to 4.9, "teaspoon" to 4.9, "fl oz" to 29.6)

    fun base(item: FoodItem): FoodUnit = if (item.unit == ML) FoodUnit.ML else FoodUnit.GRAM

    fun density(item: FoodItem): Double? {
        if (item.source != FoodCatalog.SOURCE_USDA) return null
        return item.portions.mapNotNull { portion ->
            val match = measure.find(portion.label) ?: return@mapNotNull null
            val count = match.groupValues[1].toDoubleOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            val ml = millilitres[match.groupValues[2].lowercase()] ?: return@mapNotNull null
            (portion.grams / (count * ml)) to match.groupValues[3].isBlank()
        }.sortedByDescending { it.second }.firstOrNull()?.first?.takeIf { it in 0.1..2.5 }
    }

    fun approximate(unit: FoodUnit, item: FoodItem): Boolean = unit.volume != base(item).volume && density(item) == null

    fun toBase(amount: Double, unit: FoodUnit, item: FoodItem): Double {
        val quantity = amount * unit.size
        val baseIsVolume = base(item).volume
        val density = density(item) ?: 1.0
        return when {
            unit.volume == baseIsVolume -> quantity
            unit.volume -> quantity * density
            else -> quantity / density
        }
    }

    fun format(value: Double, unit: String?): String = "%.0f %s".format(Locale.getDefault(), value, unit ?: GRAMS)
}
