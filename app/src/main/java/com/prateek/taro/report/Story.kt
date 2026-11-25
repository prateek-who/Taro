package com.prateek.taro.report

import com.prateek.taro.util.Sprees
import com.prateek.taro.util.StreakMark
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class Moment(val at: Long, val steps: Int)

data class Stillness(val date: LocalDate, val from: Long, val minutes: Int)

data class Turn(val low: DayInput, val turn: DayInput, val madeUp: Boolean, val rested: Boolean, val streakBefore: Boolean)

data class Climax(val day: DayInput, val score: Int, val walkMinutes: Int, val hours: List<Int>)

data class LateNight(val usualMinute: Int, val date: LocalDate, val minute: Int)

data class SamePeak(val dates: List<LocalDate>, val hour: Int)

data class Persona(val startHour: Int, val before: List<Int>, val now: Int) {
    val usual: Int? get() = before.takeIf { it.isNotEmpty() }?.average()?.toInt()
}

data class NextUp(val spree: Int?, val streak: Int, val slowDay: DayOfWeek?, val slowSteps: Int)

enum class Plot { COMEBACK, BREAKTHROUGH, STEADY, REST, PLAIN }

data class StoryFacts(
    val plot: Plot,
    val turn: Turn?,
    val climax: Climax?,
    val odd: Moment?,
    val still: Stillness?,
    val late: LateNight?,
    val samePeak: SamePeak?,
    val topFood: Pair<String, Int>?,
    val persona: Persona?,
    val next: NextUp,
    val sleepMinutes: Long,
    val stillMinutes: List<Pair<Long, Int>>,
)

object StoryFinder {
    const val PERSONA_HOURS = 3
    private const val ODD_FROM = 1
    private const val ODD_UNTIL = 5
    private const val MIN_ODD_STEPS = 20
    private const val ODD_GAP_MS = 30 * 60_000L
    private const val MIN_STILL_MINUTES = 150
    private const val LATE_MINUTES = 45
    private const val STEADY_MINUTES = 30
    private const val MIN_NIGHTS = 4
    private const val PEAK_STEPS = 1_000
    private const val MIN_WEEKS = 3
    private const val REST_CHANGE = -15
    private const val MINUTE = 60_000L
    private const val AWAKE_FROM = 7
    private const val AWAKE_UNTIL = 23
    private const val DAY_MINUTES = 24 * 60

    fun oddSteps(minutes: List<Pair<Long, Int>>, zone: ZoneId): Moment? {
        val night = minutes.filter { (at, steps) ->
            steps > 0 && Instant.ofEpochMilli(at).atZone(zone).hour in ODD_FROM until ODD_UNTIL
        }.sortedBy { it.first }
        val clusters = mutableListOf<MutableList<Pair<Long, Int>>>()
        night.forEach { minute ->
            val last = clusters.lastOrNull()
            if (last != null && minute.first - last.last().first <= ODD_GAP_MS) last += minute else clusters += mutableListOf(minute)
        }
        return clusters.maxByOrNull { cluster -> cluster.sumOf { it.second } }
            ?.takeIf { cluster -> cluster.sumOf { it.second } >= MIN_ODD_STEPS }
            ?.let { cluster -> Moment(cluster.first().first, cluster.sumOf { it.second }) }
    }

    fun stillness(byDay: Map<LocalDate, List<Pair<Long, Int>>>, asleep: List<Pair<Long, Long>>, zone: ZoneId): Stillness? =
        byDay.flatMap { (date, rows) ->
            val awakeFrom = date.atTime(AWAKE_FROM, 0).atZone(zone).toInstant().toEpochMilli()
            val awakeUntil = date.atTime(AWAKE_UNTIL, 0).atZone(zone).toInstant().toEpochMilli()
            rows.filter { it.second > 0 }.map { it.first }.sorted().zipWithNext().flatMap { (a, b) ->
                asleep.fold(listOf(maxOf(a + MINUTE, awakeFrom) to minOf(b, awakeUntil))) { spans, (sleepFrom, sleepTo) ->
                    spans.flatMap { (from, to) -> listOf(from to minOf(to, sleepFrom), maxOf(from, sleepTo) to to) }.filter { (from, to) -> to > from }
                }.map { (from, to) -> Stillness(date, from, ((to - from) / MINUTE).toInt()) }
            }
        }.maxByOrNull { it.minutes }?.takeIf { it.minutes >= MIN_STILL_MINUTES }

    fun turn(days: List<DayInput>, marks: Map<LocalDate, StreakMark>): Turn? {
        val low = days.filter { it.complete && it.goal > 0 && it.steps < it.goal }.maxByOrNull { it.goal - it.steps } ?: return null
        val back = days.firstOrNull { it.date.isAfter(low.date) && it.goal > 0 && it.steps >= it.goal } ?: return null
        return Turn(
            low = low,
            turn = back,
            madeUp = marks[low.date] == StreakMark.MADE_UP,
            rested = marks[low.date] == StreakMark.REST,
            streakBefore = marks[low.date.minusDays(1)] != null,
        )
    }

    fun lateNight(bedtimes: List<Pair<LocalDate, Int>>): LateNight? {
        if (bedtimes.size < MIN_NIGHTS) return null
        val shifted = bedtimes.map { (date, minute) -> date to if (minute < DAY_MINUTES / 2) minute + DAY_MINUTES else minute }
        val sorted = shifted.map { it.second }.sorted()
        val median = sorted[sorted.size / 2]
        val (date, latest) = shifted.maxBy { it.second }
        val others = shifted.filter { it.first != date }
        if (latest - median < LATE_MINUTES || others.any { kotlin.math.abs(it.second - median) > STEADY_MINUTES }) return null
        return LateNight(median % DAY_MINUTES, date, latest % DAY_MINUTES)
    }

    fun samePeak(hours: Map<LocalDate, List<Int>>): SamePeak? =
        hours.mapNotNull { (date, perHour) ->
            perHour.indices.maxByOrNull { perHour[it] }?.takeIf { perHour[it] >= PEAK_STEPS }?.let { date to it }
        }.groupBy({ it.second }, { it.first }).filterValues { it.size >= 2 }
            .maxByOrNull { it.value.size }?.let { (hour, dates) -> SamePeak(dates.sorted(), hour) }

    fun persona(now: List<Int>, before: List<List<Int>>): Persona? {
        if (now.sum() <= 0) return null
        val start = (0..now.size - PERSONA_HOURS).maxBy { hour -> (hour until hour + PERSONA_HOURS).sumOf { now[it] } }
        fun share(perHour: List<Int>) = perHour.sum().takeIf { it > 0 }?.let { total -> (start until start + PERSONA_HOURS).sumOf { perHour[it] } * 100 / total }
        return Persona(start, before.mapNotNull(::share), share(now)!!)
    }

    fun next(streak: Int, recent: Map<LocalDate, Int>): NextUp {
        val tracked = recent.filterValues { it > 0 }
        val weeks = tracked.keys.map { it.toEpochDay() / 7 }.distinct().size
        val slow = if (weeks >= MIN_WEEKS) {
            tracked.entries.groupBy { it.key.dayOfWeek }.mapValues { (_, days) -> days.sumOf { it.value } / days.size }.minByOrNull { it.value }
        } else {
            null
        }
        return NextUp(Sprees.DAYS.indexOfFirst { it > streak }.takeIf { it >= 0 && streak > 0 }, streak, slow?.key, slow?.value ?: 0)
    }

    fun plot(report: PeriodReport, turn: Turn?, newBest: Boolean): Plot = when {
        turn != null -> Plot.COMEBACK
        newBest -> Plot.BREAKTHROUGH
        report.goalDaysPossible >= 5 && report.goalDays == report.goalDaysPossible -> Plot.STEADY
        (report.stepsChange ?: 0) <= REST_CHANGE -> Plot.REST
        else -> Plot.PLAIN
    }
}
