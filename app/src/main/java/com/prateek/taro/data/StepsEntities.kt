package com.prateek.taro.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

data class DataSnapshot(
    val days: List<DailySteps> = emptyList(),
    val minutes: List<MinuteSteps> = emptyList(),
    val calibration: List<CalibrationPoint> = emptyList(),
    val activities: List<LoggedActivity> = emptyList(),
    val weights: List<WeightLog> = emptyList(),
    val sleeps: List<SleepSession> = emptyList(),
    val screenEvents: List<ScreenEvent> = emptyList(),
    val foodLogs: List<FoodLog> = emptyList(),
    val customFoods: List<CustomFood> = emptyList(),
)

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

@Entity(tableName = "food_logs", indices = [Index("date")])
data class FoodLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val meal: String,
    val name: String,
    val grams: Double?,
    val amount: String?,
    val kcal: Double,
    val protein: Double,
    val carbs: Double?,
    val fat: Double?,
    @ColumnInfo(name = "food_key") val foodKey: String?,
    @ColumnInfo(name = "logged_at") val loggedAt: Long,
    val unit: String? = null,
)

@Entity(tableName = "custom_foods")
data class CustomFood(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kcal: Double,
    val protein: Double,
    val carbs: Double?,
    val fat: Double?,
    @ColumnInfo(name = "serving_label") val servingLabel: String?,
    @ColumnInfo(name = "serving_grams") val servingGrams: Double?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val ingredients: String? = null,
    @ColumnInfo(name = "cooked_grams") val cookedGrams: Double? = null,
    val servings: Int? = null,
    val unit: String? = null,
)
