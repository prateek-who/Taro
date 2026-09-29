package com.nvllz.stepsy.sleep

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.sqrt

data class Night(val wakeDate: LocalDate, val startAt: Long, val endAt: Long) {
    val minutes: Long get() = (endAt - startAt) / 60_000L
}

enum class SleepStatus { SHORT, A_BIT_SHORT, HEALTHY, LONG }

data class SleepSpan(val start: Long, val end: Long)

object SleepDates {
    fun today(zone: ZoneId = ZoneId.systemDefault()): LocalDate = LocalDate.now(zone)

    fun wakeDateOf(endMs: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(endMs).atZone(zone).toLocalDate()

    fun manualSpan(wakeDate: LocalDate, bedMinute: Int, wakeMinute: Int, zone: ZoneId = ZoneId.systemDefault()): SleepSpan {
        val bedDate = if (bedMinute > wakeMinute) wakeDate.minusDays(1) else wakeDate
        return SleepSpan(at(bedDate, bedMinute, zone), at(wakeDate, wakeMinute, zone))
    }

    private fun at(date: LocalDate, minuteOfDay: Int, zone: ZoneId): Long =
        LocalDateTime.of(date, LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)).atZone(zone).toInstant().toEpochMilli()
}

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
