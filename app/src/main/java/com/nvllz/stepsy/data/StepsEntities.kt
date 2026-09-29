package com.nvllz.stepsy.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "History")
data class DailySteps(
    @PrimaryKey val date: String,
    @ColumnInfo(name = "stepsy") val steps: Int,
)

@Entity(tableName = "minute_steps", indices = [Index("date")])
data class MinuteSteps(
    @PrimaryKey @ColumnInfo(name = "minute_start") val minuteStart: Long,
    val date: String,
    val steps: Int,
    @ColumnInfo(name = "ascent_m", defaultValue = "0") val ascentMeters: Float = 0f,
)

@Entity(tableName = "calibration_points")
data class CalibrationPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "recorded_at") val recordedAt: Long,
    val cadence: Double,
    @ColumnInfo(name = "step_length_m") val stepLengthM: Double,
    @ColumnInfo(name = "distance_m") val distanceM: Double,
    val steps: Int,
    val source: String,
)

@Entity(tableName = "logged_activities", indices = [Index("date")])
data class LoggedActivity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val name: String,
    val kcal: Double,
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int?,
    @ColumnInfo(name = "logged_at") val loggedAt: Long,
)

@Entity(tableName = "weight_entries")
data class WeightLog(
    @PrimaryKey val date: String,
    val kg: Double,
    @ColumnInfo(name = "logged_at") val loggedAt: Long,
)

@Entity(tableName = "sleep_sessions", indices = [Index(value = ["wake_date"], unique = true)])
data class SleepSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "start_at") val startAt: Long,
    @ColumnInfo(name = "end_at") val endAt: Long,
    @ColumnInfo(name = "wake_date") val wakeDate: String,
    val source: String,
    val confirmed: Boolean,
) {
    val durationMinutes: Long get() = (endAt - startAt) / 60_000L
}

@Entity(tableName = "screen_events")
data class ScreenEvent(
    @PrimaryKey val at: Long,
    @ColumnInfo(name = "screen_on") val screenOn: Boolean,
)
