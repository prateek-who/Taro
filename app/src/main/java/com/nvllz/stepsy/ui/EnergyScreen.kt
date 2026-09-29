package com.nvllz.stepsy.ui

import java.util.Date
import java.text.SimpleDateFormat
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
import com.nvllz.stepsy.ui.components.PrimaryButton
import com.nvllz.stepsy.ui.components.StepsBarChart
import com.nvllz.stepsy.ui.theme.StepsyTheme
import com.nvllz.stepsy.util.AppPreferences
import com.nvllz.stepsy.util.Util
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt
import com.nvllz.stepsy.ui.components.Panel
import com.nvllz.stepsy.ui.components.StatPill
import com.nvllz.stepsy.ui.components.StatTile
import com.nvllz.stepsy.ui.components.SectionLabel
import com.nvllz.stepsy.ui.components.TintChip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import com.nvllz.stepsy.util.CalorieGoal
import com.nvllz.stepsy.energy.DietGoal
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size

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
            if (history == null || todayBurn == null) {
                Panel(modifier = Modifier.padding(top = 24.dp)) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
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
                            color = StepsyTheme.colors.accent,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                        PrimaryButton(
                            text = stringResource(R.string.energy_add_details),
                            onClick = { profileStep = if (age == null) ProfileStep.AGE else ProfileStep.SEX },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                val colors = StepsyTheme.colors
                val restingColor = lerp(colors.flame, colors.special, 0.3f).copy(alpha = 0.55f).compositeOver(colors.background)
                val digestionColor = lerp(colors.flame, colors.special, 0.6f)

                HeroRing(
                    segments = listOf(
                        RingSegment(todayBurn.resting, restingColor),
                        RingSegment(todayBurn.active, colors.flame),
                        RingSegment(todayBurn.logged, colors.special),
                        RingSegment(todayBurn.digestion, digestionColor),
                    ),
                    target = history.dailyNeed,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.header_today).uppercase(),
                        fontSize = 13.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent,
                    )
                    RollingText(
                        text = kcal(todayBurn.total),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.energy_ring_caption, kcal(history.dailyNeed)),
                        fontSize = 14.sp,
                        color = colors.accent,
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                ) {
                    StatPill(R.drawable.ic_sleep, "${stringResource(R.string.energy_resting)} ${kcal(todayBurn.resting)}", restingColor, lit = false)
                    StatPill(R.drawable.ic_steps, "${stringResource(R.string.energy_steps_label)} ${kcal(todayBurn.active)}", colors.flame, lit = false)
                    StatPill(R.drawable.ic_calories, "${stringResource(R.string.energy_logged)} ${kcal(todayBurn.logged)}", colors.special, lit = false)
                    StatPill(R.drawable.ic_calorie_goal, "${stringResource(R.string.energy_digestion)} ${kcal(todayBurn.digestion)}", digestionColor, lit = false)
                }

                val target = calorieGoal?.let { Metabolism.calorieTarget(history.dailyNeed, it.goal, it.adjustment) } ?: history.dailyNeed
                Panel(modifier = Modifier.padding(top = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.calorie_goal_eat_today).uppercase(),
                                fontSize = 12.sp,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.accent,
                            )
                            RollingText(
                                text = kcal(target),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                            )
                            Text(stringResource(R.string.energy_eat_today_caption), fontSize = 13.sp, color = colors.accent)
                        }
                        TintChip(
                            text = goalChip(calorieGoal),
                            color = if (calorieGoal == null) colors.accent else colors.goal,
                            onClick = { editingGoal = true },
                        )
                    }
                    if (calorieGoal != null && target < history.restingPerDay) {
                        TintChip(
                            text = stringResource(R.string.calorie_goal_below_resting),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                ) {
                    StatTile(
                        label = stringResource(R.string.energy_daily_need),
                        value = stringResource(R.string.energy_kcal, kcal(history.dailyNeed)),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = stringResource(R.string.energy_projected),
                        value = stringResource(
                            R.string.energy_kcal,
                            kcal(Metabolism.withDigestion(history.restingPerDay + todayBurn.active + todayBurn.logged)),
                        ),
                        color = colors.flame,
                        modifier = Modifier.weight(1f),
                    )
                }

                SectionLabel(stringResource(R.string.weight_title))
                Panel {
                    if (weightPoints.isEmpty()) {
                        Text(
                            text = stringResource(R.string.weight_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = colors.accent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        )
                    } else {
                        val latest = weightPoints.last()
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.energy_weight_trend).uppercase(),
                                    fontSize = 12.sp,
                                    letterSpacing = 2.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.accent,
                                )
                                RollingText(
                                    text = Util.formatWeight(latest.trend),
                                    style = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                )
                            }
                            weekChange(weightPoints)?.let {
                                TintChip(stringResource(R.string.weight_week_chip, Util.formatWeightChange(it)), colors.accent)
                            }
                        }
                        WeightChart(
                            points = weightPoints.filter { !it.date.isBefore(Util.logicalToday().minusDays(30)) },
                            toDisplay = Util::kgToDisplay,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                        )
                        PaceMessage(
                            assessment = WeightTrend.assess(
                                WeightTrend.weeklyRate(weightPoints),
                                calorieGoal?.goal,
                                calorieGoal?.adjustment ?: 0,
                                latest.trend,
                            ),
                            goal = calorieGoal?.goal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                    ) {
                        PrimaryButton(
                            text = stringResource(R.string.weight_log),
                            onClick = { loggingWeight = true },
                            icon = R.drawable.ic_weight,
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(stringResource(R.string.weight_history), onClick = { rootNavigator.push(WeightScreen) })
                    }
                }

                SectionLabel(stringResource(R.string.energy_logged_title))
                Panel {
                    if (activities.isEmpty()) {
                        Text(
                            text = stringResource(R.string.energy_logged_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = colors.accent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        )
                    } else {
                        activities.forEach { activity ->
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
                        icon = R.drawable.ic_calories,
                        tint = colors.special,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                    )
                }

                val week = history.pastDays + todayBurn
                var selectedBar by remember { mutableStateOf<Int?>(null) }
                SectionLabel(stringResource(R.string.energy_last_7_days))
                Panel(modifier = Modifier.height(260.dp)) {
                    Text(
                        text = stringResource(R.string.energy_last_7_days_caption),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = colors.accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                    StepsBarChart(
                        values = week.map { it.total.roundToInt() },
                        labels = week.map { day ->
                            Calendar.getInstance().apply { timeInMillis = Util.dateStringToCalendarMillis(day.date) }
                                .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()).orEmpty()
                        },
                        goal = 0,
                        barTint = colors.flame,
                        selectedIndex = selectedBar,
                        onBarClick = { selectedBar = if (selectedBar == it) null else it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }

                Text(
                    text = stringResource(R.string.energy_footnote),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = colors.accent,
                    modifier = Modifier.padding(top = 20.dp, bottom = 24.dp),
                )
            }
        }
    }

    if (loggingWeight) {
        LogWeightDialog(
            title = stringResource(R.string.weight_dialog_title),
            initialKg = weightPoints.lastOrNull()?.kg ?: AppPreferences.weight,
            onSave = { kg, date ->
                WeightJournal.log(context, date.toString(), kg)
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
            onSave = { name, minutes, kcal, date ->
                Database.getInstance(context).addActivity(
                    LoggedActivity(
                        date = date.toString(),
                        name = name,
                        kcal = kcal,
                        durationMinutes = minutes,
                        loggedAt = System.currentTimeMillis(),
                    )
                )
                logging = false
                activityVersion++
                val message = if (date == Util.logicalToday()) {
                    context.getString(R.string.activity_saved)
                } else {
                    context.getString(R.string.saved_for_date, SimpleDateFormat(AppPreferences.dateFormatString, Locale.getDefault()).format(Date(Util.dateStringToCalendarMillis(date.toString()))))
                }
                toast.show(message, ToastKind.SUCCESS)
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
private fun goalChip(goal: CalorieGoal?): String = when (goal?.goal) {
    null -> stringResource(R.string.calorie_goal_set)
    DietGoal.CUT -> stringResource(R.string.calorie_goal_chip_cut, "\u2212${goal.adjustment}")
    DietGoal.MAINTAIN -> stringResource(R.string.calorie_goal_maintain)
    DietGoal.BULK -> stringResource(R.string.calorie_goal_chip_bulk, goal.adjustment.toString())
}

@Composable
private fun LoggedActivityRow(activity: LoggedActivity, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(StepsyTheme.colors.special.copy(alpha = 0.08f))
            .padding(start = 14.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(activity.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            activity.durationMinutes?.let {
                Text(stringResource(R.string.activity_minutes_value, it), fontSize = 13.sp, color = StepsyTheme.colors.accent)
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
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
