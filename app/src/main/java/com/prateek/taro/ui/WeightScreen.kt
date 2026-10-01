package com.prateek.taro.ui

import androidx.compose.material3.MaterialTheme
import com.prateek.taro.ui.components.TintChip
import com.prateek.taro.ui.components.StatTile
import com.prateek.taro.ui.components.SectionLabel
import com.prateek.taro.ui.components.RollingText
import com.prateek.taro.ui.components.PickerRow
import com.prateek.taro.ui.components.Panel
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
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
import com.prateek.taro.R
import com.prateek.taro.energy.TrendPoint
import com.prateek.taro.energy.WeightJournal
import com.prateek.taro.energy.WeightTrend
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.PrimaryButton
import com.prateek.taro.ui.components.RangeChip
import com.prateek.taro.ui.components.ScrollingColumn
import com.prateek.taro.ui.components.SettingsCard
import com.prateek.taro.ui.components.SettingsDivider
import com.prateek.taro.ui.components.StatRow
import com.prateek.taro.ui.components.TaroScaffold
import com.prateek.taro.ui.components.SwitchRow
import com.prateek.taro.ui.components.TimePickerDialog
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.ui.components.ToggleGroup
import com.prateek.taro.ui.components.WeightChart
import com.prateek.taro.ui.components.WeightSeriesToggles
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Util
import com.prateek.taro.util.WeightReminder
import com.prateek.taro.util.WeightReminderScheduler
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

    val dataVersion = rememberDataVersion()
    var showWeighIns by rememberSaveable { mutableStateOf(true) }
    var showTrend by rememberSaveable { mutableStateOf(true) }
    val allPoints = remember(version, dataVersion) { WeightJournal.points(context) }
    val today = Util.logicalToday()
    val insights by rememberBodyInsights(version)
    val points = insights?.model.tissueTrend(allPoints).filter { !it.date.isBefore(today.minusDays(rangeDays.toLong())) }
    val calorieGoal by AppPreferences.calorieGoalFlow().collectAsStateWithLifecycle(AppPreferences.calorieGoal)
    val reminder by AppPreferences.weightReminderFlow().collectAsStateWithLifecycle(AppPreferences.weightReminder)
    val dateFormat = remember { SimpleDateFormat(AppPreferences.dateFormatString, Locale.getDefault()) }

    fun updateReminder(updated: WeightReminder) {
        AppPreferences.weightReminder = updated
        WeightReminderScheduler.schedule(context)
    }

    fun formatDate(point: TrendPoint) = dateFormat.format(Date(Util.dateStringToCalendarMillis(point.date.toString())))

    TaroScaffold(title = stringResource(R.string.weight_title), onBack = onBack) { padding ->
        ScrollingColumn(padding, modifier = Modifier.padding(horizontal = 16.dp)) {
            val colors = TaroTheme.colors
            val latest = allPoints.lastOrNull()

            Panel(modifier = Modifier.padding(top = 8.dp)) {
                if (latest == null) {
                    Text(
                        text = stringResource(R.string.weight_empty),
                        color = colors.accent,
                        modifier = Modifier.padding(12.dp),
                    )
                } else {
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
                                style = MaterialTheme.typography.displaySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                            )
                            Text(
                                text = stringResource(R.string.weight_latest, Util.formatWeight(latest.kg), formatDate(latest)),
                                fontSize = 13.sp,
                                color = colors.accent,
                            )
                        }
                        weekChange(allPoints)?.let {
                            TintChip(stringResource(R.string.weight_week_chip, Util.formatWeightChange(it)), colors.accent)
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
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                    )
                }
            }

            ToggleGroup(
                options = listOf(
                    30 to stringResource(R.string.weight_range_month),
                    90 to stringResource(R.string.weight_range_quarter),
                    365 to stringResource(R.string.weight_range_year),
                ),
                selected = rangeDays,
                onSelect = { rangeDays = it },
                modifier = Modifier.padding(top = 16.dp, bottom = 10.dp),
            )

            if (points.isNotEmpty()) {
                Panel {
                    WeightChart(
                        points = points,
                        toDisplay = Util::kgToDisplay,
                        showWeighIns = showWeighIns,
                        showTrend = showTrend,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .padding(4.dp),
                    )
                    WeightSeriesToggles(
                        showWeighIns = showWeighIns,
                        showTrend = showTrend,
                        onChange = { weighIns, trend ->
                            showWeighIns = weighIns
                            showTrend = trend
                        },
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                ) {
                    StatTile(
                        label = stringResource(R.string.weight_range_change),
                        value = Util.formatWeightChange(points.last().trend - points.first().trend),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = stringResource(R.string.weight_lowest),
                        value = Util.formatWeight(points.minOf { it.kg }),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = stringResource(R.string.weight_count),
                        value = points.size.toString(),
                        modifier = Modifier.weight(0.8f),
                    )
                }
            }

            PrimaryButton(
                text = stringResource(R.string.weight_log),
                onClick = { dialog = WeightDialog.Log },
                icon = R.drawable.ic_weight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )

            SectionLabel(stringResource(R.string.weight_reminder))
            Panel {
                SwitchRow(
                    text = stringResource(R.string.weight_reminder),
                    checked = reminder.enabled,
                    onCheckedChange = { updateReminder(reminder.copy(enabled = it)) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                )
                AnimatedVisibility(visible = reminder.enabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                        ToggleGroup(
                            options = listOf(
                                false to stringResource(R.string.weight_reminder_daily),
                                true to stringResource(R.string.weight_reminder_weekly),
                            ),
                            selected = reminder.weekly,
                            onSelect = { updateReminder(reminder.copy(weekly = it)) },
                        )
                        if (reminder.weekly) {
                            FlowRow {
                                CALENDAR_DAYS.forEach { (calendarDay, day) ->
                                    RangeChip(
                                        label = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                        selected = reminder.dayOfWeek == calendarDay,
                                        onClick = { updateReminder(reminder.copy(dayOfWeek = calendarDay)) },
                                    )
                                }
                            }
                        }
                        val time = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, reminder.minuteOfDay / 60)
                            set(Calendar.MINUTE, reminder.minuteOfDay % 60)
                        }
                        PickerRow(
                            icon = R.drawable.ic_day_start,
                            label = stringResource(R.string.weight_reminder_time),
                            value = android.text.format.DateFormat.getTimeFormat(context).format(time.time),
                            onClick = { dialog = WeightDialog.ReminderTime },
                        )
                    }
                }
            }

            if (allPoints.isNotEmpty()) {
                SectionLabel(stringResource(R.string.weight_entries))
                Panel(modifier = Modifier.padding(bottom = 24.dp)) {
                    val reversed = allPoints.asReversed()
                    reversed.forEachIndexed { index, point ->
                        val previous = reversed.getOrNull(index + 1)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.accentOpaque)
                                .clickable { dialog = WeightDialog.Edit(point) }
                                .padding(start = 14.dp, top = 6.dp, bottom = 6.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(formatDate(point), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                previous?.let {
                                    Text(
                                        text = Util.formatWeightChange(point.kg - it.kg),
                                        fontSize = 12.sp,
                                        color = colors.accent,
                                    )
                                }
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
                                    tint = colors.accent,
                                    modifier = Modifier.size(20.dp),
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
