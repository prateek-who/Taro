package com.prateek.taro.ui

import kotlinx.coroutines.delay
import com.prateek.taro.ui.components.LocalTabActive
import com.prateek.taro.ui.components.rememberInfoState
import com.prateek.taro.ui.components.InfoPopup
import com.prateek.taro.ui.components.ToggleGroup
import com.prateek.taro.ui.components.infoOf
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.LocalNavigator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.TaroNavigationBar
import com.prateek.taro.ui.components.SwipeableTabs
import androidx.compose.foundation.pager.rememberPagerState
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.R
import com.prateek.taro.ui.components.ConfettiBurst
import com.prateek.taro.ui.components.MessageDialog
import com.prateek.taro.ui.components.MonthCalendar
import com.prateek.taro.ui.components.NumberInputDialog
import com.prateek.taro.ui.components.PauseDialogs
import com.prateek.taro.ui.components.SecondaryButton
import com.prateek.taro.ui.components.StepsBarChart
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Util
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.prateek.taro.ui.components.PillSelector
import com.prateek.taro.ui.components.RollingText
import com.prateek.taro.ui.components.StatPill
import com.prateek.taro.ui.components.HeroRing
import com.prateek.taro.ui.components.RingSegment
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import com.prateek.taro.ui.components.Panel
import com.prateek.taro.ui.theme.TaroMotion
import kotlin.math.abs
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import com.prateek.taro.ui.theme.rememberAnimationsEnabled
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

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

object HomeTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 0u,
            title = stringResource(R.string.tab_home),
            icon = painterResource(R.drawable.ic_home),
        )

    @Composable
    override fun Content() {
        MainScreen(LocalTracking.current, LocalMainActions.current)
    }
}

private val BOTTOM_BAR_HEIGHT = 80.dp

object RootScreen : Screen {
    @Composable
    override fun Content() {
        val toast = LocalToast.current
        DisposableEffect(Unit) {
            toast.bottomOffset = BOTTOM_BAR_HEIGHT
            onDispose { toast.bottomOffset = 0.dp }
        }
        TabNavigator(HomeTab) { tabNavigator ->
            val tabs = remember { listOf(HomeTab, EnergyTab, SleepTab) }
            val pagerState = rememberPagerState(initialPage = tabs.indexOf(tabNavigator.current).coerceAtLeast(0)) { tabs.size }
            Scaffold(
                containerColor = TaroTheme.colors.background,
                bottomBar = { TaroNavigationBar(tabs, pagerState) },
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

private val calendarDayOfWeek = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
)

@Composable
private fun MainScreen(tracking: TrackingState, actions: MainActions) {
    val steps = tracking.steps
    val paused = tracking.paused
    val refreshKey = tracking.refreshKey * 1_000_003 + rememberDataVersion()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toast = LocalToast.current

    var selection by remember { mutableStateOf(loadSelection(context)) }
    var past7Days by remember { mutableStateOf(loadPast7DaysMode(context)) }
    var selectedDate by rememberSaveable { mutableStateOf(Util.logicalToday()) }
    var pickedDay by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var calendarJump by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<MainDialog?>(null) }

    val goalTarget by AppPreferences.dailyGoalTargetFlow().collectAsStateWithLifecycle(AppPreferences.dailyGoalTarget)
    val goalHistory by AppPreferences.goalHistoryFlow().collectAsStateWithLifecycle(AppPreferences.goalHistory)
    val goalChartLine by AppPreferences.dailyGoalChartLineFlow().collectAsStateWithLifecycle(AppPreferences.dailyGoalChartLine)

    val factors by AppPreferences.energyFactorsFlow().collectAsStateWithLifecycle(AppPreferences.energyFactors)
    val quickLog = rememberQuickLog()
    val years = rememberRefreshed(refreshKey) { yearsWithData(context) }
    val minDate = rememberRefreshed(refreshKey) { firstEntryDate(context) }
    val summary = rememberRefreshed(selection, pickedDay, steps, refreshKey, factors, goalHistory) {
        pickedDay?.let { daySummary(context, it) } ?: summary(context, selection, steps)
    }
    val chart = rememberRefreshed(past7Days, selectedDate, steps, refreshKey) { chartData(context, past7Days, selectedDate, steps) }
    val goal = rememberRefreshed(goalTarget, goalHistory, refreshKey) { goalLine(context, goalTarget) }
    val isToday = pickedDay == null && selection == Selection.Range(StepRange.TODAY)
    val dayView = isToday || pickedDay != null
    val ringValue = summary.steps
    val ringTarget = if (goalTarget > 0) summary.goalTotal else 0
    val dayTotals = rememberRefreshed(refreshKey) { stepsByDay(context) }
    val heat = remember(dayTotals, steps, goalHistory) {
        heatFractions(dayTotals + (Util.logicalToday() to steps), goalHistory)
    }
    val scrollState = rememberScrollState()
    var heroBottom by remember { mutableFloatStateOf(1f) }
    val collapsed by remember { derivedStateOf { scrollState.value > heroBottom * 0.85f } }
    var fabVisible by remember { mutableStateOf(true) }
    LaunchedEffect(scrollState) {
        var last = 0
        snapshotFlow { scrollState.value }.collect { value ->
            if (abs(value - last) > 12 || value == 0) {
                fabVisible = value <= last
                last = value
            }
        }
    }
    val haptics = LocalHapticFeedback.current
    val selectionOptions = StepRange.entries.map { Selection.Range(it) to stringResource(it.label) } +
        years.map { Selection.Year(it) to it.toString() }
    val goalMet = goalTarget > 0 && steps >= goalTarget
    var celebrate by remember { mutableStateOf(false) }

    LaunchedEffect(goalMet) {
        val today = Util.todayDateString()
        if (goalMet && AppPreferences.lastCelebrationDate != today) {
            AppPreferences.lastCelebrationDate = today
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

    fun resetCalendarToToday() {
        selectedDate = Util.logicalToday()
        calendarJump++
    }

    fun select(newSelection: Selection) {
        selection = newSelection
        pickedDay = null
        saveSelection(context, newSelection)
        if (newSelection == Selection.Range(StepRange.SEVEN_DAYS)) {
            resetCalendarToToday()
            past7Days = true
        }
    }

    Scaffold(
        containerColor = TaroTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter = scaleIn(TaroMotion.bouncy()) + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                PauseFab(
                    paused = paused,
                    onClick = actions.onTogglePause,
                    onLongClick = { if (!paused) dialog = MainDialog.Pause },
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .statusBarsPadding(),
        ) {
            AppOverflowMenu(
                actions = quickLogActions(quickLog, Util.logicalToday()) +
                    MenuEntry(R.string.home_edit_steps, R.string.home_edit_steps_hint, R.drawable.ic_steps, TaroTheme.colors.goal) { dialog = MainDialog.EditSteps },
            )
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                ) {
                    PillSelector(
                        options = selectionOptions,
                        selected = if (pickedDay == null) selection else null,
                        onSelect = { if (it != null) select(it) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onPlaced { heroBottom = it.positionInParent().y + it.size.height }
                            .graphicsLayer {
                                val fraction = (scrollState.value / heroBottom).coerceIn(0f, 1f)
                                alpha = 1f - fraction * 0.9f
                                scaleX = 1f - fraction * 0.12f
                                scaleY = scaleX
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Hero(
                            summary = summary,
                            goal = ringTarget,
                            ringValue = ringValue,
                            dayView = dayView,
                            onStepsLongClick = { if (isToday) dialog = MainDialog.EditSteps },
                        )
                        AnimatedVisibility(visible = pickedDay != null) {
                            SecondaryButton(
                                text = stringResource(R.string.back_to_range, selectionLabel(selection)),
                                onClick = { pickedDay = null },
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        goal?.let {
                            if (it.streak) {
                                StatPill(
                                    icon = R.drawable.ic_calories,
                                    text = it.text,
                                    color = TaroTheme.colors.special,
                                    lit = true,
                                    modifier = Modifier.padding(top = 10.dp),
                                    info = infoOf(R.string.info_streak_title, R.string.info_streak),
                                )
                            } else {
                                Text(
                                    text = it.text.uppercase(),
                                    fontSize = 14.sp,
                                    letterSpacing = 1.sp,
                                    color = TaroTheme.colors.accent,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Panel(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .height(330.dp),
                    ) {
                        ToggleGroup(
                            options = listOf(true to stringResource(R.string.header_7d), false to stringResource(R.string.home_calendar_week)),
                            selected = past7Days,
                            onSelect = {
                                past7Days = it
                                savePast7DaysMode(context, it)
                                resetCalendarToToday()
                            },
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                        CenteredText(
                            text = if (past7Days) chart.range else "${chart.header} · ${chart.range}",
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
                        )
                        StepsBarChart(
                            values = chart.values,
                            labels = chart.labels,
                            goal = if (goalTarget > 0 && goalChartLine) goalTarget else 0,
                            highlightGoal = goalTarget,
                            goals = if (goalTarget > 0) chart.dates.map(goalHistory::on) else null,
                            appearKey = chart.range,
                            selectedIndex = pickedDay?.let { chart.dates.indexOf(it) }?.takeIf { it >= 0 },
                            onBarClick = { index ->
                                val date = chart.dates[index]
                                if (!date.isAfter(Util.logicalToday())) {
                                    selectedDate = date
                                    pickedDay = date.takeIf { it != Util.logicalToday() }
                                    scope.launch { scrollState.animateScrollTo(0) }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        )
                    }

                    StepsByHourPanel(
                        date = selectedDate,
                        refreshKey = if (selectedDate == Util.logicalToday()) "$refreshKey-$steps" else refreshKey,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    )

                    Panel(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 52.dp)) {
                        MonthCalendar(
                            heat = heat,
                            selected = selectedDate,
                            minDate = minDate,
                            maxDate = Util.logicalToday(),
                            firstDayOfWeek = calendarDayOfWeek[AppPreferences.firstDayOfWeek - 1],
                            jumpKey = calendarJump,
                            onSelect = {
                                selectedDate = it
                                past7Days = false
                                pickedDay = it.takeIf { date -> date != Util.logicalToday() }
                                scope.launch { scrollState.animateScrollTo(0) }
                            },
                        )
                    }

                    Spacer(Modifier.navigationBarsPadding().height(24.dp))
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = collapsed,
                    enter = slideInVertically(TaroMotion.snappy()) { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut(),
                ) {
                    CompactHeader(
                        summary = summary,
                        progress = if (ringTarget > 0) ringValue.toFloat() / ringTarget else 0f,
                        lit = ringTarget > 0 && ringValue >= ringTarget,
                        onClick = { scope.launch { scrollState.animateScrollTo(0) } },
                    )
                }
            }
        }
    }

    if (celebrate) {
        ConfettiBurst(onFinished = { celebrate = false })
    }

    QuickLogDialogs(quickLog, Util.logicalToday())

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Hero(summary: Summary, goal: Int, ringValue: Int, dayView: Boolean, onStepsLongClick: () -> Unit) {
    val lit = goal > 0 && ringValue >= goal
    val stepsInfo = rememberInfoState()
    val multiplier = Util.goalMultiplier(ringValue, goal)
    val caption = when {
        !dayView && goal > 0 -> stringResource(R.string.ring_of_goal_average, Util.formatSteps(goal), Util.formatSteps(summary.averageSteps ?: 0))
        !dayView -> stringResource(R.string.ring_average, Util.formatSteps(summary.averageSteps ?: 0))
        goal > 0 -> stringResource(R.string.ring_of_goal, Util.formatSteps(goal))
        else -> stringResource(R.string.ring_steps)
    }

    HeroRing(
        segments = listOf(RingSegment(if (goal > 0) ringValue.toDouble() else 0.0, TaroTheme.colors.goal)),
        target = goal.coerceAtLeast(1).toDouble(),
        lit = lit,
        gradient = true,
    ) {
        AnimatedContent(
            targetState = summary.header.uppercase(),
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "hero header",
        ) {
            Text(it, fontSize = 13.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold, color = TaroTheme.colors.accent)
        }
        RollingText(
            text = Util.formatSteps(summary.steps),
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 52.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(onClick = { stepsInfo.open = true }, onLongClick = onStepsLongClick),
        )
        InfoPopup(stepsInfo, infoOf(R.string.info_steps_count_title, R.string.info_steps_count), TaroTheme.colors.goal)
        AnimatedContent(
            targetState = caption,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "hero caption",
        ) {
            Text(it, fontSize = 14.sp, color = TaroTheme.colors.accent)
        }
        AnimatedVisibility(
            visible = multiplier != null,
            enter = scaleIn(TaroMotion.bouncy()) + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            Text(
                text = multiplier.orEmpty(),
                color = TaroTheme.colors.special,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(CircleShape)
                    .background(TaroTheme.colors.special.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
    ) {
        StatPill(R.drawable.ic_steps, summary.distance, TaroTheme.colors.goal, lit, info = infoOf(R.string.info_distance_title, R.string.info_distance))
        StatPill(R.drawable.ic_calories, summary.calories, TaroTheme.colors.flame, lit, info = infoOf(R.string.info_home_kcal_title, R.string.info_home_kcal))
    }
}

@Composable
private fun selectionLabel(selection: Selection): String = when (selection) {
    is Selection.Range -> stringResource(selection.range.label)
    is Selection.Year -> selection.year.toString()
}

@Composable
private fun CenteredText(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun CompactHeader(summary: Summary, progress: Float, lit: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(TaroTheme.colors.background.copy(alpha = 0.96f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        HeroRing(
            segments = listOf(RingSegment(progress.toDouble(), TaroTheme.colors.goal)),
            target = 1.0,
            lit = lit,
            gradient = true,
            diameter = 44.dp,
            strokeWidth = 4.dp,
        ) {}
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(summary.header.uppercase(), fontSize = 11.sp, letterSpacing = 2.sp, color = TaroTheme.colors.accent)
            RollingText(
                text = Util.stepsPlural(LocalContext.current, summary.steps),
                style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            )
        }
        Text(summary.distance, fontSize = 14.sp, color = TaroTheme.colors.goal)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PauseFab(paused: Boolean, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    val container by animateColorAsState(
        if (paused) TaroTheme.colors.special else MaterialTheme.colorScheme.onSurface,
        label = "fab color",
    )
    val halo = TaroTheme.colors.special
    val breathing = if (paused && rememberAnimationsEnabled()) {
        rememberInfiniteTransition(label = "fab halo")
            .animateFloat(0f, 1f, infiniteRepeatable(tween(1800)), label = "halo")
    } else {
        null
    }
    val label = stringResource(if (paused) R.string.action_resume else R.string.action_pause)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .drawBehind {
                val t = breathing?.value ?: return@drawBehind
                val grow = 2.dp.toPx() + 10.dp.toPx() * t
                drawRoundRect(
                    color = halo.copy(alpha = 0.5f * (1f - t)),
                    topLeft = Offset(-grow, -grow),
                    size = Size(size.width + grow * 2, size.height + grow * 2),
                    cornerRadius = CornerRadius(size.height / 2 + grow),
                    style = Stroke(2.dp.toPx()),
                )
            }
            .shadow(10.dp, CircleShape, ambientColor = container, spotColor = container)
            .clip(CircleShape)
            .background(container)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = label,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .animateContentSize(TaroMotion.snappy())
            .height(56.dp)
            .padding(horizontal = 16.dp),
    ) {
        AnimatedContent(
            targetState = paused,
            transitionSpec = {
                (scaleIn(TaroMotion.bouncy(), initialScale = 0.4f) + fadeIn())
                    .togetherWith(scaleOut(targetScale = 0.4f) + fadeOut())
            },
            label = "fab icon",
        ) { isPaused ->
            Icon(
                painter = painterResource(if (isPaused) R.drawable.ic_play else R.drawable.ic_pause),
                contentDescription = label,
                tint = TaroTheme.colors.background,
            )
        }
        if (paused) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TaroTheme.colors.background,
                modifier = Modifier.padding(start = 8.dp, end = 4.dp),
            )
        }
    }
}
