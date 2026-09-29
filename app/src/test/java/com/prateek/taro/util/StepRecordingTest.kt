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
    }

    @Test
    fun climbIsMeasuredAndCappedPerStep() {
        val tracker = AscentTracker()
        for (s in 0 until 600) tracker.addAltitude(s * 1000L, if (s < 60) 100.0 else 100.0 + minOf(s - 60, 300) * 0.1)
        assertEquals(30.0, tracker.drain().values.sum(), 1.5)
        assertEquals(2.0, AscentTracker.credited(ascentM = 15.0, steps = 10), 1e-9)
    }
}
