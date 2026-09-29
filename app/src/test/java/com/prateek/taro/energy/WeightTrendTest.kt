package com.prateek.taro.energy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WeightTrendTest {

    private val start = LocalDate.of(2026, 9, 1)

    @Test
    fun trendSmoothsNoiseAndNeedsEnoughData() {
        val noisy = (0 until 28).map { day ->
            WeightPoint(start.plusDays(day.toLong()), 80.0 - 0.5 / 7 * day + if (day % 2 == 0) 0.8 else -0.8)
        }
        assertEquals(-0.5, WeightTrend.weeklyRate(WeightTrend.trend(noisy))!!, 0.12)
        assertNull(WeightTrend.weeklyRate(WeightTrend.trend(noisy.take(3))))
    }

    @Test
    fun paceVerdictFollowsTheGoal() {
        assertEquals(PaceStatus.ON_TRACK, WeightTrend.assess(-0.45, DietGoal.CUT, 500, 80.0).status)
        assertEquals(PaceStatus.TOO_FAST, WeightTrend.assess(-0.9, DietGoal.CUT, 500, 80.0).status)
        assertEquals(PaceStatus.WRONG_WAY, WeightTrend.assess(0.2, DietGoal.CUT, 500, 80.0).status)
        assertEquals(PaceStatus.NOT_ENOUGH_DATA, WeightTrend.assess(null, DietGoal.CUT, 500, 80.0).status)
    }
}
