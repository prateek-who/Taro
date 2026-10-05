package com.prateek.taro.calibration

import android.content.Context
import com.prateek.taro.data.CalibrationPoint
import com.prateek.taro.data.StepsDatabase
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.StepCalibration

object CalibrationRepository {
    const val SOURCE_GPS = "gps"
    const val SOURCE_KNOWN_DISTANCE = "known_distance"

    private const val HISTORY_MS = 180L * 24 * 60 * 60 * 1000

    fun save(context: Context, windows: List<CalibrationWindow>, source: String, pace: CalibrationPace): Boolean {
        val now = System.currentTimeMillis()
        val dao = StepsDatabase.get(context).steps()
        dao.clearCalibration(pace.key, source)
        dao.insertCalibration(
            windows.map {
                CalibrationPoint(
                    recordedAt = now,
                    cadence = it.cadence,
                    stepLengthM = it.stepLengthM,
                    distanceM = it.distanceM,
                    steps = it.steps,
                    source = source,
                    pace = pace.key,
                )
            }
        )
        return refit(context)
    }

    private fun used(context: Context): List<CalibrationPoint> {
        val all = StepsDatabase.get(context).steps().calibrationSince(System.currentTimeMillis() - HISTORY_MS)
        val anyMeasured = all.any { it.source == SOURCE_KNOWN_DISTANCE }
        return all.groupBy { it.pace }.flatMap { (pace, points) ->
            val measured = points.filter { it.source == SOURCE_KNOWN_DISTANCE }
            if (measured.isNotEmpty() || (pace == null && anyMeasured)) measured else points
        }
    }

    private fun samples(points: List<CalibrationPoint>) = points.map { CalibrationSample(it.cadence, it.stepLengthM, it.distanceM) }

    fun refit(context: Context): Boolean {
        val points = used(context)
        val fit = StepLengthModel.fit(samples(points))
        val estimateM = AppPreferences.estimatedStepLength / 100.0
        val trusted = points.any { it.source == SOURCE_KNOWN_DISTANCE } || fit.walkingStepM?.let { StepLengthModel.plausible(it, estimateM) } == true
        val walking = fit.walkingStepM?.takeIf { trusted }
        AppPreferences.stepCalibration = if (walking == null && fit.runningStepM == null) null else StepCalibration(
            walkingStepCm = ((walking ?: estimateM) * 100).toFloat(),
            walkingSlope = if (walking != null) fit.walkingSlope.toFloat() else 0f,
            referenceCadence = fit.referenceCadence.toFloat(),
            runningStepCm = fit.runningStepM?.let { (it * 100).toFloat() },
            samples = (if (walking != null) fit.walkingSamples else 0) + fit.runningSamples,
        )
        return fit.walkingStepM == null || trusted
    }

    fun overview(context: Context): CalibrationOverview {
        val points = used(context)
        val paces = points.groupBy { CalibrationPace.fromKey(it.pace) }.mapNotNull { (pace, group) ->
            val steps = group.sumOf { it.steps }
            if (pace == null || steps == 0) return@mapNotNull null
            pace to PaceResult(
                stepLengthM = group.sumOf { it.distanceM } / steps,
                cadence = group.map { it.cadence }.average(),
                measured = group.first().source == SOURCE_KNOWN_DISTANCE,
            )
        }.toMap()
        return CalibrationOverview(paces, StepLengthModel.coversPaceRange(samples(points)))
    }

    fun reset(context: Context) {
        StepsDatabase.get(context).steps().clearCalibration()
        AppPreferences.stepCalibration = null
    }
}
