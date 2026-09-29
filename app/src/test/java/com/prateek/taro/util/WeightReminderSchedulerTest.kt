package com.prateek.taro.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class WeightReminderSchedulerTest {

    private val zone = TimeZone.getTimeZone("Asia/Kolkata")

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int = 0): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(y, m - 1, d, h, min, 0)
        }.timeInMillis

    private fun daily(minute: Int) = WeightReminder(enabled = true, weekly = false, dayOfWeek = Calendar.MONDAY, minuteOfDay = minute)

    @Test
    fun dailyLaterTodayFiresToday() {
        assertEquals(at(2026, 9, 29, 8), WeightReminderScheduler.nextTrigger(at(2026, 9, 29, 6), daily(8 * 60), zone))
    }

    @Test
    fun dailyAlreadyPassedFiresTomorrow() {
        assertEquals(at(2026, 9, 30, 8), WeightReminderScheduler.nextTrigger(at(2026, 9, 29, 9), daily(8 * 60), zone))
    }

    @Test
    fun weeklyWaitsForTheChosenDay() {
        val sunday = WeightReminder(enabled = true, weekly = true, dayOfWeek = Calendar.SUNDAY, minuteOfDay = 7 * 60 + 30)
        assertEquals(at(2026, 10, 4, 7, 30), WeightReminderScheduler.nextTrigger(at(2026, 9, 29, 12), sunday, zone))
    }

    @Test
    fun weeklyOnTheDayButLaterWaitsAWeek() {
        val tuesday = WeightReminder(enabled = true, weekly = true, dayOfWeek = Calendar.TUESDAY, minuteOfDay = 8 * 60)
        assertEquals(at(2026, 10, 6, 8), WeightReminderScheduler.nextTrigger(at(2026, 9, 29, 9), tuesday, zone))
    }
}
