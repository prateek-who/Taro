package com.prateek.taro.energy

import java.time.DayOfWeek
import java.time.LocalDate

data class HourlyActive(val date: LocalDate, val hours: DoubleArray)

object ActivityProfile {
    const val DAYS = 28L
    private const val MIN_DAYS = 5
    private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    fun expectedRemaining(days: List<HourlyActive>, today: LocalDate, fractionElapsed: Double): Double? {
        val past = days.filter { it.date.isBefore(today) }
        val weekend = today.dayOfWeek in WEEKEND
        val matching = past.filter { (it.date.dayOfWeek in WEEKEND) == weekend }
        val pool = if (matching.size >= MIN_DAYS) matching else past
        if (pool.size < MIN_DAYS) return null
        return pool.map { remaining(it.hours, fractionElapsed.coerceIn(0.0, 1.0) * it.hours.size) }.average()
    }

    private fun remaining(hours: DoubleArray, position: Double): Double = hours.indices.sumOf { hour ->
        val share = ((hour + 1) - maxOf(position, hour.toDouble())).coerceIn(0.0, 1.0)
        hours[hour] * share
    }
}
