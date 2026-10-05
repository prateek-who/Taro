package com.prateek.taro.food

import android.content.Context
import com.prateek.taro.R
import com.prateek.taro.data.CustomFood

data class Portion(val label: String, val grams: Double)

data class FoodItem(
    val key: String,
    val name: String,
    val source: String,
    val kcal: Double,
    val protein: Double,
    val carbs: Double?,
    val fat: Double?,
    val portions: List<Portion>,
    val recipeId: Long? = null,
    val unit: String = Units.GRAMS,
) {
    val words: List<String> by lazy(LazyThreadSafetyMode.PUBLICATION) { FoodCatalog.words(name) }

    fun kcalFor(grams: Double) = kcal * grams / 100
    fun proteinFor(grams: Double) = protein * grams / 100
    fun carbsFor(grams: Double) = carbs?.let { it * grams / 100 }
    fun fatFor(grams: Double) = fat?.let { it * grams / 100 }
}

object FoodCatalog {
    const val SOURCE_INDIAN = "in"
    const val SOURCE_CUSTOM = "custom"
    const val SOURCE_USDA = "usda"
    private const val MAX_BOOST = 10
    private const val BOOST_PER_LOG = 4

    @Volatile
    private var cache: List<FoodItem>? = null

    fun load(context: Context): List<FoodItem> = cache ?: synchronized(this) {
        cache ?: context.assets.open("foods.tsv").bufferedReader().useLines { lines ->
            lines.mapNotNull(::parse).toList()
        }.also { cache = it }
    }

    fun parse(line: String): FoodItem? {
        val cols = line.split('\t')
        if (cols.size < 8) return null
        val portions = cols[7].split('|').mapNotNull { part ->
            val label = part.substringBeforeLast(':', "")
            val grams = part.substringAfterLast(':', "").toDoubleOrNull()
            if (label.isBlank() || grams == null || grams <= 0) null else Portion(label, grams)
        }
        return FoodItem(
            key = cols[0],
            name = cols[1],
            source = cols[2],
            kcal = cols[3].toDoubleOrNull() ?: return null,
            protein = cols[4].toDoubleOrNull() ?: 0.0,
            carbs = cols[5].toDoubleOrNull(),
            fat = cols[6].toDoubleOrNull(),
            portions = portions,
        )
    }

    fun customKey(id: Long) = "custom:$id"

    fun custom(food: CustomFood) = FoodItem(
        key = customKey(food.id),
        name = food.name,
        source = SOURCE_CUSTOM,
        kcal = food.kcal,
        protein = food.protein,
        carbs = food.carbs,
        fat = food.fat,
        portions = listOfNotNull(
            food.servingGrams?.takeIf { it > 0 }?.let { Portion(food.servingLabel?.takeIf(String::isNotBlank) ?: "1 serving", it) },
            food.servings?.takeIf { it > 1 }?.let { count -> food.servingGrams?.let { Portion("whole recipe", it * count) } },
        ),
        recipeId = food.id.takeIf { food.ingredients != null },
        unit = food.unit ?: Units.GRAMS,
    )

    fun words(text: String) = text.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }

    fun search(items: List<FoodItem>, query: String, limit: Int = 40, boost: Map<String, Int> = emptyMap()): List<FoodItem> {
        val tokens = words(query)
        if (tokens.isEmpty()) return emptyList()
        return items.mapNotNull { item ->
            val nameWords = item.words
            var score = 0
            for (token in tokens) {
                val index = nameWords.indexOfFirst { it.startsWith(token) }
                when {
                    index == 0 -> score += 30
                    index > 0 -> score += 20 - minOf(index, 10)
                    token.length >= 3 && item.name.contains(token, ignoreCase = true) -> score += 5
                    else -> return@mapNotNull null
                }
            }
            score += when (item.source) {
                SOURCE_CUSTOM -> 40
                SOURCE_INDIAN -> 25
                else -> 0
            }
            score -= nameWords.size
            score += minOf(boost[item.key] ?: 0, MAX_BOOST) * BOOST_PER_LOG
            item to score
        }.sortedByDescending { it.second }.take(limit).map { it.first }
    }
}
