package com.nvllz.stepsy.calibration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedCalibrationTest {

    @Test
    fun phasesAdvanceOnTheirOwn() {
        assertEquals(GuidedPhase.USUAL, GuidedPlan.progress(0, emptyList()).phase)
        assertEquals(1, GuidedPlan.progress(179, emptyList()).secondsLeft)
        assertEquals(GuidedPhase.RELAXED, GuidedPlan.progress(180, emptyList()).phase)
        assertEquals(GuidedPhase.BRISK, GuidedPlan.progress(300, emptyList()).phase)
        assertNull(GuidedPlan.progress(420, emptyList()).phase)
    }

    @Test
    fun nextEndsOnlyTheCurrentPhase() {
        val skipped = GuidedPlan.progress(61, listOf(60L))
        assertEquals(GuidedPhase.RELAXED, skipped.phase)
        assertEquals(119, skipped.secondsLeft)
    }

    @Test
    fun twoTapsSkipTwoPhases() {
        assertEquals(GuidedPhase.BRISK, GuidedPlan.progress(71, listOf(60L, 70L)).phase)
    }

    @Test
    fun liveCadenceFromRecentSteps() {
        val recorder = CalibrationRecorder()
        for (s in 0 until 60) recorder.addStep(s * 1000L, 2)
        assertEquals(120, recorder.recentCadence(59_000))
    }

    @Test
    fun cadenceNeedsAFewSeconds() {
        val recorder = CalibrationRecorder()
        recorder.addStep(0, 2)
        assertNull(recorder.recentCadence(2_000))
    }

    @Test
    fun paceRangeNeedsVariety() {
        val steady = listOf(118.0, 120.0, 121.0).map { CalibrationSample(it, 0.78) }
        val varied = listOf(100.0, 112.0, 124.0).map { CalibrationSample(it, 0.75) }
        assertFalse(StepLengthModel.coversPaceRange(steady))
        assertTrue(StepLengthModel.coversPaceRange(varied))
    }
}
