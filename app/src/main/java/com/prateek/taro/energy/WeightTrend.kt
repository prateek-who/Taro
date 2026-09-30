package com.prateek.taro.energy

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.pow

data class WeightPoint(val date: LocalDate, val kg: Double)

data class TrendPoint(val date: LocalDate, val kg: Double, val trend: Double)

enum class PaceStatus { NOT_ENOUGH_DATA, ON_TRACK, SLOWER, FASTER, TOO_FAST, WRONG_WAY, STEADY, DRIFTING }

data class PaceAssessment(val status: PaceStatus, val actualPerWeek: Double?, val expectedPerWeek: Double)

object WeightTrend {
    const val SMOOTHING = 0.1
    private const val RATE_WINDOW_DAYS = 21L
    private const val MIN_SPAN_DAYS = 10L
    private const val MIN_READINGS = 6
    private const val PLAN_TOLERANCE = 0.4
    private const val MAINTAIN_TOLERANCE_KG = 0.2
    private const val WRONG_WAY_KG = 0.05
    private const val MAX_LOSS_SHARE = 0.01
    private const val MAX_GAIN_SHARE = 0.005

    fun trend(entries: List<WeightPoint>): List<TrendPoint> {
        val sorted = entries.sortedBy { it.date }
        var previous: TrendPoint? = null
        return sorted.map { entry ->
            val trend = previous?.let {
                val days = ChronoUnit.DAYS.between(it.date, entry.date).coerceAtLeast(1)
                val weight = 1 - (1 - SMOOTHING).pow(days.toDouble())
                it.trend + weight * (entry.kg - it.trend)
            } ?: entry.kg
            TrendPoint(entry.date, entry.kg, trend).also { previous = it }
        }
    }

    fun weeklyRate(points: List<TrendPoint>): Double? {
        val last = points.lastOrNull() ?: return null
        return slopePerDay(points.filter { ChronoUnit.DAYS.between(it.date, last.date) < RATE_WINDOW_DAYS })?.times(7)
    }

    fun slopePerDay(window: List<TrendPoint>): Double? {
        if (window.size < MIN_READINGS) return null
        if (ChronoUnit.DAYS.between(window.first().date, window.last().date) < MIN_SPAN_DAYS) return null

        val xs = window.map { ChronoUnit.DAYS.between(window.first().date, it.date).toDouble() }
        val ys = window.map { it.trend }
        val meanX = xs.average()
        val meanY = ys.average()
        val covariance = xs.indices.sumOf { (xs[it] - meanX) * (ys[it] - meanY) }
        val variance = xs.sumOf { (it - meanX) * (it - meanX) }
        return if (variance > 0) covariance / variance else null
    }

    fun assess(ratePerWeek: Double?, goal: DietGoal?, adjustment: Int, currentKg: Double): PaceAssessment {
        val expected = goal?.let { Metabolism.weeklyChangeKg(it, adjustment) } ?: 0.0
        val rate = ratePerWeek ?: return PaceAssessment(PaceStatus.NOT_ENOUGH_DATA, null, expected)

        val status = when (goal) {
            null, DietGoal.MAINTAIN -> if (abs(rate) <= MAINTAIN_TOLERANCE_KG) PaceStatus.STEADY else PaceStatus.DRIFTING
            DietGoal.CUT -> when {
                rate >= WRONG_WAY_KG -> PaceStatus.WRONG_WAY
                -rate > currentKg * MAX_LOSS_SHARE -> PaceStatus.TOO_FAST
                else -> compareToPlan(rate, expected)
            }
            DietGoal.BULK -> when {
                rate <= -WRONG_WAY_KG -> PaceStatus.WRONG_WAY
                rate > currentKg * MAX_GAIN_SHARE -> PaceStatus.TOO_FAST
                else -> compareToPlan(rate, expected)
            }
        }
        return PaceAssessment(status, rate, expected)
    }

    private fun compareToPlan(rate: Double, expected: Double): PaceStatus {
        if (expected == 0.0) return PaceStatus.ON_TRACK
        val ratio = rate / expected
        return when {
            ratio < 1 - PLAN_TOLERANCE -> PaceStatus.SLOWER
            ratio > 1 + PLAN_TOLERANCE -> PaceStatus.FASTER
            else -> PaceStatus.ON_TRACK
        }
    }
}
