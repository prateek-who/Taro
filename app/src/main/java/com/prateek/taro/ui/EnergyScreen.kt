package com.prateek.taro.ui

import com.prateek.taro.ui.components.ActionChip
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.time.LocalDate
import com.prateek.taro.ui.components.dayLabel
import com.prateek.taro.ui.components.DaySwitcher
import com.prateek.taro.ui.components.LocalTabActive
import com.prateek.taro.ui.components.infoOf
import com.prateek.taro.ui.components.InfoBox
import com.prateek.taro.energy.ActivityProfile
import androidx.compose.foundation.clickable
import com.prateek.taro.ui.components.NumberInputDialog
import com.prateek.taro.food.Meals
import com.prateek.taro.data.FoodLog
import androidx.compose.runtime.saveable.rememberSaveable
import java.util.Date
import java.text.SimpleDateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableIntStateOf
import com.prateek.taro.data.LoggedActivity
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.RingSegment
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.util.Database
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
import com.prateek.taro.R
import com.prateek.taro.energy.DailyEnergy
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prateek.taro.energy.WeightJournal
import com.prateek.taro.energy.WeightTrend
import com.prateek.taro.ui.components.SecondaryButton
import com.prateek.taro.ui.components.WeightChart
import com.prateek.taro.ui.components.WeightSeriesToggles
import com.prateek.taro.util.WeightReminderScheduler
import com.prateek.taro.energy.Metabolism
import com.prateek.taro.ui.components.HeroRing
import com.prateek.taro.ui.components.RollingText
import com.prateek.taro.ui.components.PrimaryButton
import com.prateek.taro.ui.components.StepsBarChart
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Util
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt
import com.prateek.taro.ui.components.Panel
import com.prateek.taro.ui.components.StatPill
import com.prateek.taro.ui.components.StatTile
import com.prateek.taro.ui.components.SectionLabel
import com.prateek.taro.ui.components.TintChip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import com.prateek.taro.util.CalorieGoal
import com.prateek.taro.energy.DietGoal
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
    val tabActive = LocalTabActive.current
    LaunchedEffect(tabActive) {
        if (tabActive && !AppPreferences.infoHintShown) {
            delay(900)
            toast.show(context.getString(R.string.info_hint), ToastKind.INFO)
            AppPreferences.infoHintShown = true
        }
    }
    val dataVersion = rememberDataVersion()
    val factors by AppPreferences.energyFactorsFlow().collectAsStateWithLifecycle(AppPreferences.energyFactors)
    val insights by rememberBodyInsights(today, age, sex, height)
    val historyLoad = rememberInBackground(age, sex, weight, height, today, tracking.refreshKey, dataVersion, factors) { DailyEnergy.history(context, factors) }
    val history = historyLoad?.value
    val todayBurn = rememberInBackground(history, tracking.steps, now, dataVersion) {
        history?.let { DailyEnergy.today(context, it, tracking.steps, now) }
    }?.value
    LaunchedEffect(tabActive) { if (!tabActive) EnergyTabState.date = null }
    val selectedDate = EnergyTabState.date?.takeIf { !it.isAfter(Util.logicalToday()) } ?: Util.logicalToday()
    fun select(date: LocalDate) {
        EnergyTabState.date = date.takeIf { it != Util.logicalToday() }
    }
    val day = selectedDate.toString()
    val isToday = day == today
    val pastBurn = rememberInBackground(history, day, dataVersion) { history?.takeIf { !isToday }?.let { DailyEnergy.day(context, it, day) } }?.value
    val activities = remember(day, dataVersion) { Database.getInstance(context).activitiesOn(day) }

    var profileStep by remember { mutableStateOf<ProfileStep?>(null) }
    val quickLog = rememberQuickLog()
    var editingGoal by remember { mutableStateOf(false) }
    var showWeighIns by rememberSaveable { mutableStateOf(true) }
    var showTrend by rememberSaveable { mutableStateOf(true) }
    val foodToday = remember(day, dataVersion) { Database.getInstance(context).foodOn(day) }
    val incompleteDays by AppPreferences.incompleteFoodDaysFlow().collectAsStateWithLifecycle(AppPreferences.incompleteFoodDays)
    val proteinPerKg by AppPreferences.proteinPerKgFlow().collectAsStateWithLifecycle(AppPreferences.proteinPerKg)
    val meals by AppPreferences.mealsFlow(context).collectAsStateWithLifecycle(AppPreferences.meals(context))
    var editingFood by remember { mutableStateOf<FoodLog?>(null) }
    var editingProtein by remember { mutableStateOf(false) }
    val weightPoints = remember(dataVersion, tracking.refreshKey) { WeightJournal.points(context) }
    val scroll = EnergyTabState.scroll
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.currentOrThrow
    val rootNavigator = navigator.parent ?: navigator
    val calorieGoal by AppPreferences.calorieGoalFlow().collectAsStateWithLifecycle(AppPreferences.calorieGoal)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        AppOverflowMenu(
            center = { DaySwitcher(date = selectedDate, today = Util.logicalToday(), onChange = ::select) },
            actions = quickLogActions(quickLog, selectedDate),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        ) {
            if (historyLoad == null || (history != null && todayBurn == null) || (history != null && !isToday && pastBurn == null)) {
                Box(modifier = Modifier.height(320.dp))
            } else if (history == null || todayBurn == null) {
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
                            color = TaroTheme.colors.accent,
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
                val colors = TaroTheme.colors
                val expectedRest = insights?.hourly?.let { ActivityProfile.expectedRemaining(it, Util.logicalToday(), Util.fractionOfToday(now)) }
                val projected = Metabolism.withDigestion(
                    history.restingPerDay + todayBurn.active + todayBurn.logged + factors.activity * (expectedRest ?: 0.0)
                )
                val todayNeed = if (expectedRest != null) projected else maxOf(history.dailyNeed, projected)
                val needSd = factors.needSd(
                    history.restingPerDay / factors.resting,
                    todayBurn.active / factors.activity + (expectedRest ?: history.averageActive / factors.activity),
                    todayBurn.logged / factors.workout,
                )
                val shown = pastBurn ?: todayBurn
                val ringTarget = if (isToday) todayNeed else history.dailyNeed
                val restingColor = lerp(colors.flame, colors.special, 0.3f).copy(alpha = 0.55f).compositeOver(colors.background)
                val digestionColor = lerp(colors.flame, colors.special, 0.6f)

                HeroRing(
                    segments = listOf(
                        RingSegment(Metabolism.withDigestion(shown.resting), restingColor),
                        RingSegment(Metabolism.withDigestion(shown.active), colors.flame),
                        RingSegment(Metabolism.withDigestion(shown.logged), colors.special),
                    ),
                    target = ringTarget,
                    overflow = false,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(
                        text = dayLabel(selectedDate, Util.logicalToday()).uppercase(),
                        fontSize = 13.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent,
                    )
                    RollingText(
                        text = kcal(shown.total),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                    InfoBox(infoOf(R.string.info_ring_title, R.string.info_ring), Modifier.clip(RoundedCornerShape(10.dp)), color = colors.flame) {
                        Text(
                            text = stringResource(if (isToday) R.string.energy_ring_caption else R.string.energy_ring_caption_past, kcal(ringTarget)),
                            fontSize = 14.sp,
                            color = colors.accent,
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                ) {
                    StatPill(R.drawable.ic_sleep, "${stringResource(R.string.energy_resting)} ${kcal(shown.resting)}", restingColor, lit = false, info = infoOf(R.string.info_resting_title, R.string.info_resting))
                    StatPill(R.drawable.ic_steps, "${stringResource(R.string.energy_steps_label)} ${kcal(shown.active)}", colors.flame, lit = false, info = infoOf(R.string.info_steps_title, R.string.info_steps))
                    StatPill(R.drawable.ic_calories, "${stringResource(R.string.energy_logged)} ${kcal(shown.logged)}", colors.special, lit = false, info = infoOf(R.string.info_logged_title, R.string.info_logged))
                    StatPill(R.drawable.ic_calorie_goal, "${stringResource(R.string.energy_digestion)} ${kcal(shown.digestion)}", digestionColor, lit = false, info = infoOf(R.string.info_digestion_title, R.string.info_digestion))
                }

                if (activities.isNotEmpty()) {
                    Panel(modifier = Modifier.padding(top = 10.dp)) {
                        activities.forEach { activity ->
                            LoggedActivityRow(activity) {
                                Database.getInstance(context).deleteActivity(activity.id)
                                toast.show(context.getString(R.string.activity_deleted), ToastKind.INFO)
                            }
                        }
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
                        info = infoOf(R.string.info_usual_title, R.string.info_usual),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = stringResource(if (isToday) R.string.energy_projected else R.string.energy_day_total),
                        value = stringResource(R.string.energy_kcal, kcal(if (isToday) projected else shown.total)),
                        color = colors.flame,
                        modifier = Modifier.weight(1f),
                        info = if (isToday) infoOf(R.string.info_projected_title, R.string.info_projected) else infoOf(R.string.info_day_total_title, R.string.info_day_total),
                    )
                }


                val dayNeed = if (isToday) todayNeed else shown.total
                val target = calorieGoal?.let { Metabolism.calorieTarget(dayNeed, it.goal, it.adjustment) } ?: dayNeed
                val eaten = foodToday.totals()
                val left = target - eaten.kcal
                val proteinTarget = weight.toDouble() * proteinPerKg
                Panel(modifier = Modifier.padding(top = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            InfoBox(
                                info = if (isToday) infoOf(R.string.info_eat_title, R.string.info_eat) else infoOf(R.string.info_eat_past_title, R.string.info_eat_past),
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                                color = colors.goal,
                            ) {
                                Text(
                                    text = stringResource(if (isToday) R.string.calorie_goal_eat_today else R.string.food_target_day).uppercase(),
                                    fontSize = 12.sp,
                                    letterSpacing = 2.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.accent,
                                )
                            }
                            RollingText(
                                text = kcal(kotlin.math.abs(left)),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Normal,
                                    color = if (left < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                ),
                            )
                            Text(
                                text = if (isToday) {
                                    stringResource(if (left < 0) R.string.food_over else R.string.food_left, kcal(target)) +
                                        " " + stringResource(R.string.body_plus_minus, kcal(needSd))
                                } else {
                                    stringResource(if (left < 0) R.string.food_over_past else R.string.food_under_past, kcal(target))
                                },
                                fontSize = 13.sp,
                                color = colors.accent,
                            )
                        }
                        ActionChip(
                            text = goalChip(calorieGoal),
                            color = if (calorieGoal == null) MaterialTheme.colorScheme.onSurface else colors.goal,
                            onClick = { editingGoal = true },
                            trailingIcon = R.drawable.ic_expand_more,
                            info = infoOf(R.string.info_goal_title, R.string.info_goal),
                        )
                    }
                    ProgressLine(
                        fraction = (eaten.kcal / target).toFloat(),
                        color = if (left < 0) MaterialTheme.colorScheme.error else colors.goal,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, top = 14.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.food_eaten_label).uppercase(),
                                fontSize = 11.sp,
                                letterSpacing = 1.5.sp,
                                color = colors.accent,
                            )
                            RollingText(
                                text = stringResource(R.string.energy_kcal, kcal(eaten.kcal)),
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, color = colors.goal),
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        if (eaten.kcal > 0) FoodDayChip(day, incompleteDays)
                    }
                    InfoBox(
                        info = infoOf(R.string.info_protein_title, R.string.info_protein),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        color = PROTEIN_BLUE,
                        onClick = { editingProtein = true },
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = stringResource(R.string.food_protein_target, kcal(eaten.protein), kcal(proteinTarget)),
                                fontSize = 13.sp,
                                color = PROTEIN_BLUE,
                                modifier = Modifier.padding(bottom = 6.dp),
                            )
                            ProgressLine(fraction = (eaten.protein / proteinTarget).toFloat(), color = PROTEIN_BLUE)
                        }
                    }
                    if (calorieGoal != null && target < history.restingPerDay) {
                        TintChip(
                            text = stringResource(R.string.calorie_goal_below_resting),
                            color = MaterialTheme.colorScheme.error,
                            info = infoOf(R.string.info_below_title, R.string.info_below),
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp),
                        )
                    }
                }

                MealsSection(
                    date = selectedDate,
                    meals = meals,
                    logs = foodToday,
                    onAdd = { meal -> rootNavigator.push(AddFoodScreen(selectedDate, meal)) },
                    onEdit = { editingFood = it },
                    onDelete = { log ->
                        Database.getInstance(context).deleteFood(log.id)
                        toast.show(context.getString(R.string.food_removed, log.name), ToastKind.INFO)
                    },
                )

                SectionLabel(stringResource(R.string.timeline_by_hour), info = infoOf(R.string.info_hour_title, R.string.info_hour))
                EnergyByHourPanel(date = selectedDate, dayNeed = if (isToday) todayNeed else null, refreshKey = if (isToday) "${tracking.refreshKey}-${tracking.steps}" else tracking.refreshKey)

                SectionLabel(stringResource(R.string.body_title), info = infoOf(R.string.info_body_title, R.string.info_body))
                BodyCard(insights?.model, loaded = insights != null)

                SectionLabel(stringResource(R.string.weight_title), info = infoOf(R.string.info_weight_title, R.string.info_weight))
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
                                TintChip(
                                    text = stringResource(R.string.weight_week_chip, Util.formatWeightChange(it)),
                                    color = colors.accent,
                                    info = infoOf(R.string.info_week_title, R.string.info_week),
                                )
                            }
                        }
                        WeightChart(
                            points = insights?.model.tissueTrend(weightPoints).filter { !it.date.isBefore(Util.logicalToday().minusDays(30)) },
                            toDisplay = Util::kgToDisplay,
                            showWeighIns = showWeighIns,
                            showTrend = showTrend,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                        )
                        WeightSeriesToggles(
                            showWeighIns = showWeighIns,
                            showTrend = showTrend,
                            onChange = { weighIns, trend ->
                                showWeighIns = weighIns
                                showTrend = trend
                            },
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
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
                            onClick = { quickLog.weight = true },
                            icon = R.drawable.ic_weight,
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(stringResource(R.string.weight_history), onClick = { rootNavigator.push(WeightScreen) })
                    }
                }

                val week = history.pastDays + todayBurn
                val selectedBar = week.indexOfFirst { it.date == day }.takeIf { it >= 0 }
                SectionLabel(stringResource(R.string.energy_last_7_days), info = infoOf(R.string.info_week_chart_title, R.string.info_week_chart))
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
                        onBarClick = { index ->
                            select(LocalDate.parse(week[index].date))
                            scope.launch { scroll.animateScrollTo(0) }
                        },
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

    editingFood?.let { log ->
        EditFoodDialog(log, onDismiss = { editingFood = null })
    }

    if (editingProtein) {
        NumberInputDialog(
            title = stringResource(R.string.food_protein_dialog),
            initial = Util.formatMeasure(proteinPerKg.toDouble()),
            hint = "g / kg",
            decimal = true,
            supportingText = stringResource(R.string.food_protein_hint),
            onConfirm = { input ->
                editingProtein = false
                Util.parseMeasure(input)?.let { AppPreferences.proteinPerKg = it.toFloat() }
            },
            onDismiss = { editingProtein = false },
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

    QuickLogDialogs(quickLog, selectedDate)

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
            .background(TaroTheme.colors.special.copy(alpha = 0.08f))
            .padding(start = 14.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(activity.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            activity.durationMinutes?.let {
                Text(stringResource(R.string.activity_minutes_value, it), fontSize = 13.sp, color = TaroTheme.colors.accent)
            }
        }
        Text(
            text = stringResource(R.string.energy_kcal, kcal(activity.kcal)),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TaroTheme.colors.special,
        )
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.activity_delete, activity.name),
                tint = TaroTheme.colors.accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private object EnergyTabState {
    var date by mutableStateOf<LocalDate?>(null)
    val scroll = ScrollState(0)
}
