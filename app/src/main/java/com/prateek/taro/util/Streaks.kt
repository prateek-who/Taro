package com.prateek.taro.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class Forgive(val key: String) { REST("rest"), MAKE_UP("make_up") }

fun forgiveOf(key: String?): Forgive? = Forgive.entries.firstOrNull { it.key == key }

enum class StreakMark { MET, MADE_UP, REST }

data class Challenge(val missed: LocalDate, val target: Double)

data class Unanswered(val date: LocalDate, val shortfall: Double, val restAllowed: Boolean)

data class Streak(
    val current: Int = 0,
    val longest: Int = 0,
    val marks: Map<LocalDate, StreakMark> = emptyMap(),
    val challenge: Challenge? = null,
    val unanswered: Unanswered? = null,
    val reached: List<LocalDate> = emptyList(),
) {
    fun reachedOn(length: Int): LocalDate? = reached.getOrNull(length - 1)

    fun runStart(today: LocalDate): LocalDate? {
        var day = if (marks[today] != null) today else today.minusDays(1)
        if (challenge?.missed == day) day = day.minusDays(1)
        if (current <= 0 || marks[day] == null) return null
        while (marks[day.minusDays(1)] != null) day = day.minusDays(1)
        return day
    }
}

object Streaks {
    const val REST_EVERY_DAYS = 7L

    fun compute(
        start: LocalDate,
        end: LocalDate,
        today: LocalDate?,
        value: (LocalDate) -> Double,
        goal: (LocalDate) -> Double,
        choices: Map<LocalDate, Forgive> = emptyMap(),
        makeUp: Boolean = false,
        preferRest: Boolean = true,
    ): Streak {
        val marks = mutableMapOf<LocalDate, StreakMark>()
        val reached = mutableListOf<LocalDate>()
        var count = 0
        var longest = 0
        var lastRest: LocalDate? = null
        var pending: Pair<LocalDate, Double>? = null
        var challenge: Challenge? = null
        var unanswered: Unanswered? = null

        fun restAllowed(date: LocalDate) = lastRest?.let { ChronoUnit.DAYS.between(it, date) >= REST_EVERY_DAYS } ?: true
        fun add(date: LocalDate, days: Int) {
            repeat(days) {
                count++
                if (count > reached.size) reached += date
            }
            longest = maxOf(longest, count)
        }
        fun rest(date: LocalDate) {
            lastRest = date
            marks[date] = StreakMark.REST
        }
        fun resolve(date: LocalDate, shortfall: Double): Forgive? {
            val choice = choices[date]
            val restOk = restAllowed(date)
            if (choice == null && today != null && date == today.minusDays(1) && makeUp) {
                unanswered = Unanswered(date, shortfall, restOk)
            }
            val wanted = choice ?: if (preferRest || !makeUp) Forgive.REST else Forgive.MAKE_UP
            return when {
                wanted == Forgive.MAKE_UP && makeUp -> Forgive.MAKE_UP
                restOk -> Forgive.REST
                makeUp -> Forgive.MAKE_UP
                else -> null
            }
        }

        var date = start
        while (!date.isAfter(end)) {
            val target = goal(date)
            val done = value(date)
            val met = target > 0 && done >= target
            val open = date == today
            val waiting = pending
            if (waiting != null) {
                val (missed, shortfall) = waiting
                if (target > 0 && done >= target + shortfall) {
                    marks[missed] = StreakMark.MADE_UP
                    marks[date] = StreakMark.MET
                    add(date, 2)
                    pending = null
                    date = date.plusDays(1)
                    continue
                }
                if (open) {
                    challenge = Challenge(missed, target + shortfall)
                    date = date.plusDays(1)
                    continue
                }
                pending = null
                if (restAllowed(missed)) rest(missed) else count = 0
            }
            if (met) {
                marks[date] = StreakMark.MET
                add(date, 1)
            } else if (!open && count > 0) {
                val shortfall = (target - done).coerceAtLeast(0.0)
                when (resolve(date, shortfall)) {
                    Forgive.REST -> rest(date)
                    Forgive.MAKE_UP -> pending = date to shortfall
                    null -> count = 0
                }
            }
            date = date.plusDays(1)
        }
        return Streak(count, longest, marks, challenge, unanswered, reached)
    }

    fun ofDates(dates: Collection<LocalDate>, today: LocalDate? = null): Streak {
        val set = dates.toSet()
        val first = set.minOrNull() ?: return Streak()
        val last = maxOf(set.max(), today ?: set.max())
        return compute(first, last, today, value = { if (it in set) 1.0 else 0.0 }, goal = { 1.0 })
    }
}
