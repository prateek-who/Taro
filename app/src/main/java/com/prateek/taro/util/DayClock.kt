package com.prateek.taro.util

import java.time.LocalDate
import java.util.Calendar
import java.util.TimeZone

object DayClock {

    private fun format(calendar: Calendar) = "%04d-%02d-%02d".format(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH),
    )

    fun dateOf(millis: Long, dayStartMinutes: Int, zone: TimeZone = TimeZone.getDefault()): String {
        val calendar = Calendar.getInstance(zone).apply {
            timeInMillis = millis
            add(Calendar.MINUTE, -dayStartMinutes)
        }
        return format(calendar)
    }

    fun startOf(date: String, dayStartMinutes: Int, zone: TimeZone = TimeZone.getDefault()): Long {
        val day = LocalDate.parse(date)
        return Calendar.getInstance(zone).apply {
            clear()
            set(day.year, day.monthValue - 1, day.dayOfMonth, 0, 0, 0)
            add(Calendar.MINUTE, dayStartMinutes)
        }.timeInMillis
    }

    fun momentOf(date: String, minuteOfDay: Int, dayStartMinutes: Int, zone: TimeZone = TimeZone.getDefault()): Long {
        val day = LocalDate.parse(date).let { if (minuteOfDay < dayStartMinutes) it.plusDays(1) else it }
        return Calendar.getInstance(zone).apply {
            clear()
            set(day.year, day.monthValue - 1, day.dayOfMonth, minuteOfDay / 60, minuteOfDay % 60, 0)
        }.timeInMillis
    }

    fun minuteOfDay(millis: Long, zone: TimeZone = TimeZone.getDefault()): Int {
        val calendar = Calendar.getInstance(zone).apply { timeInMillis = millis }
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    fun nextStart(nowMs: Long, dayStartMinutes: Int, zone: TimeZone = TimeZone.getDefault()): Long {
        val tomorrow = LocalDate.parse(dateOf(nowMs, dayStartMinutes, zone)).plusDays(1).toString()
        return startOf(tomorrow, dayStartMinutes, zone)
    }

    fun fractionOfDay(nowMs: Long, dayStartMinutes: Int, zone: TimeZone = TimeZone.getDefault()): Double {
        val start = startOf(dateOf(nowMs, dayStartMinutes, zone), dayStartMinutes, zone)
        val end = nextStart(nowMs, dayStartMinutes, zone)
        return ((nowMs - start).toDouble() / (end - start)).coerceIn(0.0, 1.0)
    }
}
