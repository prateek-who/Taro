package com.prateek.taro.energy

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyEnergyTest {

    @Test
    fun untrackedDaysStayEmpty() {
        val day = DailyEnergy.pastDayBurn("2026-09-20", restingPerDay = 1_700.0, tracked = false, active = 0.0, logged = 0.0)
        assertEquals(0.0, day.total, 1e-9)
    }

    @Test
    fun trackedDaysIncludeFullRestingBurn() {
        val day = DailyEnergy.pastDayBurn("2026-09-28", restingPerDay = 1_700.0, tracked = true, active = 250.0, logged = 0.0)
        assertEquals(1_950.0 / 0.9, day.total, 1e-9)
        assertEquals(1_950.0 / 9, day.digestion, 1e-9)
    }

    @Test
    fun aLoggedWorkoutMakesTheDayTracked() {
        val day = DailyEnergy.pastDayBurn("2026-09-27", restingPerDay = 1_700.0, tracked = false, active = 0.0, logged = 300.0)
        assertEquals(2_000.0 / 0.9, day.total, 1e-9)
    }
}
