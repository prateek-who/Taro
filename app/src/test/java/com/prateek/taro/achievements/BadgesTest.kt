package com.prateek.taro.achievements

import com.prateek.taro.util.GoalHistory
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

        val more = inputs.copy(
            days = inputs.days + DayStat(start.plusDays(7), 4_960),
            claimed = mapOf("first_pullup" to start.plusDays(3)),
        )
        assertEquals(start.plusDays(7), result(more, "zeno").progress.earnedOn)
        assertEquals(start.plusDays(3), result(more, "first_pullup").progress.earnedOn)
        assertNull(result(more, "muscle_up").progress.earnedOn)
        assertEquals(Badges.evaluate(more).count { it.earned && it.def.id != "achievement_hunter" && it.def.id != "platinum" }.toDouble(), result(more, "achievement_hunter").progress.value, 0.0)
        val history = GoalHistory.decode(GoalHistory.changed(null, 5_000, 20_000, start.plusDays(6)), 20_000)
        assertEquals(5_000, history.on(start))
        assertEquals(start.plusDays(3), result(inputs.copy(goal = 20_000, goals = history), "streak_3").progress.earnedOn)
        assertNull(result(inputs.copy(goal = 20_000), "streak_3").progress.earnedOn)
        val minute = 60_000L
        assertEquals(3, AchievementData.longestWalk(listOf(0L to 80, minute to 90, 2 * minute to 70, 3 * minute to 10, 5 * minute to 100)))
    }
}
