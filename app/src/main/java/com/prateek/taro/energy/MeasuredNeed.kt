package com.prateek.taro.energy

import java.time.LocalDate
import kotlin.math.min

data class BalanceDay(val date: LocalDate, val intake: Double, val burn: Double)

data class Measured(val need: Double, val formula: Double, val days: Int, val confidence: Double) {
    val adjustment: Double get() = (need - formula) * confidence
}

object MeasuredNeed {
    const val WINDOW_DAYS = 28
    const val MIN_INTAKE = 1_200.0
    private const val MIN_DAYS = 10
    private const val FULL_DAYS = 21
    private const val FULL_WEIGH_INS = 12
    private const val MAX_GAP = 1_000.0

    fun estimate(days: List<BalanceDay>, weights: List<TrendPoint>, from: LocalDate, to: LocalDate): Measured? {
        val logged = days.filter { it.intake >= MIN_INTAKE && !it.date.isBefore(from) && !it.date.isAfter(to) }
        if (logged.size < MIN_DAYS) return null
        val window = weights.filter { !it.date.isBefore(from) && !it.date.isAfter(to) }
        val slope = WeightTrend.slopePerDay(window) ?: return null

        val formula = logged.map { it.burn }.average()
        val need = logged.map { it.intake }.average() - slope * Metabolism.KCAL_PER_KG
        val confidence = min(1.0, logged.size.toDouble() / FULL_DAYS) * min(1.0, window.size.toDouble() / FULL_WEIGH_INS)
        return Measured(need.coerceIn(formula - MAX_GAP, formula + MAX_GAP), formula, logged.size, confidence)
    }
}
