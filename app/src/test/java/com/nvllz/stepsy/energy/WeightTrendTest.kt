package com.nvllz.stepsy.energy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WeightTrendTest {

    private val start = LocalDate.of(2026, 9, 1)

    private fun daily(vararg kg: Double) = kg.mapIndexed { day, value -> WeightPoint(start.plusDays(day.toLong()), value) }

    private fun steadyLoss(days: Int, from: Double, perWeek: Double) =
        (0 until days).map { WeightPoint(start.plusDays(it.toLong()), from + perWeek / 7 * it) }

    @Test
    fun trendStartsAtTheFirstReading() {
        assertEquals(80.0, WeightTrend.trend(daily(80.0)).single().trend, 1e-9)
    }

    @Test
    fun trendMovesTenPercentPerDay() {
        assertEquals(79.9, WeightTrend.trend(daily(80.0, 79.0)).last().trend, 1e-9)
    }

    @Test
    fun aGapPullsTheTrendHarder() {
        val gap = listOf(WeightPoint(start, 80.0), WeightPoint(start.plusDays(3), 79.0))
        assertEquals(80.0 - (1 - 0.9 * 0.9 * 0.9), WeightTrend.trend(gap).last().trend, 1e-9)
    }

    @Test
    fun noisyScaleStillShowsTheRealDirection() {
        val noisy = steadyLoss(28, 80.0, -0.5).mapIndexed { i, p -> p.copy(kg = p.kg + if (i % 2 == 0) 0.8 else -0.8) }
        val rate = WeightTrend.weeklyRate(WeightTrend.trend(noisy))!!
        assertEquals(-0.5, rate, 0.12)
    }

    @Test
    fun rateNeedsEnoughReadings() {
        assertNull(WeightTrend.weeklyRate(WeightTrend.trend(daily(80.0, 79.9, 79.8))))
    }

    @Test
    fun rateNeedsTenDaysOfSpan() {
        assertNull(WeightTrend.weeklyRate(WeightTrend.trend(steadyLoss(8, 80.0, -0.5))))
    }

    @Test
    fun cutOnPlanIsOnTrack() {
        assertEquals(PaceStatus.ON_TRACK, WeightTrend.assess(-0.45, DietGoal.CUT, 500, 80.0).status)
    }

    @Test
    fun cutTooSlowAndTooFast() {
        assertEquals(PaceStatus.SLOWER, WeightTrend.assess(-0.15, DietGoal.CUT, 500, 80.0).status)
        assertEquals(PaceStatus.FASTER, WeightTrend.assess(-0.7, DietGoal.CUT, 500, 80.0).status)
        assertEquals(PaceStatus.TOO_FAST, WeightTrend.assess(-0.9, DietGoal.CUT, 500, 80.0).status)
    }

    @Test
    fun gainingOnACutIsTheWrongWay() {
        assertEquals(PaceStatus.WRONG_WAY, WeightTrend.assess(0.2, DietGoal.CUT, 500, 80.0).status)
    }

    @Test
    fun bulkGainingTooFast() {
        assertEquals(PaceStatus.TOO_FAST, WeightTrend.assess(0.6, DietGoal.BULK, 300, 80.0).status)
        assertEquals(PaceStatus.ON_TRACK, WeightTrend.assess(0.3, DietGoal.BULK, 300, 80.0).status)
    }

    @Test
    fun maintainingSteadyOrDrifting() {
        assertEquals(PaceStatus.STEADY, WeightTrend.assess(-0.1, DietGoal.MAINTAIN, 0, 80.0).status)
        assertEquals(PaceStatus.DRIFTING, WeightTrend.assess(0.4, null, 0, 80.0).status)
    }

    @Test
    fun noRateMeansNotEnoughData() {
        assertEquals(PaceStatus.NOT_ENOUGH_DATA, WeightTrend.assess(null, DietGoal.CUT, 500, 80.0).status)
    }
}
