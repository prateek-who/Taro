package com.prateek.taro.report

import com.prateek.taro.energy.Metabolism
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.math.max

enum class ReportKind { WEEK, MONTH }

data class Period(val kind: ReportKind, val start: LocalDate) {
    val end: LocalDate get() = if (kind == ReportKind.WEEK) start.plusDays(6) else start.withDayOfMonth(start.lengthOfMonth())

    val days: Int get() = ChronoUnit.DAYS.between(start, end).toInt() + 1

    fun previous() = if (kind == ReportKind.WEEK) Period(kind, start.minusWeeks(1)) else Period(kind, start.minusMonths(1))

    fun next() = if (kind == ReportKind.WEEK) Period(kind, start.plusWeeks(1)) else Period(kind, start.plusMonths(1))

    fun contains(date: LocalDate) = !date.isBefore(start) && !date.isAfter(end)

    val key: String get() = "${kind.name}|$start"

    companion object {
        fun containing(kind: ReportKind, date: LocalDate, firstDay: DayOfWeek): Period = when (kind) {
            ReportKind.WEEK -> Period(kind, date.with(TemporalAdjusters.previousOrSame(firstDay)))
            ReportKind.MONTH -> Period(kind, date.withDayOfMonth(1))
        }

        fun parse(key: String?): Period? = key?.split('|')?.takeIf { it.size == 2 }?.let { (kind, start) ->
            runCatching { Period(ReportKind.valueOf(kind), LocalDate.parse(start)) }.getOrNull()
        }
    }
}

data class DayInput(
    val date: LocalDate,
    val steps: Int,
    val distanceM: Double,
    val activeKcal: Double,
    val burned: Double,
    val eaten: Double?,
    val protein: Double,
    val fullyLogged: Boolean,
    val goal: Int,
    val complete: Boolean,
)

data class Totals(val steps: Int, val distanceM: Double, val activeKcal: Double)

data class ReportInputs(
    val period: Period,
    val days: List<DayInput>,
    val previous: Totals?,
    val weightStart: Double?,
    val weightEnd: Double?,
    val nightMinutes: List<Long>,
    val bedtimeSpread: Long?,
    val napMinutes: List<Long>,
    val topFoods: List<String>,
    val badges: List<Int>,
    val proteinTarget: Double,
)

enum class WeightVerdict { MATCHES, LOST_MORE, LOST_LESS }

sealed interface Highlight {
    data class MoreSteps(val percent: Int) : Highlight
    data class FewerSteps(val percent: Int) : Highlight
    data class Deficit(val kcal: Double, val kg: Double) : Highlight
    data class Surplus(val kcal: Double, val kg: Double) : Highlight
    data class GoalEveryDay(val days: Int) : Highlight
    data object WeightOnTrack : Highlight
    data class Steps(val steps: Int) : Highlight
}

data class PeriodReport(
    val inputs: ReportInputs,
    val inProgress: Boolean,
    val steps: Int,
    val distanceM: Double,
    val activeKcal: Double,
    val averageSteps: Int,
    val goalDays: Int,
    val goalDaysPossible: Int,
    val bestDay: DayInput?,
    val loggedDays: Int,
    val burned: Double,
    val eaten: Double,
    val expectedKg: Double?,
    val actualKg: Double?,
    val verdict: WeightVerdict?,
    val proteinDays: Int,
    val averageEaten: Double?,
    val stepsChange: Int?,
    val highlights: List<Highlight>,
) {
    val balance: Double get() = eaten - burned
}

object Reports {
    private const val MIN_LOGGED_DAYS = 3
    private const val MATCH_KG = 0.3
    private const val MATCH_SHARE = 0.25
    private const val NOTABLE_CHANGE = 10

    private fun change(now: Double, before: Double?): Int? =
        before?.takeIf { it > 0 }?.let { ((now - it) / it * 100).toInt() }

    fun build(inputs: ReportInputs): PeriodReport {
        val days = inputs.days
        val steps = days.sumOf { it.steps }
        val tracked = days.filter { it.steps > 0 }
        val logged = days.filter { it.complete && it.fullyLogged && it.burned > 0 && it.eaten != null }
        val burned = logged.sumOf { it.burned }
        val eaten = logged.sumOf { it.eaten ?: 0.0 }
        val expected = if (logged.size >= MIN_LOGGED_DAYS) (eaten - burned) / Metabolism.KCAL_PER_KG else null
        val actual = if (inputs.weightStart != null && inputs.weightEnd != null) inputs.weightEnd - inputs.weightStart else null
        val verdict = if (expected != null && actual != null) {
            val gap = actual - expected
            when {
                abs(gap) <= max(MATCH_KG, abs(expected) * MATCH_SHARE) -> WeightVerdict.MATCHES
                gap < 0 -> WeightVerdict.LOST_MORE
                else -> WeightVerdict.LOST_LESS
            }
        } else {
            null
        }
        val goalDays = days.count { it.goal > 0 && it.steps >= it.goal }
        val possible = days.count { it.goal > 0 && (it.complete || it.steps >= it.goal) }
        val stepsChange = change(steps.toDouble(), inputs.previous?.steps?.toDouble())

        val highlights = buildList {
            stepsChange?.let {
                if (it >= NOTABLE_CHANGE) add(Highlight.MoreSteps(it)) else if (it <= -NOTABLE_CHANGE) add(Highlight.FewerSteps(-it))
            }
            if (possible >= 5 && goalDays == possible) add(Highlight.GoalEveryDay(goalDays))
            expected?.let {
                if (eaten < burned) add(Highlight.Deficit(burned - eaten, -it)) else add(Highlight.Surplus(eaten - burned, it))
            }
            if (verdict == WeightVerdict.MATCHES) add(Highlight.WeightOnTrack)
            if (isEmpty()) add(Highlight.Steps(steps))
        }

        return PeriodReport(
            inputs = inputs,
            inProgress = days.any { !it.complete },
            steps = steps,
            distanceM = days.sumOf { it.distanceM },
            activeKcal = days.sumOf { it.activeKcal },
            averageSteps = if (days.isEmpty()) 0 else steps / days.size,
            goalDays = goalDays,
            goalDaysPossible = possible,
            bestDay = tracked.maxByOrNull { it.steps },
            loggedDays = logged.size,
            burned = burned,
            eaten = eaten,
            expectedKg = expected,
            actualKg = actual,
            verdict = verdict,
            proteinDays = if (inputs.proteinTarget > 0) days.count { it.protein >= inputs.proteinTarget } else 0,
            averageEaten = logged.takeIf { it.isNotEmpty() }?.let { eaten / it.size },
            stepsChange = stepsChange,
            highlights = highlights,
        )
    }
}
