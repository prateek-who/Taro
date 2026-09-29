package com.prateek.taro.energy

import android.content.Context
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Database
import com.prateek.taro.util.Util

data class ActivityTotals(val steps: Int, val activeKcal: Double, val distanceM: Double)

object ActivityEnergy {

    private const val RUNNING_TO_WALKING_STEP = 1.3

    fun body(): Body {
        val manual = AppPreferences.manualStepLength
        val calibration = AppPreferences.stepCalibration
        val walkingCm = manual ?: calibration?.walkingStepCm ?: AppPreferences.estimatedStepLength
        val walking = walkingCm / 100.0
        return Body(
            weightKg = AppPreferences.weight,
            walkingStepM = walking,
            runningStepM = calibration?.runningStepCm?.div(100.0) ?: (walking * RUNNING_TO_WALKING_STEP),
            walkingSlope = if (manual == null) calibration?.walkingSlope?.toDouble() ?: 0.0 else 0.0,
            referenceCadence = calibration?.referenceCadence?.toDouble() ?: 100.0,
        )
    }

    fun range(context: Context, from: String, to: String, liveTodaySteps: Int? = null): ActivityTotals {
        val db = Database.getInstance(context)
        val body = body()
        val steps = db.getEntries(from, to).associate { it.date to it.steps }.toMutableMap()
        val today = Util.todayDateString()
        if (liveTodaySteps != null && today in from..to) steps[today] = liveTodaySteps

        val minutesByDate = db.getMinutes(from, to).groupBy({ it.date }) { MinuteSample(it.steps, it.ascentMeters.toDouble()) }

        return steps.entries.fold(ActivityTotals(0, 0.0, 0.0)) { total, (date, daySteps) ->
            val minutes = minutesByDate[date].orEmpty()
            ActivityTotals(
                steps = total.steps + daySteps,
                activeKcal = total.activeKcal + EnergyModel.dayKcal(body, minutes, daySteps),
                distanceM = total.distanceM + EnergyModel.dayDistanceM(body, minutes, daySteps),
            )
        }
    }

    fun day(context: Context, date: String, liveTodaySteps: Int? = null) = range(context, date, date, liveTodaySteps)
}
