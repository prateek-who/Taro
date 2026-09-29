package com.prateek.taro.achievements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BadgesTest {

    private val start = LocalDate.of(2026, 9, 1)

    private fun result(inputs: AchievementInputs, id: String) = Badges.evaluate(inputs).single { it.def.id == id }

    @Test
    fun thresholdsStreaksAndEarnedDates() {
        val steps = listOf(4_000, 6_000, 12_000, 6_500, 6_000, 3_000, 16_000)
        val inputs = AchievementInputs(
            days = steps.mapIndexed { i, s -> DayStat(start.plusDays(i.toLong()), s, ascentM = 120.0) },
            goal = 5_000,
        )
        assertEquals(start.plusDays(2), result(inputs, "day_10k").progress.earnedOn)
        assertEquals(start.plusDays(3), result(inputs, "streak_3").progress.earnedOn)
        assertEquals(4.0, result(inputs, "streak_7").progress.value, 0.0)
        assertNull(result(inputs, "streak_7").progress.earnedOn)
        assertEquals(start.plusDays(2), result(inputs, "double_goal").progress.earnedOn)
        assertEquals(start.plusDays(2), result(inputs, "eiffel").progress.earnedOn)
        assertEquals(0.84, result(inputs, "burj").progress.value / 1_000, 0.001)
    }
}
