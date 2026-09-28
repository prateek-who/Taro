package com.nvllz.stepsy.energy

import org.junit.Assert.assertEquals
import org.junit.Test

class MetabolismTest {

    @Test
    fun mifflinStJeorMale() {
        assertEquals(1_648.75, Metabolism.restingKcalPerDay(70.0, 175.0, 30, Sex.MALE), 1e-9)
    }

    @Test
    fun mifflinStJeorFemale() {
        assertEquals(1_295.25, Metabolism.restingKcalPerDay(60.0, 165.0, 35, Sex.FEMALE), 1e-9)
    }

    @Test
    fun unspecifiedSitsBetweenMaleAndFemale() {
        val male = Metabolism.restingKcalPerDay(70.0, 175.0, 30, Sex.MALE)
        val female = Metabolism.restingKcalPerDay(70.0, 175.0, 30, Sex.FEMALE)
        assertEquals((male + female) / 2, Metabolism.restingKcalPerDay(70.0, 175.0, 30, Sex.UNSPECIFIED), 1.0)
    }

    @Test
    fun restingAccruesThroughTheDay() {
        assertEquals(800.0, Metabolism.restingSoFar(1_600.0, 0.5), 1e-9)
        assertEquals(1_600.0, Metabolism.restingSoFar(1_600.0, 1.4), 1e-9)
    }

    @Test
    fun dailyNeedAddsDigestion() {
        assertEquals(2_000.0, Metabolism.dailyNeed(1_500.0, 300.0), 1e-9)
    }

    @Test
    fun sexKeysRoundTrip() {
        Sex.entries.forEach { assertEquals(it, Sex.fromKey(it.key)) }
    }
}
