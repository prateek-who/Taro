package com.prateek.taro.energy

import org.junit.Assert.assertEquals
import org.junit.Test

class AscentTrackerTest {

    private fun feed(tracker: AscentTracker, fromS: Int, toS: Int, altitude: (Int) -> Double) {
        for (s in fromS until toS) tracker.addAltitude(s * 1000L, altitude(s))
    }

    @Test
    fun steadyClimbIsMeasured() {
        val tracker = AscentTracker()
        feed(tracker, 0, 600) { if (it < 60) 100.0 else 100.0 + minOf(it - 60, 300) * 0.1 }
        val total = tracker.drain().values.sum()
        assertEquals(30.0, total, 1.5)
    }

    @Test
    fun jitterDoesNotAddUp() {
        val tracker = AscentTracker()
        feed(tracker, 0, 3_600) { 50.0 + if (it % 2 == 0) 0.6 else -0.6 }
        assertEquals(0.0, tracker.drain().values.sum(), 1e-9)
    }

    @Test
    fun descentIsNotCounted() {
        val tracker = AscentTracker()
        feed(tracker, 0, 600) { 200.0 - it * 0.1 }
        assertEquals(0.0, tracker.drain().values.sum(), 1e-9)
    }

    @Test
    fun goingDownAndBackUpCountsTheClimbOnly() {
        val tracker = AscentTracker()
        feed(tracker, 0, 1_200) { if (it < 600) 100.0 - it * 0.02 else 88.0 + (it - 600) * 0.02 }
        assertEquals(12.0, tracker.drain().values.sum(), 1.5)
    }

    @Test
    fun liftRidesAreCappedByStepCount() {
        assertEquals(2.0, AscentTracker.credited(ascentM = 15.0, steps = 10), 1e-9)
        assertEquals(0.0, AscentTracker.credited(ascentM = 15.0, steps = 0), 1e-9)
        assertEquals(3.0, AscentTracker.credited(ascentM = 3.0, steps = 100), 1e-9)
    }
}
