package com.prateek.taro.energy

import org.junit.Assert.assertEquals
import org.junit.Test

class EnergyTest {

    private val body = Body(weightKg = 70.0, walkingStepM = 0.75, runningStepM = 1.0)

    @Test
    fun walkingFollowsHeightAndSpeedAndRunningFollowsAcsm() {
        val runKm = EnergyModel.metresPerMinute(body, 160) / 1000.0
        val speed = 100 * 0.75 / 60
        val steadyMinute = (3.85 + 5.97 * speed * speed / 1.7) * 70.0 / 1000 * 5
        assertEquals(steadyMinute, EnergyModel.minuteKcal(body, MinuteSample(steps = 100)), 1e-9)
        assertEquals(EnergyModel.minuteKcal(body, MinuteSample(steps = 80)) / 4, EnergyModel.minuteKcal(body, MinuteSample(steps = 20)), 1e-9)
        assertEquals(1.0, EnergyModel.minuteKcal(body, MinuteSample(steps = 160)) / runKm / body.weightKg, 1e-9)
        assertEquals(EnergyModel.minuteKcal(body, MinuteSample(steps = 80)) * 125, EnergyModel.walkingStepsKcal(body, 10_000), 1e-9)
    }

    @Test
    fun restingBurnNeedAndCalorieTarget() {
        assertEquals(1_648.75, Metabolism.restingKcalPerDay(70.0, 175.0, 30, Sex.MALE), 1e-9)
        assertEquals(1_295.25, Metabolism.restingKcalPerDay(60.0, 165.0, 35, Sex.FEMALE), 1e-9)
        assertEquals(2_000.0, Metabolism.dailyNeed(1_500.0, 300.0), 1e-9)
        assertEquals(1_800.0, Metabolism.calorieTarget(2_300.0, DietGoal.CUT, 500), 1e-9)
        assertEquals(2_600.0, Metabolism.calorieTarget(2_300.0, DietGoal.BULK, 300), 1e-9)
    }

    @Test
    fun workoutsAreNetOfRestingAndCustomActivitiesRoundTrip() {
        assertEquals(406.0, Activities.estimateActiveKcal(met = 6.8, weightKg = 70.0, minutes = 60), 1e-9)
        assertEquals(0.0, Activities.estimateActiveKcal(met = 0.8, weightKg = 70.0, minutes = 60), 1e-9)
        val saved = listOf(CustomActivity("Calisthenics", 4.5), CustomActivity("Push|ups", 8.0))
        assertEquals(
            listOf(CustomActivity("Calisthenics", 4.5), CustomActivity("Pushups", 8.0)),
            Activities.decode(Activities.encode(saved)),
        )
    }
}
