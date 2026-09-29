package com.prateek.taro.calibration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationTest {

    private fun walk(recorder: CalibrationRecorder, seconds: Int, stepsPerSecond: Int, speed: Double, fromS: Int = 0) {
        for (s in fromS until fromS + seconds) {
            recorder.addStep(s * 1000L + 200, stepsPerSecond)
            recorder.addSpeed(s * 1000L + 500, speed)
        }
    }

    @Test
    fun steadyWalkProducesAccurateWindows() {
        val recorder = CalibrationRecorder()
        walk(recorder, seconds = 90, stepsPerSecond = 2, speed = 1.5)
        val windows = recorder.windows()
        assertEquals(3, windows.size)
        windows.forEach {
            assertEquals(120.0, it.cadence, 1e-9)
            assertEquals(0.75, it.stepLengthM, 1e-9)
        }
    }
}
