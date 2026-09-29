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

    @Test
    fun windowsWithPoorGpsAreDropped() {
        val recorder = CalibrationRecorder()
        for (s in 0 until 30) {
            recorder.addStep(s * 1000L, 2)
            if (s % 2 == 0) recorder.addSpeed(s * 1000L, 1.5)
        }
        assertTrue(recorder.windows().isEmpty())
    }

    @Test
    fun patchyButUsableGpsStillProducesAWindow() {
        val recorder = CalibrationRecorder()
        for (s in 0 until 30) {
            recorder.addStep(s * 1000L, 2)
            if (s % 4 != 0) recorder.addSpeed(s * 1000L, 1.5)
        }
        val window = recorder.windows().single()
        assertEquals(0.75, window.stepLengthM, 1e-9)
    }

    @Test
    fun standingStillIsNotAWindow() {
        val recorder = CalibrationRecorder()
        for (s in 0 until 30) recorder.addSpeed(s * 1000L, 0.0)
        assertTrue(recorder.windows().isEmpty())
    }

    @Test
    fun fitUsesMedianWithoutCadenceSpread() {
        val fit = StepLengthModel.fit(
            listOf(CalibrationSample(110.0, 0.70), CalibrationSample(111.0, 0.74), CalibrationSample(112.0, 2.5)),
        )
        assertEquals(0.72, fit.walkingStepM!!, 1e-9)
        assertEquals(0.0, fit.walkingSlope, 1e-9)
        assertEquals(2, fit.walkingSamples)
    }

    @Test
    fun fitLearnsLongerStepsAtHigherCadence() {
        val samples = listOf(90.0, 100.0, 110.0, 120.0).map { CalibrationSample(it, 0.40 + 0.003 * it) }
        val fit = StepLengthModel.fit(samples)
        assertEquals(0.003, fit.walkingSlope, 1e-9)
        assertEquals(0.40 + 0.003 * fit.referenceCadence, fit.walkingStepM!!, 1e-9)
    }

    @Test
    fun runningIsFittedSeparately() {
        val fit = StepLengthModel.fit(
            listOf(CalibrationSample(105.0, 0.72), CalibrationSample(165.0, 1.05), CalibrationSample(170.0, 1.10)),
        )
        assertEquals(0.72, fit.walkingStepM!!, 1e-9)
        assertEquals(1.075, fit.runningStepM!!, 1e-9)
    }

    @Test
    fun noSamplesMeansNoFit() {
        val fit = StepLengthModel.fit(emptyList())
        assertNull(fit.walkingStepM)
        assertNull(fit.runningStepM)
    }
}
