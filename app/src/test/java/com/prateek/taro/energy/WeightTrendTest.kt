package com.prateek.taro.energy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class WeightTrendTest {

    private val start = LocalDate.of(2026, 9, 1)

    @Test
    fun trendAndEnergyFilterFollowTheScale() {
        val noisy = (0 until 28).map { day ->
            WeightPoint(start.plusDays(day.toLong()), 80.0 - 0.5 / 7 * day + if (day % 2 == 0) 0.8 else -0.8)
        }
        assertEquals(-0.5, WeightTrend.weeklyRate(WeightTrend.trend(noisy))!!, 0.12)
        assertNull(WeightTrend.weeklyRate(WeightTrend.trend(noisy.take(3))))

        val random = Random(7)
        val restingAt = { kg: Double -> Metabolism.restingKcalPerDay(kg, 175.0, 30, Sex.MALE) }
        var tissue = 80.0
        val days = (0 until 60).map { day ->
            val active = 150 + random.nextDouble() * 550
            val intake = 1_900 + random.nextDouble() * 900
            val weight = tissue + (random.nextDouble() - 0.5) * 0.8
            tissue += (intake - 1.15 * restingAt(tissue) - active - 0.1 * intake) / Metabolism.KCAL_PER_KG
            FilterDay(start.plusDays(day.toLong()), intake.takeIf { day % 7 != 3 }, null, 420, active, 0.0, weight.takeIf { day % 5 != 2 })
        }
        val model = EnergyFilter.run(days, restingAt)!!
        val truth = Metabolism.withDigestion(1.15 * restingAt(tissue) + 400)
        val formula = Metabolism.withDigestion(restingAt(tissue) + 400)
        assertEquals(truth, model.factors.need(restingAt(model.latest.tissue), 400.0), 120.0)
        assertTrue(truth - formula > 250)
        assertNull(EnergyFilter.run(days.map { it.copy(weight = null) }, restingAt))

        var lifter = 80.0
        val training = (0 until 120).map { day ->
            val workout = if ((day / 21) % 2 == 1 && day % 7 != 6) 500.0 else 0.0
            val intake = 2_000 + random.nextDouble() * 800
            val weight = lifter + (random.nextDouble() - 0.5) * 0.8
            lifter += (intake - restingAt(lifter) - 300 - 0.65 * workout - 0.1 * intake) / Metabolism.KCAL_PER_KG
            FilterDay(start.plusDays(day.toLong()), intake, null, 420, 300.0, workout, weight)
        }
        val learned = EnergyFilter.run(training, restingAt)!!.factors
        assertEquals(0.65, learned.workout, 0.25)
        assertTrue(learned.workoutSd < EnergyFilter.PRIOR_WORKOUT_SD * 0.8)
    }

    @Test
    fun paceVerdictFollowsTheGoal() {
        assertEquals(PaceStatus.ON_TRACK, WeightTrend.assess(-0.45, DietGoal.CUT, 500, 80.0).status)
        assertEquals(PaceStatus.TOO_FAST, WeightTrend.assess(-0.9, DietGoal.CUT, 500, 80.0).status)
        assertEquals(PaceStatus.WRONG_WAY, WeightTrend.assess(0.2, DietGoal.CUT, 500, 80.0).status)
        assertEquals(PaceStatus.NOT_ENOUGH_DATA, WeightTrend.assess(null, DietGoal.CUT, 500, 80.0).status)
    }
}
