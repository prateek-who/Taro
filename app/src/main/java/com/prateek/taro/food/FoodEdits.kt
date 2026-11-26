package com.prateek.taro.food

import android.content.Context
import com.prateek.taro.data.CustomFood
import com.prateek.taro.util.Database

enum class EditScope { ENTRY, FOOD, PAST }

object FoodEdits {
    fun canUpdate(item: FoodItem) = item.recipeId == null

    fun isCustom(item: FoodItem) = item.source == FoodCatalog.SOURCE_CUSTOM

    fun saveValues(context: Context, item: FoodItem, kcal: Double, protein: Double, carbs: Double?, fat: Double?, past: Boolean): String {
        val database = Database.getInstance(context)
        val foods = database.customFoods()
        val base = foods.firstOrNull { FoodCatalog.customKey(it.id) == item.key }
            ?: foods.firstOrNull { it.ingredients == null && it.name.equals(item.name, ignoreCase = true) }
            ?: CustomFood(
                name = item.name,
                kcal = kcal,
                protein = protein,
                carbs = carbs,
                fat = fat,
                servingLabel = item.portions.firstOrNull()?.label,
                servingGrams = item.portions.firstOrNull()?.grams,
                createdAt = System.currentTimeMillis(),
                unit = item.unit.takeIf { it != Units.GRAMS },
            )
        val updated = base.copy(kcal = kcal, protein = protein, carbs = carbs, fat = fat)
        val id = database.saveCustomFood(updated).takeIf { updated.id == 0L } ?: updated.id
        val saved = updated.copy(id = id)
        if (past) {
            database.retargetFoodLogs(item.key, saved)
            if (item.key != FoodCatalog.customKey(saved.id)) database.recalculateFoodLogs(saved)
        }
        return FoodCatalog.customKey(saved.id)
    }
}
