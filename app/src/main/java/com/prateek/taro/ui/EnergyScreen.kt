package com.prateek.taro.ui

import com.prateek.taro.ui.components.BentoTile
import androidx.compose.foundation.shape.CircleShape
import com.prateek.taro.ui.components.Bone
import com.prateek.taro.ui.components.BoneRows
import com.prateek.taro.ui.components.BoneTile
import com.prateek.taro.ui.components.BoneText
import com.prateek.taro.ui.components.Skeleton
import androidx.compose.ui.graphics.Color
import com.prateek.taro.food.Macros
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.prateek.taro.widget.FoodWidget
import java.time.LocalDate
import com.prateek.taro.ui.components.DaySwitcher
import com.prateek.taro.ui.components.LocalTabActive
import com.prateek.taro.ui.components.infoOf
import com.prateek.taro.ui.components.InfoBox
import com.prateek.taro.data.FoodLog
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.prateek.taro.data.LoggedActivity
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.util.Database
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prateek.taro.R
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import cafe.adriel.voyager.core.screen.Screen
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalDensity
import com.prateek.taro.ui.components.LinkText
import com.prateek.taro.ui.components.LegendItem
import com.prateek.taro.ui.components.StackBar
import com.prateek.taro.ui.components.PairedBars
import com.prateek.taro.ui.components.KeyFact
import com.prateek.taro.ui.components.Fact
import com.prateek.taro.ui.components.BentoTitle
import com.prateek.taro.ui.components.BentoNote
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.foundation.layout.aspectRatio
import androidx.annotation.StringRes
import com.prateek.taro.energy.ActivityProfile
import com.prateek.taro.energy.DailyEnergy
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prateek.taro.energy.WeightJournal
import com.prateek.taro.energy.Metabolism
import com.prateek.taro.ui.components.PrimaryButton
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Util
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt
import com.prateek.taro.ui.components.Meter
import com.prateek.taro.ui.components.Panel
import com.prateek.taro.ui.components.SectionLabel
import androidx.compose.ui.graphics.lerp
import com.prateek.taro.util.CalorieGoal
import com.prateek.taro.energy.DietGoal
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size

object FoodPage : Screen {
    @Composable
    override fun Content() {
        EnergyContent(LocalTracking.current)
    }
}

private fun kcal(value: Double) = NumberFormat.getIntegerInstance().format(value.roundToInt())

private enum class ProfileStep { AGE, SEX }

private const val RANGE_MIN_KCAL = 20
private const val WEEK_ASPECT = 2.4f
private const val MIN_LOGGED_DAYS = 3
private const val CLOSE_KG = 0.15
private const val USUAL_SHARE = 0.05

@Composable
private fun MacroMeter(@StringRes label: Int, grams: Double, budget: Double, kcalPerGram: Double, range: ClosedFloatingPointRange<Double>, color: Color, @StringRes info: Int) {
    val high = Macros.grams(budget, kcalPerGram, range.endInclusive)
    val scale = maxOf(high, grams).takeIf { it > 0 } ?: 1.0
    Meter(
        label = stringResource(label),
        value = stringResource(R.string.food_macro_value, kcal(grams)),
        fraction = (grams / scale).toFloat(),
        color = color,
        info = infoOf(label, info),
    )
}

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
    var picked by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val selectedDate = picked?.takeIf { !it.isAfter(Util.logicalToday()) } ?: Util.logicalToday()
    fun select(date: LocalDate) {
        picked = date.takeIf { it != Util.logicalToday() }
    }
    val day = selectedDate.toString()
    val isToday = day == today
    val pastBurn = rememberInBackground(history, day, dataVersion) { history?.takeIf { !isToday }?.let { DailyEnergy.day(context, it, day) } }?.value
    val activities = remember(day, dataVersion) { Database.getInstance(context).activitiesOn(day) }

    var profileStep by remember { mutableStateOf<ProfileStep?>(null) }
    val quickLog = rememberQuickLog()
    var editingGoal by remember { mutableStateOf(false) }
    var editingActivity by remember { mutableStateOf<LoggedActivity?>(null) }
    val foodToday = remember(day, dataVersion) { Database.getInstance(context).foodOn(day) }
    val incompleteDays by AppPreferences.incompleteFoodDaysFlow().collectAsStateWithLifecycle(AppPreferences.incompleteFoodDays)
    val proteinPerKg by AppPreferences.proteinPerKgFlow().collectAsStateWithLifecycle(AppPreferences.proteinPerKg)
    val meals by AppPreferences.mealsFlow(context).collectAsStateWithLifecycle(AppPreferences.meals(context))
    var editingFood by remember { mutableStateOf<FoodLog?>(null) }
    var editingProtein by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val navigator = LocalNavigator.currentOrThrow
    val rootNavigator = navigator.parent ?: navigator
    val weightPoints = rememberInBackground(dataVersion, tracking.refreshKey) { WeightJournal.points(context) }?.value.orEmpty()
    val calorieGoal by AppPreferences.calorieGoalFlow().collectAsStateWithLifecycle(AppPreferences.calorieGoal)
    val spacing = TaroTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        AppOverflowMenu(
            center = { DaySwitcher(date = selectedDate, today = Util.logicalToday(), onChange = ::select) },
            actions = quickLogActions(quickLog, selectedDate),
            back = true,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(start = spacing.loose, end = spacing.loose, bottom = spacing.loose),
        ) {
            if (historyLoad == null || (history != null && todayBurn == null) || (history != null && !isToday && pastBurn == null)) {
                FoodSkeleton()
            } else if (history == null || todayBurn == null) {
                BentoTile(color = TaroTheme.colors.special, tinted = true, modifier = Modifier.fillMaxWidth().padding(top = spacing.loose)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.energy_missing_title),
                            style = TaroTheme.text.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(R.string.energy_missing_message),
                            style = TaroTheme.text.note,
                            textAlign = TextAlign.Center,
                            color = TaroTheme.colors.accent,
                            modifier = Modifier.padding(vertical = spacing.gap),
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
                val type = TaroTheme.text
                val need = DailyEnergy.todayNeed(history, todayBurn, insights?.hourly, now)
                val todayNeed = need.need
                val shown = pastBurn ?: todayBurn
                val restingColor = restingTone()
                val digestionColor = lerp(colors.flame, colors.special, 0.6f)

                fun goalTarget(kcal: Double) = DailyEnergy.eatTarget(kcal, calorieGoal)
                val dayNeed = if (isToday) need.rest else Metabolism.withDigestion(shown.base, factors.digestion)
                val target = goalTarget(dayNeed)
                val usualTarget = goalTarget(todayNeed)
                if (isToday) {
                    LaunchedEffect((target / 10).roundToInt()) {
                        withContext(Dispatchers.IO) {
                            AppPreferences.saveEatTarget(today, target)
                            FoodWidget.refresh(context)
                        }
                    }
                }
                val eaten = foodToday.totals()
                val left = target - eaten.kcal
                val usualLeft = usualTarget - eaten.kcal
                val over = left < 0
                val ranged = isToday && !over && usualLeft - left >= RANGE_MIN_KCAL
                val heroColor = if (over) MaterialTheme.colorScheme.error else colors.special

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = spacing.tight, top = spacing.tight),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        InfoBox(
                            info = if (isToday) infoOf(R.string.info_eat_title, R.string.info_eat) else infoOf(R.string.info_eat_past_title, R.string.info_eat_past),
                            modifier = Modifier.clip(RoundedCornerShape(spacing.tight)),
                            color = heroColor,
                        ) {
                            Text(
                                text = stringResource(
                                    when {
                                        isToday && over -> R.string.food_hero_over
                                        isToday -> R.string.food_hero_left
                                        over -> R.string.food_hero_over_past
                                        else -> R.string.food_hero_under_past
                                    },
                                ).uppercase(),
                                style = type.label,
                                color = heroColor,
                            )
                        }
                        Text(
                            text = buildAnnotatedString {
                                append(kcal(kotlin.math.abs(left)))
                                if (ranged) withStyle(type.fact.toSpanStyle().copy(color = colors.accent)) { append(" " + stringResource(R.string.food_hero_to, kcal(usualLeft))) }
                            },
                            style = type.hero,
                            color = heroColor,
                        )
                        BentoNote(
                            when {
                                ranged -> stringResource(R.string.food_hero_note_range)
                                isToday && over -> stringResource(R.string.food_hero_note_over, kcal(target))
                                isToday -> stringResource(R.string.food_hero_note)
                                else -> stringResource(R.string.food_hero_note_past, kcal(target))
                            },
                        )
                        LinkText(
                            text = goalNote(calorieGoal),
                            color = if (calorieGoal == null) colors.accent else colors.goal,
                            onClick = { editingGoal = true },
                            trailingIcon = R.drawable.ic_edit,
                            info = infoOf(R.string.info_goal_title, R.string.info_goal),
                            modifier = Modifier.padding(top = spacing.tight),
                        )
                        if (calorieGoal != null && target < history.restingPerDay) {
                            BentoNote(stringResource(R.string.calorie_goal_below_resting), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                val proteinTarget = weight.toDouble() * proteinPerKg
                val budget = if (isToday) usualTarget else target
                val carbs = foodToday.sumOf { it.carbs ?: 0.0 }
                val fat = foodToday.sumOf { it.fat ?: 0.0 }
                Panel(modifier = Modifier.padding(top = spacing.loose)) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(spacing.hair),
                        modifier = Modifier.padding(spacing.hair),
                    ) {
                        val scale = maxOf(usualTarget.takeIf { isToday } ?: 0.0, target, eaten.kcal)
                        Meter(
                            label = stringResource(R.string.food_eaten_label),
                            value = if (ranged) {
                                stringResource(R.string.food_eaten_range, kcal(eaten.kcal), kcal(target), kcal(usualTarget))
                            } else {
                                stringResource(R.string.food_eaten_of, kcal(eaten.kcal), kcal(target))
                            },
                            fraction = (eaten.kcal / scale).toFloat(),
                            color = heroColor,
                            band = if (ranged) (target / scale).toFloat()..(usualTarget / scale).toFloat() else null,
                            info = infoOf(R.string.info_goal_title, R.string.info_goal),
                            onEdit = { editingGoal = true },
                        )
                        Meter(
                            label = stringResource(R.string.food_protein),
                            value = stringResource(R.string.food_protein_of, kcal(eaten.protein), kcal(proteinTarget)),
                            fraction = (eaten.protein / proteinTarget).toFloat(),
                            color = PROTEIN_BLUE,
                            info = infoOf(R.string.info_protein_title, R.string.info_protein),
                            onEdit = { editingProtein = true },
                        )
                        MacroMeter(R.string.food_carbs, carbs, budget, Macros.CARB_KCAL, Macros.CARB_SHARE, CARB_VIOLET, R.string.info_carbs)
                        MacroMeter(R.string.food_fat, fat, budget, Macros.FAT_KCAL, Macros.FAT_SHARE, FAT_CORAL, R.string.info_fat)
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
                    extraAction = if (eaten.kcal > 0) ({ FoodDayToggle(day, incompleteDays) }) else null,
                )

                SectionLabel(stringResource(R.string.food_through_day), info = infoOf(R.string.info_hour_title, R.string.info_hour))
                BurnByHourPanel(date = selectedDate, usual = if (isToday) insights?.hourly?.let { ActivityProfile.usualHours(it, Util.logicalToday()) } else null, refreshKey = if (isToday) "${tracking.refreshKey}-${tracking.steps}" else tracking.refreshKey)

                val week = history.pastDays + todayBurn
                val glance = rememberInBackground(week, incompleteDays, dataVersion) { weekGlance(context, week, incompleteDays, history.restingPerDay) }?.value
                SectionLabel(stringResource(R.string.energy_last_7_days), info = infoOf(R.string.info_week_chart_title, R.string.info_week_chart))
                Panel {
                    Column(modifier = Modifier.padding(spacing.tight)) {
                        PairedBars(
                            values = glance?.days ?: week.map { it.total to null },
                            first = colors.flame,
                            second = colors.special,
                            labels = week.map { LocalDate.parse(it.date).dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(WEEK_ASPECT),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.loose), modifier = Modifier.padding(top = spacing.gap)) {
                            LegendItem(colors.flame, stringResource(R.string.timeline_burned), "")
                            LegendItem(colors.special, stringResource(R.string.timeline_eaten), "")
                        }
                        val logging = glance?.net?.takeIf { glance.loggedDays >= MIN_LOGGED_DAYS }?.let { it / glance.loggedDays * week.size / Metabolism.KCAL_PER_KG }
                        val trend = insights?.model.tissueTrend(weightPoints)
                        val scale = trend.lastOrNull()?.let { last ->
                            trend.lastOrNull { !it.date.isAfter(last.date.minusDays(week.size.toLong())) }?.let { last.trend - it.trend }
                        }
                        if (logging != null && scale != null) {
                            Row(modifier = Modifier.padding(top = spacing.loose)) {
                                Fact(Util.formatWeightChange(logging), stringResource(R.string.food_logging_says), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                                Fact(Util.formatWeightChange(scale), stringResource(R.string.food_scale_says), colors.goal, Modifier.weight(1f))
                            }
                            BentoNote(
                                text = stringResource(
                                    when {
                                        kotlin.math.abs(scale - logging) < CLOSE_KG -> R.string.food_scale_close
                                        scale < logging -> R.string.food_scale_lower
                                        else -> R.string.food_scale_higher
                                    },
                                ),
                                modifier = Modifier.padding(top = spacing.tight),
                            )
                        } else {
                            BentoNote(stringResource(R.string.food_scale_wait), Modifier.padding(top = spacing.tight))
                        }
                    }
                }

                SectionLabel(stringResource(R.string.food_burned), info = if (isToday) infoOf(R.string.info_projected_title, R.string.info_projected) else infoOf(R.string.info_day_total_title, R.string.info_day_total))
                Panel {
                    Column(modifier = Modifier.padding(spacing.tight)) {
                        BentoTitle(stringResource(if (isToday) R.string.food_burned_so_far else R.string.food_burned_day, kcal(shown.total)))
                        val usual = history.dailyNeed
                        val compare = if (isToday) need.projected else shown.total
                        val gap = compare - usual
                        BentoNote(
                            text = when {
                                kotlin.math.abs(gap) < usual * USUAL_SHARE -> stringResource(if (isToday) R.string.food_course_usual else R.string.food_day_usual, kcal(if (isToday) compare else usual))
                                gap > 0 -> stringResource(if (isToday) R.string.food_course_more else R.string.food_day_more, kcal(if (isToday) compare else usual), kcal(gap))
                                else -> stringResource(if (isToday) R.string.food_course_less else R.string.food_day_less, kcal(if (isToday) compare else usual), kcal(-gap))
                            },
                        )
                        StackBar(
                            parts = listOf(shown.resting to restingColor, shown.active to colors.flame, shown.logged to colors.special, shown.digestion to digestionColor),
                            modifier = Modifier.padding(top = spacing.gap),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.loose), modifier = Modifier.padding(top = spacing.gap)) {
                            KeyFact(restingColor, kcal(shown.resting), stringResource(R.string.energy_resting), info = infoOf(R.string.info_resting_title, R.string.info_resting))
                            KeyFact(colors.flame, kcal(shown.active), stringResource(R.string.energy_steps_label), info = infoOf(R.string.info_steps_title, R.string.info_steps))
                            if (shown.logged.roundToInt() > 0) KeyFact(colors.special, kcal(shown.logged), stringResource(R.string.food_workout), info = infoOf(R.string.info_logged_title, R.string.info_logged))
                            if (shown.digestion.roundToInt() > 0) KeyFact(digestionColor, kcal(shown.digestion), stringResource(R.string.energy_digestion), info = infoOf(R.string.info_digestion_title, R.string.info_digestion))
                        }
                        if (activities.isNotEmpty()) {
                            Column(modifier = Modifier.padding(top = spacing.gap)) {
                                activities.forEach { activity ->
                                    LoggedActivityRow(
                                        activity = activity,
                                        onEdit = { editingActivity = activity },
                                        onDelete = {
                                            Database.getInstance(context).deleteActivity(activity.id)
                                            toast.show(context.getString(R.string.activity_deleted), ToastKind.INFO)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                DetectedRideCards(quickLog, modifier = Modifier.padding(top = spacing.gap))

                UnderLoggedCard(insights?.underLogged.orEmpty(), modifier = Modifier.padding(top = spacing.gap))

                Text(
                    text = stringResource(R.string.energy_footnote),
                    style = TaroTheme.text.note,
                    textAlign = TextAlign.Center,
                    color = colors.accent,
                    modifier = Modifier.padding(top = spacing.loose, bottom = spacing.wide),
                )
            }
        }
    }

    editingFood?.let { log ->
        EditFoodDialog(log, onDismiss = { editingFood = null })
    }

    if (editingProtein) ProteinTargetDialog(onDismiss = { editingProtein = false })

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

    editingActivity?.let { old ->
        LogActivityDialog(
            weightKg = AppPreferences.weight,
            editing = old,
            onSave = { name, minutes, kcal, date, finishedAt ->
                Database.getInstance(context).updateActivity(old.copy(date = date.toString(), name = name, kcal = kcal, durationMinutes = minutes, loggedAt = finishedAt))
                editingActivity = null
                toast.show(context.getString(R.string.activity_saved), ToastKind.SUCCESS)
            },
            onDismiss = { editingActivity = null },
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
private fun goalNote(goal: CalorieGoal?): String = when (goal?.goal) {
    DietGoal.CUT -> stringResource(R.string.food_goal_cut, Util.formatSteps(goal.adjustment))
    DietGoal.BULK -> stringResource(R.string.food_goal_bulk, Util.formatSteps(goal.adjustment))
    DietGoal.MAINTAIN -> stringResource(R.string.food_goal_maintain)
    null -> stringResource(R.string.food_goal_none)
}

@Composable
private fun LoggedActivityRow(activity: LoggedActivity, onEdit: () -> Unit, onDelete: () -> Unit) {
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val iconSize = with(LocalDensity.current) { type.meterValue.lineHeight.toDp() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(spacing.gap))
            .clickable(onClick = onEdit)
            .padding(start = spacing.tight, top = spacing.tight, bottom = spacing.tight),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(activity.name, style = type.meterValue, color = MaterialTheme.colorScheme.onSurface)
            activity.durationMinutes?.let {
                Text(stringResource(R.string.activity_minutes_value, it), style = type.note, color = TaroTheme.colors.accent)
            }
        }
        Text(
            text = stringResource(R.string.energy_kcal, kcal(activity.kcal)),
            style = type.meterValue,
            color = TaroTheme.colors.special,
        )
        Icon(
            painter = painterResource(R.drawable.ic_edit),
            contentDescription = stringResource(R.string.activity_edit_title),
            tint = TaroTheme.colors.accent,
            modifier = Modifier
                .padding(start = spacing.tight)
                .size(iconSize),
        )
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.activity_delete, activity.name),
                tint = TaroTheme.colors.accent,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}


@Composable
private fun FoodSkeleton() {
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val bar = with(LocalDensity.current) { type.note.lineHeight.toDp() } / 2
    Skeleton(Modifier.padding(top = spacing.loose)) {
        BoneTile(Modifier.fillMaxWidth()) {
            BoneText(type.label, 0.3f)
            BoneText(type.hero, 0.5f, Modifier.padding(top = spacing.tight))
            BoneText(type.note, 0.7f)
            repeat(4) {
                Row(modifier = Modifier.padding(top = spacing.loose)) {
                    BoneText(type.meterLabel, 0.3f, Modifier.weight(1f))
                    BoneText(type.meterValue, 0.4f, Modifier.weight(1f))
                }
                Bone(Modifier.fillMaxWidth().height(bar).padding(top = spacing.hair), CircleShape)
            }
        }
        BoneTile(Modifier.fillMaxWidth().padding(top = spacing.gap)) {
            BoneRows(3)
        }
    }
}
