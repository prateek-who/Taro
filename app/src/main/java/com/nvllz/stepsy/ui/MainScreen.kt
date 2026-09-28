package com.nvllz.stepsy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.nvllz.stepsy.R
import com.nvllz.stepsy.ui.components.MessageDialog
import com.nvllz.stepsy.ui.components.MonthCalendar
import com.nvllz.stepsy.ui.components.NumberInputDialog
import com.nvllz.stepsy.ui.components.PauseDialogs
import com.nvllz.stepsy.ui.components.RangeChip
import com.nvllz.stepsy.ui.components.StepsBarChart
import com.nvllz.stepsy.ui.theme.StepsyTheme
import com.nvllz.stepsy.util.AppPreferences
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

@Stable
class TrackingState(steps: Int, paused: Boolean, showOnboarding: Boolean) {
    var steps by mutableIntStateOf(steps)
    var paused by mutableStateOf(paused)
    var refreshKey by mutableIntStateOf(0)
    var showOnboarding by mutableStateOf(showOnboarding)
}

class MainActions(
    val onTogglePause: () -> Unit,
    val onPauseFor: (durationMinutes: Int, specificEndTime: Long) -> Unit,
    val onPauseIndefinitely: () -> Unit,
    val onUpdateSteps: (Int) -> Unit,
    val onOnboardingDone: () -> Unit,
)

val LocalTracking = staticCompositionLocalOf<TrackingState> { error("TrackingState not provided") }
val LocalMainActions = staticCompositionLocalOf<MainActions> { error("MainActions not provided") }

object HomeScreen : Screen {
    @Composable
    override fun Content() {
        val tracking = LocalTracking.current
        val navigator = LocalNavigator.currentOrThrow
        MainScreen(tracking, LocalMainActions.current, onOpen = { navigator.push(it) })
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
private fun MainScreen(tracking: TrackingState, actions: MainActions, onOpen: (Screen) -> Unit) {
    val steps = tracking.steps
    val paused = tracking.paused
    val refreshKey = tracking.refreshKey
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var selection by remember { mutableStateOf(loadSelection(context)) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var past7Days by remember { mutableStateOf(loadPast7DaysMode(context)) }
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var calendarJump by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<MainDialog?>(null) }

    val goalTarget by AppPreferences.dailyGoalTargetFlow().collectAsStateWithLifecycle(AppPreferences.dailyGoalTarget)
    val goalChartLine by AppPreferences.dailyGoalChartLineFlow().collectAsStateWithLifecycle(AppPreferences.dailyGoalChartLine)

    val years = remember(refreshKey) { yearsWithData(context) }
    val minDate = remember(refreshKey) { firstEntryDate(context) }
    val summary = remember(selection, steps, refreshKey) { summary(context, selection, steps) }
    val chart = remember(past7Days, selectedDate, steps, refreshKey) { chartData(context, past7Days, selectedDate, steps) }
    val day = remember(selectedDate, steps, refreshKey) { dayStats(context, selectedDate) }
    val goal = remember(goalTarget, refreshKey) { goalLine(context, goalTarget) }
    val isToday = selection == Selection.Range(StepRange.TODAY)

    fun showMessage(textRes: Int) {
        scope.launch { snackbar.showSnackbar(context.getString(textRes)) }
    }

    fun resetCalendarToToday() {
        selectedDate = LocalDate.now()
        calendarJump++
    }

    fun select(newSelection: Selection) {
        selection = newSelection
        saveSelection(context, newSelection)
        if (newSelection == Selection.Range(StepRange.SEVEN_DAYS)) {
            resetCalendarToToday()
            past7Days = true
        }
    }

    Scaffold(
        containerColor = StepsyTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar, modifier = Modifier.navigationBarsPadding()) },
        floatingActionButton = {
            PauseFab(
                paused = paused,
                onClick = actions.onTogglePause,
                onLongClick = { if (!paused) dialog = MainDialog.Pause },
                modifier = Modifier.navigationBarsPadding(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding(),
        ) {
            OverflowMenu(onOpen)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                SummarySection(
                    summary = summary,
                    onStepsLongClick = { if (isToday) dialog = MainDialog.EditSteps },
                )
                RangePicker(
                    expanded = expanded,
                    onToggle = { expanded = !expanded },
                    selection = selection,
                    years = years,
                    onSelect = ::select,
                )
                goal?.let {
                    Text(
                        text = it.text.uppercase(),
                        fontSize = 16.sp,
                        fontWeight = if (it.streak) FontWeight.Bold else FontWeight.Normal,
                        color = if (it.streak) StepsyTheme.colors.special else StepsyTheme.colors.accent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    )
                }
            }

            HorizontalDivider(
                color = StepsyTheme.colors.accent.copy(alpha = 0.3f),
                modifier = Modifier.padding(horizontal = 30.dp, vertical = 15.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(12.dp),
            ) {
                CenteredText(
                    text = chart.header,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable {
                        past7Days = !past7Days
                        savePast7DaysMode(context, past7Days)
                        resetCalendarToToday()
                    },
                )
                CenteredText(chart.range, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
                StepsBarChart(
                    values = chart.values,
                    labels = chart.labels,
                    goal = if (goalTarget > 0 && goalChartLine) goalTarget else 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }

            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 52.dp)) {
                MonthCalendar(
                    selected = selectedDate,
                    minDate = minDate,
                    maxDate = LocalDate.now(),
                    firstDayOfWeek = calendarDayOfWeek[AppPreferences.firstDayOfWeek - 1],
                    jumpKey = calendarJump,
                    onSelect = {
                        selectedDate = it
                        past7Days = false
                    },
                )
                DaySection(day)
            }

            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }

    when (val current = dialog) {
        MainDialog.EditSteps -> NumberInputDialog(
            title = stringResource(R.string.edit_step_count),
            initial = steps.toString(),
            hint = "",
            onConfirm = { input ->
                dialog = null
                val newSteps = input.toIntOrNull()
                when {
                    newSteps == null -> showMessage(R.string.invalid_step_count)
                    newSteps < steps -> dialog = MainDialog.ConfirmDecrease(newSteps)
                    else -> {
                        actions.onUpdateSteps(newSteps)
                        showMessage(R.string.steps_updated)
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
                showMessage(R.string.steps_updated)
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
        OnboardingDialog(onDone = actions.onOnboardingDone)
    }
}

@Composable
private fun OverflowMenu(onOpen: (Screen) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val items = listOf(
        Triple(R.string.achievements_title, R.drawable.ic_small_trophy, AchievementsScreen),
        Triple(R.string.header_data_backup, R.drawable.ic_small_backup, BackupScreen),
        Triple(R.string.daily_goals, R.drawable.ic_small_target, DailyGoalsScreen),
        Triple(R.string.settings, R.drawable.ic_small_settings, SettingsScreen),
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
        Box {
            IconButton(onClick = { open = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = stringResource(androidx.appcompat.R.string.abc_action_menu_overflow_description),
                )
            }
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                items.forEach { (label, icon, screen) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(label)) },
                        leadingIcon = {
                            Icon(painterResource(icon), contentDescription = null)
                        },
                        onClick = {
                            open = false
                            onOpen(screen)
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SummarySection(summary: Summary, onStepsLongClick: () -> Unit) {
    CenteredText(
        text = summary.header.uppercase(),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp),
    )
    Text(
        text = summary.steps,
        fontSize = 38.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(bottom = 4.dp)
            .combinedClickable(onClick = {}, onLongClick = onStepsLongClick),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(bottom = 14.dp),
    ) {
        IconStat(R.drawable.ic_steps, summary.distance)
        summary.calories?.let { IconStat(R.drawable.ic_calories, it) }
    }
    summary.average?.let {
        Text(stringResource(R.string.avg_distance), fontSize = 20.sp, color = StepsyTheme.colors.accent)
        Text(it, fontSize = 18.sp, color = StepsyTheme.colors.accent)
    }
}

@Composable
private fun IconStat(icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = StepsyTheme.colors.accent,
            modifier = Modifier
                .padding(end = 6.dp)
                .size(24.dp),
        )
        Text(text, fontSize = 18.sp, color = StepsyTheme.colors.accent)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.RangePicker(
    expanded: Boolean,
    onToggle: () -> Unit,
    selection: Selection,
    years: List<Int>,
    onSelect: (Selection) -> Unit,
) {
    IconButton(onClick = onToggle, modifier = Modifier.align(Alignment.CenterHorizontally)) {
        Icon(
            painter = painterResource(if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more),
            contentDescription = stringResource(R.string.expand_collapse),
            tint = StepsyTheme.colors.accent,
        )
    }
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(tween(300)) + fadeIn(tween(300)),
        exit = shrinkVertically(tween(300)) + fadeOut(tween(300)),
    ) {
        Column {
            FlowRow(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
            ) {
                StepRange.entries.forEach { range ->
                    RangeChip(
                        label = stringResource(range.label),
                        selected = selection == Selection.Range(range),
                        onClick = { onSelect(Selection.Range(range)) },
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                years.forEach { year ->
                    RangeChip(
                        label = year.toString(),
                        selected = selection == Selection.Year(year),
                        onClick = { onSelect(Selection.Year(year)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DaySection(day: DayStats) {
    CenteredText(day.header, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 5.dp))
    CenteredText(day.details, fontSize = 19.sp, modifier = Modifier.padding(bottom = 20.dp))
    CenteredText(stringResource(R.string.total_distance), fontSize = 20.sp, fontWeight = FontWeight.Bold)
    CenteredText(day.monthTotal, fontSize = 18.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(bottom = 10.dp))
    CenteredText(stringResource(R.string.avg_distance), fontSize = 20.sp, fontWeight = FontWeight.Bold)
    CenteredText(day.monthAverage, fontSize = 18.sp, fontWeight = FontWeight.Light)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PauseFab(paused: Boolean, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(56.dp)
            .shadow(6.dp, shape)
            .clip(shape)
            .background(if (paused) StepsyTheme.colors.accent else MaterialTheme.colorScheme.primary)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = stringResource(if (paused) R.string.action_resume else R.string.action_pause),
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Icon(
            painter = painterResource(if (paused) R.drawable.ic_play else R.drawable.ic_pause),
            contentDescription = stringResource(if (paused) R.string.action_resume else R.string.action_pause),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
