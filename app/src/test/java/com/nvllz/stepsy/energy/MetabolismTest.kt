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

    @Test
    fun cutEatsBelowNeed() {
        assertEquals(1_800.0, Metabolism.calorieTarget(2_300.0, DietGoal.CUT, 500), 1e-9)
    }

    @Test
    fun bulkEatsAboveNeed() {
        assertEquals(2_600.0, Metabolism.calorieTarget(2_300.0, DietGoal.BULK, 300), 1e-9)
    }

    @Test
    fun maintainIgnoresAdjustment() {
        assertEquals(2_300.0, Metabolism.calorieTarget(2_300.0, DietGoal.MAINTAIN, 500), 1e-9)
    }

    @Test
    fun fiveHundredDeficitIsAboutHalfAKiloAWeek() {
        assertEquals(-0.4545, Metabolism.weeklyChangeKg(DietGoal.CUT, 500), 1e-4)
    }

    @Test
    fun digestionIsTenPercentOfTotal() {
        val total = Metabolism.withDigestion(1_800.0)
        assertEquals(total * 0.1, total - 1_800.0, 1e-9)
    }

    @Test
    fun eightHoursOfSleepSavesFivePercentOfAThird() {
        assertEquals(1_680.0 / 3 * 0.05, Metabolism.sleepSaving(1_680.0, 8 * 60L), 1e-9)
    }

    @Test
    fun noSleepSavesNothing() {
        assertEquals(0.0, Metabolism.sleepSaving(1_680.0, 0), 1e-9)
    }
}
