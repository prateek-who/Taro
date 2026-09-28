package com.nvllz.stepsy.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.nvllz.stepsy.R
import com.nvllz.stepsy.energy.ActivityEnergy
import com.nvllz.stepsy.util.AppPreferences
import com.nvllz.stepsy.util.Database
import com.nvllz.stepsy.util.StreakCalculator
import com.nvllz.stepsy.util.Util
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class StepRange(val key: String, @StringRes val label: Int, @StringRes val header: Int) {
    TODAY("TODAY", R.string.label_today, R.string.header_today),
    WEEK("WEEK", R.string.label_week, R.string.header_week),
    MONTH("MONTH", R.string.label_month, R.string.header_month),
    SEVEN_DAYS("7 DAYS", R.string.label_7d, R.string.header_7d),
    THIRTY_DAYS("30 DAYS", R.string.label_30d, R.string.header_30d),
    ALL_TIME("ALL TIME", R.string.label_all_time, R.string.header_all_time),
}

sealed interface Selection {
    data class Range(val range: StepRange) : Selection
    data class Year(val year: Int) : Selection
}

data class Summary(
    val header: String,
    val steps: String,
    val distance: String,
    val calories: String?,
    val average: String?,
)

data class ChartData(val header: String, val range: String, val values: List<Int>, val labels: List<String>)

data class DayStats(val header: String, val details: String, val monthTotal: String, val monthAverage: String)

data class GoalLine(val text: String, val streak: Boolean)

private const val RANGE_PREFS = "RangePrefs"
private const val KEY_SELECTED_RANGE = "selected_range"
private const val KEY_SELECTED_YEAR = "selected_year"
private const val KEY_IS_YEAR_SELECTED = "is_year_selected"
private const val KEY_PAST_7_DAYS = "is_chart_past_7_days"

private fun Context.rangePrefs() = getSharedPreferences(RANGE_PREFS, Context.MODE_PRIVATE)

fun loadSelection(context: Context): Selection {
    val prefs = context.rangePrefs()
    val year = prefs.getInt(KEY_SELECTED_YEAR, -1)
    if (prefs.getBoolean(KEY_IS_YEAR_SELECTED, false) && year != -1) return Selection.Year(year)
    val key = prefs.getString(KEY_SELECTED_RANGE, null)
    return Selection.Range(StepRange.entries.firstOrNull { it.key == key } ?: StepRange.TODAY)
}

fun saveSelection(context: Context, selection: Selection) = context.rangePrefs().edit {
    when (selection) {
        is Selection.Range -> putString(KEY_SELECTED_RANGE, selection.range.key)
        is Selection.Year -> putInt(KEY_SELECTED_YEAR, selection.year)
    }
    putBoolean(KEY_IS_YEAR_SELECTED, selection is Selection.Year)
}

fun loadPast7DaysMode(context: Context) = context.rangePrefs().getBoolean(KEY_PAST_7_DAYS, true)

fun savePast7DaysMode(context: Context, enabled: Boolean) = context.rangePrefs().edit {
    putBoolean(KEY_PAST_7_DAYS, enabled)
}

fun yearsWithData(context: Context): List<Int> {
    val db = Database.getInstance(context)
    val firstYear = db.firstEntry?.take(4)?.toIntOrNull() ?: return emptyList()
    return (firstYear..Calendar.getInstance().get(Calendar.YEAR)).filter { year ->
        db.getSumSteps("%04d-01-01".format(year), "%04d-12-31".format(year)) > 0
    }
}

fun firstEntryDate(context: Context): LocalDate =
    Database.getInstance(context).firstEntry?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()

private fun dateFormat() = SimpleDateFormat(AppPreferences.dateFormatString, Locale.getDefault())

private fun distance(context: Context, meters: Double) =
    context.getString(R.string.distance_today, Util.metersToDistance(meters), Util.distanceUnit())

private fun stepsWithDistance(context: Context, steps: Int) =
    context.getString(R.string.steps_format, Util.formatSteps(steps), Util.stepsToDistance(steps), Util.distanceUnit())

private fun rangeDates(range: StepRange, db: Database): Pair<String, String> {
    val calendar = Calendar.getInstance()
    fun start(days: Int): Pair<String, String> {
        val end = Util.calendarToDateString(calendar)
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        return Util.calendarToDateString(calendar) to end
    }
    return when (range) {
        StepRange.WEEK -> {
            calendar.firstDayOfWeek = AppPreferences.firstDayOfWeek
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            val first = Util.calendarToDateString(calendar)
            calendar.add(Calendar.DAY_OF_YEAR, 6)
            first to Util.calendarToDateString(calendar)
        }
        StepRange.MONTH -> {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val first = Util.calendarToDateString(calendar)
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            first to Util.calendarToDateString(calendar)
        }
        StepRange.SEVEN_DAYS -> start(6)
        StepRange.THIRTY_DAYS -> start(29)
        StepRange.ALL_TIME, StepRange.TODAY -> {
            val today = Util.todayDateString()
            (db.firstEntry ?: today) to (db.lastEntry ?: today)
        }
    }
}

fun summary(context: Context, selection: Selection, todaySteps: Int): Summary {
    if (selection == Selection.Range(StepRange.TODAY)) {
        val today = ActivityEnergy.day(context, Util.todayDateString(), liveTodaySteps = todaySteps)
        return Summary(
            header = context.getString(R.string.header_today),
            steps = Util.stepsPlural(context, todaySteps),
            distance = distance(context, today.distanceM),
            calories = context.getString(R.string.calories, today.activeKcal.roundToInt()),
            average = null,
        )
    }

    val db = Database.getInstance(context)
    val (header, dates) = when (selection) {
        is Selection.Year ->
            context.getString(R.string.header_year, selection.year) to
                ("%04d-01-01".format(selection.year) to "%04d-12-31".format(selection.year))
        is Selection.Range -> {
            val dates = rangeDates(selection.range, db)
            val header = if (selection.range == StepRange.ALL_TIME) {
                context.getString(R.string.since_date, dateFormat().format(Date(Util.dateStringToCalendarMillis(dates.first))))
            } else {
                context.getString(selection.range.header)
            }
            header to dates
        }
    }

    val totals = ActivityEnergy.range(context, dates.first, dates.second)
    return Summary(
        header = header,
        steps = Util.stepsPlural(context, totals.steps),
        distance = distance(context, totals.distanceM),
        calories = null,
        average = stepsWithDistance(context, db.avgSteps(dates.first, dates.second)),
    )
}

fun chartData(context: Context, past7Days: Boolean, selected: LocalDate, todaySteps: Int): ChartData {
    val start = Calendar.getInstance().apply {
        if (past7Days) {
            add(Calendar.DAY_OF_YEAR, -6)
        } else {
            timeInMillis = Util.dateStringToCalendarMillis(selected.toString())
            firstDayOfWeek = AppPreferences.firstDayOfWeek
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        }
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val days = (0 until 7).map { offset ->
        (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
    }
    val dates = days.map(Util::calendarToDateString)
    val stepsByDate = Database.getInstance(context).getEntries(dates.first(), dates.last()).associate { it.date to it.steps }
    val today = Util.todayDateString()
    val values = dates.map { if (it == today) todaySteps else stepsByDate[it] ?: 0 }
    val labels = days.map { it.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()).orEmpty() }

    val format = dateFormat()
    val header = if (past7Days) {
        context.getString(R.string.header_7d)
    } else {
        context.getString(R.string.week_display_format, start.get(Calendar.WEEK_OF_YEAR))
    }
    val range = context.getString(
        R.string.week_display_range,
        format.format(days.first().time),
        format.format(days.last().time),
    )
    return ChartData(header.uppercase(), range, values, labels)
}

fun dayStats(context: Context, selected: LocalDate): DayStats {
    val db = Database.getInstance(context)
    val date = selected.toString()
    val day = ActivityEnergy.day(context, date)
    val steps = day.steps
    val monthStart = selected.withDayOfMonth(1).toString()
    val monthEnd = selected.withDayOfMonth(selected.lengthOfMonth()).toString()

    return DayStats(
        header = dateFormat().format(Date(Util.dateStringToCalendarMillis(date))),
        details = context.getString(
            R.string.steps_day_display,
            Util.stepsPlural(context, steps),
            Util.metersToDistance(day.distanceM),
            Util.distanceUnit(),
            day.activeKcal.roundToInt(),
        ),
        monthTotal = stepsWithDistance(context, db.getSumSteps(monthStart, monthEnd)),
        monthAverage = stepsWithDistance(context, db.avgSteps(monthStart, monthEnd)),
    )
}

fun goalLine(context: Context, target: Int): GoalLine? {
    StreakCalculator.calculateGoalStreak(context, Database.getInstance(context), target)?.let { (_, text) ->
        return GoalLine(text, streak = true)
    }
    if (target <= 0) return null
    return GoalLine(context.getString(R.string.goal_streak_dead_line, Util.stepsPlural(context, target)), streak = false)
}
