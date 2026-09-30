package com.prateek.taro.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodCatalogTest {
    @Test
    fun parsesRowsAndRanksIndianFoodsFirst() {
        val roti = FoodCatalog.parse("in:roti\tRoti / chapati, whole wheat\tin\t297\t9.8\t50\t7\t1 small roti:30|1 medium roti:40")!!
        val usda = FoodCatalog.parse("sr:1\tBread, roti, commercially prepared\tusda\t280\t9\t48\t6\t")!!
        assertEquals(40.0, roti.portions[1].grams, 0.0)
        assertEquals(118.8, roti.kcalFor(40.0), 0.01)
        assertNull(FoodCatalog.parse("broken\trow"))
        assertEquals(listOf(roti, usda), FoodCatalog.search(listOf(usda, roti), "rot"))
        assertEquals(emptyList<FoodItem>(), FoodCatalog.search(listOf(usda, roti), "paneer"))

        val items = listOf(Ingredient.of(roti, 400.0), Ingredient(null, "Water", 200.0, 0.0, 0.0, null, null))
        val recipe = Recipes.build("Dough", Recipes.decode(Recipes.encode(items)), cookedGrams = 500.0, servings = 4, servingLabel = "1 serving")
        assertEquals(1_188.0 / 5, recipe.kcal, 0.01)
        assertEquals(125.0, recipe.servingGrams!!, 0.0)
        assertEquals(listOf(125.0, 500.0), FoodCatalog.custom(recipe).portions.map { it.grams })
    }
}
