package com.prateek.taro.energy

import org.junit.Assert.assertEquals
import org.junit.Test

class EnergyModelTest {

    private val body = Body(weightKg = 70.0, walkingStepM = 0.75, runningStepM = 1.0)

    @Test
    fun walkingCostsHalfKcalPerKgPerKm() {
        val kcal = EnergyModel.minuteKcal(body, MinuteSample(steps = 100))
        val km = EnergyModel.metresPerMinute(body, 100) / 1000.0
        assertEquals(0.5, kcal / km / body.weightKg, 1e-9)
    }

    @Test
    fun runningCostsOneKcalPerKgPerKm() {
        val kcal = EnergyModel.minuteKcal(body, MinuteSample(steps = 160))
        val km = EnergyModel.metresPerMinute(body, 160) / 1000.0
        assertEquals(1.0, kcal / km / body.weightKg, 1e-9)
    }

    @Test
    fun climbingAddsVerticalCost() {
        val flat = EnergyModel.minuteKcal(body, MinuteSample(steps = 100))
        val uphill = EnergyModel.minuteKcal(body, MinuteSample(steps = 100, ascentM = 5.0))
        assertEquals(1.8 * 5.0 * 70.0 / 1000.0 * 5.0, uphill - flat, 1e-9)
    }

    @Test
    fun descentIsNotNegative() {
        val flat = EnergyModel.minuteKcal(body, MinuteSample(steps = 100))
        val downhill = EnergyModel.minuteKcal(body, MinuteSample(steps = 100, ascentM = -5.0))
        assertEquals(flat, downhill, 1e-9)
    }

    @Test
    fun unrecordedStepsFallBackToWalkingCost() {
        val minutes = listOf(MinuteSample(100), MinuteSample(100))
        val withGap = EnergyModel.dayKcal(body, minutes, totalSteps = 1_200)
        val expected = EnergyModel.minuteKcal(body, MinuteSample(100)) * 2 + EnergyModel.walkingStepsKcal(body, 1_000)
        assertEquals(expected, withGap, 1e-9)
    }

    @Test
    fun tenThousandWalkingStepsForAverageAdult() {
        assertEquals(262.5, EnergyModel.walkingStepsKcal(body, 10_000), 1e-9)
    }

    @Test
    fun recordedStepsAboveDailyTotalAreNotSubtracted() {
        val kcal = EnergyModel.dayKcal(body, listOf(MinuteSample(120)), totalSteps = 100)
        assertEquals(EnergyModel.minuteKcal(body, MinuteSample(120)), kcal, 1e-9)
    }
}
