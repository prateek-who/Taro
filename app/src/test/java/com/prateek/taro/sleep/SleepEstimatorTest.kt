package com.prateek.taro.sleep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SleepEstimatorTest {

    private val hour = 60 * 60 * 1000L
    private val minute = 60 * 1000L
    private val start = 18 * hour
    private val end = 39 * hour

    private fun off(h: Double) = ScreenState((h * hour).toLong(), false)
    private fun on(h: Double) = ScreenState((h * hour).toLong(), true)

    @Test
    fun shortNapIsNotANight() {
        assertEquals(false, SleepEstimator.isPlausibleNight(11 * hour, 11 * hour + 42 * minute))
        assertEquals(true, SleepEstimator.isPlausibleNight(23 * hour, 30 * hour))
    }

    @Test
    fun simpleNight() {
        val sleep = SleepEstimator.estimate(start, end, listOf(on(17.0), off(23.5), on(31.0)), emptyMap())
        assertEquals(SleepWindow((23.5 * hour).toLong(), 31 * hour), sleep)
    }

    @Test
    fun briefCheckAndBathroomTripDoNotSplitTheNight() {
        val screen = listOf(on(17.0), off(23.0), on(27.0), off(27.0 + 2.0 / 60), on(31.0))
        val steps = mapOf(27 * hour + 5 * minute to 25, 27 * hour + 6 * minute to 20)
        val sleep = SleepEstimator.estimate(start, end, screen, steps)
        assertEquals(SleepWindow(23 * hour, 31 * hour), sleep)
    }

    @Test
    fun eveningTvBreakIsNotPartOfSleep() {
        val screen = listOf(on(17.0), off(20.0), on(21.0), off(23.0), on(30.5))
        val sleep = SleepEstimator.estimate(start, end, screen, emptyMap())
        assertEquals(SleepWindow(23 * hour, (30.5 * hour).toLong()), sleep)
    }

    @Test
    fun walkingWithTheScreenOffIsAwake() {
        val screen = listOf(on(17.0), off(19.0), on(31.0))
        val walk = (0 until 90).associate { 19 * hour + it * minute to 100 }
        val sleep = SleepEstimator.estimate(start, end, screen, walk)
        assertEquals((20.5 * hour).toLong(), sleep!!.start)
    }

    @Test
    fun screenAlreadyOffBeforeTheWindowCounts() {
        val sleep = SleepEstimator.estimate(start, end, listOf(off(16.0), on(22.0), off(23.0), on(30.0)), emptyMap())
        assertEquals(SleepWindow(23 * hour, 30 * hour), sleep)
    }

    @Test
    fun noLongIdleBlockMeansNoSleep() {
        val screen = (0 until 20).flatMap { listOf(on(18.0 + it), off(18.0 + it + 0.2)) }
        assertNull(SleepEstimator.estimate(start, end, screen, emptyMap()))
    }
}
