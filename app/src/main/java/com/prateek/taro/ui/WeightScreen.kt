package com.prateek.taro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import com.prateek.taro.R
import com.prateek.taro.energy.DietGoal
import com.prateek.taro.energy.TrendPoint
import com.prateek.taro.energy.WeightJournal
import com.prateek.taro.energy.WeightTrend
import com.prateek.taro.ui.components.BentoNote
import com.prateek.taro.ui.components.BentoTile
import com.prateek.taro.ui.components.BentoValue
import com.prateek.taro.ui.components.CardStats
import com.prateek.taro.ui.components.ChangeOrRemove
import com.prateek.taro.ui.components.EditRow
import com.prateek.taro.ui.components.Fact
import com.prateek.taro.ui.components.IconDisc
import com.prateek.taro.ui.components.LinkText
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.Panel
import com.prateek.taro.ui.components.RowDivider
import com.prateek.taro.ui.components.SectionLabel
import com.prateek.taro.ui.components.SwitchRow
import com.prateek.taro.ui.components.TaroDialog
import com.prateek.taro.ui.components.TextTabs
import com.prateek.taro.ui.components.TimeRow
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.ui.components.dayLabel
import com.prateek.taro.ui.components.infoOf
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Util
import com.prateek.taro.util.WeightReminder
import com.prateek.taro.util.WeightReminderScheduler
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale

object WeightScreen : Screen {
    @Composable
    override fun Content() {
        WeightContent()
    }
}

private val CALENDAR_DAYS = listOf(
    Calendar.MONDAY to DayOfWeek.MONDAY,
    Calendar.TUESDAY to DayOfWeek.TUESDAY,
    Calendar.WEDNESDAY to DayOfWeek.WEDNESDAY,
    Calendar.THURSDAY to DayOfWeek.THURSDAY,
    Calendar.FRIDAY to DayOfWeek.FRIDAY,
    Calendar.SATURDAY to DayOfWeek.SATURDAY,
    Calendar.SUNDAY to DayOfWeek.SUNDAY,
)
private const val CHART_ASPECT = 1.7f
private const val SHOWN_ENTRIES = 14
private const val DAY_LETTER_ALPHA = 0.22f
private const val LETTER_SHARE = 1.5f

@Composable
private fun WeightContent() {
    val context = LocalContext.current
    val toast = LocalToast.current
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val quickLog = rememberQuickLog()
    var range by rememberSaveable { mutableStateOf(TrendRange.QUARTER) }
    var editing by remember { mutableStateOf<TrendPoint?>(null) }
    var editingReminder by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf<String?>(null) }
    var showAll by rememberSaveable { mutableStateOf(false) }

    val dataVersion = rememberDataVersion()
    val raw = rememberRefreshed(dataVersion) { WeightJournal.points(context) }
    val insights by rememberBodyInsights()
    val allPoints = insights?.model.tissueTrend(raw)
    val calorieGoal by AppPreferences.calorieGoalFlow().collectAsStateWithLifecycle(AppPreferences.calorieGoal)
    val reminder by AppPreferences.weightReminderFlow().collectAsStateWithLifecycle(AppPreferences.weightReminder)
    val today = Util.logicalToday()
    val latest = allPoints.lastOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        AppOverflowMenu(back = true, title = stringResource(R.string.weight_title), actions = quickLogActions(quickLog, today))
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.gap),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.loose, end = spacing.loose, bottom = spacing.loose),
        ) {
            BentoTile(colors.goal, label = stringResource(R.string.energy_weight_trend), tinted = true, modifier = Modifier.fillMaxWidth()) {
                if (latest == null) {
                    BentoNote(stringResource(R.string.weight_empty))
                    LinkText(stringResource(R.string.weight_log), colors.goal, onClick = { quickLog.weight = true }, icon = R.drawable.ic_add)
                    return@BentoTile
                }
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                    BentoValue(
                        "%.1f".format(Locale.getDefault(), Util.kgToDisplay(latest.trend)),
                        MaterialTheme.colorScheme.onSurface,
                        Modifier.weight(1f),
                        unit = Util.weightUnit(),
                    )
                    weekChange(allPoints)?.let {
                        Text(
                            stringResource(R.string.weight_week_chip, Util.formatWeightChange(it)),
                            style = TaroTheme.text.meterValue,
                            color = colors.goal,
                            modifier = Modifier.padding(bottom = spacing.tight),
                        )
                    }
                }
                PaceMessage(
                    assessment = WeightTrend.assess(
                        WeightTrend.weeklyRate(allPoints),
                        calorieGoal?.goal,
                        calorieGoal?.adjustment ?: 0,
                        latest.trend,
                    ),
                    goal = calorieGoal?.goal,
                    modifier = Modifier.padding(top = spacing.tight),
                )
            }

            if (latest != null) {
                Panel {
                    TextTabs(
                        options = TrendRange.entries.map { it to stringResource(it.label) },
                        selected = range,
                        onSelect = { range = it },
                        color = colors.goal,
                    )
                    WeightCourse(allPoints, insights?.model, range.days, CHART_ASPECT)
                    val shown = allPoints.filter { !it.date.isBefore(today.minusDays(range.days.toLong())) }
                    if (shown.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = spacing.gap)) {
                            val style = TaroTheme.text.factCompact
                            Fact(
                                Util.formatWeightChange(shown.last().trend - shown.first().trend),
                                stringResource(R.string.weight_range_change),
                                colors.goal,
                                Modifier.weight(1f),
                                style = style,
                            )
                            Fact(Util.formatWeight(shown.minOf { it.kg }), stringResource(R.string.weight_lowest), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f), style = style)
                            Fact(
                                Util.formatWeightChange(latest.kg - latest.trend),
                                stringResource(R.string.weight_water),
                                PROTEIN_BLUE,
                                Modifier.weight(1f),
                                info = infoOf(R.string.weight_water, R.string.info_weight_water),
                                style = style,
                            )
                            Fact(shown.size.toString(), stringResource(R.string.weight_count), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f), style = style)
                        }
                    }
                }
            }

            SectionLabel(stringResource(R.string.weight_reminder_section))
            Panel {
                EditRow(
                    icon = R.drawable.ic_notification,
                    color = colors.special,
                    label = stringResource(R.string.weight_reminder),
                    value = reminderText(reminder),
                    onClick = { editingReminder = true },
                )
            }

            if (allPoints.isNotEmpty()) {
                SectionLabel(stringResource(R.string.weight_entries))
                Panel {
                    val reversed = allPoints.asReversed()
                    val visible = if (showAll) reversed else reversed.take(SHOWN_ENTRIES)
                    visible.forEachIndexed { index, point ->
                        if (index > 0) RowDivider()
                        val key = point.date.toString()
                        WeighInRow(
                            point = point,
                            previous = reversed.getOrNull(index + 1),
                            goal = calorieGoal?.goal,
                            expanded = open == key,
                            onClick = { open = if (open == key) null else key },
                            onChange = { editing = point },
                            onRemove = {
                                WeightJournal.delete(context, key)
                                open = null
                                toast.show(context.getString(R.string.weight_deleted), ToastKind.INFO)
                            },
                        )
                    }
                    if (!showAll && reversed.size > SHOWN_ENTRIES) {
                        LinkText(
                            stringResource(R.string.weight_show_all, reversed.size),
                            colors.goal,
                            onClick = { showAll = true },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }
        }
    }

    QuickLogDialogs(quickLog, today)
    editing?.let { point ->
        LogWeightDialog(
            title = stringResource(R.string.weight_dialog_edit_title, dayLabel(point.date, today)),
            initialKg = point.kg,
            initialDate = point.date,
            onSave = { kg, date ->
                WeightJournal.move(context, point.date.toString(), date.toString(), kg)
                editing = null
                toast.show(context.getString(R.string.weight_saved), ToastKind.SUCCESS)
            },
            onDismiss = { editing = null },
        )
    }
    if (editingReminder) {
        ReminderDialog(
            initial = reminder,
            onSave = {
                AppPreferences.weightReminder = it
                WeightReminderScheduler.schedule(context)
                editingReminder = false
            },
            onDismiss = { editingReminder = false },
        )
    }
}

@Composable
private fun WeighInRow(
    point: TrendPoint,
    previous: TrendPoint?,
    goal: DietGoal?,
    expanded: Boolean,
    onClick: () -> Unit,
    onChange: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val change = previous?.let { point.kg - it.kg }
    val toward = when {
        change == null || goal == null || goal == DietGoal.MAINTAIN -> false
        goal == DietGoal.CUT -> change < 0
        else -> change > 0
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(spacing.gap))
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.tight, vertical = spacing.gap),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.gap)) {
            IconDisc(R.drawable.ic_weight, colors.goal)
            Text(dayLabel(point.date, Util.logicalToday()), style = type.title, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            change?.let { Text(Util.formatWeightChange(it), style = type.note, color = if (toward) colors.goal else colors.accent) }
            Text(Util.formatWeight(point.kg), style = type.meterValue, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
        }
        if (expanded) {
            CardStats(
                listOf(
                    Triple(MaterialTheme.colorScheme.onSurface, Util.formatWeight(point.kg), stringResource(R.string.weight_scale)),
                    Triple(colors.goal, Util.formatWeight(point.trend), stringResource(R.string.energy_weight_trend).lowercase()),
                    Triple(PROTEIN_BLUE, Util.formatWeightChange(point.kg - point.trend), stringResource(R.string.weight_water).lowercase()),
                )
            )
        }
        ChangeOrRemove(expanded, onChange, onRemove)
    }
}

@Composable
private fun reminderText(reminder: WeightReminder): String {
    if (!reminder.enabled) return stringResource(R.string.weight_reminder_off)
    val context = LocalContext.current
    val time = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, reminder.minuteOfDay / 60)
        set(Calendar.MINUTE, reminder.minuteOfDay % 60)
    }
    val clock = android.text.format.DateFormat.getTimeFormat(context).format(Date(time.timeInMillis))
    if (!reminder.weekly) return stringResource(R.string.weight_reminder_daily_at, clock)
    val day = CALENDAR_DAYS.firstOrNull { it.first == reminder.dayOfWeek }?.second?.getDisplayName(TextStyle.FULL, Locale.getDefault()).orEmpty()
    return stringResource(R.string.weight_reminder_weekly_at, day, clock)
}

@Composable
private fun ReminderDialog(initial: WeightReminder, onSave: (WeightReminder) -> Unit, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf(initial) }
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    TaroDialog(
        title = stringResource(R.string.weight_reminder),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        onConfirm = { onSave(draft) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.gap)) {
            SwitchRow(text = stringResource(R.string.weight_remind_me), checked = draft.enabled, onCheckedChange = { draft = draft.copy(enabled = it) })
            if (draft.enabled) {
                TextTabs(
                    options = listOf(false to stringResource(R.string.weight_reminder_daily), true to stringResource(R.string.weight_reminder_weekly)),
                    selected = draft.weekly,
                    onSelect = { draft = draft.copy(weekly = it) },
                    color = colors.goal,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                if (draft.weekly) {
                    val letter = with(LocalDensity.current) { type.title.lineHeight.toDp() } * LETTER_SHARE
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        CALENDAR_DAYS.forEach { (calendarDay, day) ->
                            val on = draft.dayOfWeek == calendarDay
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(letter)
                                    .clip(CircleShape)
                                    .background(if (on) colors.goal.copy(alpha = DAY_LETTER_ALPHA) else Color.Transparent)
                                    .clickable { draft = draft.copy(dayOfWeek = calendarDay) },
                            ) {
                                Text(
                                    day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                    style = type.title,
                                    color = if (on) MaterialTheme.colorScheme.onSurface else colors.accent,
                                )
                            }
                        }
                    }
                }
                TimeRow(label = stringResource(R.string.weight_reminder_time), minuteOfDay = draft.minuteOfDay, onChange = { draft = draft.copy(minuteOfDay = it) })
                Text(stringResource(R.string.weight_reminder_hint), style = type.note, color = colors.accent)
            }
        }
    }
}
