package com.nvllz.stepsy.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.nvllz.stepsy.R
import com.nvllz.stepsy.energy.TrendPoint
import com.nvllz.stepsy.energy.WeightJournal
import com.nvllz.stepsy.energy.WeightTrend
import com.nvllz.stepsy.ui.components.LocalToast
import com.nvllz.stepsy.ui.components.PrimaryButton
import com.nvllz.stepsy.ui.components.RangeChip
import com.nvllz.stepsy.ui.components.ScrollingColumn
import com.nvllz.stepsy.ui.components.SettingsCard
import com.nvllz.stepsy.ui.components.SettingsDivider
import com.nvllz.stepsy.ui.components.StatRow
import com.nvllz.stepsy.ui.components.StepsyScaffold
import com.nvllz.stepsy.ui.components.SwitchRow
import com.nvllz.stepsy.ui.components.TimePickerDialog
import com.nvllz.stepsy.ui.components.ToastKind
import com.nvllz.stepsy.ui.components.ToggleGroup
import com.nvllz.stepsy.ui.components.WeightChart
import com.nvllz.stepsy.ui.theme.StepsyTheme
import com.nvllz.stepsy.util.AppPreferences
import com.nvllz.stepsy.util.Util
import com.nvllz.stepsy.util.WeightReminder
import com.nvllz.stepsy.util.WeightReminderScheduler
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale

object WeightScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        WeightContent(onBack = { navigator.pop() })
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

private sealed interface WeightDialog {
    data object Log : WeightDialog
    data class Edit(val point: TrendPoint) : WeightDialog
    data object ReminderTime : WeightDialog
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeightContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val toast = LocalToast.current
    var version by remember { mutableIntStateOf(0) }
    var rangeDays by rememberSaveable { mutableIntStateOf(90) }
    var dialog by remember { mutableStateOf<WeightDialog?>(null) }

    val allPoints = remember(version) { WeightJournal.points(context) }
    val today = Util.logicalToday()
    val points = allPoints.filter { !it.date.isBefore(today.minusDays(rangeDays.toLong())) }
    val calorieGoal by AppPreferences.calorieGoalFlow().collectAsStateWithLifecycle(AppPreferences.calorieGoal)
    val reminder by AppPreferences.weightReminderFlow().collectAsStateWithLifecycle(AppPreferences.weightReminder)
    val dateFormat = remember { SimpleDateFormat(AppPreferences.dateFormatString, Locale.getDefault()) }

    fun updateReminder(updated: WeightReminder) {
        AppPreferences.weightReminder = updated
        WeightReminderScheduler.schedule(context)
    }

    fun formatDate(point: TrendPoint) = dateFormat.format(Date(Util.dateStringToCalendarMillis(point.date.toString())))

    StepsyScaffold(title = stringResource(R.string.weight_title), onBack = onBack) { padding ->
        ScrollingColumn(padding, modifier = Modifier.padding(horizontal = 16.dp)) {
            ToggleGroup(
                options = listOf(
                    30 to stringResource(R.string.weight_range_month),
                    90 to stringResource(R.string.weight_range_quarter),
                    365 to stringResource(R.string.weight_range_year),
                ),
                selected = rangeDays,
                onSelect = { rangeDays = it },
                modifier = Modifier.padding(vertical = 12.dp),
            )

            SettingsCard {
                if (points.isEmpty()) {
                    Text(
                        text = stringResource(R.string.weight_empty),
                        modifier = Modifier
                            .padding(20.dp)
                            .alpha(0.7f),
                    )
                } else {
                    WeightChart(
                        points = points,
                        toDisplay = Util::kgToDisplay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .padding(16.dp),
                    )
                    val latest = allPoints.last()
                    StatRow(stringResource(R.string.weight_trend), Util.formatWeight(latest.trend))
                    weekChange(allPoints)?.let {
                        SettingsDivider()
                        StatRow(stringResource(R.string.weight_change_week), Util.formatWeightChange(it))
                    }
                    SettingsDivider()
                    PaceMessage(
                        assessment = WeightTrend.assess(
                            WeightTrend.weeklyRate(allPoints),
                            calorieGoal?.goal,
                            calorieGoal?.adjustment ?: 0,
                            latest.trend,
                        ),
                        goal = calorieGoal?.goal,
                        modifier = Modifier.padding(20.dp),
                    )
                }
                PrimaryButton(
                    text = stringResource(R.string.weight_log),
                    onClick = { dialog = WeightDialog.Log },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                )
            }

            SettingsCard {
                SwitchRow(
                    text = stringResource(R.string.weight_reminder),
                    checked = reminder.enabled,
                    onCheckedChange = { updateReminder(reminder.copy(enabled = it)) },
                    contentPadding = PaddingValues(20.dp),
                )
                if (reminder.enabled) {
                    SettingsDivider()
                    ToggleGroup(
                        options = listOf(
                            false to stringResource(R.string.weight_reminder_daily),
                            true to stringResource(R.string.weight_reminder_weekly),
                        ),
                        selected = reminder.weekly,
                        onSelect = { updateReminder(reminder.copy(weekly = it)) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                    if (reminder.weekly) {
                        FlowRow(modifier = Modifier.padding(horizontal = 16.dp)) {
                            CALENDAR_DAYS.forEach { (calendarDay, day) ->
                                RangeChip(
                                    label = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                    selected = reminder.dayOfWeek == calendarDay,
                                    onClick = { updateReminder(reminder.copy(dayOfWeek = calendarDay)) },
                                )
                            }
                        }
                    }
                    SettingsDivider()
                    val time = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, reminder.minuteOfDay / 60)
                        set(Calendar.MINUTE, reminder.minuteOfDay % 60)
                    }
                    StatRow(
                        label = stringResource(R.string.weight_reminder_time),
                        value = android.text.format.DateFormat.getTimeFormat(context).format(time.time),
                        onClick = { dialog = WeightDialog.ReminderTime },
                    )
                }
            }

            if (allPoints.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.weight_entries).uppercase(),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp),
                )
                SettingsCard {
                    allPoints.asReversed().forEachIndexed { index, point ->
                        if (index > 0) SettingsDivider()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dialog = WeightDialog.Edit(point) }
                                .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(formatDate(point), fontSize = 16.sp)
                                Text(
                                    text = "${stringResource(R.string.weight_trend)} ${Util.formatWeight(point.trend)}",
                                    fontSize = 13.sp,
                                    modifier = Modifier.alpha(0.6f),
                                )
                            }
                            Text(Util.formatWeight(point.kg), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = {
                                WeightJournal.delete(context, point.date.toString())
                                version++
                                toast.show(context.getString(R.string.weight_deleted), ToastKind.INFO)
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete),
                                    contentDescription = stringResource(R.string.weight_delete, formatDate(point)),
                                    tint = StepsyTheme.colors.accent,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    when (val current = dialog) {
        WeightDialog.Log -> LogWeightDialog(
            title = stringResource(R.string.weight_dialog_title),
            initialKg = allPoints.lastOrNull()?.kg ?: AppPreferences.weight,
            onSave = { kg, date ->
                WeightJournal.log(context, date.toString(), kg)
                WeightReminderScheduler.dismissNotification(context)
                dialog = null
                version++
                toast.show(context.getString(R.string.weight_saved), ToastKind.SUCCESS)
            },
            onDismiss = { dialog = null },
        )
        is WeightDialog.Edit -> LogWeightDialog(
            title = stringResource(R.string.weight_dialog_edit_title, formatDate(current.point)),
            initialKg = current.point.kg,
            initialDate = current.point.date,
            onSave = { kg, date ->
                WeightJournal.move(context, current.point.date.toString(), date.toString(), kg)
                dialog = null
                version++
                toast.show(context.getString(R.string.weight_saved), ToastKind.SUCCESS)
            },
            onDismiss = { dialog = null },
        )
        WeightDialog.ReminderTime -> TimePickerDialog(
            title = stringResource(R.string.weight_reminder_time),
            initialHour = reminder.minuteOfDay / 60,
            initialMinute = reminder.minuteOfDay % 60,
            is24Hour = android.text.format.DateFormat.is24HourFormat(context),
            onConfirm = { hour, minute ->
                dialog = null
                updateReminder(reminder.copy(minuteOfDay = hour * 60 + minute))
            },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}
