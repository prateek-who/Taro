package com.nvllz.stepsy.calibration

import android.content.Context
import com.nvllz.stepsy.data.CalibrationPoint
import com.nvllz.stepsy.data.StepsDatabase
import com.nvllz.stepsy.util.AppPreferences
import com.nvllz.stepsy.util.StepCalibration

object CalibrationRepository {
    const val SOURCE_GPS = "gps"
    const val SOURCE_KNOWN_DISTANCE = "known_distance"

    private const val HISTORY_MS = 180L * 24 * 60 * 60 * 1000

    fun save(context: Context, windows: List<CalibrationWindow>, source: String): StepLengthFit {
        val now = System.currentTimeMillis()
        val dao = StepsDatabase.get(context).steps()
        dao.insertCalibration(
            windows.map {
                CalibrationPoint(
                    recordedAt = now,
                    cadence = it.cadence,
                    stepLengthM = it.stepLengthM,
                    distanceM = it.distanceM,
                    steps = it.steps,
                    source = source,
                )
            }
        )
        return refit(context)
    }

    fun refit(context: Context): StepLengthFit {
        val points = StepsDatabase.get(context).steps().calibrationSince(System.currentTimeMillis() - HISTORY_MS)
        val fit = StepLengthModel.fit(points.map { CalibrationSample(it.cadence, it.stepLengthM, it.distanceM) })
        AppPreferences.stepCalibration = fit.walkingStepM?.let { walking ->
            StepCalibration(
                walkingStepCm = (walking * 100).toFloat(),
                walkingSlope = fit.walkingSlope.toFloat(),
                referenceCadence = fit.referenceCadence.toFloat(),
                runningStepCm = fit.runningStepM?.let { (it * 100).toFloat() },
                samples = fit.walkingSamples + fit.runningSamples,
            )
        }
        return fit
    }

    fun reset(context: Context) {
        StepsDatabase.get(context).steps().clearCalibration()
        AppPreferences.stepCalibration = null
    }
}
