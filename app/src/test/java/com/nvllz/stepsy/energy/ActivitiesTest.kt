package com.nvllz.stepsy.energy

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivitiesTest {

    @Test
    fun estimateExcludesRestingBurn() {
        assertEquals(406.0, Activities.estimateActiveKcal(met = 6.8, weightKg = 70.0, minutes = 60), 1e-9)
    }

    @Test
    fun halfAnHourOfYoga() {
        assertEquals(52.5, Activities.estimateActiveKcal(met = 2.5, weightKg = 70.0, minutes = 30), 1e-9)
    }

    @Test
    fun metBelowRestingNeverGoesNegative() {
        assertEquals(0.0, Activities.estimateActiveKcal(met = 0.8, weightKg = 70.0, minutes = 60), 1e-9)
    }
}
