package com.prateek.taro.achievements

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.prateek.taro.data.SleepSession
import com.prateek.taro.energy.ActivityEnergy
import com.prateek.taro.energy.DietGoal
import com.prateek.taro.energy.EnergyModel
import com.prateek.taro.energy.MinuteSample
import com.prateek.taro.energy.WeightJournal
import com.prateek.taro.sleep.Night
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Database
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object AchievementData {
    private const val EARLY_HOUR = 7
    private const val LATE_HOUR = 22
    private const val NOON = 12
    private const val WALKING_STEPS_PER_MINUTE = 60

    fun longestWalk(minutes: List<Pair<Long, Int>>): Int {
        var best = 0
        var run = 0
        var previous: Long? = null
        for ((start, steps) in minutes.sortedBy { it.first }) {
            run = if (steps < WALKING_STEPS_PER_MINUTE) 0 else if (previous != null && start - previous == 60_000L) run + 1 else 1
            best = maxOf(best, run)
            previous = start
        }
        return best
    }
    val LAST_BACKUP_DATE = stringPreferencesKey("last_backup_date")
    private val SEEN = stringSetPreferencesKey("achievements_seen")

    private fun date(text: String?): LocalDate? = text?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }

    fun inputs(context: Context): AchievementInputs {
        val db = Database.getInstance(context)
        val snapshot = db.snapshot()
        val zone = ZoneId.systemDefault()
        val body = ActivityEnergy.body()
        val minutesByDate = snapshot.minutes.groupBy { it.date }
        val days = snapshot.days.mapNotNull { day ->
            val parsed = date(day.date) ?: return@mapNotNull null
            val minutes = minutesByDate[day.date].orEmpty()
            val samples = minutes.map { MinuteSample(it.steps, it.ascentMeters.toDouble()) }
            val hours = minutes.map { Instant.ofEpochMilli(it.minuteStart).atZone(zone).hour to it.steps }
            DayStat(
                date = parsed,
                steps = day.steps,
                distanceM = EnergyModel.dayDistanceM(body, samples, day.steps),
                activeKcal = EnergyModel.dayKcal(body, samples, day.steps),
                ascentM = minutes.sumOf { it.ascentMeters.toDouble() },
                earlySteps = hours.filter { it.first < EARLY_HOUR }.sumOf { it.second },
                lateSteps = hours.filter { it.first >= LATE_HOUR }.sumOf { it.second },
                stepsBeforeNoon = hours.filter { it.first < NOON }.sumOf { it.second },
                longestWalkMin = longestWalk(minutes.map { it.minuteStart to it.steps }),
            )
        }
        val accuracy = listOfNotNull(date(AppPreferences.accuracyResults.stepDate), date(AppPreferences.accuracyResults.distanceDate)).minOrNull()
        val calibrated = snapshot.calibration.minOfOrNull { it.recordedAt }?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        return AchievementInputs(
            days = days,
            goal = AppPreferences.dailyGoalTarget,
            goals = AppPreferences.goalHistory,
            workouts = snapshot.activities.mapNotNull { activity -> date(activity.date)?.let { it to activity.kcal } },
            weights = WeightJournal.points(context),
            losingWeight = AppPreferences.calorieGoal?.goal != DietGoal.BULK,
            nights = snapshot.sleeps.filterNot { it.nap }.map(SleepSession::toNightOrNull).filterNotNull(),
            calibratedOn = calibrated,
            accuracyCheckedOn = accuracy,
            backedUpOn = date(AppPreferences.stringValue(LAST_BACKUP_DATE)),
            firstDayOfWeek = DayOfWeek.of(((AppPreferences.firstDayOfWeek + 5) % 7) + 1),
            foods = snapshot.foodLogs.orEmpty().groupBy { it.date }.mapNotNull { (day, logs) ->
                date(day)?.let { FoodDayStat(it, logs.sumOf { log -> log.kcal }, logs.sumOf { log -> log.protein }, logs.map { log -> log.meal }.distinct().size) }
            },
            proteinTarget = AppPreferences.weight * AppPreferences.proteinPerKg,
            recipes = snapshot.customFoods.orEmpty().filter { it.ingredients != null }.map { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() },
            customFoods = snapshot.customFoods.orEmpty().filter { it.ingredients == null }.map { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() },
            claimed = AppPreferences.claimedBadges,
        )
    }

    fun evaluate(context: Context): List<BadgeResult> = Badges.evaluate(inputs(context))

    suspend fun newlyEarned(context: Context): List<BadgeResult> {
        val earned = evaluate(context).filter { it.earned }
        val prefs = AppPreferences.dataStore.data.first()
        val seen = prefs[SEEN]
        AppPreferences.dataStore.edit { it[SEEN] = earned.map { result -> result.def.id }.toSet() + seen.orEmpty() }
        if (seen == null) return emptyList()
        return earned.filter { it.def.id !in seen }
    }
}

private fun SleepSession.toNightOrNull(): Night? =
    runCatching { Night(LocalDate.parse(wakeDate), startAt, endAt) }.getOrNull()
