package com.nvllz.stepsy

import com.nvllz.stepsy.util.MinuteRecorder
import org.junit.Assert.assertEquals
import org.junit.Test

class MinuteRecorderTest {

    private val minute = MinuteRecorder.MINUTE_MS

    @Test
    fun stepsWithinOneMinuteStayInThatMinute() {
        val recorder = MinuteRecorder()
        recorder.record(10, fromMs = 5 * minute + 1_000, toMs = 5 * minute + 6_000)
        assertEquals(mapOf(5 * minute to 10), recorder.drain())
    }

    @Test
    fun batchedStepsSpreadAcrossMinutes() {
        val recorder = MinuteRecorder()
        recorder.record(300, fromMs = 10 * minute, toMs = 13 * minute)
        val drained = recorder.drain()
        assertEquals(mapOf(10 * minute to 100, 11 * minute to 100, 12 * minute to 100), drained)
    }

    @Test
    fun spreadingNeverLosesOrInventsSteps() {
        val recorder = MinuteRecorder()
        recorder.record(97, fromMs = 2 * minute + 17_000, toMs = 4 * minute + 43_000)
        assertEquals(97, recorder.drain().values.sum())
    }

    @Test
    fun impossibleCadenceIsLeftUnassigned() {
        val recorder = MinuteRecorder()
        recorder.record(2_000, fromMs = minute, toMs = 2 * minute)
        assertEquals(emptyMap<Long, Int>(), recorder.drain())
    }

    @Test
    fun longGapWithManyStepsIsLeftUnassigned() {
        val recorder = MinuteRecorder()
        recorder.record(1_500, fromMs = 0, toMs = 120 * minute)
        assertEquals(emptyMap<Long, Int>(), recorder.drain())
    }

    @Test
    fun unknownStartPutsSmallDeltaInCurrentMinute() {
        val recorder = MinuteRecorder()
        recorder.record(40, fromMs = null, toMs = 7 * minute + 30_000)
        assertEquals(mapOf(7 * minute to 40), recorder.drain())
    }

    @Test
    fun drainClearsPendingMinutes() {
        val recorder = MinuteRecorder()
        recorder.record(5, fromMs = null, toMs = minute)
        recorder.drain()
        assertEquals(emptyMap<Long, Int>(), recorder.drain())
    }
}
