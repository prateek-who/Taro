package com.prateek.taro.sleep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.util.Calendar
import java.util.TimeZone

class SleepInsightsTest {

    private val zone = TimeZone.getTimeZone("Asia/Kolkata")
    private val today = LocalDate.of(2026, 9, 29)

    private fun night(daysAgo: Long, bedHour: Int, bedMinute: Int, hours: Double): Night {
        val wake = today.minusDays(daysAgo)
        val bedDay = if (bedHour >= 12) wake.minusDays(1) else wake
        val start = Calendar.getInstance(zone).apply {
            clear()
            set(bedDay.year, bedDay.monthValue - 1, bedDay.dayOfMonth, bedHour, bedMinute, 0)
        }.timeInMillis
        return Night(wake, start, start + (hours * 3_600_000).toLong())
    }

    @Test
    fun statusBands() {
        assertEquals(SleepStatus.SHORT, SleepInsights.status(5 * 60L))
        assertEquals(SleepStatus.A_BIT_SHORT, SleepInsights.status(6 * 60L + 30))
        assertEquals(SleepStatus.HEALTHY, SleepInsights.status(8 * 60L))
        assertEquals(SleepStatus.LONG, SleepInsights.status(10 * 60L))
    }

    @Test
    fun averageOfNights() {
        assertEquals(450L, SleepInsights.average(listOf(night(0, 23, 0, 7.0), night(1, 23, 0, 8.0))))
        assertNull(SleepInsights.average(emptyList()))
    }

    @Test
    fun bedtimesAcrossMidnightAreClose() {
        val nights = listOf(night(0, 23, 50, 7.5), night(1, 0, 10, 7.5), night(2, 0, 0, 7.5))
        assertEquals(8L, SleepInsights.bedtimeSpreadMinutes(nights, zone))
    }

    @Test
    fun irregularBedtimesSpreadWide() {
        val nights = listOf(night(0, 22, 0, 8.0), night(1, 1, 30, 7.0), night(2, 23, 0, 7.5))
        assertEquals(88L, SleepInsights.bedtimeSpreadMinutes(nights, zone))
    }

    @Test
    fun streakCountsConsecutiveHealthyNights() {
        val nights = listOf(night(0, 23, 0, 7.5), night(1, 23, 0, 8.0), night(2, 23, 0, 5.0), night(3, 23, 0, 8.0))
        assertEquals(2, SleepInsights.streak(nights, today))
    }

    @Test
    fun streakStartsFromLastNightIfTodayIsNotLoggedYet() {
        val nights = listOf(night(1, 23, 0, 7.5), night(2, 23, 0, 8.0))
        assertEquals(2, SleepInsights.streak(nights, today))
    }
}
