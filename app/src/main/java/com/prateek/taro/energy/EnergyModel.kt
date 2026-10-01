package com.prateek.taro.energy

import kotlin.math.max

data class Body(
    val weightKg: Double,
    val walkingStepM: Double,
    val runningStepM: Double,
    val walkingSlope: Double = 0.0,
    val referenceCadence: Double = 100.0,
    val heightM: Double = 1.7,
)

data class MinuteSample(val steps: Int, val ascentM: Double = 0.0)

object EnergyModel {
    const val RUNNING_CADENCE = 145

    private const val KCAL_PER_LITRE_O2 = 5.0
    private const val WALK_BASE = 3.85
    private const val WALK_SPEED = 5.97
    private const val STEADY_CADENCE = 80
    private const val WALK_VERTICAL = 1.8
    private const val RUN_HORIZONTAL = 0.2
    private const val RUN_VERTICAL = 0.9

    fun isRunning(cadence: Int) = cadence >= RUNNING_CADENCE

    fun stepLengthM(body: Body, cadence: Int): Double {
        if (isRunning(cadence)) return body.runningStepM
        val adjusted = body.walkingStepM + body.walkingSlope * (cadence - body.referenceCadence)
        return adjusted.coerceIn(body.walkingStepM * 0.6, body.walkingStepM * 1.4)
    }

    fun metresPerMinute(body: Body, cadence: Int) = cadence * stepLengthM(body, cadence)

    private fun kcal(netVo2MlPerKg: Double, weightKg: Double) = netVo2MlPerKg * weightKg / 1000.0 * KCAL_PER_LITRE_O2

    fun minuteKcal(body: Body, sample: MinuteSample): Double {
        if (sample.steps <= 0) return 0.0
        val ascent = max(0.0, sample.ascentM)
        val vo2 = if (isRunning(sample.steps)) {
            RUN_HORIZONTAL * metresPerMinute(body, sample.steps) + RUN_VERTICAL * ascent
        } else {
            walkingVo2(body, sample.steps) + WALK_VERTICAL * ascent
        }
        return kcal(vo2, body.weightKg)
    }

    private fun walkingVo2(body: Body, steps: Int): Double {
        val cadence = max(steps, STEADY_CADENCE)
        val minutes = steps.toDouble() / cadence
        val speed = metresPerMinute(body, cadence) / 60
        return (WALK_BASE + WALK_SPEED * speed * speed / body.heightM) * minutes
    }

    fun walkingStepsKcal(body: Body, steps: Int): Double {
        val counted = max(0, steps)
        val speed = metresPerMinute(body, STEADY_CADENCE) / 60
        return kcal((WALK_BASE + WALK_SPEED * speed * speed / body.heightM) * counted / STEADY_CADENCE, body.weightKg)
    }

    fun dayKcal(body: Body, minutes: List<MinuteSample>, totalSteps: Int): Double {
        val recorded = minutes.sumOf { it.steps }
        return minutes.sumOf { minuteKcal(body, it) } + walkingStepsKcal(body, totalSteps - recorded)
    }

    fun dayDistanceM(body: Body, minutes: List<MinuteSample>, totalSteps: Int): Double {
        val recorded = minutes.sumOf { it.steps }
        return minutes.sumOf { metresPerMinute(body, it.steps) } + max(0, totalSteps - recorded) * body.walkingStepM
    }
}
