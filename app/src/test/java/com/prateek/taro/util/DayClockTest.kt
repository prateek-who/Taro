package com.prateek.taro.util

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
    }
}
