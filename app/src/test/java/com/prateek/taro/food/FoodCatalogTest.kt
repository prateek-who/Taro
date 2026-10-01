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

        val day = java.time.LocalDate.of(2026, 10, 1)
        val orders = MealOrders(emptyList()).with(day, listOf("b", "a"), today = day.plusDays(3), fallback = listOf("a", "b"))
        assertEquals(listOf("b", "a"), orders.on(day))
        assertEquals(listOf("a", "b"), orders.on(day.plusDays(1)))

        fun row(y: Float, vararg cells: Pair<String, Float>) = cells.map { (text, x) -> OcrToken(text, x - 20, y, x + 20, y + 14) }
        val label = LabelParser.parse(
            row(0f, "Nutritional" to 60f, "Information" to 140f) +
                row(30f, "Per" to 190f, "100g" to 215f, "Per serve" to 300f, "%RDA" to 380f) +
                row(60f, "Energy" to 40f, "(kcal)" to 90f, "350" to 210f, "105" to 300f, "5%" to 380f) +
                row(90f, "Protein" to 40f, "(g)" to 90f, "12.5" to 210f, "3.8" to 300f) +
                row(120f, "Carbohydrate" to 50f, "6O,2" to 212f, "18.1" to 300f) +
                row(150f, "of which Sugars" to 60f, "10" to 210f, "3" to 300f) +
                row(180f, "Total Fat" to 45f, "6.8" to 210f, "2" to 300f) +
                row(210f, "Saturated Fat" to 50f, "2.1" to 210f) +
                row(260f, "Serving size 30 g" to 80f),
        )
        assertEquals(LabelValues(350.0, 12.5, 60.2, 6.8, 30.0), label)
        assertEquals(350.0, LabelParser.parse(row(0f, "Energy" to 40f, "1464" to 120f, "kJ" to 150f, "350" to 190f, "kcal" to 220f)).kcal!!, 0.0)
    }
}
