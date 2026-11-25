package com.prateek.taro.ui

import com.prateek.taro.ui.components.enter
import com.prateek.taro.ui.components.BudgetBar
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.CircleShape
import com.prateek.taro.ui.components.TileBones
import com.prateek.taro.ui.components.BoneTile
import com.prateek.taro.ui.components.Bone
import com.prateek.taro.ui.components.BoneText
import com.prateek.taro.ui.components.Skeleton
import androidx.compose.ui.platform.LocalConfiguration
import com.prateek.taro.sleep.SleepInsights
import com.prateek.taro.ui.components.NightColumns
import com.prateek.taro.ui.components.MeterBar
import com.prateek.taro.ui.components.TIER_NAMES
import com.prateek.taro.util.StepTiers
import androidx.compose.ui.text.style.TextAlign
import androidx.annotation.StringRes
import com.prateek.taro.food.Macros
import com.prateek.taro.ui.components.SpreeLabel
import com.prateek.taro.ui.components.spreeStyles
import com.prateek.taro.util.Sprees
import com.prateek.taro.ui.components.TierNumber
import com.prateek.taro.ui.components.tierStyles
import com.prateek.taro.ui.components.tierGlow
import com.prateek.taro.ui.components.TierLabel
import com.prateek.taro.ui.components.TierRing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.prateek.taro.ui.components.BentoTitle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.prateek.taro.R
import com.prateek.taro.energy.DailyEnergy
import com.prateek.taro.energy.DayBurn
import com.prateek.taro.energy.EnergyHistory
import com.prateek.taro.energy.WeightJournal
import com.prateek.taro.ui.components.BentoNote
import com.prateek.taro.ui.components.BentoTile
import com.prateek.taro.ui.components.BentoValue
import com.prateek.taro.ui.components.DayStrip
import com.prateek.taro.ui.components.KeyFact
import com.prateek.taro.ui.components.PairedBars
import com.prateek.taro.ui.components.Sparkline
import com.prateek.taro.ui.components.StripSpan
import com.prateek.taro.ui.components.WeekDots
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Database
import com.prateek.taro.util.Streak
import com.prateek.taro.util.Util
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt


object HomeTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 0u,
            title = stringResource(R.string.tab_home),
            icon = painterResource(R.drawable.ic_home),
        )

    @Composable
    override fun Content() {
        HomeScreen(LocalTracking.current)
    }
}

private fun kcal(value: Double) = NumberFormat.getIntegerInstance().format(value.roundToInt())

private const val RANGE_MIN_KCAL = 20

@Composable
private fun HomeScreen(tracking: TrackingState) {
    val context = LocalContext.current
    val dataVersion = rememberDataVersion()
    val refreshKey = tracking.refreshKey * 1_000_003 + dataVersion
    val navigator = LocalNavigator.currentOrThrow
    val rootNavigator = navigator.parent ?: navigator
    val quickLog = rememberQuickLog()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = System.currentTimeMillis()
        }
    }
    val today = Util.logicalToday()
    val spacing = TaroTheme.spacing
    val name by AppPreferences.nameFlow().collectAsStateWithLifecycle(AppPreferences.name)
    val goalTarget by AppPreferences.dailyGoalTargetFlow().collectAsStateWithLifecycle(AppPreferences.dailyGoalTarget)
    val streakAsk by AppPreferences.streakAskFlow().collectAsStateWithLifecycle(AppPreferences.streakAsk)
    val dayTotals = rememberRefreshed(refreshKey) { stepsByDay(context) }
    val streak = rememberStepStreak(dayTotals, tracking.steps)
    val day = rememberRefreshed(today, refreshKey, now / 600_000) { dayGlance(context, today) }
    val factors by AppPreferences.energyFactorsFlow().collectAsStateWithLifecycle(AppPreferences.energyFactors)
    val weight by AppPreferences.weightFlow().collectAsStateWithLifecycle(AppPreferences.weight)
    val historyLoad = rememberInBackground(today, refreshKey, factors, weight) { DailyEnergy.history(context, factors) }
    val history = historyLoad?.value
    val burn = rememberInBackground(history, tracking.steps / LIVE_STEP_BUCKET, now) { history?.let { DailyEnergy.today(context, it, tracking.steps, now) } }?.value

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            AppOverflowMenu(
                center = {
                    Text(
                        text = greeting(name),
                        style = TaroTheme.text.greeting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = spacing.tight),
                    )
                },
                actions = quickLogActions(quickLog, today),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.gap),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = spacing.loose, end = spacing.loose, bottom = spacing.loose),
            ) {
                streak.unanswered?.takeIf { streakAsk && goalTarget > 0 }?.let { StreakAskCard(it, streak.current, goalTarget) }
                ReportCard()
                DayTile(today, day, now) { rootNavigator.push(DayScreen(today.toString())) }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.gap), modifier = Modifier.height(IntrinsicSize.Max)) {
                    StepsTile(tracking.steps, goalTarget, streak, Modifier.weight(1f).fillMaxHeight()) { rootNavigator.push(StepsPage) }
                    FoodTile(historyLoad != null, history, burn, now, Modifier.weight(1f).fillMaxHeight()) { rootNavigator.push(FoodPage) }
                }
                SleepTile(refreshKey) { rootNavigator.push(SleepPage) }
                WeekTile(history, burn, refreshKey) { rootNavigator.push(FoodPage) }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.gap), modifier = Modifier.height(IntrinsicSize.Max)) {
                    WeightTile(refreshKey, Modifier.weight(1f).fillMaxHeight(), onEmpty = { quickLog.weight = true }) { rootNavigator.push(WeightScreen) }
                    StreakTile(streak, today, Modifier.weight(1f).fillMaxHeight()) { rootNavigator.push(StepsPage) }
                }
            }
        }
        SpreeAnnouncer(
            streak,
            today,
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = spacing.loose),
        )
    }
    QuickLogDialogs(quickLog, today)
}

@Composable
private fun greeting(name: String): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val part = stringResource(
        when (hour) {
            in 5..11 -> R.string.home_morning
            in 12..16 -> R.string.home_afternoon
            else -> R.string.home_evening
        }
    )
    return if (name.isBlank()) part else stringResource(R.string.home_greeting_named, part, name.trim())
}

@Composable
private fun DayTile(date: LocalDate, day: DayGlance, now: Long, onClick: () -> Unit) {
    val colors = TaroTheme.colors
    val dateText = remember(date) {
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMM")
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()))
    }
    val spans = day.sleep.map { StripSpan(it.start, it.end, colors.sleep) } +
        day.walks.map { StripSpan(it.start, it.end, colors.goal) } +
        day.workouts.map { StripSpan(it.start, it.end, colors.flame) }
    BentoTile(order = 0, color = colors.accent, onClick = onClick) {
        BentoTitle(dateText, note = stringResource(R.string.home_your_day_note))
        DayStrip(
            start = day.start,
            end = day.end,
            now = now,
            spans = spans,
            dots = day.meals,
            modifier = Modifier.padding(top = TaroTheme.spacing.gap),
        )
        val sleepMinutes = day.sleep.sumOf { (minOf(it.end, now, day.end) - maxOf(it.start, day.start)).coerceAtLeast(0) } / 60_000
        val workoutMinutes = day.workouts.sumOf { it.end - it.start } / 60_000
        Row(
            horizontalArrangement = Arrangement.spacedBy(TaroTheme.spacing.gap),
            modifier = Modifier.padding(top = TaroTheme.spacing.loose),
        ) {
            KeyFact(colors.sleep, minutesText(sleepMinutes), stringResource(R.string.home_key_sleep), Modifier.weight(1f))
            KeyFact(colors.goal, minutesText(day.walkMinutes.toLong()), stringResource(R.string.home_key_walking), Modifier.weight(1f))
            if (workoutMinutes > 0) KeyFact(colors.flame, minutesText(workoutMinutes), stringResource(R.string.home_key_workout), Modifier.weight(1f))
            KeyFact(colors.special, day.meals.size.toString(), pluralStringResource(R.plurals.home_key_meals, day.meals.size), Modifier.weight(1f))
        }
    }
}

@Composable
fun minutesText(minutes: Long): String =
    if (minutes < 60) stringResource(R.string.home_minutes, minutes) else stringResource(R.string.sleep_duration, (minutes / 60).toInt(), (minutes % 60).toInt())

@Composable
private fun StepsTile(steps: Int, goal: Int, streak: Streak, modifier: Modifier, onClick: () -> Unit) {
    val colors = TaroTheme.colors
    val target = streak.challenge?.target?.roundToInt() ?: goal
    val type = TaroTheme.text
    val styles = tierStyles()
    BentoTile(order = 1, color = tierGlow(steps, goal, styles), tinted = true, onClick = onClick, modifier = modifier) {
        TierLabel(steps, goal, stringResource(R.string.energy_steps_label))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TaroTheme.spacing.gap),
        ) {
            TierRing(steps, goal, Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(RING_TEXT_SHARE)) {
                    TierNumber(Util.formatSteps(steps), type.ringValue, steps, goal, fit = true)
                    if (target > 0) FitText(stringResource(R.string.home_of, Util.formatSteps(target)), type.ringNote, colors.accent)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        val tier = if (target > 0) StepTiers.reached(steps, target) else -1
        val next = when {
            streak.challenge != null -> null
            target <= 0 -> null
            tier < 0 -> stringResource(R.string.notification_to_go, Util.formatSteps(target - steps))
            tier < StepTiers.LAST -> stringResource(R.string.notification_next_tier, Util.formatSteps(StepTiers.threshold(tier + 1, target) - steps), stringResource(TIER_NAMES[tier + 1]))
            else -> stringResource(R.string.notification_top)
        }
        next?.let { BentoNote(it, Modifier.align(Alignment.CenterHorizontally).padding(top = TaroTheme.spacing.gap), colors.accent) }
        if (streak.challenge != null) BentoNote(stringResource(R.string.home_making_up), Modifier.padding(top = TaroTheme.spacing.gap), colors.special)
    }
}

@Composable
private fun FoodTile(loaded: Boolean, history: EnergyHistory?, burn: DayBurn?, now: Long, modifier: Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val colors = TaroTheme.colors
    val dataVersion = rememberDataVersion()
    val calorieGoal by AppPreferences.calorieGoalFlow().collectAsStateWithLifecycle(AppPreferences.calorieGoal)
    val weight by AppPreferences.weightFlow().collectAsStateWithLifecycle(AppPreferences.weight)
    val proteinPerKg by AppPreferences.proteinPerKgFlow().collectAsStateWithLifecycle(AppPreferences.proteinPerKg)
    val today = Util.millisToDateString(now)
    val insights by rememberBodyInsights(today)
    val food = rememberRefreshed(today, dataVersion) { Database.getInstance(context).foodOn(today).totals() }

    BentoTile(order = 2, label = stringResource(R.string.home_food), color = colors.special, tinted = true, onClick = onClick, modifier = modifier) {
        if (!loaded || (history != null && burn == null)) {
            FoodTileSkeleton()
            return@BentoTile
        }
        if (history == null || burn == null) {
            BentoNote(stringResource(R.string.energy_missing_title), Modifier.padding(top = TaroTheme.spacing.tight))
            return@BentoTile
        }
        Column(modifier = Modifier.weight(1f).enter()) {
            val need = DailyEnergy.todayNeed(history, burn, insights?.hourly, now)
            val floor = DailyEnergy.eatTarget(need.rest, calorieGoal)
            val top = maxOf(floor, DailyEnergy.eatTarget(need.need, calorieGoal))
            val ranged = top - floor >= RANGE_MIN_KCAL
            val left = floor - food.kcal
            val topLeft = top - food.kcal
            val known = food.protein * Macros.PROTEIN_KCAL + food.carbs * Macros.CARB_KCAL + food.fat * Macros.FAT_KCAL
            val squeeze = if (known > food.kcal && known > 0) food.kcal / known else 1.0
            val parts = listOf(
                food.protein * Macros.PROTEIN_KCAL * squeeze to PROTEIN_BLUE,
                food.carbs * Macros.CARB_KCAL * squeeze to CARB_VIOLET,
                food.fat * Macros.FAT_KCAL * squeeze to FAT_CORAL,
                (food.kcal - known * squeeze).coerceAtLeast(0.0) to colors.special.copy(alpha = UNKNOWN_KCAL_ALPHA),
            )
            val type = TaroTheme.text
            val spacing = TaroTheme.spacing
            if (topLeft < 0) {
                BentoValue(kcal(-topLeft), MaterialTheme.colorScheme.error)
                BentoNote(stringResource(R.string.home_kcal_over))
            } else {
                BentoValue(kcal(left.coerceAtLeast(0.0)), colors.special)
                BentoNote(if (ranged) stringResource(R.string.home_kcal_up_to, kcal(topLeft)) else stringResource(R.string.home_kcal_left))
            }
            BudgetBar(
                parts = parts,
                eaten = food.kcal,
                burned = burn.total,
                floor = floor,
                top = top,
                modifier = Modifier.padding(top = spacing.gap),
            )
            Row(modifier = Modifier.padding(top = spacing.tight)) {
                FitText(stringResource(R.string.home_food_eaten, kcal(food.kcal)), type.ringNote, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f), TextAlign.Start)
                FitText(stringResource(R.string.home_food_burned, kcal(burn.total)), type.ringNote, colors.flame, Modifier.weight(1f), TextAlign.End)
            }
            Spacer(Modifier.weight(1f))
            val proteinTarget = weight.toDouble() * proteinPerKg
            TileMeter(R.string.food_protein, stringResource(R.string.food_protein_of, kcal(food.protein), kcal(proteinTarget)), food.protein, PROTEIN_BLUE, target = proteinTarget)
            TileMeter(R.string.food_carbs, stringResource(R.string.food_macro_value, kcal(food.carbs)), food.carbs, CARB_VIOLET, range = Macros.CARB_SHARE.let { Macros.grams(top, Macros.CARB_KCAL, it.start)..Macros.grams(top, Macros.CARB_KCAL, it.endInclusive) })
            TileMeter(R.string.food_fat, stringResource(R.string.food_macro_value, kcal(food.fat)), food.fat, FAT_CORAL, range = Macros.FAT_SHARE.let { Macros.grams(top, Macros.FAT_KCAL, it.start)..Macros.grams(top, Macros.FAT_KCAL, it.endInclusive) })
        }
    }
}

@Composable
private fun TileMeter(@StringRes label: Int, value: String, grams: Double, color: Color, target: Double? = null, range: ClosedFloatingPointRange<Double>? = null) {
    val type = TaroTheme.text
    val scale = maxOf((target ?: 0.0) * TARGET_ROOM, (range?.endInclusive ?: 0.0) * TARGET_ROOM, grams).takeIf { it > 0 } ?: 1.0
    Column(modifier = Modifier.padding(top = TaroTheme.spacing.tight)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(label), style = type.ringNote, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, modifier = Modifier.padding(end = TaroTheme.spacing.tight))
            FitText(value, type.ringNote, TaroTheme.colors.accent, Modifier.weight(1f), TextAlign.End)
        }
        MeterBar(
            fraction = (grams / scale).toFloat(),
            color = color,
            band = range?.let { (it.start / scale).toFloat()..(it.endInclusive / scale).toFloat() },
            marker = target?.let { (it / scale).toFloat() },
            height = TaroTheme.spacing.tight,
            modifier = Modifier.padding(top = TaroTheme.spacing.hair),
        )
    }
}

@Composable
private fun SleepTile(refreshKey: Int, onClick: () -> Unit) {
    val context = LocalContext.current
    val colors = TaroTheme.colors
    val glance = rememberRefreshed(refreshKey) { sleepGlance(context) }
    val timeFormat = remember { android.text.format.DateFormat.getTimeFormat(context) }
    BentoTile(order = 3, label = stringResource(R.string.tab_sleep), color = colors.sleep, tinted = true, onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(TaroTheme.spacing.gap)) {
            Column(modifier = Modifier.weight(1f)) {
                val last = glance.last
                if (last == null) {
                    BentoNote(stringResource(R.string.home_sleep_none), Modifier.padding(top = TaroTheme.spacing.tight))
                } else {
                    val minutes = last.durationMinutes
                    BentoValue(stringResource(R.string.sleep_duration, (minutes / 60).toInt(), (minutes % 60).toInt()), colors.sleep)
                    BentoNote(stringResource(R.string.home_sleep_times, timeFormat.format(Date(last.startAt)), timeFormat.format(Date(last.endAt))))
                    val short = SleepInsights.TARGET_MIN - minutes
                    val targetHours = (SleepInsights.TARGET_MIN / 60).toInt()
                    BentoNote(
                        if (short > 0) {
                            stringResource(R.string.home_sleep_short, if (short >= 60) stringResource(R.string.sleep_duration, (short / 60).toInt(), (short % 60).toInt()) else stringResource(R.string.home_sleep_minutes, short.toInt()), targetHours)
                        } else {
                            stringResource(R.string.home_sleep_met, targetHours)
                        },
                        color = if (short > 0) colors.accent else colors.sleep,
                    )
                }
            }
            val locale = LocalConfiguration.current.locales[0]
            NightColumns(
                hours = glance.week,
                target = SleepInsights.TARGET_MIN / 60.0,
                labels = glance.days.map { it.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, locale) },
                targetLabel = stringResource(R.string.home_sleep_target_label, (SleepInsights.TARGET_MIN / 60).toInt()),
                color = colors.sleep,
                modifier = Modifier
                    .weight(CHART_SHARE)
                    .aspectRatio(SLEEP_CHART_ASPECT),
            )
        }
    }
}

@Composable
private fun WeekTile(history: EnergyHistory?, burn: DayBurn?, refreshKey: Int, onClick: () -> Unit) {
    val context = LocalContext.current
    val colors = TaroTheme.colors
    val incomplete by AppPreferences.incompleteFoodDaysFlow().collectAsStateWithLifecycle(AppPreferences.incompleteFoodDays)
    val loaded = rememberInBackground(history, burn?.date, refreshKey, incomplete) {
        if (history == null || burn == null) null else weekGlance(context, history.pastDays + burn, incomplete, history.restingPerDay)
    }
    if (loaded == null || history != null && burn != null && loaded.value == null) {
        Skeleton { BoneTile(Modifier.fillMaxWidth().aspectRatio(WEEK_SKELETON)) { TileBones(chart = false) } }
        return
    }
    val week = loaded.value ?: return
    BentoTile(order = 4, label = stringResource(R.string.home_this_week), color = colors.accent, onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(TaroTheme.spacing.gap)) {
            Column(modifier = Modifier.weight(1f)) {
                val net = week.net
                if (net == null) {
                    BentoNote(stringResource(R.string.home_week_empty), Modifier.padding(top = TaroTheme.spacing.tight))
                } else {
                    val sign = if (net > 0) "+" else "−"
                    BentoValue("$sign${kcal(abs(net))}", if (net > 0) colors.special else colors.goal, unit = stringResource(R.string.home_kcal_unit))
                    BentoNote(
                        stringResource(
                            if (net > 0) R.string.home_week_over else R.string.home_week_under,
                            Util.formatWeightChange(net / KCAL_PER_KG),
                            context.resources.getQuantityString(R.plurals.home_logged_days, week.loggedDays, week.loggedDays),
                        )
                    )
                }
            }
            PairedBars(
                values = week.days,
                first = colors.flame,
                second = colors.special,
                modifier = Modifier
                    .weight(CHART_SHARE)
                    .aspectRatio(CHART_ASPECT),
            )
        }
    }
}

private const val KCAL_PER_KG = 7_700.0
private const val CHART_SHARE = 0.6f
private const val CHART_ASPECT = 2.4f
private const val SLEEP_CHART_ASPECT = 1.9f

@Composable
private fun WeightTile(refreshKey: Int, modifier: Modifier, onEmpty: () -> Unit, onClick: () -> Unit) {
    val context = LocalContext.current
    val colors = TaroTheme.colors
    val insights by rememberBodyInsights()
    val points = rememberRefreshed(refreshKey) { WeightJournal.points(context) }
    val trend = remember(points, insights) {
        insights?.model.tissueTrend(points).filter { !it.date.isBefore(Util.logicalToday().minusDays(30)) }
    }
    BentoTile(
        order = 5,
        label = stringResource(R.string.weight_title),
        color = colors.accent,
        onClick = if (points.isEmpty()) onEmpty else onClick,
        modifier = modifier,
    ) {
        val latest = trend.lastOrNull()
        if (latest == null) {
            BentoNote(stringResource(R.string.home_weight_empty), Modifier.padding(top = TaroTheme.spacing.tight))
            return@BentoTile
        }
        BentoValue(Util.formatWeight(latest.trend), MaterialTheme.colorScheme.onSurface)
        weekChange(points)?.let { BentoNote(stringResource(R.string.home_per_week, Util.formatWeightChange(it)), color = colors.goal) }
        Spacer(Modifier.weight(1f))
        Sparkline(
            values = trend.map { it.trend },
            color = colors.goal,
            modifier = Modifier
                .padding(top = TaroTheme.spacing.gap)
                .fillMaxWidth()
                .aspectRatio(CHART_ASPECT * 1.5f),
        )
    }
}

@Composable
private fun StreakTile(streak: Streak, today: LocalDate, modifier: Modifier, onClick: () -> Unit) {
    val marks = weekMarks(streak, today).map { it.second }
    val spree = Sprees.reached(streak.current)
    BentoTile(order = 6, color = spreeStyles().getOrNull(spree)?.color ?: TaroTheme.colors.special, tinted = spree >= 0, onClick = onClick, modifier = modifier) {
        SpreeLabel(streak.current, stringResource(R.string.home_streak))
        StreakSpree(streak)
        Spacer(Modifier.weight(1f))
        WeekDots(
            marks = marks,
            modifier = Modifier
                .padding(top = TaroTheme.spacing.gap)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun FitText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Center) {
    BasicText(
        text = text,
        style = style.copy(color = color, textAlign = align),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(maxFontSize = style.fontSize),
        modifier = modifier,
    )
}

private const val RING_TEXT_SHARE = 0.7f
private const val UNKNOWN_KCAL_ALPHA = 0.7f
private const val TARGET_ROOM = 1.25

@Composable
private fun ColumnScope.FoodTileSkeleton() {
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    Skeleton(Modifier.weight(1f)) {
        BoneText(type.value, 0.6f, Modifier.padding(top = spacing.tight))
        BoneText(type.note, 0.8f)
        Bone(Modifier.fillMaxWidth().padding(top = spacing.gap + spacing.tight).height(spacing.gap + spacing.hair), CircleShape)
        Bone(Modifier.fillMaxWidth(0.7f).padding(top = spacing.hair * 2).height(spacing.hair * 2), CircleShape)
        BoneText(type.ringNote, 0.9f, Modifier.padding(top = spacing.tight))
        Spacer(Modifier.weight(1f))
        repeat(3) {
            BoneText(type.ringNote, 0.75f, Modifier.padding(top = spacing.tight))
            Bone(Modifier.fillMaxWidth().height(spacing.tight), CircleShape)
        }
    }
}

private const val WEEK_SKELETON = 2.6f
