package com.prateek.taro.util

import com.prateek.taro.data.CustomFood
import com.prateek.taro.data.FoodLog
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import com.prateek.taro.data.DataSnapshot
import android.content.Context
import com.prateek.taro.data.DailySteps
import com.prateek.taro.data.LoggedActivity
import com.prateek.taro.data.ScreenEvent
import com.prateek.taro.data.SleepSession
import com.prateek.taro.data.WeightLog
import com.prateek.taro.data.MinuteSteps
import com.prateek.taro.data.StepsDao
import com.prateek.taro.data.StepsDatabase
import java.util.Calendar
import java.util.TimeZone

internal class Database private constructor(private val dao: StepsDao) {

    internal val firstEntry: String?
        get() = dao.firstDate()

    internal val lastEntry: String?
        get() = dao.lastDate()

    internal fun avgSteps(minDate: String, maxDate: String): Int = dao.averageSteps(minDate, maxDate).toInt()

    internal fun getSumSteps(minDate: String, maxDate: String): Int = dao.sumSteps(minDate, maxDate)

    internal fun getEntries(minDate: String?, maxDate: String?): List<Entry> {
        if (minDate == null || maxDate == null) return emptyList()
        return dao.days(minDate, maxDate).map { Entry(Util.dateStringToCalendarMillis(it.date), it.date, it.steps) }
    }

    /** Overwrites the step count for the given date. Used by MotionService for live tracking. */
    internal fun addEntry(date: String, steps: Int) = dao.upsertDay(DailySteps(date, steps))

    internal fun addMinuteSteps(timestampMs: Long, date: String, steps: Int, ascentMeters: Float = 0f) =
        dao.addToMinute(timestampMs - timestampMs % 60_000L, date, steps, ascentMeters)

    internal fun stepsInMinute(minuteStart: Long): Int = dao.stepsInMinute(minuteStart) ?: 0

    internal fun getMinutes(minDate: String, maxDate: String): List<MinuteSteps> = dao.minutes(minDate, maxDate)

    internal fun addActivity(activity: LoggedActivity): Long = changed { dao.insertActivity(activity) }

    internal fun deleteActivity(id: Long) = changed { dao.deleteActivity(id) }

    internal fun activitiesOn(date: String): List<LoggedActivity> = dao.activitiesOn(date)

    internal fun activityKcal(from: String, to: String): Double = dao.activityKcal(from, to)

    internal fun saveWeight(entry: WeightLog) = changed { dao.upsertWeight(entry) }

    internal fun deleteWeight(date: String) = changed { dao.deleteWeight(date) }

    internal fun weightsSince(from: String): List<WeightLog> = dao.weightsSince(from)

    internal fun saveSleep(session: SleepSession) = changed { dao.saveSleep(session) }

    internal fun sleepOn(wakeDate: String): SleepSession? = dao.sleepOn(wakeDate)

    internal fun sleepsSince(from: String): List<SleepSession> = dao.sleepsSince(from)

    internal fun deleteSleep(id: Long) = changed { dao.deleteSleep(id) }

    internal fun recordScreen(at: Long, screenOn: Boolean) = dao.insertScreenEvent(ScreenEvent(at, screenOn))

    internal fun screenEvents(from: Long, to: Long): List<ScreenEvent> =
        listOfNotNull(dao.lastScreenEventBefore(from)) + dao.screenEvents(from, to)

    internal fun logFood(log: FoodLog): Long = changed { dao.insertFoodLog(log) }

    internal fun updateFood(log: FoodLog) = changed { dao.updateFoodLog(log) }

    internal fun deleteFood(id: Long) = changed { dao.deleteFoodLog(id) }

    internal fun foodOn(date: String): List<FoodLog> = dao.foodLogsOn(date)

    internal fun foodBetween(from: String, to: String): List<FoodLog> = dao.foodLogsBetween(from, to)

    internal fun recentFood(limit: Int): List<FoodLog> = dao.recentFoodLogs(limit)

    internal fun saveCustomFood(food: CustomFood): Long = changed { dao.saveCustomFood(food) }

    internal fun deleteCustomFood(id: Long) = changed { dao.deleteCustomFood(id) }

    internal fun customFoods(): List<CustomFood> = dao.allCustomFoods()

    internal fun customFood(id: Long): CustomFood? = dao.customFood(id)

    internal fun lastScreenEventBefore(before: Long): ScreenEvent? = dao.lastScreenEventBefore(before)

    internal fun screenEventCount(since: Long): Int = dao.screenEventCount(since)

    internal fun pruneScreenEvents(before: Long) = dao.pruneScreenEvents(before)

    internal fun snapshot(): DataSnapshot = DataSnapshot(
        days = dao.allDays(),
        minutes = dao.allMinutes(),
        calibration = dao.allCalibration(),
        activities = dao.allActivities(),
        weights = dao.allWeights(),
        sleeps = dao.allSleeps(),
        screenEvents = dao.allScreenEvents(),
        foodLogs = dao.allFoodLogs(),
        customFoods = dao.allCustomFoods(),
    )

    internal fun restore(snapshot: DataSnapshot) = changed { dao.restoreAll(snapshot) }

    fun clearAllAndImport(entries: List<Pair<String, Int>>) = changed {
        dao.replaceHistory(entries.map { (date, steps) -> DailySteps(date, steps) })
    }

    internal class Entry(
        val timestamp: Long,
        val date: String,
        val steps: Int
    )

    private fun <T> changed(block: () -> T): T = block().also { versionFlow.update { it + 1 } }

    companion object {
        private val versionFlow = MutableStateFlow(0)
        val changes: StateFlow<Int> = versionFlow

        @Volatile
        private var instance: Database? = null

        internal fun getInstance(context: Context): Database = instance ?: synchronized(this) {
            instance ?: Database(StepsDatabase.get(context).steps()).also { instance = it }
        }

        /**
         * Converts a legacy unix-ms "midnight local" timestamp to a yyyy-MM-dd string
         * without relying on the current device timezone.
         *
         * The old schema stored the start of each local day (midnight) as a unix
         * millisecond timestamp. That timestamp is always within ±12 h of a UTC
         * midnight boundary. Snapping to the nearest UTC midnight and reading the
         * UTC date therefore always recovers the intended calendar date, regardless
         * of what timezone the device is currently in or whether DST has changed.
         */
        internal fun snapTimestampToDate(timestampMs: Long): String {
            val msIntoDay = timestampMs % 86_400_000L
            val snappedMs = if (msIntoDay >= 12 * 3_600_000L) {
                timestampMs + (86_400_000L - msIntoDay)
            } else {
                timestampMs - msIntoDay
            }
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = snappedMs
            }
            return "%04d-%02d-%02d".format(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }
    }
}
