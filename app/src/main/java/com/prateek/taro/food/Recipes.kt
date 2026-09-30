package com.prateek.taro.food

import com.prateek.taro.data.CustomFood

data class Ingredient(
    val key: String?,
    val name: String,
    val grams: Double,
    val kcal: Double,
    val protein: Double,
    val carbs: Double?,
    val fat: Double?,
) {
    companion object {
        fun of(item: FoodItem, grams: Double) = Ingredient(
            key = item.key,
            name = item.name,
            grams = grams,
            kcal = item.kcalFor(grams),
            protein = item.proteinFor(grams),
            carbs = item.carbsFor(grams),
            fat = item.fatFor(grams),
        )
    }
}

data class RecipeTotals(val grams: Double, val kcal: Double, val protein: Double, val carbs: Double?, val fat: Double?)

object Recipes {
    private const val FIELD = '\t'
    private const val RECORD = '\n'

    private fun clean(text: String) = text.filterNot { it == FIELD || it == RECORD }.trim()

    fun encode(items: List<Ingredient>): String = items.joinToString(RECORD.toString()) {
        listOf(it.key.orEmpty(), clean(it.name), it.grams, it.kcal, it.protein, it.carbs ?: "", it.fat ?: "").joinToString(FIELD.toString())
    }

    fun decode(text: String?): List<Ingredient> = text.orEmpty().split(RECORD).mapNotNull { line ->
        val cols = line.split(FIELD)
        if (cols.size < 7) return@mapNotNull null
        Ingredient(
            key = cols[0].ifBlank { null },
            name = cols[1],
            grams = cols[2].toDoubleOrNull() ?: return@mapNotNull null,
            kcal = cols[3].toDoubleOrNull() ?: return@mapNotNull null,
            protein = cols[4].toDoubleOrNull() ?: 0.0,
            carbs = cols[5].toDoubleOrNull(),
            fat = cols[6].toDoubleOrNull(),
        )
    }

    fun totals(items: List<Ingredient>) = RecipeTotals(
        grams = items.sumOf { it.grams },
        kcal = items.sumOf { it.kcal },
        protein = items.sumOf { it.protein },
        carbs = items.mapNotNull { it.carbs }.takeIf { it.isNotEmpty() }?.sum(),
        fat = items.mapNotNull { it.fat }.takeIf { it.isNotEmpty() }?.sum(),
    )

    fun build(
        name: String,
        items: List<Ingredient>,
        cookedGrams: Double?,
        servings: Int,
        servingLabel: String,
        id: Long = 0,
        createdAt: Long = System.currentTimeMillis(),
    ): CustomFood {
        val totals = totals(items)
        val weight = cookedGrams?.takeIf { it > 0 } ?: totals.grams
        val per100 = 100 / weight
        return CustomFood(
            id = id,
            name = name.trim(),
            kcal = totals.kcal * per100,
            protein = totals.protein * per100,
            carbs = totals.carbs?.times(per100),
            fat = totals.fat?.times(per100),
            servingLabel = servingLabel,
            servingGrams = weight / servings.coerceAtLeast(1),
            createdAt = createdAt,
            ingredients = encode(items),
            cookedGrams = cookedGrams?.takeIf { it > 0 },
            servings = servings.coerceAtLeast(1),
        )
    }
}
