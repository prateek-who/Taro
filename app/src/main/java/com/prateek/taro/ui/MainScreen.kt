package com.prateek.taro.ui

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import com.prateek.taro.ui.components.SpreeLabel
import com.prateek.taro.ui.components.spreeStyles
import com.prateek.taro.util.Sprees
import com.prateek.taro.ui.theme.rememberAnimationsEnabled
import com.prateek.taro.ui.components.shake
import com.prateek.taro.ui.components.TierNumber
import com.prateek.taro.ui.components.tierStyles
import com.prateek.taro.ui.components.TierStamp
import com.prateek.taro.ui.components.TierMedal
import com.prateek.taro.ui.components.TierBar
import com.prateek.taro.util.StepTiers
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.Animatable
import com.prateek.taro.ui.components.DayMark
import com.prateek.taro.ui.components.WeekDots
import com.prateek.taro.ui.components.LinkText
import com.prateek.taro.ui.components.RowDivider
import com.prateek.taro.ui.components.BentoTile
import com.prateek.taro.ui.components.KeyFact
import com.prateek.taro.ui.components.interpolate
import com.prateek.taro.ui.components.PaceChart
import com.prateek.taro.ui.components.TextTabs
import com.prateek.taro.ui.components.DaySwitcher
import com.prateek.taro.util.Streak
import com.prateek.taro.util.StepPaces
import com.prateek.taro.util.StepPace
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.format.TextStyle
import java.time.Month
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.delay
import com.prateek.taro.ui.components.LocalTabActive
import com.prateek.taro.ui.components.infoOf
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.LocalNavigator
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.TaroNavigationBar
import com.prateek.taro.ui.components.SwipeableTabs
import androidx.compose.foundation.pager.rememberPagerState
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.foundation.layout.aspectRatio
import com.prateek.taro.ui.components.BentoNote
import com.prateek.taro.ui.components.Fact
import com.prateek.taro.ui.components.SectionLabel
import com.prateek.taro.ui.components.ConfettiBurst
import com.prateek.taro.ui.components.MessageDialog
import com.prateek.taro.ui.components.MonthCalendar
import com.prateek.taro.ui.components.NumberInputDialog
import com.prateek.taro.ui.components.PauseDialogs
import com.prateek.taro.ui.components.StepsBarChart
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Util
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.prateek.taro.ui.components.Panel
import kotlin.math.abs

@Immutable
data class TrackingState(
    val steps: Int,
    val paused: Boolean,
    val refreshKey: Int,
    val showOnboarding: Boolean,
)

class MainActions(
    val onTogglePause: () -> Unit,
    val onPauseFor: (durationMinutes: Int, specificEndTime: Long) -> Unit,
    val onPauseIndefinitely: () -> Unit,
    val onUpdateSteps: (Int) -> Unit,
    val onOnboardingDone: () -> Unit,
)

val LocalTracking = compositionLocalOf<TrackingState> { error("TrackingState not provided") }
val LocalMainActions = staticCompositionLocalOf<MainActions> { error("MainActions not provided") }

object StepsPage : Screen {
    @Composable
    override fun Content() {
        MainScreen(LocalTracking.current, LocalMainActions.current)
    }
}

private val BOTTOM_BAR_HEIGHT = 80.dp
private val ROOT_TABS: List<Tab> = listOf(HomeTab, TrendsTab, YouTab)
private const val WEEK_CHART_ASPECT = 1.6f

object RootScreen : Screen {
    @Composable
    override fun Content() {
        val toast = LocalToast.current
        DisposableEffect(Unit) {
            toast.bottomOffset = if (ROOT_TABS.size > 1) BOTTOM_BAR_HEIGHT else 0.dp
            onDispose { toast.bottomOffset = 0.dp }
        }
        TabNavigator(HomeTab) { tabNavigator ->
            val tabs = ROOT_TABS
            val showBar = tabs.size > 1
            val pagerState = rememberPagerState(initialPage = tabs.indexOf(tabNavigator.current).coerceAtLeast(0)) { tabs.size }
            Scaffold(
                containerColor = TaroTheme.colors.background,
                bottomBar = { if (showBar) TaroNavigationBar(tabs, pagerState) },
            ) { padding ->
                SwipeableTabs(
                    tabs = tabs,
                    pagerState = pagerState,
                    modifier = Modifier
                        .padding(padding)
                        .consumeWindowInsets(padding),
                )
            }
        }
    }
}

private sealed interface MainDialog {
    data object EditSteps : MainDialog
    data class ConfirmDecrease(val steps: Int) : MainDialog
    data object Pause : MainDialog
}

private enum class HistoryTab { WEEK, MONTH, YEAR }

private val calendarDayOfWeek = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
)

private const val PACE_ASPECT = 1.9f
private const val STAMP_LAND_MS = 140L
private const val HAPTIC_GAP_MS = 110L
private const val SHAKE_PER_TIER = 0.25f
private const val WALK_TIME_SHARE = 0.24f
private const val PACE_EVEN_SHARE = 0.03

@Composable
private fun MainScreen(tracking: TrackingState, actions: MainActions) {
    val steps = tracking.steps
    val paused = tracking.paused
    val refreshKey = tracking.refreshKey * 1_000_003 + rememberDataVersion()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toast = LocalToast.current
    val spacing = TaroTheme.spacing
    val colors = TaroTheme.colors

    val today = Util.logicalToday()
    var date by rememberSaveable { mutableStateOf(today) }
    var historyTab by rememberSaveable { mutableStateOf(HistoryTab.WEEK) }
    var calendarJump by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<MainDialog?>(null) }
    val isToday = date == today

    val goalTarget by AppPreferences.dailyGoalTargetFlow().collectAsStateWithLifecycle(AppPreferences.dailyGoalTarget)
    val goalHistory by AppPreferences.goalHistoryFlow().collectAsStateWithLifecycle(AppPreferences.goalHistory)
    val goalChartLine by AppPreferences.dailyGoalChartLineFlow().collectAsStateWithLifecycle(AppPreferences.dailyGoalChartLine)
    val factors by AppPreferences.energyFactorsFlow().collectAsStateWithLifecycle(AppPreferences.energyFactors)
    val quickLog = rememberQuickLog()
    val minDate = rememberRefreshed(refreshKey) { firstEntryDate(context) }
    val summary = rememberRefreshed(date, steps, refreshKey, factors) {
        if (date == Util.logicalToday()) summary(context, Selection.Range(StepRange.TODAY), steps) else daySummary(context, date)
    }
    val goal = if (goalTarget > 0) goalHistory.on(date) else 0
    val day = rememberInBackground(date, summary.steps, goal, refreshKey) { stepsDay(context, date, summary.steps, goal) }?.value
    val tallyVersion = rememberDataVersion()
    val tally = rememberInBackground(date, tallyVersion) { if (date == Util.logicalToday()) null else dayTally(context, date) }?.value
    val chart = rememberRefreshed(date, steps, refreshKey) { chartData(context, false, date, steps) }
    val months = rememberRefreshed(date.year, steps, refreshKey) { monthlyAverages(context, date.year, Util.logicalToday(), steps) }
    val dayTotals = rememberRefreshed(refreshKey) { stepsByDay(context) }
    val streak = rememberStepStreak(dayTotals, steps)
    val heat = remember(dayTotals, steps, goalHistory) { heatFractions(dayTotals + (today to steps), goalHistory) }
    val scrollState = rememberScrollState()
    val haptics = LocalHapticFeedback.current
    val goalMet = goalTarget > 0 && steps >= goalTarget
    var celebrate by remember { mutableStateOf(false) }
    val animations = rememberAnimationsEnabled()
    val shake = remember { Animatable(0f) }
    var stamp by remember { mutableStateOf<Int?>(null) }
    val tier = StepTiers.reached(summary.steps, goal)

    LaunchedEffect(isToday, tier) {
        if (!isToday || tier < 0) return@LaunchedEffect
        val key = Util.todayDateString()
        val seen = AppPreferences.tierSeen?.split(":")?.takeIf { it.firstOrNull() == key }?.getOrNull(1)?.toIntOrNull() ?: -1
        if (tier <= seen) return@LaunchedEffect
        AppPreferences.tierSeen = "$key:$tier"
        if (!animations) return@LaunchedEffect
        stamp = tier
        delay(STAMP_LAND_MS)
        launch { shake.shake(1f + tier * SHAKE_PER_TIER) }
        repeat(tier + 1) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(HAPTIC_GAP_MS)
        }
    }

    LaunchedEffect(goalMet) {
        val todayKey = Util.todayDateString()
        if (goalMet && AppPreferences.lastCelebrationDate != todayKey) {
            AppPreferences.lastCelebrationDate = todayKey
            celebrate = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val tabActive = LocalTabActive.current
    LaunchedEffect(tabActive) {
        if (tabActive && !AppPreferences.stepsHintShown) {
            delay(1_500)
            toast.show(context.getString(R.string.home_steps_hint), ToastKind.INFO)
            AppPreferences.stepsHintShown = true
        }
    }

    fun showMessage(textRes: Int, kind: ToastKind) {
        toast.show(context.getString(textRes), kind)
    }

    fun open(picked: LocalDate) {
        date = picked
        scope.launch { scrollState.animateScrollTo(0) }
    }

    var editGoal by remember { mutableStateOf(false) }
    if (editGoal) StepGoalDialog(onDismiss = { editGoal = false })
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        AppOverflowMenu(
            back = true,
            center = {
                DaySwitcher(
                    date = date,
                    today = today,
                    onChange = {
                        date = it
                        calendarJump++
                    },
                )
            },
            actions = quickLogActions(quickLog, today) +
                MenuEntry(R.string.home_edit_steps, R.string.home_edit_steps_hint, R.drawable.ic_steps, colors.goal) { dialog = MainDialog.EditSteps },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(start = spacing.loose, end = spacing.loose, bottom = spacing.loose),
        ) {
            StepsHero(
                steps = summary.steps,
                goal = goal,
                pace = day?.pace,
                cadence = day?.cadence,
                isToday = isToday,
                paused = paused && isToday,
                date = date,
                shake = shake.value,
                onLongClick = { if (isToday) dialog = MainDialog.EditSteps },
                onEditGoal = { editGoal = true },
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.loose),
            ) {
                Fact(summary.distanceValue, summary.distanceUnit, colors.goal, info = infoOf(R.string.info_distance_title, R.string.info_distance), icon = R.drawable.ic_distance, style = TaroTheme.text.factCompact)
                Fact(summary.caloriesValue, stringResource(R.string.home_kcal_unit), colors.flame, info = infoOf(R.string.info_home_kcal_title, R.string.info_home_kcal), icon = R.drawable.ic_calories, style = TaroTheme.text.factCompact)
                if (goalTarget > 0) {
                    Fact(
                        streak.current.toString(),
                        pluralStringResource(R.plurals.streak_fact, streak.current),
                        colors.special,
                        info = infoOf(R.string.info_streak_title, R.string.info_streak),
                        icon = R.drawable.ic_badge_week,
                        style = TaroTheme.text.factCompact,
                    )
                }
            }

            streak.challenge?.takeIf { isToday }?.let {
                StreakChallengeCard(it, streak, steps, goalTarget, today, Modifier.padding(top = spacing.loose))
            }

            tally?.takeIf { !isToday }?.let { TallyCard(it, stringResource(R.string.tally_title), play = true, modifier = Modifier.padding(top = spacing.loose)) }

            day?.let { loaded ->
                PacePanel(loaded.pace, goal, date, Modifier.padding(top = spacing.loose))
                SectionLabel(stringResource(R.string.steps_walks))
                WalksPanel(loaded.walks, summary.steps)
            }

            if (goalTarget > 0) {
                SectionLabel(stringResource(R.string.home_streak))
                StreakPanel(streak, today)
            }

            SectionLabel(stringResource(R.string.steps_history))
            Panel {
                Column(modifier = Modifier.padding(spacing.tight)) {
                    TextTabs(
                        options = listOf(
                            HistoryTab.WEEK to stringResource(R.string.steps_tab_week),
                            HistoryTab.MONTH to stringResource(R.string.steps_tab_month),
                            HistoryTab.YEAR to stringResource(R.string.steps_tab_year),
                        ),
                        selected = historyTab,
                        onSelect = { historyTab = it },
                        color = colors.goal,
                    )
                    when (historyTab) {
                        HistoryTab.WEEK -> {
                            StepsBarChart(
                                values = chart.values,
                                labels = chart.labels,
                                goal = if (goalTarget > 0 && goalChartLine) goalTarget else 0,
                                highlightGoal = goalTarget,
                                goals = if (goalTarget > 0) chart.dates.map(goalHistory::on) else null,
                                appearKey = chart.range,
                                selectedIndex = chart.dates.indexOf(date).takeIf { it >= 0 },
                                onBarClick = { index -> chart.dates[index].takeIf { !it.isAfter(today) }?.let(::open) },
                                modifier = Modifier
                                    .padding(top = spacing.loose)
                                    .fillMaxWidth()
                                    .aspectRatio(WEEK_CHART_ASPECT),
                            )
                            val counted = chart.dates.indices.filter { !chart.dates[it].isAfter(today) && chart.values[it] > 0 }
                            if (counted.isNotEmpty()) {
                                val average = counted.sumOf { chart.values[it] } / counted.size
                                val met = counted.count { goalHistory.on(chart.dates[it]).let { g -> g > 0 && chart.values[it] >= g } }
                                HistoryKeys(average, met, counted.size, counted.maxOf { chart.values[it] }, goalTarget > 0)
                            }
                        }
                        HistoryTab.MONTH -> {
                            MonthCalendar(
                                heat = heat,
                                selected = date,
                                minDate = minDate,
                                maxDate = today,
                                firstDayOfWeek = calendarDayOfWeek[AppPreferences.firstDayOfWeek - 1],
                                jumpKey = calendarJump,
                                onSelect = ::open,
                                modifier = Modifier.padding(top = spacing.tight),
                            )
                            val month = dayTotals.filterKeys { it.year == date.year && it.month == date.month } + listOfNotNull((today to steps).takeIf { today.year == date.year && today.month == date.month })
                            val counted = month.filterValues { it > 0 }
                            if (counted.isNotEmpty()) {
                                HistoryKeys(
                                    average = counted.values.average().toInt(),
                                    met = counted.count { (d, v) -> goalHistory.on(d).let { g -> g > 0 && v >= g } },
                                    days = counted.size,
                                    best = counted.values.max(),
                                    showGoal = goalTarget > 0,
                                )
                            }
                        }
                        HistoryTab.YEAR -> {
                            StepsBarChart(
                                values = months,
                                labels = Month.entries.map { it.getDisplayName(TextStyle.NARROW, Locale.getDefault()) },
                                goal = if (goalTarget > 0 && goalChartLine) goalTarget else 0,
                                highlightGoal = goalTarget,
                                appearKey = date.year,
                                showMultiplier = false,
                                modifier = Modifier
                                    .padding(top = spacing.loose)
                                    .fillMaxWidth()
                                    .aspectRatio(WEEK_CHART_ASPECT),
                            )
                            BentoNote(stringResource(R.string.steps_year_note, date.year), Modifier.padding(top = spacing.tight))
                        }
                    }
                }
            }

            LinkText(
                text = stringResource(if (paused) R.string.steps_resume else R.string.steps_pause),
                color = if (paused) colors.goal else colors.accent,
                onClick = { if (paused) actions.onTogglePause() else dialog = MainDialog.Pause },
                icon = if (paused) R.drawable.ic_play else R.drawable.ic_pause,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = spacing.loose),
            )
        }
    }

    if (celebrate) {
        ConfettiBurst(onFinished = { celebrate = false })
    }
    stamp?.let { TierStamp(it, summary.steps, onFinished = { stamp = null }) }

    QuickLogDialogs(quickLog, today)

    when (val current = dialog) {
        MainDialog.EditSteps -> NumberInputDialog(
            title = stringResource(R.string.edit_step_count),
            initial = steps.toString(),
            hint = "",
            onConfirm = { input ->
                dialog = null
                val newSteps = input.toIntOrNull()
                when {
                    newSteps == null -> showMessage(R.string.invalid_step_count, ToastKind.ERROR)
                    newSteps < steps -> dialog = MainDialog.ConfirmDecrease(newSteps)
                    else -> {
                        actions.onUpdateSteps(newSteps)
                        showMessage(R.string.steps_updated, ToastKind.SUCCESS)
                    }
                }
            },
            onDismiss = { dialog = null },
        )
        is MainDialog.ConfirmDecrease -> MessageDialog(
            title = stringResource(R.string.confirm_decrease_title),
            message = stringResource(R.string.confirm_decrease_message),
            onConfirm = {
                dialog = null
                actions.onUpdateSteps(current.steps)
                showMessage(R.string.steps_updated, ToastKind.SUCCESS)
            },
            onDismiss = { dialog = null },
        )
        MainDialog.Pause -> PauseDialogs(
            onPauseFor = { minutes, endTime ->
                dialog = null
                actions.onPauseFor(minutes, endTime)
            },
            onPauseIndefinitely = {
                dialog = null
                actions.onPauseIndefinitely()
            },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }

    if (tracking.showOnboarding) {
        val navigator = LocalNavigator.currentOrThrow
        OnboardingDialog(
            onDone = actions.onOnboardingDone,
            onRestore = { (navigator.parent ?: navigator).push(BackupScreen) },
        )
    }
}

@Composable
private fun rememberClock(date: LocalDate): (Double) -> String {
    val context = LocalContext.current
    val format = remember { android.text.format.DateFormat.getTimeFormat(context) }
    val start = remember(date) { Util.dayStartMillis(date) }
    return { hour -> format.format(Date(start + (hour * HOUR_MILLIS).toLong())) }
}

@Composable
private fun rememberHourMarks(date: LocalDate): List<Pair<Double, String>> {
    val context = LocalContext.current
    val start = remember(date) { Util.dayStartMillis(date) }
    return remember(start) {
        val format = SimpleDateFormat(if (android.text.format.DateFormat.is24HourFormat(context)) "H:mm" else "h a", Locale.getDefault())
        listOf(0.0, 6.0, 12.0, 18.0).map { it to format.format(Date(start + (it * HOUR_MILLIS).toLong())).lowercase(Locale.getDefault()) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StepsHero(steps: Int, goal: Int, pace: StepPace?, cadence: Double?, isToday: Boolean, paused: Boolean, date: LocalDate, shake: Float, onLongClick: () -> Unit, onEditGoal: () -> Unit) {
    val colors = TaroTheme.colors
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val clock = rememberClock(date)
    val styles = tierStyles()
    val tier = StepTiers.reached(steps, goal)
    val glow = if (tier >= 1) styles[tier].color else colors.goal
    fun reachedAt(index: Int) = pace?.let { StepPaces.reachedAt(it.today, StepTiers.threshold(index, goal).toDouble()) }
    BentoTile(
        color = glow,
        tinted = true,
        padding = spacing.loose,
        modifier = Modifier
            .padding(top = spacing.tight)
            .fillMaxWidth()
            .graphicsLayer { translationX = shake * size.width * SHAKE_SHARE },
    ) {
        if (tier >= 1) TierMedal(tier, Modifier.padding(bottom = spacing.tight))
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.combinedClickable(interactionSource = null, indication = null, onClick = {}, onLongClick = onLongClick),
        ) {
            TierNumber(Util.formatSteps(steps), type.hero, steps, goal)
            if (goal > 0 || isToday) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(start = spacing.tight, bottom = spacing.tight)
                        .clip(RoundedCornerShape(spacing.tight))
                        .then(if (isToday) Modifier.clickable(onClick = onEditGoal) else Modifier)
                        .padding(horizontal = spacing.hair),
                ) {
                    if (goal > 0) Text(stringResource(R.string.steps_of_goal, Util.formatSteps(goal)), style = type.unit, color = colors.accent)
                    if (isToday) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = stringResource(R.string.steps_goal_edit),
                            tint = colors.accent,
                            modifier = Modifier
                                .padding(start = spacing.hair)
                                .size(with(LocalDensity.current) { type.unit.lineHeight.toDp() }),
                        )
                    }
                }
            }
        }
        if (goal > 0) {
            TierBar(
                steps = steps,
                goal = goal,
                course = pace?.projected?.takeIf { isToday && tier < 0 },
                modifier = Modifier.padding(top = spacing.gap),
            )
        }
        val ink = MaterialTheme.colorScheme.onSurface
        val line = when {
            paused -> AnnotatedString(stringResource(R.string.steps_paused))
            goal <= 0 -> null
            tier >= 0 -> buildAnnotatedString {
                val style = styles[tier]
                val name = stringResource(style.name)
                val at = reachedAt(tier)
                withStyle(SpanStyle(color = if (style.holo) styles[StepTiers.LAST - 1].color else style.color)) {
                    append(if (at != null) stringResource(R.string.tier_reached_at, name, clock(at)) else stringResource(R.string.tier_reached, name))
                }
                append(" ")
                if (tier < StepTiers.LAST) {
                    val next = styles[tier + 1]
                    withStyle(SpanStyle(color = ink)) { append(stringResource(R.string.tier_next, Util.formatSteps(StepTiers.threshold(tier + 1, goal) - steps))) }
                    append(" ")
                    withStyle(SpanStyle(color = if (next.holo) styles[StepTiers.LAST - 1].color else next.color)) { append(stringResource(next.name)) }
                    append(".")
                } else {
                    append(stringResource(R.string.tier_top))
                }
            }
            isToday -> buildAnnotatedString {
                val left = goal - steps
                withStyle(SpanStyle(color = ink)) { append(stringResource(R.string.steps_to_go, Util.formatSteps(left))) }
                cadence?.let { append(pluralStringResource(R.plurals.steps_walk_minutes, (left / it).roundToInt().coerceAtLeast(1), (left / it).roundToInt().coerceAtLeast(1))) }
                append(".")
                pace?.goalHour?.let {
                    append(" ")
                    append(stringResource(R.string.steps_usually_by))
                    withStyle(SpanStyle(color = ink)) { append(" " + clock(it)) }
                    append(".")
                }
            }
            else -> AnnotatedString(stringResource(R.string.steps_short, Util.formatSteps(goal - steps)))
        }
        line?.takeIf { it.isNotEmpty() }?.let {
            Text(it, style = type.meterValue, color = colors.accent, modifier = Modifier.padding(top = spacing.gap))
        }
    }
}

private const val SHAKE_SHARE = 0.02f

@Composable
private fun PacePanel(pace: StepPace, goal: Int, date: LocalDate, modifier: Modifier = Modifier) {
    val colors = TaroTheme.colors
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val clock = rememberClock(date)
    val marks = rememberHourMarks(date)
    var selected by remember(pace) { mutableStateOf<Double?>(null) }
    val usual = pace.usual?.let { curve -> curve.indices.map { it.toDouble() to curve[it] } }
    val kind = stringResource(if (pace.weekend) R.string.pace_weekend else R.string.pace_weekday)
    val end = pace.now ?: StepPaces.HOURS.toDouble()
    val diff = pace.usualByNow?.let { pace.total - it }
    val title = when {
        diff == null -> stringResource(R.string.pace_title)
        kotlin.math.abs(diff) <= maxOf(pace.usualByNow ?: 0, 1) * PACE_EVEN_SHARE -> stringResource(R.string.pace_even, kind)
        diff > 0 -> stringResource(R.string.pace_ahead, Util.formatSteps(diff), kind)
        else -> stringResource(R.string.pace_behind, Util.formatSteps(-diff), kind)
    }
    val note = when {
        pace.usual == null -> stringResource(R.string.pace_learning)
        pace.now != null -> pace.projected?.let { stringResource(R.string.pace_course, Util.formatSteps(roundTo(it))) }
        else -> pace.usualTotal?.let { stringResource(R.string.pace_usual_total, kind, Util.formatSteps(it)) }
    }
    BentoTile(colors.goal, tinted = true, modifier = modifier.fillMaxWidth()) {
        Text(title, style = type.title, color = MaterialTheme.colorScheme.onSurface)
        note?.let { BentoNote(it) }
        PaceChart(
            today = pace.today,
            usual = usual,
            course = pace.course,
            goal = goal,
            goalLabel = stringResource(R.string.pace_goal_label, Util.formatSteps(goal)),
            hourLabels = marks,
            color = colors.goal,
            selected = selected,
            onSelect = { selected = it },
            modifier = Modifier
                .padding(top = spacing.gap)
                .fillMaxWidth()
                .aspectRatio(PACE_ASPECT),
        )
        val at = selected ?: end
        val todayAt = interpolate(pace.today, minOf(at, pace.today.last().first)).roundToInt()
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.loose), modifier = Modifier.padding(top = spacing.gap)) {
            KeyFact(colors.goal, Util.formatSteps(todayAt), stringResource(R.string.pace_key_today, clock(minOf(at, pace.today.last().first))))
            usual?.let { KeyFact(colors.accent, Util.formatSteps(interpolate(it, at).roundToInt()), stringResource(R.string.pace_key_usual, clock(at))) }
            if (selected == null) {
                pace.goalHour?.let {
                    KeyFact(colors.goal, clock(it), stringResource(if (it <= end) R.string.pace_key_goal_at else R.string.pace_key_goal_likely))
                }
            }
        }
    }
}

private fun roundTo(value: Int) = (value / ROUND_STEPS.toDouble()).roundToInt() * ROUND_STEPS

private const val ROUND_STEPS = 100
private const val HOUR_MILLIS = 3_600_000L

@Composable
private fun WalksPanel(walks: List<WalkRow>, total: Int) {
    val colors = TaroTheme.colors
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val context = LocalContext.current
    val format = remember { android.text.format.DateFormat.getTimeFormat(context) }
    if (walks.isEmpty()) {
        Panel { BentoNote(stringResource(R.string.steps_walks_empty), Modifier.padding(spacing.tight)) }
        return
    }
    val longest = walks.maxOf { it.walk.minutes }.coerceAtLeast(1)
    Panel {
        Column(modifier = Modifier.padding(horizontal = spacing.tight)) {
            walks.forEachIndexed { index, row ->
                if (index > 0) RowDivider()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.gap),
                    modifier = Modifier.padding(vertical = spacing.gap),
                ) {
                    Text(format.format(Date(row.walk.start)), style = type.note, color = colors.accent, maxLines = 1, modifier = Modifier.weight(WALK_TIME_SHARE))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(row.walk.minutes.toFloat() / longest)
                                .height(spacing.tight)
                                .clip(CircleShape)
                                .background(if (row.longest) colors.goal else colors.goal.copy(alpha = WALK_FADED_ALPHA)),
                        )
                        Text(
                            text = buildAnnotatedString {
                                append(stringResource(R.string.steps_walk_minutes_row, row.walk.minutes))
                                if (row.longest) withStyle(SpanStyle(color = colors.goal)) { append(" · " + stringResource(R.string.steps_walk_longest)) }
                            },
                            style = type.note,
                            color = colors.accent,
                            modifier = Modifier.padding(top = spacing.hair),
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(Util.formatSteps(row.walk.steps), style = type.meterLabel, color = MaterialTheme.colorScheme.onSurface)
                        Text("%.1f %s".format(Locale.getDefault(), Util.metersToDistance(row.distanceM), Util.distanceUnit()), style = type.note, color = colors.accent)
                    }
                }
            }
        }
    }
    val walked = walks.sumOf { it.walk.steps }
    BentoNote(
        pluralStringResource(R.plurals.steps_walks_note, walks.size, walks.size, minutesText(walks.sumOf { it.walk.minutes }.toLong()), Util.formatSteps((total - walked).coerceAtLeast(0))),
        Modifier.padding(start = spacing.hair, top = spacing.tight),
    )
}

private const val WALK_FADED_ALPHA = 0.55f

@Composable
private fun StreakPanel(streak: Streak, today: LocalDate) {
    val context = LocalContext.current
    val week = weekMarks(streak, today)
    val rests = week.count { it.second == DayMark.REST }
    val spree = Sprees.reached(streak.current)
    BentoTile(color = spreeStyles().getOrNull(spree)?.color ?: TaroTheme.colors.special, tinted = spree >= 0) {
        SpreeLabel(streak.current, fallback = null)
        StreakSpree(streak)
        WeekDots(
            marks = week.map { it.second },
            labels = week.map { it.first.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()) },
            modifier = Modifier
                .padding(top = TaroTheme.spacing.loose)
                .fillMaxWidth(),
        )
        BentoNote(
            if (rests > 0) context.resources.getQuantityString(R.plurals.home_rest_used, rests, rests) else stringResource(R.string.home_rest_left),
            Modifier.padding(top = TaroTheme.spacing.tight),
        )
    }
}

@Composable
private fun HistoryKeys(average: Int, met: Int, days: Int, best: Int, showGoal: Boolean) {
    val colors = TaroTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(TaroTheme.spacing.loose), modifier = Modifier.padding(top = TaroTheme.spacing.gap)) {
        KeyFact(colors.goal, Util.formatSteps(average), stringResource(R.string.steps_key_average))
        if (showGoal) KeyFact(colors.special, stringResource(R.string.steps_key_goal_days_value, met, days), stringResource(R.string.steps_key_goal_days))
        KeyFact(colors.accent, Util.formatSteps(best), stringResource(R.string.steps_key_best))
    }
}
