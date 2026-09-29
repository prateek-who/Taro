package com.nvllz.stepsy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableIntStateOf
import com.nvllz.stepsy.data.LoggedActivity
import com.nvllz.stepsy.ui.components.LocalToast
import com.nvllz.stepsy.ui.components.RingSegment
import com.nvllz.stepsy.ui.components.ToastKind
import com.nvllz.stepsy.util.Database
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.nvllz.stepsy.R
import com.nvllz.stepsy.energy.DailyEnergy
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.nvllz.stepsy.energy.WeightJournal
import com.nvllz.stepsy.energy.WeightTrend
import com.nvllz.stepsy.ui.components.SecondaryButton
import com.nvllz.stepsy.ui.components.WeightChart
import com.nvllz.stepsy.util.WeightReminderScheduler
import com.nvllz.stepsy.energy.Metabolism
import com.nvllz.stepsy.ui.components.HeroRing
import com.nvllz.stepsy.ui.components.RollingText
import com.nvllz.stepsy.ui.components.LegendItem
import com.nvllz.stepsy.ui.components.PrimaryButton
import com.nvllz.stepsy.ui.components.SettingsCard
import com.nvllz.stepsy.ui.components.SettingsDivider
import com.nvllz.stepsy.ui.components.StatRow
import com.nvllz.stepsy.ui.components.StepsBarChart
import com.nvllz.stepsy.ui.theme.StepsyTheme
import com.nvllz.stepsy.util.AppPreferences
import com.nvllz.stepsy.util.Util
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

object EnergyTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 1u,
            title = stringResource(R.string.tab_energy),
            icon = painterResource(R.drawable.ic_calories),
        )

    @Composable
    override fun Content() {
        EnergyContent(LocalTracking.current)
    }
}

private fun kcal(value: Double) = NumberFormat.getIntegerInstance().format(value.roundToInt())

private enum class ProfileStep { AGE, SEX }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EnergyContent(tracking: TrackingState) {
    val context = LocalContext.current
    val age by AppPreferences.ageFlow().collectAsStateWithLifecycle(AppPreferences.age)
    val sex by AppPreferences.sexFlow().collectAsStateWithLifecycle(AppPreferences.sex)
    val weight by AppPreferences.weightFlow().collectAsStateWithLifecycle(AppPreferences.weight)
    val height by AppPreferences.heightFlow().collectAsStateWithLifecycle(AppPreferences.height)

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = System.currentTimeMillis()
        }
    }
    val today = Util.millisToDateString(now)

    val toast = LocalToast.current
    var activityVersion by remember { mutableIntStateOf(0) }
    val history = remember(age, sex, weight, height, today, tracking.refreshKey, activityVersion) { DailyEnergy.history(context) }
    val todayBurn = remember(history, tracking.steps, now, activityVersion) {
        history?.let { DailyEnergy.today(context, it.restingPerDay, tracking.steps, now) }
    }
    val activities = remember(today, activityVersion) { Database.getInstance(context).activitiesOn(today) }

    var profileStep by remember { mutableStateOf<ProfileStep?>(null) }
    var logging by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf(false) }
    var loggingWeight by remember { mutableStateOf(false) }
    var weightVersion by remember { mutableIntStateOf(0) }
    val weightPoints = remember(weightVersion, tracking.refreshKey) { WeightJournal.points(context) }
    val navigator = LocalNavigator.currentOrThrow
    val rootNavigator = navigator.parent ?: navigator
    val calorieGoal by AppPreferences.calorieGoalFlow().collectAsStateWithLifecycle(AppPreferences.calorieGoal)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding(),
    ) {
        AppOverflowMenu()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.energy_header).uppercase(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
            )

            if (history == null || todayBurn == null) {
                SettingsCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.energy_missing_title),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(R.string.energy_missing_message),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(vertical = 12.dp)
                                .alpha(0.8f),
                        )
                        PrimaryButton(
                            text = stringResource(R.string.energy_add_details),
                            onClick = { profileStep = if (age == null) ProfileStep.AGE else ProfileStep.SEX },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                HeroRing(
                    segments = listOf(
                        RingSegment(todayBurn.resting, StepsyTheme.colors.accent),
                        RingSegment(todayBurn.active, StepsyTheme.colors.flame),
                        RingSegment(todayBurn.logged, StepsyTheme.colors.special),
                        RingSegment(todayBurn.digestion, StepsyTheme.colors.accent.copy(alpha = 0.45f)),
                    ),
                    target = history.dailyNeed,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RollingText(
                            text = kcal(todayBurn.total),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                        Text(stringResource(R.string.energy_burned_so_far), fontSize = 14.sp, color = StepsyTheme.colors.accent)
                        Text(
                            text = stringResource(R.string.energy_caption),
                            fontSize = 12.sp,
                            modifier = Modifier.alpha(0.7f),
                        )
                        Text(
                            text = stringResource(R.string.energy_of_need, kcal(history.dailyNeed)),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .alpha(0.7f),
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                ) {
                    LegendItem(StepsyTheme.colors.accent, stringResource(R.string.energy_resting), kcal(todayBurn.resting))
                    LegendItem(StepsyTheme.colors.flame, stringResource(R.string.energy_steps_label), kcal(todayBurn.active))
                    LegendItem(StepsyTheme.colors.special, stringResource(R.string.energy_logged), kcal(todayBurn.logged))
                    LegendItem(StepsyTheme.colors.accent.copy(alpha = 0.45f), stringResource(R.string.energy_digestion), kcal(todayBurn.digestion))
                }

                SettingsCard {
                    val kcalLabel = @Composable { value: Double -> stringResource(R.string.energy_kcal, kcal(value)) }
                    StatRow(stringResource(R.string.energy_daily_need), kcalLabel(history.dailyNeed))
                    SettingsDivider()
                    StatRow(
                        label = stringResource(R.string.calorie_goal),
                        value = if (calorieGoal == null) stringResource(R.string.calorie_goal_set) else calorieGoalSummary(calorieGoal),
                        onClick = { editingGoal = true },
                    )
                    calorieGoal?.let { goal ->
                        val target = Metabolism.calorieTarget(history.dailyNeed, goal.goal, goal.adjustment)
                        SettingsDivider()
                        StatRow(stringResource(R.string.calorie_goal_eat_today), kcalLabel(target))
                        if (target < history.restingPerDay) {
                            Text(
                                text = stringResource(R.string.calorie_goal_below_resting),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                            )
                        }
                    }
                    SettingsDivider()
                    StatRow(
                        stringResource(R.string.energy_projected),
                        kcalLabel(Metabolism.withDigestion(history.restingPerDay + todayBurn.active + todayBurn.logged)),
                    )
                    SettingsDivider()
                    StatRow(stringResource(R.string.energy_total_so_far), kcalLabel(todayBurn.total))
                }

                SectionTitle(R.string.weight_title)
                SettingsCard {
                    if (weightPoints.isEmpty()) {
                        Text(
                            text = stringResource(R.string.weight_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                                .alpha(0.7f),
                        )
                    } else {
                        val latest = weightPoints.last()
                        StatRow(stringResource(R.string.weight_trend), Util.formatWeight(latest.trend))
                        weekChange(weightPoints)?.let {
                            SettingsDivider()
                            StatRow(stringResource(R.string.weight_change_week), Util.formatWeightChange(it))
                        }
                        WeightChart(
                            points = weightPoints.filter { !it.date.isBefore(Util.logicalToday().minusDays(30)) },
                            toDisplay = Util::kgToDisplay,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        PaceMessage(
                            assessment = WeightTrend.assess(
                                WeightTrend.weeklyRate(weightPoints),
                                calorieGoal?.goal,
                                calorieGoal?.adjustment ?: 0,
                                latest.trend,
                            ),
                            goal = calorieGoal?.goal,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        PrimaryButton(
                            text = stringResource(R.string.weight_log),
                            onClick = { loggingWeight = true },
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(stringResource(R.string.weight_history), onClick = { rootNavigator.push(WeightScreen) })
                    }
                }

                SectionTitle(R.string.energy_logged_title)
                SettingsCard {
                    if (activities.isEmpty()) {
                        Text(
                            text = stringResource(R.string.energy_logged_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                                .alpha(0.7f),
                        )
                    } else {
                        activities.forEachIndexed { index, activity ->
                            if (index > 0) SettingsDivider()
                            LoggedActivityRow(activity) {
                                Database.getInstance(context).deleteActivity(activity.id)
                                activityVersion++
                                toast.show(context.getString(R.string.activity_deleted), ToastKind.INFO)
                            }
                        }
                    }
                    PrimaryButton(
                        text = stringResource(R.string.energy_add_activity),
                        onClick = { logging = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                }

                val week = history.pastDays + todayBurn
                SectionTitle(R.string.energy_last_7_days)
                Text(
                    text = stringResource(R.string.energy_last_7_days_caption),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .alpha(0.6f),
                )
                StepsBarChart(
                    values = week.map { it.total.roundToInt() },
                    labels = week.map { day ->
                        Calendar.getInstance().apply { timeInMillis = Util.dateStringToCalendarMillis(day.date) }
                            .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()).orEmpty()
                    },
                    goal = 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                )

                Text(
                    text = stringResource(R.string.energy_footnote),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 20.dp, bottom = 24.dp)
                        .alpha(0.6f),
                )
            }
        }
    }

    if (loggingWeight) {
        LogWeightDialog(
            title = stringResource(R.string.weight_dialog_title),
            initialKg = weightPoints.lastOrNull()?.kg ?: AppPreferences.weight,
            onSave = {
                WeightJournal.log(context, Util.todayDateString(), it)
                WeightReminderScheduler.dismissNotification(context)
                loggingWeight = false
                weightVersion++
                activityVersion++
                toast.show(context.getString(R.string.weight_saved), ToastKind.SUCCESS)
            },
            onDismiss = { loggingWeight = false },
        )
    }

    if (editingGoal) {
        CalorieGoalDialog(
            current = calorieGoal,
            onSave = {
                AppPreferences.calorieGoal = it
                editingGoal = false
                toast.show(context.getString(R.string.calorie_goal_saved), ToastKind.SUCCESS)
            },
            onDismiss = { editingGoal = false },
        )
    }

    if (logging) {
        LogActivityDialog(
            weightKg = weight.toDouble(),
            onSave = { name, minutes, kcal ->
                Database.getInstance(context).addActivity(
                    LoggedActivity(
                        date = Util.todayDateString(),
                        name = name,
                        kcal = kcal,
                        durationMinutes = minutes,
                        loggedAt = System.currentTimeMillis(),
                    )
                )
                logging = false
                activityVersion++
                toast.show(context.getString(R.string.activity_saved), ToastKind.SUCCESS)
            },
            onDismiss = { logging = false },
        )
    }

    when (profileStep) {
        ProfileStep.AGE -> AgeDialog(
            current = age,
            onConfirm = {
                AppPreferences.age = it
                profileStep = if (sex == null) ProfileStep.SEX else null
            },
            onInvalid = {},
            onDismiss = { profileStep = null },
        )
        ProfileStep.SEX -> SexDialog(
            current = sex,
            onSelect = {
                AppPreferences.sex = it
                profileStep = null
            },
            onDismiss = { profileStep = null },
        )
        null -> Unit
    }
}

@Composable
private fun SectionTitle(textRes: Int) {
    Text(
        text = stringResource(textRes).uppercase(),
        fontSize = 14.sp,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun LoggedActivityRow(activity: LoggedActivity, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(activity.name, fontSize = 16.sp)
            activity.durationMinutes?.let {
                Text(stringResource(R.string.activity_minutes_value, it), fontSize = 13.sp, modifier = Modifier.alpha(0.6f))
            }
        }
        Text(
            text = stringResource(R.string.energy_kcal, kcal(activity.kcal)),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = StepsyTheme.colors.special,
        )
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.activity_delete, activity.name),
                tint = StepsyTheme.colors.accent,
            )
        }
    }
}
