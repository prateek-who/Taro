package com.prateek.taro.ui

import com.prateek.taro.report.ReportData
import java.time.temporal.WeekFields
import com.prateek.taro.util.DayScore
import com.prateek.taro.energy.MinuteSample
import com.prateek.taro.util.StepPaces
import com.prateek.taro.util.StepPace
import com.prateek.taro.util.Walks
import com.prateek.taro.util.Walk
import com.prateek.taro.util.GoalHistory
import android.content.Context
import androidx.annotation.StringRes
import com.prateek.taro.R
import com.prateek.taro.energy.ActivityEnergy
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Database
import com.prateek.taro.util.Forgive
import com.prateek.taro.util.Streak
import com.prateek.taro.util.Streaks
import com.prateek.taro.util.Util
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
    val steps: Int,
    val distance: String,
    val calories: String,
    val averageSteps: Int?,
    val goalTotal: Int = 0,
    val distanceValue: String = "",
    val distanceUnit: String = "",
    val caloriesValue: String = "",
)

data class ChartData(
    val header: String,
    val range: String,
    val values: List<Int>,
    val labels: List<String>,
    val dates: List<LocalDate>,
)


fun firstEntryDate(context: Context): LocalDate =
    Database.getInstance(context).firstEntry?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()

private fun dateFormat() = SimpleDateFormat(AppPreferences.dateFormatString, Locale.getDefault())

private fun distance(context: Context, meters: Double) =
    context.getString(R.string.distance_today, Util.metersToDistance(meters), Util.distanceUnit())

private fun distanceValue(meters: Double) = "%.2f".format(Locale.getDefault(), Util.metersToDistance(meters))

private fun kcalValue(kcal: Double) = Util.formatSteps((kcal * AppPreferences.energyFactors.activity).roundToInt())

private fun kcal(context: Context, kcal: Double) = context.getString(R.string.kcal_short, kcalValue(kcal))

private fun rangeDates(range: StepRange, db: Database): Pair<String, String> {
    val calendar = Util.todayCalendar()
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
            steps = todaySteps,
            distance = distance(context, today.distanceM),
            distanceValue = distanceValue(today.distanceM),
            distanceUnit = Util.distanceUnit(),
            calories = kcal(context, today.activeKcal),
            caloriesValue = kcalValue(today.activeKcal),
            averageSteps = null,
            goalTotal = AppPreferences.goalHistory.on(Util.logicalToday()),
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
        steps = totals.steps,
        distance = distance(context, totals.distanceM),
        distanceValue = distanceValue(totals.distanceM),
        distanceUnit = Util.distanceUnit(),
        calories = kcal(context, totals.activeKcal),
        caloriesValue = kcalValue(totals.activeKcal),
        averageSteps = db.avgSteps(dates.first, dates.second),
        goalTotal = runCatching { AppPreferences.goalHistory.total(LocalDate.parse(dates.first), LocalDate.parse(dates.second)) }.getOrDefault(0),
    )
}

fun chartData(context: Context, past7Days: Boolean, selected: LocalDate, todaySteps: Int): ChartData {
    val start = Util.todayCalendar().apply {
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
    return ChartData(header.uppercase(), range, values, labels, dates.map(LocalDate::parse))
}

fun daySummary(context: Context, date: LocalDate): Summary {
    val day = ActivityEnergy.day(context, date.toString())
    return Summary(
        header = dateFormat().format(Date(Util.dateStringToCalendarMillis(date.toString()))),
        steps = day.steps,
        distance = distance(context, day.distanceM),
        distanceValue = distanceValue(day.distanceM),
        distanceUnit = Util.distanceUnit(),
        calories = kcal(context, day.activeKcal),
        caloriesValue = kcalValue(day.activeKcal),
        averageSteps = null,
        goalTotal = AppPreferences.goalHistory.on(date),
    )
}

fun stepStreak(days: Map<LocalDate, Int>, todaySteps: Int, goals: GoalHistory, choices: Map<LocalDate, Forgive>, preferRest: Boolean): Streak {
    val today = Util.logicalToday()
    val steps = days + (today to todaySteps)
    val first = steps.filterValues { it > 0 }.keys.minOrNull() ?: return Streak()
    return Streaks.compute(
        start = first,
        end = today,
        today = today,
        value = { (steps[it] ?: 0).toDouble() },
        goal = { goals.on(it).toDouble() },
        choices = choices,
        makeUp = true,
        preferRest = preferRest,
    )
}


fun stepsByDay(context: Context): Map<LocalDate, Int> {
    val db = Database.getInstance(context)
    return db.getEntries(db.firstEntry, db.lastEntry)
        .mapNotNull { entry -> runCatching { LocalDate.parse(entry.date) }.getOrNull()?.let { it to entry.steps } }
        .toMap()
}

fun heatFractions(steps: Map<LocalDate, Int>, goals: GoalHistory): Map<LocalDate, Float> {
    val most = (steps.values.maxOrNull() ?: 0).toFloat()
    return steps.mapValues { (date, value) ->
        val scale = goals.on(date).takeIf { it > 0 }?.toFloat() ?: most
        if (scale <= 0f) 0f else (value / scale).coerceAtMost(1f)
    }
}

data class WalkRow(val walk: Walk, val distanceM: Double, val longest: Boolean)

data class StepsDay(val pace: StepPace, val walks: List<WalkRow>, val cadence: Double?)

private const val HOUR_MS = 3_600_000L
private const val WALKING_CADENCE = 60

fun stepsDay(context: Context, date: LocalDate, total: Int, goal: Int, nowMs: Long = System.currentTimeMillis()): StepsDay {
    val database = Database.getInstance(context)
    val rows = database.getMinutes(date.minusDays(StepPaces.DAYS).toString(), date.toString()).groupBy { it.date }
    fun hours(day: LocalDate) = StepPaces.hourly(rows[day.toString()].orEmpty().map { it.minuteStart to it.steps }, Util.dayStartMillis(day))
    val past = (1..StepPaces.DAYS).associate { date.minusDays(it) to hours(date.minusDays(it)) }
    val start = Util.dayStartMillis(date)
    val now = if (date == Util.logicalToday()) ((nowMs - start).toDouble() / HOUR_MS).coerceIn(0.0, StepPaces.HOURS.toDouble()) else null
    val pace = StepPaces.build(date, hours(date), total, past, now, goal)

    val today = rows[date.toString()].orEmpty()
    val walks = Walks.merged(Walks.all(today.map { it.minuteStart to it.steps }), WALK_MERGE_MS)
    val longest = walks.maxByOrNull { it.minutes }
    val walkRows = walks.map { walk ->
        val samples = today.filter { it.minuteStart >= walk.start && it.minuteStart < walk.end }
        WalkRow(walk, ActivityEnergy.minutes(samples.map { MinuteSample(it.steps, it.ascentMeters.toDouble()) }).distanceM, walk == longest && walks.size > 1)
    }
    val walking = rows.values.flatten().map { it.steps }.filter { it >= WALKING_CADENCE }.sorted()
    val cadence = walking.takeIf { it.isNotEmpty() }?.let { it[it.size / 2].toDouble() }
    return StepsDay(pace, walkRows, cadence)
}

data class GoalSuggestions(val average: Int?, val bestWeek: Int?)

fun goalSuggestions(context: Context, today: LocalDate): GoalSuggestions {
    val days = stepsByDay(context)
    fun tracked(range: IntRange) = range.mapNotNull { days[today.minusDays(it.toLong())]?.takeIf { steps -> steps > 0 } }
    val average = tracked(1..SUGGEST_DAYS).takeIf { it.size >= MIN_WEEK_DAYS }?.average()
    val bestWeek = (0 until SUGGEST_WEEKS).mapNotNull { week -> tracked(week * 7 + 1..week * 7 + 7).takeIf { it.size >= MIN_WEEK_DAYS }?.average() }.maxOrNull()
    fun round(value: Double) = ((value / GOAL_STEP).roundToInt() * GOAL_STEP).coerceAtLeast(GOAL_STEP)
    return GoalSuggestions(average?.let(::round), bestWeek?.let(::round))
}

const val GOAL_STEP = 500
private const val SUGGEST_DAYS = 28
private const val SUGGEST_WEEKS = 12
private const val MIN_WEEK_DAYS = 5

data class DayTally(val date: LocalDate, val score: DayScore, val weekBest: Int?) {
    val bestOfWeek: Boolean get() = weekBest != null && score.score > weekBest
}

fun dayTally(context: Context, date: LocalDate): DayTally? {
    val weekStart = date.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
    val scores = ReportData.scores(context, weekStart, date)
    val score = scores[date] ?: return null
    return DayTally(date, score, scores.filterKeys { it.isBefore(date) }.values.maxOfOrNull { it.score })
}

fun monthlyAverages(context: Context, year: Int, today: LocalDate, todaySteps: Int): List<Int> {
    val entries = Database.getInstance(context).getEntries("%04d-01-01".format(year), "%04d-12-31".format(year))
        .mapNotNull { entry -> runCatching { LocalDate.parse(entry.date) }.getOrNull()?.let { it to if (it == today) todaySteps else entry.steps } }
        .filter { it.second > 0 }
    return (1..12).map { month -> entries.filter { it.first.monthValue == month }.map { it.second }.takeIf { it.isNotEmpty() }?.average()?.toInt() ?: 0 }
}
