package com.prateek.taro.food

import android.content.Context
import com.prateek.taro.R
import java.time.LocalTime

data class MealSlot(val key: String, val name: String)

object Meals {
    private const val FIELD = '|'
    private const val RECORD = '\n'
    const val MAX_NAME = 24

    fun defaults(context: Context) = listOf(MealSlot("food", context.getString(R.string.meal_default)))

    fun cleanName(name: String) = name.filterNot { it == FIELD || it == RECORD }.trim().take(MAX_NAME)

    fun encode(meals: List<MealSlot>): String = meals.joinToString(RECORD.toString()) { "${it.key}$FIELD${cleanName(it.name)}" }

    fun decode(text: String?): List<MealSlot>? = text?.split(RECORD)?.mapNotNull { line ->
        val key = line.substringBefore(FIELD, "")
        val name = line.substringAfter(FIELD, "")
        if (key.isBlank() || name.isBlank()) null else MealSlot(key, name)
    }?.takeIf { it.isNotEmpty() }

    fun newKey() = "meal_${System.currentTimeMillis()}"

    fun forTime(meals: List<MealSlot>, time: LocalTime = LocalTime.now()): String {
        val usual = when (time.hour) {
            in 4..10 -> "breakfast"
            in 11..15 -> "lunch"
            in 16..18 -> "snack"
            else -> "dinner"
        }
        return meals.firstOrNull { it.key == usual || it.name.contains(usual, ignoreCase = true) }?.key ?: meals.first().key
    }
}
