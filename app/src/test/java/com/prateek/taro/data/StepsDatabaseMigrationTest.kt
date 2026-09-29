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
    fun version2HistorySurvivesUpgrade() {
        legacyDatabase("v2.db", 2) {
            execSQL("CREATE TABLE History (date TEXT PRIMARY KEY, stepsy INT NOT NULL)")
            execSQL("INSERT INTO History VALUES ('2026-09-01', 5000), ('2026-09-02', 7200)")
        }

        val db = StepsDatabase.build(context, "v2.db")
        val days = db.steps().days("2026-01-01", "2026-12-31")
        db.close()

        assertEquals(listOf(DailySteps("2026-09-01", 5000), DailySteps("2026-09-02", 7200)), days)
    }

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
    fun minuteStepsAccumulate() {
        context.deleteDatabase("minutes.db")
        val db = StepsDatabase.build(context, "minutes.db")
        val dao = db.steps()

        dao.addToMinute(60_000L, "2026-09-28", 12, 0f)
        dao.addToMinute(60_000L, "2026-09-28", 30, 1.5f)
        dao.addToMinute(120_000L, "2026-09-28", 8, 0f)
        val minutes = dao.minutes("2026-09-28", "2026-09-28")
        db.close()

        assertEquals(listOf(MinuteSteps(60_000L, "2026-09-28", 42, 1.5f), MinuteSteps(120_000L, "2026-09-28", 8, 0f)), minutes)
    }

    @Test
    fun importReplacesHistoryAndClearsMinutes() {
        context.deleteDatabase("import.db")
        val db = StepsDatabase.build(context, "import.db")
        val dao = db.steps()

        dao.upsertDay(DailySteps("2026-01-01", 10))
        dao.addToMinute(60_000L, "2026-01-01", 10, 0f)
        dao.replaceHistory(listOf(DailySteps("2025-12-31", 900)))
        val days = dao.days("2025-01-01", "2026-12-31")
        val minutes = dao.minutes("2025-01-01", "2026-12-31")
        db.close()

        assertEquals(listOf(DailySteps("2025-12-31", 900)), days)
        assertEquals(emptyList<MinuteSteps>(), minutes)
    }

    @Test
    fun calibrationPointsAreStoredAfterUpgrade() {
        legacyDatabase("v2cal.db", 2) {
            execSQL("CREATE TABLE History (date TEXT PRIMARY KEY, stepsy INT NOT NULL)")
        }

        val db = StepsDatabase.build(context, "v2cal.db")
        val dao = db.steps()
        dao.insertCalibration(listOf(CalibrationPoint(recordedAt = 1_000L, cadence = 112.0, stepLengthM = 0.74, distanceM = 41.4, steps = 56, source = "gps")))
        val points = dao.calibrationSince(0L)
        db.close()

        assertEquals(1, points.size)
        assertEquals(0.74, points.single().stepLengthM, 1e-9)
    }

    @Test
    fun loggedActivitiesAreStoredAndSummedAfterUpgrade() {
        legacyDatabase("v2act.db", 2) {
            execSQL("CREATE TABLE History (date TEXT PRIMARY KEY, stepsy INT NOT NULL)")
        }

        val db = StepsDatabase.build(context, "v2act.db")
        val dao = db.steps()
        val gym = dao.insertActivity(LoggedActivity(date = "2026-09-29", name = "Gym", kcal = 250.0, durationMinutes = 45, loggedAt = 1L))
        dao.insertActivity(LoggedActivity(date = "2026-09-29", name = "Swim", kcal = 180.0, durationMinutes = null, loggedAt = 2L))
        dao.insertActivity(LoggedActivity(date = "2026-09-30", name = "Yoga", kcal = 90.0, durationMinutes = 60, loggedAt = 3L))
        val total = dao.activityKcal("2026-09-29", "2026-09-30")
        dao.deleteActivity(gym)
        val remaining = dao.activitiesOn("2026-09-29")
        db.close()

        assertEquals(520.0, total, 1e-9)
        assertEquals(listOf("Swim"), remaining.map { it.name })
    }

    @Test
    fun weightEntriesAreOnePerDayAfterUpgrade() {
        legacyDatabase("v2weight.db", 2) {
            execSQL("CREATE TABLE History (date TEXT PRIMARY KEY, stepsy INT NOT NULL)")
        }

        val db = StepsDatabase.build(context, "v2weight.db")
        val dao = db.steps()
        dao.upsertWeight(WeightLog("2026-09-28", 80.4, 1L))
        dao.upsertWeight(WeightLog("2026-09-29", 80.1, 2L))
        dao.upsertWeight(WeightLog("2026-09-29", 79.9, 3L))
        dao.deleteWeight("2026-09-28")
        val weights = dao.weightsSince("2026-01-01")
        db.close()

        assertEquals(listOf(WeightLog("2026-09-29", 79.9, 3L)), weights)
    }

    @Test
    fun sleepIsOnePerWakeDateAndScreenEventsPrune() {
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
        val events = dao.screenEvents(0L, 10_000L)
        db.close()

        assertEquals(1, sleeps.size)
        assertEquals("manual", sleeps.single().source)
        assertEquals(true, sleeps.single().confirmed)
        assertEquals(listOf(ScreenEvent(5_000L, true)), events)
    }
}
