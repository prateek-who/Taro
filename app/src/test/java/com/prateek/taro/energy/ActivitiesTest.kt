package com.prateek.taro.energy

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivitiesTest {

    @Test
    fun customActivitiesRoundTrip() {
        val list = listOf(CustomActivity("Calisthenics", 4.5), CustomActivity("Climbing", 8.0))
        assertEquals(list, Activities.decode(Activities.encode(list)))
    }

    @Test
    fun customActivityNamesDropSeparators() {
        val encoded = Activities.encode(listOf(CustomActivity("Push|ups\n", 3.0)))
        assertEquals(listOf(CustomActivity("Pushups", 3.0)), Activities.decode(encoded))
    }

    @Test
    fun decodeSkipsBrokenRecords() {
        assertEquals(emptyList<CustomActivity>(), Activities.decode(null))
        assertEquals(listOf(CustomActivity("Dance", 5.0)), Activities.decode("junk\nDance|5.0\n|3.0\nX|abc"))
    }

    @Test
    fun addingSameNameReplacesIt() {
        val list = listOf(CustomActivity("Calisthenics", 3.0), CustomActivity("Climbing", 8.0))
        val updated = Activities.withAdded(list, CustomActivity("calisthenics", 8.0))
        assertEquals(listOf(CustomActivity("Climbing", 8.0), CustomActivity("calisthenics", 8.0)), updated)
    }

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
