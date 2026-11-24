package com.prateek.taro.util

import kotlin.math.roundToInt

object StepTiers {
    val MULTIPLES = listOf(1.0, 1.25, 1.5, 2.0, 2.5, 3.0, 4.0)
    val LAST = MULTIPLES.lastIndex

    fun reached(steps: Int, goal: Int): Int = if (goal <= 0) -1 else MULTIPLES.indexOfLast { steps >= threshold(it, goal) }

    fun threshold(multiple: Double, goal: Int): Int = (goal * multiple).roundToInt()

    fun threshold(index: Int, goal: Int): Int = threshold(MULTIPLES[index], goal)
}

object Sprees {
    val DAYS = listOf(3, 7, 14, 30, 60, 100)
    val LAST = DAYS.lastIndex

    fun reached(days: Int): Int = DAYS.indexOfLast { days >= it }
}
