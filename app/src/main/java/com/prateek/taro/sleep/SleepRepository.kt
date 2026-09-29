package com.prateek.taro.sleep

import android.content.Context
import com.prateek.taro.data.SleepSession
import com.prateek.taro.util.Database
import java.time.LocalDate
import java.util.Calendar

object SleepRepository {
    const val SOURCE_GOOGLE = "google"
    const val SOURCE_PHONE = "phone"
    const val SOURCE_MANUAL = "manual"

    private const val WINDOW_START_HOUR = 18
    private const val WINDOW_END_HOUR = 15
    private const val EARLIEST_ESTIMATE_HOUR = 5
    private const val UP_FOR_MS = 30 * 60 * 1000L

    fun sessions(context: Context, days: Long): List<SleepSession> =
        Database.getInstance(context).sleepsSince(SleepDates.today().minusDays(days).toString())

    fun saveDetected(context: Context, start: Long, end: Long, source: String) {
        if (!SleepEstimator.isPlausibleNight(start, end)) return
        val database = Database.getInstance(context)
        val wakeDate = SleepDates.wakeDateOf(end).toString()
        val existing = database.sleepOn(wakeDate)
        if (existing != null && (existing.confirmed || existing.source == SOURCE_MANUAL)) return
        if (existing?.source == SOURCE_GOOGLE && source == SOURCE_PHONE) return
        database.saveSleep(SleepSession(startAt = start, endAt = end, wakeDate = wakeDate, source = source, confirmed = false))
    }

    fun saveManual(context: Context, start: Long, end: Long) {
        Database.getInstance(context).saveSleep(
            SleepSession(startAt = start, endAt = end, wakeDate = SleepDates.wakeDateOf(end).toString(), source = SOURCE_MANUAL, confirmed = true)
        )
    }

    fun confirm(context: Context, session: SleepSession) {
        Database.getInstance(context).saveSleep(session.copy(confirmed = true))
    }

    fun delete(context: Context, session: SleepSession) {
        Database.getInstance(context).deleteSleep(session.id)
    }

    private fun at(date: LocalDate, hour: Int): Long = Calendar.getInstance().apply {
        clear()
        set(date.year, date.monthValue - 1, date.dayOfMonth, hour, 0, 0)
    }.timeInMillis

    fun estimateLastNight(context: Context, nowMs: Long = System.currentTimeMillis()) {
        val today = Calendar.getInstance().apply { timeInMillis = nowMs }.let {
            LocalDate.of(it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1, it.get(Calendar.DAY_OF_MONTH))
        }
        if (nowMs < at(today, EARLIEST_ESTIMATE_HOUR)) return

        val windowStart = at(today.minusDays(1), WINDOW_START_HOUR)
        val windowEnd = minOf(nowMs, at(today, WINDOW_END_HOUR))
        val database = Database.getInstance(context)
        val screen = database.screenEvents(windowStart, windowEnd).map { ScreenState(it.at, it.screenOn) }
        if (screen.isEmpty()) return
        val steps = database.getMinutes(today.minusDays(1).toString(), today.toString())
            .associate { it.minuteStart to it.steps }

        val sleep = SleepEstimator.estimate(windowStart, windowEnd, screen, steps) ?: return
        if (sleep.end > nowMs - UP_FOR_MS) return
        saveDetected(context, sleep.start, sleep.end, SOURCE_PHONE)
    }
}
