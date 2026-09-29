package com.prateek.taro.sleep

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SleepDatesTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val wake = LocalDate.of(2026, 9, 29)
    private val minute = 60_000L

    private fun minutes(span: SleepSpan) = (span.end - span.start) / minute

    @Test
    fun bedtimeBeforeMidnightStartsTheDayBefore() {
        val span = SleepDates.manualSpan(wake, 22 * 60, 6 * 60, zone)
        assertEquals(8 * 60L, minutes(span))
        assertEquals(LocalDate.of(2026, 9, 28), SleepDates.wakeDateOf(span.start, zone))
        assertEquals(wake, SleepDates.wakeDateOf(span.end, zone))
    }

    @Test
    fun bedtimeAfterMidnightStaysOnTheWakeDay() {
        val span = SleepDates.manualSpan(wake, 30, 7 * 60, zone)
        assertEquals(6 * 60L + 30, minutes(span))
        assertEquals(wake, SleepDates.wakeDateOf(span.start, zone))
    }

    @Test
    fun daylightSavingNightUsesRealElapsedTime() {
        val berlin = ZoneId.of("Europe/Berlin")
        val span = SleepDates.manualSpan(LocalDate.of(2026, 3, 29), 23 * 60, 7 * 60, berlin)
        assertEquals(7 * 60L, minutes(span))
    }

    @Test
    fun wakeDateIsTheCalendarDateOfWaking() {
        val span = SleepDates.manualSpan(wake, 23 * 60, 5 * 60 + 45, zone)
        assertEquals(wake, SleepDates.wakeDateOf(span.end, zone))
    }
}
