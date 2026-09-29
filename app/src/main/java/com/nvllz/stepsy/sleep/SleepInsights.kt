package com.nvllz.stepsy.sleep

import java.time.LocalDate
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.sqrt

data class Night(val wakeDate: LocalDate, val startAt: Long, val endAt: Long) {
    val minutes: Long get() = (endAt - startAt) / 60_000L
}

enum class SleepStatus { SHORT, A_BIT_SHORT, HEALTHY, LONG }

object SleepInsights {
    const val HEALTHY_MIN = 7 * 60L
    const val HEALTHY_MAX = 9 * 60L
    private const val SHORT_BELOW = 6 * 60L
    private const val LONG_ABOVE = 9 * 60L + 30

    fun average(nights: List<Night>): Long? = nights.takeIf { it.isNotEmpty() }?.map { it.minutes }?.average()?.toLong()

    fun status(minutes: Long): SleepStatus = when {
        minutes < SHORT_BELOW -> SleepStatus.SHORT
        minutes < HEALTHY_MIN -> SleepStatus.A_BIT_SHORT
        minutes <= LONG_ABOVE -> SleepStatus.HEALTHY
        else -> SleepStatus.LONG
    }

    fun inRange(night: Night) = night.minutes in HEALTHY_MIN..HEALTHY_MAX

    fun bedtimeSpreadMinutes(nights: List<Night>, zone: TimeZone = TimeZone.getDefault()): Long? {
        if (nights.size < 3) return null
        val fromNoon = nights.map { night ->
            val calendar = Calendar.getInstance(zone).apply { timeInMillis = night.startAt }
            val minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
            ((minuteOfDay - 12 * 60 + 24 * 60) % (24 * 60)).toDouble()
        }
        val mean = fromNoon.average()
        return sqrt(fromNoon.sumOf { (it - mean) * (it - mean) } / fromNoon.size).toLong()
    }

    fun streak(nights: List<Night>, today: LocalDate): Int {
        val byDate = nights.associateBy { it.wakeDate }
        var day = today
        if (byDate[day] == null) day = day.minusDays(1)
        var count = 0
        while (true) {
            val night = byDate[day] ?: break
            if (!inRange(night)) break
            count++
            day = day.minusDays(1)
        }
        return count
    }
}
