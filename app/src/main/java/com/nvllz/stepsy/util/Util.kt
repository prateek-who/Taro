package com.nvllz.stepsy.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.nvllz.stepsy.R
import java.text.NumberFormat
import java.time.LocalDate
import java.util.*

object Util {
    enum class UnitSystem {
        METRIC, IMPERIAL
    }

    internal val calendar: Calendar
        get() = todayCalendar()

    internal fun todayDateString(): String = millisToDateString(System.currentTimeMillis())

    internal fun todayCalendar(): Calendar = Calendar.getInstance().apply {
        timeInMillis = dateStringToCalendarMillis(todayDateString())
        firstDayOfWeek = AppPreferences.firstDayOfWeek
    }

    internal fun logicalToday(): LocalDate = LocalDate.parse(todayDateString())

    internal fun fractionOfToday(nowMs: Long = System.currentTimeMillis()): Double =
        DayClock.fractionOfDay(nowMs, AppPreferences.dayStartMinutes)

    internal fun nextDayStartMillis(nowMs: Long = System.currentTimeMillis()): Long =
        DayClock.nextStart(nowMs, AppPreferences.dayStartMinutes)

    internal fun dateStringToCalendarMillis(date: String): Long {
        return try {
            val parts = date.split("-")
            val cal = Calendar.getInstance().apply {
                firstDayOfWeek = AppPreferences.firstDayOfWeek
                set(Calendar.YEAR, parts[0].toInt())
                set(Calendar.MONTH, parts[1].toInt() - 1)
                set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        } catch (_: Exception) {
            0L
        }
    }

    internal fun millisToDateString(millis: Long): String = DayClock.dateOf(millis, AppPreferences.dayStartMinutes)

    internal fun calendarToDateString(cal: Calendar): String {
        return "%04d-%02d-%02d".format(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    internal fun distanceUnit(): String = if (AppPreferences.unitSystem == UnitSystem.IMPERIAL) "mi" else "km"
    internal fun weightUnit(): String   = if (AppPreferences.unitSystem == UnitSystem.IMPERIAL) "lbs" else "kg"
    internal fun heightUnit(): String   = if (AppPreferences.unitSystem == UnitSystem.IMPERIAL) "ft/in" else "cm"
    internal fun stepLengthUnit(): String = if (AppPreferences.unitSystem == UnitSystem.IMPERIAL) "in" else "cm"

    fun stepsToDistance(steps: Number): Float {
        val meters = (steps.toInt() * AppPreferences.stepLength) / 100000
        return when (AppPreferences.unitSystem) {
            UnitSystem.METRIC   -> meters
            UnitSystem.IMPERIAL -> meters * 0.621371f
        }
    }

    private const val STEP_TO_HEIGHT = 0.415f
    private const val INSEAM_TO_HEIGHT = 0.46f

    fun estimateStepLength(heightCm: Double, legLengthCm: Int?): Float {
        val fromHeight = (heightCm * STEP_TO_HEIGHT).toFloat()
        val leg = legLengthCm ?: return fromHeight
        val plausibleLeg = (heightCm * 0.35).toFloat()..(heightCm * 0.6).toFloat()
        if (leg.toFloat() !in plausibleLeg) return fromHeight
        val fromLeg = leg * STEP_TO_HEIGHT / INSEAM_TO_HEIGHT
        return (fromHeight + fromLeg) / 2
    }

    private const val LBS_PER_KG = 2.20462

    fun kgToDisplay(kg: Double): Double = if (AppPreferences.unitSystem == UnitSystem.IMPERIAL) kg * LBS_PER_KG else kg

    fun displayToKg(value: Double): Double = if (AppPreferences.unitSystem == UnitSystem.IMPERIAL) value / LBS_PER_KG else value

    fun formatWeight(kg: Double): String = "%.1f %s".format(Locale.getDefault(), kgToDisplay(kg), weightUnit())

    fun formatWeightChange(kg: Double): String = "%+.2f %s".format(Locale.getDefault(), kgToDisplay(kg), weightUnit())

    fun formatMeasure(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else "%.1f".format(Locale.getDefault(), value)

    fun parseMeasure(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

    fun goalMultiplier(steps: Int, goal: Int): String? {
        if (goal <= 0 || steps < goal * 1.05) return null
        return "×%.1f".format(Locale.getDefault(), steps.toFloat() / goal)
    }

    internal fun formatSteps(steps: Int): String =
        if (steps >= 10_000) NumberFormat.getIntegerInstance().format(steps) else steps.toString()

    internal fun stepsPlural(context: Context, steps: Int): String =
        context.resources.getQuantityString(R.plurals.steps_formatted, steps, formatSteps(steps))

    internal fun metersToDistance(meters: Double): Float {
        val km = (meters / 1000.0).toFloat()
        return if (AppPreferences.unitSystem == UnitSystem.IMPERIAL) km * 0.621371f else km
    }

    internal fun applyTheme(theme: String) {
        when (theme) {
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            "light"  -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark"   -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
}