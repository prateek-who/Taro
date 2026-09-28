package com.nvllz.stepsy.util

import android.content.Context
import com.nvllz.stepsy.data.DailySteps
import com.nvllz.stepsy.data.LoggedActivity
import com.nvllz.stepsy.data.MinuteSteps
import com.nvllz.stepsy.data.StepsDao
import com.nvllz.stepsy.data.StepsDatabase
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

    internal fun addActivity(activity: LoggedActivity): Long = dao.insertActivity(activity)

    internal fun deleteActivity(id: Long) = dao.deleteActivity(id)

    internal fun activitiesOn(date: String): List<LoggedActivity> = dao.activitiesOn(date)

    internal fun activityKcal(from: String, to: String): Double = dao.activityKcal(from, to)

    fun clearAllAndImport(entries: List<Pair<String, Int>>) =
        dao.replaceHistory(entries.map { (date, steps) -> DailySteps(date, steps) })

    internal class Entry(
        val timestamp: Long,
        val date: String,
        val steps: Int
    )

    companion object {
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
