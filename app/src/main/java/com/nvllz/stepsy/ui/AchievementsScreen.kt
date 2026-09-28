package com.nvllz.stepsy.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.nvllz.stepsy.R
import com.nvllz.stepsy.ui.components.StepsyScaffold
import com.nvllz.stepsy.ui.theme.StepsyTheme
import com.nvllz.stepsy.util.AchievementsCacheUtil
import com.nvllz.stepsy.util.AppPreferences
import com.nvllz.stepsy.util.Database
import com.nvllz.stepsy.util.Util
import com.nvllz.stepsy.util.Util.UnitSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AchievementsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        AchievementsContent(onBack = { navigator.pop() })
    }
}

data class MilestoneAchievement(val milestone: Int, val timestamp: Long)

data class Top3DayEntry(val steps: Int, val timestamp: Long)

data class ComputedResults(
    val top3Days: List<Top3DayEntry>,
    val bestWeek: String,
    val bestMonth: String,
    val streakRecord: String,
    val avgStepsPerDay: String,
    val milestones: List<MilestoneAchievement>,
)

private val MILESTONE_TARGETS = listOf(
    10_000, 50_000, 100_000, 500_000, 750_000, 1_000_000, 1_500_000, 2_000_000, 3_000_000,
    4_000_000, 5_000_000, 6_000_000, 7_000_000, 8_000_000, 9_000_000, 10_000_000, 12_500_000,
    15_000_000, 20_000_000
)

private data class AchievementsState(
    val top3Days: List<Top3DayEntry>?,
    val bestWeek: String,
    val bestMonth: String,
    val streakRecord: String,
    val avgStepsPerDay: String,
    val milestones: List<MilestoneAchievement>,
    val placeholder: String,
)

private fun ComputedResults.toState(noData: String) = AchievementsState(
    top3Days = top3Days,
    bestWeek = bestWeek,
    bestMonth = bestMonth ?: noData,
    streakRecord = streakRecord,
    avgStepsPerDay = avgStepsPerDay,
    milestones = milestones,
    placeholder = noData,
)

private fun uniformState(value: String, streak: String = value) =
    AchievementsState(emptyList(), value, value, streak, value, emptyList(), value)

@Composable
private fun AchievementsContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val noData = stringResource(R.string.no_data_available)
    val errorText = stringResource(R.string.error_loading_data)
    val loading = stringResource(R.string.loading_data)
    val dateFormat = remember { SimpleDateFormat(AppPreferences.dateFormatString, Locale.getDefault()) }

    var state by remember {
        val cached = AchievementsCacheUtil.loadCachedResults(context)
        mutableStateOf(
            if (cached?.top3Days != null) cached.toState(noData)
            else uniformState(loading).copy(top3Days = null)
        )
    }

    LaunchedEffect(Unit) {
        state = try {
            val database = Database.getInstance(context)
            val (firstEntry, lastEntry) = withContext(Dispatchers.IO) { database.firstEntry to database.lastEntry }
            if (firstEntry == "" || lastEntry == "") {
                uniformState(noData, streak = errorText)
            } else {
                val results = withContext(Dispatchers.Default) {
                    computeAllResults(context, database, firstEntry, lastEntry, dateFormat)
                }
                val ordered = results.copy(milestones = results.milestones.sortedByDescending { it.timestamp })
                AchievementsCacheUtil.saveCachedResults(context, ordered)
                ordered.toState(noData)
            }
        } catch (_: Exception) {
            uniformState(errorText)
        }
    }

    val streakTitle = stringResource(
        R.string.streak_record,
        NumberFormat.getIntegerInstance().format(AppPreferences.dailyGoalTarget)
    )

    StepsyScaffold(title = stringResource(R.string.achievements_title), onBack = onBack) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(10.dp),
            modifier = Modifier
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            item { SectionTitle(R.string.personal_records, Modifier.padding(top = 20.dp, bottom = 16.dp)) }
            item { TopDaysCard(state.top3Days, state.placeholder, dateFormat) }
            item { RecordCard(stringResource(R.string.best_week), state.bestWeek) }
            item { RecordCard(stringResource(R.string.most_walked_month), state.bestMonth) }
            item { RecordCard(stringResource(R.string.avg_steps_per_day), state.avgStepsPerDay) }
            item { RecordCard(streakTitle, state.streakRecord, Modifier.padding(bottom = 12.dp)) }
            item {
                HorizontalDivider(
                    color = StepsyTheme.colors.accent.copy(alpha = 0.3f),
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            item { SectionTitle(R.string.milestone_achievements, Modifier.padding(top = 20.dp, bottom = 16.dp)) }

            if (state.milestones.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_milestones_reached),
                        color = StepsyTheme.colors.accent,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                    )
                }
            } else {
                itemsIndexed(state.milestones, key = { _, it -> it.milestone }) { index, milestone ->
                    MilestoneRow(milestone, dateFormat, isLast = index == state.milestones.lastIndex)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(textRes: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(textRes),
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.05.em,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun RecordSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 2.dp, end = 2.dp, bottom = 8.dp)
            .background(StepsyTheme.colors.accentOpaque, RoundedCornerShape(22.dp))
            .padding(horizontal = 13.dp, vertical = 14.dp),
    ) {
        content()
    }
}

@Composable
private fun RecordLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 15.sp,
        letterSpacing = (-0.02).em,
        modifier = modifier.alpha(0.87f),
    )
}

@Composable
private fun TopDaysCard(
    top3Days: List<Top3DayEntry>?,
    placeholder: String,
    dateFormat: DateFormat,
) {
    RecordSurface {
        Column {
            RecordLabel(
                text = stringResource(R.string.top_3_days),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            )
            Row {
                repeat(3) { index ->
                    val entry = top3Days?.getOrNull(index)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = entry?.let { formatStepsWithDistance(it.steps) } ?: placeholder,
                            color = StepsyTheme.colors.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.02).em,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = entry?.let { dateFormat.format(Date(it.timestamp)) }.orEmpty(),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.alpha(0.6f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordCard(label: String, value: String, modifier: Modifier = Modifier) {
    RecordSurface(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RecordLabel(text = label, modifier = Modifier.weight(40f))
            Text(
                text = value,
                color = StepsyTheme.colors.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(60f),
            )
        }
    }
}

@Composable
private fun MilestoneRow(
    milestone: MilestoneAchievement,
    dateFormat: DateFormat,
    isLast: Boolean,
) {
    val context = LocalContext.current
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 40.dp, vertical = 14.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(44.dp)
                    .background(StepsyTheme.colors.accentOpaque, CircleShape),
            ) {
                Text(milestoneBadge(milestone.milestone), fontSize = 22.sp)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f),
            ) {
                Text(formatMilestoneTitle(context, milestone.milestone), fontSize = 16.sp)
                Text(
                    text = dateFormat.format(Date(milestone.timestamp)),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .alpha(0.6f),
                )
            }
        }
        if (!isLast) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                modifier = Modifier.padding(start = 80.dp, end = 20.dp),
            )
        }
    }
}

private fun milestoneBadge(milestone: Int) = when {
    milestone >= 20_000_000 -> "🏁"
    milestone >= 15_000_000 -> "♾️"
    milestone >= 12_500_000 -> "🪬"
    milestone >= 10_000_000 -> "👑"
    milestone >= 9_000_000 -> "🦄"
    milestone >= 8_000_000 -> "🐉"
    milestone >= 7_000_000 -> "💫"
    milestone >= 6_000_000 -> "🏆"
    milestone >= 5_000_000 -> "💎"
    milestone >= 4_000_000 -> "🪐"
    milestone >= 3_000_000 -> "🚀"
    milestone >= 2_000_000 -> "🥇"
    milestone >= 1_500_000 -> "⚡"
    milestone >= 1_000_000 -> "🗿"
    milestone >= 750_000 -> "⛳"
    milestone >= 500_000 -> "🌟"
    milestone >= 100_000 -> "🔥"
    milestone >= 50_000 -> "💪"
    else -> "🎯"
}

private fun formattedDistance(steps: Int): String {
    val distanceKm = steps * AppPreferences.stepLength / 100000f
    val distance = if (AppPreferences.unitSystem == UnitSystem.METRIC) distanceKm else distanceKm * 0.621371f
    return "%.2f ${Util.distanceUnit()}".format(distance)
}

private fun formatStepsWithDistance(steps: Int): String {
    return "${Util.formatSteps(steps)} • ${formattedDistance(steps)}"
}

private fun formatMilestoneTitle(context: Context, steps: Int): String {
    val distancePart = formattedDistance(steps)
    return when {
        steps >= 1_000_000 -> {
            val millions = steps / 1_000_000.0
            if (millions == millions.toInt().toDouble()) {
                context.getString(R.string.million_steps_with_distance, millions.toInt(), distancePart)
            } else {
                context.getString(R.string.million_steps_decimal_with_distance, millions, distancePart)
            }
        }
        steps >= 1_000 -> context.getString(R.string.thousand_steps_with_distance, steps / 1_000, distancePart)
        else -> context.getString(R.string.steps_count_with_distance, steps, distancePart)
    }
}

private fun computeAllResults(
    context: Context,
    database: Database,
    firstEntry: String?,
    lastEntry: String?,
    dateFormat: DateFormat,
): ComputedResults {
    val entries = database.getEntries(firstEntry, lastEntry)
    val noData = context.getString(R.string.no_data_available)

    if (entries.isEmpty()) {
        return ComputedResults(emptyList(), noData, noData, noData, noData, emptyList())
    }

    val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val displayFormat = SimpleDateFormat("LLLL yyyy", Locale.getDefault())
    val firstEntryTimestamp = entries.minOf { it.timestamp }
    val milestones = calculateMilestones(entries)
    val (longestStreak, streakRange) = calculateLongestStreak(entries)
    val top3Days = entries.sortedByDescending { it.steps }.take(3)
        .map { Top3DayEntry(it.steps, it.timestamp) }

    val firstDayOfWeek = AppPreferences.firstDayOfWeek
    val weeklySteps = mutableMapOf<Long, Int>()
    val weeklyRange = mutableMapOf<Long, Pair<Long, Long>>()
    val monthlySteps = mutableMapOf<String, Int>()
    var totalSteps = 0

    for (entry in entries) {
        totalSteps += entry.steps

        val cal = Calendar.getInstance().apply {
            timeInMillis = entry.timestamp
            this.firstDayOfWeek = firstDayOfWeek
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        while (cal.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        val weekKey = cal.timeInMillis
        weeklySteps[weekKey] = (weeklySteps[weekKey] ?: 0) + entry.steps
        val existing = weeklyRange[weekKey]
        weeklyRange[weekKey] = if (existing == null) {
            entry.timestamp to entry.timestamp
        } else {
            minOf(existing.first, entry.timestamp) to maxOf(existing.second, entry.timestamp)
        }

        val monthKey = monthFormat.format(Date(entry.timestamp))
        monthlySteps[monthKey] = (monthlySteps[monthKey] ?: 0) + entry.steps
    }

    fun range(start: Long, end: Long) = "${dateFormat.format(Date(start))} - ${dateFormat.format(Date(end))}"

    val bestWeek = weeklySteps.maxByOrNull { it.value }?.let { best ->
        val (start, end) = weeklyRange.getValue(best.key)
        "${formatStepsWithDistance(best.value)}\n${range(start, end)}"
    } ?: noData

    val bestMonth = monthlySteps.maxByOrNull { it.value }?.let { best ->
        val date = monthFormat.parse(best.key) ?: Date()
        "${formatStepsWithDistance(best.value)}\n${displayFormat.format(date)}"
    } ?: noData

    val avgSteps = totalSteps / entries.size
    val avgStepsPerDay = "${formatStepsWithDistance(avgSteps)}\n" +
        context.getString(R.string.since_date, dateFormat.format(Date(firstEntryTimestamp)))

    val streakRecord = if (longestStreak > 0 && streakRange != null) {
        val dateText = if (longestStreak == 1) {
            dateFormat.format(Date(streakRange.second))
        } else {
            range(streakRange.first, streakRange.second)
        }
        context.resources.getQuantityString(R.plurals.streak_record_count, longestStreak, longestStreak) + "\n$dateText"
    } else {
        context.resources.getQuantityString(R.plurals.streak_record_count, 0, 0)
    }

    return ComputedResults(top3Days, bestWeek, bestMonth, streakRecord, avgStepsPerDay, milestones)
}

private fun calculateMilestones(entries: List<Database.Entry>): List<MilestoneAchievement> {
    val achievements = mutableListOf<MilestoneAchievement>()
    var cumulativeSteps = 0
    var nextIndex = 0

    for (entry in entries.sortedBy { it.timestamp }) {
        cumulativeSteps += entry.steps
        while (nextIndex < MILESTONE_TARGETS.size && cumulativeSteps >= MILESTONE_TARGETS[nextIndex]) {
            achievements.add(MilestoneAchievement(MILESTONE_TARGETS[nextIndex], entry.timestamp))
            nextIndex++
        }
        if (nextIndex >= MILESTONE_TARGETS.size) break
    }

    return achievements.sortedByDescending { it.milestone }
}

private fun calculateLongestStreak(entries: List<Database.Entry>): Pair<Int, Pair<Long, Long>?> {
    if (entries.isEmpty()) return 0 to null

    val dailyGoal = AppPreferences.dailyGoalTarget
    var currentStreak = 0
    var longestStreak = 0
    var streakStart: Long? = null
    var longestStreakRange: Pair<Long, Long>? = null
    var previousDate: LocalDate? = null

    for (entry in entries.sortedBy { it.date }) {
        val date = LocalDate.parse(entry.date)
        val isConsecutive = previousDate == null || date == previousDate.plusDays(1)
        val metGoal = entry.steps >= dailyGoal

        if (metGoal && isConsecutive) {
            currentStreak++
            if (streakStart == null) streakStart = entry.timestamp
            if (currentStreak > longestStreak) {
                longestStreak = currentStreak
                longestStreakRange = streakStart to entry.timestamp
            }
        } else {
            currentStreak = if (metGoal) 1 else 0
            streakStart = if (metGoal) entry.timestamp else null
        }

        previousDate = date
    }

    return longestStreak to longestStreakRange
}
