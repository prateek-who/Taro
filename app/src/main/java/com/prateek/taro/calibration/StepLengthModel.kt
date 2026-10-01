package com.prateek.taro.calibration

import com.prateek.taro.energy.EnergyModel

data class CalibrationSample(val cadence: Double, val stepLengthM: Double, val weight: Double = 1.0)

data class StepLengthFit(
    val walkingStepM: Double?,
    val walkingSlope: Double,
    val referenceCadence: Double,
    val runningStepM: Double?,
    val walkingSamples: Int,
    val runningSamples: Int,
)

object StepLengthModel {
    private val WALKING_RANGE = 0.3..1.3
    private val RUNNING_RANGE = 0.5..2.2
    private const val MAX_SLOPE = 0.01
    private const val MIN_CADENCE_SPREAD = 15.0

    private fun walkingSamples(samples: List<CalibrationSample>) =
        samples.filter { !EnergyModel.isRunning(it.cadence.toInt()) && it.stepLengthM in WALKING_RANGE }

    private val PLAUSIBLE_SHARE = 0.85..1.25

    fun plausible(walkingStepM: Double, estimateM: Double): Boolean = walkingStepM / estimateM in PLAUSIBLE_SHARE

    fun coversPaceRange(samples: List<CalibrationSample>): Boolean {
        val walking = walkingSamples(samples)
        val spread = (walking.maxOfOrNull { it.cadence } ?: 0.0) - (walking.minOfOrNull { it.cadence } ?: 0.0)
        return walking.size >= 3 && spread >= MIN_CADENCE_SPREAD
    }

    fun fit(samples: List<CalibrationSample>): StepLengthFit {
        val walking = walkingSamples(samples)
        val running = samples.filter { EnergyModel.isRunning(it.cadence.toInt()) && it.stepLengthM in RUNNING_RANGE }

        val reference = weightedMean(walking.map { it.cadence to it.weight }) ?: 100.0
        val slope = if (coversPaceRange(samples)) {
            regressionSlope(walking).coerceIn(0.0, MAX_SLOPE)
        } else {
            0.0
        }
        val walkingBase = if (slope > 0.0) {
            weightedMean(walking.map { it.stepLengthM - slope * (it.cadence - reference) to it.weight })
        } else {
            weightedMedian(walking.map { it.stepLengthM to it.weight })
        }

        return StepLengthFit(
            walkingStepM = walkingBase,
            walkingSlope = slope,
            referenceCadence = reference,
            runningStepM = weightedMedian(running.map { it.stepLengthM to it.weight }),
            walkingSamples = walking.size,
            runningSamples = running.size,
        )
    }

    private fun weightedMean(values: List<Pair<Double, Double>>): Double? {
        val total = values.sumOf { it.second }
        if (values.isEmpty() || total <= 0.0) return null
        return values.sumOf { it.first * it.second } / total
    }

    private fun weightedMedian(values: List<Pair<Double, Double>>): Double? {
        if (values.isEmpty()) return null
        val sorted = values.sortedBy { it.first }
        val half = sorted.sumOf { it.second } / 2
        var accumulated = 0.0
        sorted.forEachIndexed { index, (value, weight) ->
            accumulated += weight
            if (accumulated > half) return value
            if (accumulated == half) return (value + (sorted.getOrNull(index + 1)?.first ?: value)) / 2
        }
        return sorted.last().first
    }

    private fun regressionSlope(samples: List<CalibrationSample>): Double {
        val meanX = weightedMean(samples.map { it.cadence to it.weight }) ?: return 0.0
        val meanY = weightedMean(samples.map { it.stepLengthM to it.weight }) ?: return 0.0
        val covariance = samples.sumOf { it.weight * (it.cadence - meanX) * (it.stepLengthM - meanY) }
        val variance = samples.sumOf { it.weight * (it.cadence - meanX) * (it.cadence - meanX) }
        return if (variance > 0.0) covariance / variance else 0.0
    }
}
