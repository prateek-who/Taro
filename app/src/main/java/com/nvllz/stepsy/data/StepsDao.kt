package com.nvllz.stepsy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert

@Dao
interface StepsDao {

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

    @Query("SELECT * FROM sleep_sessions WHERE wake_date = :wakeDate")
    fun sleepOn(wakeDate: String): SleepSession?

    @Query("SELECT * FROM sleep_sessions WHERE wake_date >= :from ORDER BY wake_date ASC")
    fun sleepsSince(from: String): List<SleepSession>

    @Query("DELETE FROM sleep_sessions WHERE id = :id")
    fun deleteSleep(id: Long)

    @Transaction
    fun saveSleep(session: SleepSession) {
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
