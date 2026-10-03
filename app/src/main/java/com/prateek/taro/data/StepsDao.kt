package com.prateek.taro.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert

@Dao
interface StepsDao {

    @Query("SELECT * FROM History ORDER BY date ASC")
    fun allDays(): List<DailySteps>

    @Query("SELECT * FROM minute_steps ORDER BY minute_start ASC")
    fun allMinutes(): List<MinuteSteps>

    @Query("SELECT * FROM calibration_points ORDER BY recorded_at ASC")
    fun allCalibration(): List<CalibrationPoint>

    @Query("SELECT * FROM logged_activities WHERE date BETWEEN :from AND :to")
    fun activitiesBetween(from: String, to: String): List<LoggedActivity>

    @Query("SELECT * FROM logged_activities ORDER BY logged_at ASC")
    fun allActivities(): List<LoggedActivity>

    @Query("SELECT * FROM weight_entries ORDER BY date ASC")
    fun allWeights(): List<WeightLog>

    @Query("SELECT * FROM sleep_sessions ORDER BY wake_date ASC")
    fun allSleeps(): List<SleepSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMinutes(minutes: List<MinuteSteps>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertActivities(activities: List<LoggedActivity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWeights(weights: List<WeightLog>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSleeps(sessions: List<SleepSession>)

    @Query("DELETE FROM logged_activities")
    fun clearActivities()

    @Query("DELETE FROM weight_entries")
    fun clearWeights()

    @Query("DELETE FROM sleep_sessions")
    fun clearSleeps()

    @Query("SELECT COUNT(*) FROM screen_events WHERE at >= :since")
    fun screenEventCount(since: Long): Int

    @Query("SELECT * FROM screen_events ORDER BY at ASC")
    fun allScreenEvents(): List<ScreenEvent>

    @Query("DELETE FROM screen_events")
    fun clearScreenEvents()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertScreenEvents(events: List<ScreenEvent>)

    @Insert
    fun insertFoodLog(log: FoodLog): Long

    @androidx.room.Update
    fun updateFoodLog(log: FoodLog)

    @androidx.room.Update
    fun updateFoodLogs(logs: List<FoodLog>)

    @Query("SELECT DISTINCT meal FROM food_logs")
    fun usedMeals(): List<String>

    @Query("SELECT * FROM food_logs WHERE food_key = :key")
    fun foodLogsWithKey(key: String): List<FoodLog>

    @Query("DELETE FROM food_logs WHERE id = :id")
    fun deleteFoodLog(id: Long)

    @Query("SELECT * FROM food_logs WHERE date = :date ORDER BY logged_at ASC")
    fun foodLogsOn(date: String): List<FoodLog>

    @Query("SELECT * FROM food_logs WHERE date >= :from AND date <= :to ORDER BY date ASC, logged_at ASC")
    fun foodLogsBetween(from: String, to: String): List<FoodLog>

    @Query("SELECT * FROM food_logs ORDER BY id DESC LIMIT :limit")
    fun recentFoodLogs(limit: Int): List<FoodLog>

    @Query("SELECT * FROM food_logs ORDER BY date ASC, logged_at ASC")
    fun allFoodLogs(): List<FoodLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFoodLogs(logs: List<FoodLog>)

    @Query("DELETE FROM food_logs")
    fun clearFoodLogs()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveCustomFood(food: CustomFood): Long

    @Query("DELETE FROM custom_foods WHERE id = :id")
    fun deleteCustomFood(id: Long)

    @Query("SELECT * FROM custom_foods WHERE id = :id")
    fun customFood(id: Long): CustomFood?

    @Query("SELECT * FROM custom_foods ORDER BY name COLLATE NOCASE ASC")
    fun allCustomFoods(): List<CustomFood>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCustomFoods(foods: List<CustomFood>)

    @Query("DELETE FROM custom_foods")
    fun clearCustomFoods()

    @Transaction
    fun restoreAll(snapshot: DataSnapshot) {
        clearDays()
        clearMinutes()
        clearCalibration()
        clearActivities()
        clearWeights()
        clearSleeps()
        clearScreenEvents()
        clearFoodLogs()
        clearCustomFoods()
        insertDays(snapshot.days)
        insertMinutes(snapshot.minutes)
        insertCalibration(snapshot.calibration)
        insertActivities(snapshot.activities)
        insertWeights(snapshot.weights)
        insertSleeps(snapshot.sleeps)
        insertScreenEvents(snapshot.screenEvents.orEmpty())
        insertFoodLogs(snapshot.foodLogs.orEmpty())
        insertCustomFoods(snapshot.customFoods.orEmpty())
    }

    @Query("SELECT MIN(date) FROM History WHERE date > ''")
    fun firstDate(): String?

    @Query("SELECT MAX(date) FROM History WHERE date > ''")
    fun lastDate(): String?

    @Query("SELECT COALESCE(SUM(stepsy), 0) FROM History WHERE date >= :from AND date <= :to")
    fun sumSteps(from: String, to: String): Int

    @Query("SELECT COALESCE(AVG(stepsy), 0) FROM History WHERE date >= :from AND date <= :to")
    fun averageSteps(from: String, to: String): Double

    @Query("SELECT * FROM History WHERE date >= :from AND date <= :to ORDER BY date ASC")
    fun days(from: String, to: String): List<DailySteps>

    @Upsert
    fun upsertDay(day: DailySteps)

    @Query("DELETE FROM History")
    fun clearDays()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDays(days: List<DailySteps>)

    @Query("DELETE FROM minute_steps")
    fun clearMinutes()

    @Transaction
    fun replaceHistory(days: List<DailySteps>) {
        clearDays()
        clearMinutes()
        insertDays(days)
    }

    @Query("INSERT OR IGNORE INTO minute_steps (minute_start, date, steps, ascent_m) VALUES (:minuteStart, :date, 0, 0)")
    fun ensureMinute(minuteStart: Long, date: String)

    @Query("UPDATE minute_steps SET steps = steps + :steps, ascent_m = ascent_m + :ascentMeters WHERE minute_start = :minuteStart")
    fun incrementMinute(minuteStart: Long, steps: Int, ascentMeters: Float)

    @Transaction
    fun addToMinute(minuteStart: Long, date: String, steps: Int, ascentMeters: Float) {
        ensureMinute(minuteStart, date)
        incrementMinute(minuteStart, steps, ascentMeters)
    }

    @Query("SELECT steps FROM minute_steps WHERE minute_start = :minuteStart")
    fun stepsInMinute(minuteStart: Long): Int?

    @Query("SELECT * FROM minute_steps WHERE date >= :from AND date <= :to ORDER BY minute_start ASC")
    fun minutes(from: String, to: String): List<MinuteSteps>

    @Insert
    fun insertCalibration(points: List<CalibrationPoint>)

    @Query("SELECT * FROM calibration_points WHERE recorded_at >= :since ORDER BY recorded_at ASC")
    fun calibrationSince(since: Long): List<CalibrationPoint>

    @Query("DELETE FROM calibration_points")
    fun clearCalibration()

    @Insert
    fun insertActivity(activity: LoggedActivity): Long

    @Query("DELETE FROM logged_activities WHERE id = :id")
    fun deleteActivity(id: Long)

    @Query("SELECT * FROM logged_activities WHERE date = :date ORDER BY logged_at ASC")
    fun activitiesOn(date: String): List<LoggedActivity>

    @Query("SELECT COALESCE(SUM(kcal), 0) FROM logged_activities WHERE date >= :from AND date <= :to")
    fun activityKcal(from: String, to: String): Double

    @Upsert
    fun upsertWeight(entry: WeightLog)

    @Query("DELETE FROM weight_entries WHERE date = :date")
    fun deleteWeight(date: String)

    @Query("SELECT * FROM weight_entries WHERE date >= :from ORDER BY date ASC")
    fun weightsSince(from: String): List<WeightLog>

    @Insert
    fun insertSleep(session: SleepSession): Long

    @Update
    fun updateSleep(session: SleepSession)

    @Query("SELECT * FROM sleep_sessions WHERE wake_date = :wakeDate AND nap = 0 LIMIT 1")
    fun sleepOn(wakeDate: String): SleepSession?

    @Query("SELECT COALESCE(SUM(end_at - start_at), 0) / 60000 FROM sleep_sessions WHERE wake_date = :wakeDate")
    fun sleepMinutesOn(wakeDate: String): Long

    @Query("SELECT * FROM sleep_sessions WHERE wake_date >= :from ORDER BY wake_date ASC")
    fun sleepsSince(from: String): List<SleepSession>

    @Query("DELETE FROM sleep_sessions WHERE id = :id")
    fun deleteSleep(id: Long)

    @Transaction
    fun saveSleep(session: SleepSession) {
        if (session.nap) {
            if (session.id == 0L) insertSleep(session) else updateSleep(session)
            return
        }
        val existing = sleepOn(session.wakeDate)
        if (existing == null) insertSleep(session.copy(id = 0)) else updateSleep(session.copy(id = existing.id))
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertScreenEvent(event: ScreenEvent)

    @Query("SELECT * FROM screen_events WHERE at >= :from AND at <= :to ORDER BY at ASC")
    fun screenEvents(from: Long, to: Long): List<ScreenEvent>

    @Query("SELECT * FROM screen_events WHERE at < :before ORDER BY at DESC LIMIT 1")
    fun lastScreenEventBefore(before: Long): ScreenEvent?

    @Query("DELETE FROM screen_events WHERE at < :before")
    fun pruneScreenEvents(before: Long)
}
