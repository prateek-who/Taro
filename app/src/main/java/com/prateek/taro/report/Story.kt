package com.prateek.taro.report

import com.prateek.taro.util.StreakMark
import com.prateek.taro.util.Walk
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

data class Moment(val at: Long, val steps: Int)

data class Stillness(val date: LocalDate, val from: Long, val minutes: Int)

data class Turn(val low: DayInput, val turn: DayInput, val madeUp: Boolean, val rested: Boolean, val streakBefore: Boolean) {
    val times: Int get() = turn.steps / low.steps.coerceAtLeast(1)
}

data class LateNight(val usualMinute: Int, val date: LocalDate, val minute: Int)

data class Biggest(val day: DayInput, val rank: Int, val record: Int, val recordDate: LocalDate)

enum class TwinKind { SLEEP, STEPS }

data class Twin(val kind: TwinKind, val now: Long, val before: Long)

data class PowerHour(val hour: Int, val steps: Int, val unit: DayInput?, val units: Int)

data class FastMinute(val at: Long, val steps: Int)

data class Habit(val hour: Int, val days: Int, val of: Int, val starts: List<Long>)

data class Nap(val start: Long, val end: Long) {
    val minutes: Int get() = ((end - start) / 60_000L).toInt()
}

data class Favourite(val name: String, val count: Int, val kcal: Double, val since: LocalDate, val now: Int) {
    val next: Int get() = StoryFinder.FOOD_MILESTONES.firstOrNull { it > count } ?: ((count / 100 + 1) * 100)
}

enum class VersusKind { EATEN, STEPS }

data class Versus(val kind: VersusKind, val now: Double, val before: Double, val proteinChange: Int?)

data class Milestone(val target: Long, val total: Long, val best: DayInput?, val average: Int) {
    val left: Long get() = target - total
}

enum class StoryType { NIGHT_SHIFT, EARLY_BIRD, LUNCH_WALKER, WEEKEND_WARRIOR, CLOCKWORK, WANDERER }

data class Persona(
    val type: StoryType,
    val nightOwl: Int,
    val routine: Int,
    val weekend: Int,
    val loyalty: Int?,
    val weakDay: DayOfWeek?,
    val secret: Walk?,
)

data class Numbers(
    val walkingMinutes: Int,
    val sleepMinutes: Long,
    val eaten: Double?,
    val protein: Double?,
    val proteinChange: Int?,
)

enum class Plot { COMEBACK, BREAKTHROUGH, STEADY, REST, PLAIN }

data class StoryFacts(
    val plot: Plot,
    val turn: Turn?,
    val biggest: Biggest?,
    val numbers: Numbers,
    val twin: Twin?,
    val bedtimes: List<Int?>,
    val odd: Moment?,
    val still: Stillness?,
    val late: LateNight?,
    val power: PowerHour?,
    val fastest: List<FastMinute>,
    val habit: Habit?,
    val nap: Nap?,
    val earliest: Walk?,
    val favourite: Favourite?,
    val versus: Versus?,
    val milestone: Milestone?,
    val persona: Persona?,
)

object StoryFinder {
    val FOOD_MILESTONES = listOf(10, 25, 50, 100, 150, 200, 250, 300, 400, 500)
    private val STEP_MILESTONES = listOf(100_000L, 250_000L, 500_000L, 750_000L, 1_000_000L)
    private const val ODD_FROM = 1
    private const val ODD_UNTIL = 5
    private const val MIN_ODD_STEPS = 20
    private const val ODD_GAP_MS = 30 * 60_000L
    private const val MIN_STILL_MINUTES = 150
    private const val LATE_MINUTES = 45
    private const val STEADY_MINUTES = 30
    private const val MIN_NIGHTS = 4
    private const val REST_CHANGE = -15
    private const val MINUTE = 60_000L
    private const val AWAKE_FROM = 7
    private const val AWAKE_UNTIL = 23
    private const val DAY_MINUTES = 24 * 60
    private const val TWIN_SLEEP_MINUTES = 20
    private const val TWIN_STEPS_SHARE = 0.01
    private const val MIN_UNIT_STEPS = 50
    private const val MIN_UNITS = 3
    private const val MAX_UNITS = 99
    private const val FAST_STEPS = 100
    private const val FAST_COUNT = 3
    private const val HABIT_DAYS = 3
    private const val NIGHT_FROM = 18
    private const val MORNING_FROM = 5
    private const val MORNING_UNTIL = 10
    private const val MIDDAY_FROM = 11
    private const val MIDDAY_UNTIL = 15
    private const val NIGHT_TYPE = 0.45
    private const val MORNING_TYPE = 0.4
    private const val MIDDAY_TYPE = 0.35
    private const val WEEKEND_TYPE = 0.5
    private const val CLOCKWORK = 70
    private const val NIGHT_FULL = 0.7
    private const val WEEKEND_FULL = 0.6

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
        if (latest - median < LATE_MINUTES || others.any { abs(it.second - median) > STEADY_MINUTES }) return null
        return LateNight(median % DAY_MINUTES, date, latest % DAY_MINUTES)
    }

    fun biggest(days: List<DayInput>, allTime: Map<LocalDate, Int>): Biggest? {
        val day = days.filter { it.steps > 0 }.maxByOrNull { it.steps } ?: return null
        val record = allTime.maxByOrNull { it.value } ?: return null
        return Biggest(day, allTime.values.count { it > day.steps } + 1, record.value, record.key)
    }

    fun twin(sleepNow: Long, sleepBefore: Long, stepsNow: Int, stepsBefore: Int?): Twin? = when {
        sleepNow > 0 && sleepBefore > 0 && sleepNow != sleepBefore && abs(sleepNow - sleepBefore) <= TWIN_SLEEP_MINUTES -> Twin(TwinKind.SLEEP, sleepNow, sleepBefore)
        stepsBefore != null && stepsNow > 0 && stepsNow != stepsBefore && abs(stepsNow - stepsBefore) <= stepsBefore * TWIN_STEPS_SHARE ->
            Twin(TwinKind.STEPS, stepsNow.toLong(), stepsBefore.toLong())
        else -> null
    }

    fun powerHour(hours: List<Int>, days: List<DayInput>): PowerHour? {
        val hour = hours.indices.maxByOrNull { hours[it] } ?: return null
        val steps = hours[hour].takeIf { it > 0 } ?: return null
        val unit = days.filter { it.complete && it.steps >= MIN_UNIT_STEPS }.minByOrNull { it.steps }
        val units = unit?.let { steps / it.steps } ?: 0
        return if (unit != null && units in MIN_UNITS..MAX_UNITS) PowerHour(hour, steps, unit, units) else PowerHour(hour, steps, null, 0)
    }

    fun fastest(minutes: List<Pair<Long, Int>>): List<FastMinute> =
        minutes.filter { it.second >= FAST_STEPS }.sortedWith(compareByDescending<Pair<Long, Int>> { it.second }.thenBy { it.first })
            .take(FAST_COUNT).sortedBy { it.first }.map { FastMinute(it.first, it.second) }

    fun habit(walks: Map<LocalDate, List<Walk>>, days: Int, zone: ZoneId): Habit? {
        fun hourOf(walk: Walk) = Instant.ofEpochMilli(walk.start).atZone(zone).let { (it.hour * 60 + it.minute + 30) / 60 % 24 }
        val byHour = walks.flatMap { (date, list) -> list.map { Triple(hourOf(it), date, it.start) } }.groupBy { it.first }
        val (hour, hits) = byHour.maxByOrNull { (_, hits) -> hits.map { it.second }.distinct().size } ?: return null
        val count = hits.map { it.second }.distinct().size
        if (count < HABIT_DAYS || count * 2 < days) return null
        return Habit(hour, count, days, hits.groupBy { it.second }.values.map { same -> same.minOf { it.third } }.sorted())
    }

    fun milestone(total: Long, days: List<DayInput>): Milestone? {
        if (total <= 0) return null
        val target = STEP_MILESTONES.firstOrNull { it > total } ?: ((total / 500_000L + 1) * 500_000L)
        val tracked = days.filter { it.steps > 0 }
        return Milestone(target, total, tracked.maxByOrNull { it.steps }, if (tracked.isEmpty()) 0 else tracked.sumOf { it.steps } / tracked.size)
    }

    fun persona(hours: List<Int>, days: List<DayInput>, habit: Habit?, loyalty: Int?, longest: Walk?): Persona? {
        val total = hours.sum().takeIf { it > 0 }?.toDouble() ?: return null
        fun share(from: Int, until: Int) = (from until until).sumOf { hours[it] } / total
        val night = share(NIGHT_FROM, 24)
        val morning = share(MORNING_FROM, MORNING_UNTIL)
        val midday = share(MIDDAY_FROM, MIDDAY_UNTIL)
        val tracked = days.filter { it.steps > 0 }
        val weekend = tracked.filter { it.date.dayOfWeek == DayOfWeek.SATURDAY || it.date.dayOfWeek == DayOfWeek.SUNDAY }.sumOf { it.steps }.toDouble() /
            tracked.sumOf { it.steps }.coerceAtLeast(1)
        val routine = habit?.let { it.days * 100 / it.of.coerceAtLeast(1) } ?: run {
            val mean = tracked.map { it.steps.toDouble() }.average()
            val spread = kotlin.math.sqrt(tracked.sumOf { (it.steps - mean) * (it.steps - mean) } / tracked.size.coerceAtLeast(1))
            if (mean > 0) ((1 - spread / mean) * 100).roundToInt().coerceIn(0, 100) else 0
        }
        val type = when {
            night >= NIGHT_TYPE -> StoryType.NIGHT_SHIFT
            morning >= MORNING_TYPE -> StoryType.EARLY_BIRD
            midday >= MIDDAY_TYPE -> StoryType.LUNCH_WALKER
            weekend >= WEEKEND_TYPE -> StoryType.WEEKEND_WARRIOR
            routine >= CLOCKWORK -> StoryType.CLOCKWORK
            else -> StoryType.WANDERER
        }
        return Persona(
            type = type,
            nightOwl = (night / NIGHT_FULL * 100).roundToInt().coerceIn(0, 100),
            routine = routine,
            weekend = (weekend / WEEKEND_FULL * 100).roundToInt().coerceIn(0, 100),
            loyalty = loyalty,
            weakDay = days.filter { it.complete && it.steps > 0 }.takeIf { it.size >= 3 }?.minByOrNull { it.steps }?.date?.dayOfWeek,
            secret = longest,
        )
    }

    fun plot(report: PeriodReport, turn: Turn?, newBest: Boolean): Plot = when {
        turn != null -> Plot.COMEBACK
        newBest -> Plot.BREAKTHROUGH
        report.goalDaysPossible >= 5 && report.goalDays == report.goalDaysPossible -> Plot.STEADY
        (report.stepsChange ?: 0) <= REST_CHANGE -> Plot.REST
        else -> Plot.PLAIN
    }
}
