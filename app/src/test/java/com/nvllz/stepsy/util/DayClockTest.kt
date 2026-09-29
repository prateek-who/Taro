package com.nvllz.stepsy.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DayClockTest {

    private val india = TimeZone.getTimeZone("Asia/Kolkata")
    private val berlin = TimeZone.getTimeZone("Europe/Berlin")

    private fun at(zone: TimeZone, y: Int, m: Int, d: Int, h: Int, min: Int = 0): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(y, m - 1, d, h, min, 0)
        }.timeInMillis

    @Test
    fun midnightStartIsTheCalendarDate() {
        assertEquals("2026-09-29", DayClock.dateOf(at(india, 2026, 9, 29, 0, 1), 0, india))
        assertEquals("2026-09-28", DayClock.dateOf(at(india, 2026, 9, 28, 23, 59), 0, india))
    }

    @Test
    fun earlyHoursBelongToYesterdayWithALateStart() {
        assertEquals("2026-09-28", DayClock.dateOf(at(india, 2026, 9, 29, 2, 30), 4 * 60, india))
        assertEquals("2026-09-29", DayClock.dateOf(at(india, 2026, 9, 29, 4, 0), 4 * 60, india))
    }

    @Test
    fun dayStartsAtTheChosenTime() {
        assertEquals(at(india, 2026, 9, 29, 4, 30), DayClock.startOf("2026-09-29", 4 * 60 + 30, india))
    }

    @Test
    fun nextStartIsTomorrowAtTheChosenTime() {
        assertEquals(at(india, 2026, 9, 30, 4), DayClock.nextStart(at(india, 2026, 9, 29, 23), 4 * 60, india))
        assertEquals(at(india, 2026, 9, 29, 4), DayClock.nextStart(at(india, 2026, 9, 29, 2), 4 * 60, india))
    }

    @Test
    fun fractionOfDayCountsFromTheStart() {
        assertEquals(0.5, DayClock.fractionOfDay(at(india, 2026, 9, 29, 16), 4 * 60, india), 1e-9)
    }

    @Test
    fun daylightSavingDayIsTwentyThreeHours() {
        val halfway = at(berlin, 2026, 3, 29, 0) + 23 * 30 * 60_000L
        assertEquals(0.5, DayClock.fractionOfDay(halfway, 0, berlin), 1e-9)
    }
}
