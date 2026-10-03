package com.prateek.taro.data

import android.database.sqlite.SQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
class StepsDatabaseMigrationTest {

    private val context = RuntimeEnvironment.getApplication()

    private fun legacyDatabase(name: String, version: Int, setup: SQLiteDatabase.() -> Unit) {
        context.deleteDatabase(name)
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name).apply { parentFile?.mkdirs() }, null).use {
            it.setup()
            it.version = version
        }
    }

    private fun utcMidnight(year: Int, month: Int, day: Int, offsetHours: Int = 0): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, day)
            add(Calendar.HOUR_OF_DAY, offsetHours)
        }.timeInMillis

    @Test
    fun version1TimestampsMergeIntoDates() {
        legacyDatabase("v1.db", 1) {
            execSQL("CREATE TABLE History (timestamp INTEGER PRIMARY KEY, stepsy INT NOT NULL)")
            execSQL("INSERT INTO History VALUES (${utcMidnight(2026, 3, 29, -1)}, 1000)")
            execSQL("INSERT INTO History VALUES (${utcMidnight(2026, 3, 29, -2)}, 500)")
            execSQL("INSERT INTO History VALUES (${utcMidnight(2026, 3, 30, 5)}, 800)")
        }

        val db = StepsDatabase.build(context, "v1.db")
        val days = db.steps().days("2026-01-01", "2026-12-31")
        db.close()

        assertEquals(listOf(DailySteps("2026-03-29", 1500), DailySteps("2026-03-30", 800)), days)
    }

    @Test
    fun newestTablesWorkAfterUpgrade() {
        legacyDatabase("v2sleep.db", 2) {
            execSQL("CREATE TABLE History (date TEXT PRIMARY KEY, stepsy INT NOT NULL)")
        }

        val db = StepsDatabase.build(context, "v2sleep.db")
        val dao = db.steps()
        dao.saveSleep(SleepSession(startAt = 1_000L, endAt = 25_000_000L, wakeDate = "2026-09-29", source = "phone", confirmed = false))
        dao.saveSleep(SleepSession(startAt = 2_000L, endAt = 27_000_000L, wakeDate = "2026-09-29", source = "manual", confirmed = true))
        dao.insertScreenEvent(ScreenEvent(100L, false))
        dao.insertScreenEvent(ScreenEvent(5_000L, true))
        dao.pruneScreenEvents(1_000L)
        val sleeps = dao.sleepsSince("2026-01-01")
        dao.saveSleep(SleepSession(startAt = 30_000_000L, endAt = 31_800_000L, wakeDate = "2026-09-29", source = "manual", confirmed = true, nap = true))
        val sleepMinutes = dao.sleepMinutesOn("2026-09-29")
        val events = dao.screenEvents(0L, 10_000L)
        dao.insertFoodLog(FoodLog(date = "2026-09-30", meal = "lunch", name = "Roti", grams = 80.0, amount = "2 rotis", kcal = 238.0, protein = 7.8, carbs = null, fat = null, foodKey = "in:roti", loggedAt = 1L))
        val food = dao.foodLogsOn("2026-09-30")
        val recipeId = dao.saveCustomFood(CustomFood(name = "Dal", kcal = 110.0, protein = 6.0, carbs = null, fat = null, servingLabel = null, servingGrams = null, createdAt = 1L, ingredients = "x", cookedGrams = 900.0, servings = 4, unit = "ml"))
        val recipe = dao.customFood(recipeId)
        db.close()

        assertEquals(1, sleeps.size)
        assertEquals((27_000_000L - 2_000L + 1_800_000L) / 60_000, sleepMinutes)
        assertEquals("manual", sleeps.single().source)
        assertEquals(true, sleeps.single().confirmed)
        assertEquals(listOf(ScreenEvent(5_000L, true)), events)
        assertEquals(238.0, food.single().kcal, 0.0)
        assertEquals(4, recipe?.servings)
        assertEquals("ml", recipe?.unit)
    }
}
