package com.prateek.taro.util

import com.prateek.taro.energy.AscentTracker
import org.junit.Assert.assertEquals
import org.junit.Test

class StepRecordingTest {

    private val minute = MinuteRecorder.MINUTE_MS

    @Test
    fun batchedStepsSpreadAcrossMinutesWithoutLoss() {
        val recorder = MinuteRecorder()
        recorder.record(300, fromMs = 10 * minute, toMs = 13 * minute)
        assertEquals(mapOf(10 * minute to 100, 11 * minute to 100, 12 * minute to 100), recorder.drain())
        recorder.record(97, fromMs = 2 * minute + 17_000, toMs = 4 * minute + 43_000)
        assertEquals(97, recorder.drain().values.sum())

        val usualDay = DoubleArray(StepPaces.HOURS) { if (it in 8..19) 1000.0 else 0.0 }
        val past = (1L..7L).associate { java.time.LocalDate.of(2026, 11, 2).minusDays(it * 7) to usualDay }
        val today = DoubleArray(StepPaces.HOURS) { if (it in 8..11) 1500.0 else 0.0 }
        val pace = StepPaces.build(java.time.LocalDate.of(2026, 11, 2), today, 6000, past, now = 12.0, goal = 10_000)
        assertEquals(4000, pace.usualByNow)
        assertEquals(14_000, pace.projected)
        assertEquals(16.0, pace.goalHour!!, 1e-9)
        assertEquals(listOf(-1, 0, 1, 3, 6), listOf(9_999, 10_000, 12_500, 21_000, 45_000).map { StepTiers.reached(it, 10_000) })
        assertEquals(listOf(-1, 0, 2, 5, 5), listOf(2, 3, 14, 100, 365).map { Sprees.reached(it) })
        val tally = DayScores.score(steps = 12_852, goal = 10_000, streak = 22, walks = 3, earlySteps = 1_200, previousBest = 20_000)
        assertEquals(2.94, tally.mult, 0.0)
        assertEquals(37_785, tally.score)
        assertEquals(4.41, DayScores.score(12_852, 10_000, 22, 3, 1_200, 12_000).mult, 0.0)
    }

    @Test
    fun climbIsMeasuredAndCappedPerStep() {
        val tracker = AscentTracker()
        for (s in 0 until 600) tracker.addAltitude(s * 1000L, if (s < 60) 100.0 else 100.0 + minOf(s - 60, 300) * 0.1)
        assertEquals(30.0, tracker.drain().values.sum(), 1.5)
        assertEquals(2.0, AscentTracker.credited(ascentM = 15.0, steps = 10), 1e-9)
    }
}
