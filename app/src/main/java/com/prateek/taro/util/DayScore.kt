package com.prateek.taro.util

import kotlin.math.roundToInt

enum class BonusKind { GOAL, TIER, STREAK, WALKS, EARLY, BEST }

data class Bonus(val kind: BonusKind, val value: Double, val count: Int = 0)

data class DayScore(val steps: Int, val bonuses: List<Bonus>, val mult: Double, val score: Int) {
    fun multAfter(shown: Int): Double = DayScores.mult(bonuses.take(shown))
}

object DayScores {
    const val GOAL_BONUS = 0.5
    const val TIER_BONUS = 0.5
    const val STREAK_DAY_BONUS = 0.02
    const val STREAK_MAX_BONUS = 1.0
    const val WALK_BONUS = 0.1
    const val EARLY_BONUS = 0.2
    const val BEST_TIMES = 1.5
    const val WALK_MINUTES = 10
    const val EARLY_HOUR = 8
    const val EARLY_STEPS = 1_000

    fun score(steps: Int, goal: Int, streak: Int, walks: Int, earlySteps: Int, previousBest: Int): DayScore {
        val tier = StepTiers.reached(steps, goal)
        val bonuses = buildList {
            if (tier >= 0) add(Bonus(BonusKind.GOAL, GOAL_BONUS))
            if (tier >= 1) add(Bonus(BonusKind.TIER, tier * TIER_BONUS, tier))
            if (streak > 0) add(Bonus(BonusKind.STREAK, minOf(streak * STREAK_DAY_BONUS, STREAK_MAX_BONUS), streak))
            if (walks > 0) add(Bonus(BonusKind.WALKS, walks * WALK_BONUS, walks))
            if (earlySteps >= EARLY_STEPS) add(Bonus(BonusKind.EARLY, EARLY_BONUS))
            if (previousBest > 0 && steps > previousBest) add(Bonus(BonusKind.BEST, BEST_TIMES))
        }
        val mult = mult(bonuses)
        return DayScore(steps, bonuses, mult, (steps * mult).roundToInt())
    }

    fun mult(bonuses: List<Bonus>): Double {
        val added = 1 + bonuses.filter { it.kind != BonusKind.BEST }.sumOf { it.value }
        val times = if (bonuses.any { it.kind == BonusKind.BEST }) BEST_TIMES else 1.0
        return (added * times * 100).roundToInt() / 100.0
    }
}
