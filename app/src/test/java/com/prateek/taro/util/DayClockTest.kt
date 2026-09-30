package com.prateek.taro.util

import com.prateek.taro.energy.Body
import com.prateek.taro.energy.DayTimelines
import com.prateek.taro.energy.Metabolism
import com.prateek.taro.energy.MinuteSample
import com.prateek.taro.energy.TimedAmount
import com.prateek.taro.energy.TimedSpan
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DayClockTest {

    private fun at(zone: TimeZone, y: Int, m: Int, d: Int, h: Int): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(y, m - 1, d, h, 0, 0)
        }.timeInMillis

    @Test
    fun startOfDayMovesTheLogicalDate() {
        val india = TimeZone.getTimeZone("Asia/Kolkata")
        assertEquals("2026-09-28", DayClock.dateOf(at(india, 2026, 9, 29, 2), 4 * 60, india))
        assertEquals("2026-09-29", DayClock.dateOf(at(india, 2026, 9, 29, 4), 4 * 60, india))
        assertEquals(at(india, 2026, 9, 30, 4), DayClock.nextStart(at(india, 2026, 9, 29, 23), 4 * 60, india))
        val berlin = TimeZone.getTimeZone("Europe/Berlin")
        assertEquals(0.5, DayClock.fractionOfDay(at(berlin, 2026, 3, 29, 0) + 23 * 30 * 60_000L, 0, berlin), 1e-9)
        assertEquals(at(india, 2026, 9, 30, 2), DayClock.momentOf("2026-09-29", 2 * 60, 4 * 60, india))
        assertEquals(at(india, 2026, 9, 29, 8), DayClock.momentOf("2026-09-29", 8 * 60, 4 * 60, india))

        val start = at(india, 2026, 9, 29, 0)
        val hour = 3_600_000L
        val timeline = DayTimelines.build(
            start = start,
            end = start + 24 * hour,
            now = start + 24 * hour,
            body = Body(70.0, 0.75, 1.0),
            restingPerDay = 1_440.0,
            minutes = listOf(start + 8 * hour + 60_000L to MinuteSample(100)),
            food = listOf(TimedAmount(start + 13 * hour + 5, 600.0)),
            activities = emptyList(),
            sleep = listOf(TimedSpan(start - 2 * hour, start + 6 * hour)),
        )
        assertEquals(100, timeline.hours[8].steps)
        assertEquals(600.0, timeline.hours[13].eaten, 0.0)
        assertEquals(60, timeline.hours[5].sleepMinutes)
        assertEquals(Metabolism.withDigestion(60.0 * 0.95), timeline.hours[5].burn, 1e-6)
    }
}
