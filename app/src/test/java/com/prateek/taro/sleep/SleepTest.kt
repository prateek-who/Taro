package com.prateek.taro.sleep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SleepTest {

    private val hour = 60 * 60 * 1000L
    private val minute = 60 * 1000L

    private fun off(h: Double) = ScreenState((h * hour).toLong(), false)
    private fun on(h: Double) = ScreenState((h * hour).toLong(), true)

    @Test
    fun overnightIsFoundDespiteABathroomTrip() {
        val screen = listOf(on(17.0), off(23.0), on(27.0), off(27.0 + 2.0 / 60), on(31.0))
        val steps = mapOf(27 * hour + 5 * minute to 25, 27 * hour + 6 * minute to 20)
        assertEquals(SleepWindow(23 * hour, 31 * hour), SleepEstimator.estimate(18 * hour, 39 * hour, screen, steps))
    }

    @Test
    fun napsGapsAndMissingDataAreNotNights() {
        assertFalse(SleepEstimator.isPlausibleNight(11 * hour, 11 * hour + 42 * minute))
        assertFalse(SleepEstimator.isPlausibleNight(22 * hour, 36 * hour + 15 * minute))
        assertNull(SleepEstimator.estimate(18 * hour, 39 * hour, listOf(on(36.3), off(36.5)), emptyMap()))
    }

    @Test
    fun manualSpansCrossMidnightAndDaylightSaving() {
        val zone = ZoneId.of("Asia/Kolkata")
        val night = SleepDates.manualSpan(LocalDate.of(2026, 9, 29), 22 * 60, 6 * 60, zone)
        assertEquals(8 * 60L, (night.end - night.start) / minute)
        assertEquals(LocalDate.of(2026, 9, 29), SleepDates.wakeDateOf(night.end, zone))
        val dst = SleepDates.manualSpan(LocalDate.of(2026, 3, 29), 23 * 60, 7 * 60, ZoneId.of("Europe/Berlin"))
        assertEquals(7 * 60L, (dst.end - dst.start) / minute)
    }
}
