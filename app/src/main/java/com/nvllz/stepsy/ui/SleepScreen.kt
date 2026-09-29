package com.nvllz.stepsy.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.nvllz.stepsy.R
import com.nvllz.stepsy.data.SleepSession
import com.nvllz.stepsy.sleep.Night
import com.nvllz.stepsy.sleep.SleepInsights
import com.nvllz.stepsy.sleep.SleepRepository
import com.nvllz.stepsy.sleep.SleepStatus
import com.nvllz.stepsy.ui.components.LocalToast
import com.nvllz.stepsy.ui.components.PrimaryButton
import com.nvllz.stepsy.ui.components.SecondaryButton
import com.nvllz.stepsy.ui.components.SettingsCard
import com.nvllz.stepsy.ui.components.SettingsDivider
import com.nvllz.stepsy.ui.components.StatRow
import com.nvllz.stepsy.ui.components.StepsBarChart
import com.nvllz.stepsy.ui.components.StepsyDialog
import com.nvllz.stepsy.ui.components.TimePickerDialog
import com.nvllz.stepsy.ui.components.ToastKind
import com.nvllz.stepsy.ui.theme.StepsyTheme
import com.nvllz.stepsy.util.AppPreferences
import com.nvllz.stepsy.util.Util
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale

object SleepTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 2u,
            title = stringResource(R.string.tab_sleep),
            icon = painterResource(R.drawable.ic_sleep),
        )

    @Composable
    override fun Content() {
        SleepContent(LocalTracking.current)
    }
}

private fun SleepSession.toNight() = Night(LocalDate.parse(wakeDate), startAt, endAt)

@Composable
private fun durationLabel(minutes: Long) = stringResource(R.string.sleep_duration, (minutes / 60).toInt(), (minutes % 60).toInt())

private fun millisAt(date: LocalDate, minuteOfDay: Int): Long = Calendar.getInstance().apply {
    clear()
    set(date.year, date.monthValue - 1, date.dayOfMonth, minuteOfDay / 60, minuteOfDay % 60, 0)
}.timeInMillis

private fun minuteOfDay(millis: Long): Int = Calendar.getInstance().apply { timeInMillis = millis }.let {
    it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
}

private sealed interface SleepDialog {
    data class Edit(val wakeDate: LocalDate, val session: SleepSession?) : SleepDialog
}

@Composable
private fun SleepContent(tracking: TrackingState) {
    val context = LocalContext.current
    val toast = LocalToast.current
    var version by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<SleepDialog?>(null) }

    val today = Util.logicalToday()
    val sessions = remember(version, tracking.refreshKey) { SleepRepository.sessions(context, 14) }
    val lastNight = sessions.firstOrNull { it.wakeDate == today.toString() }
    val week = sessions.filter { !LocalDate.parse(it.wakeDate).isBefore(today.minusDays(6)) }.map { it.toNight() }
    val timeFormat = remember { android.text.format.DateFormat.getTimeFormat(context) }
    val dateFormat = remember { SimpleDateFormat(AppPreferences.dateFormatString, Locale.getDefault()) }

    fun time(millis: Long) = timeFormat.format(Date(millis))

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
            Text(
                text = stringResource(R.string.sleep_header).uppercase(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
            )

            if (lastNight != null && !lastNight.confirmed) {
                SettingsCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.sleep_confirm_title, time(lastNight.startAt), time(lastNight.endAt)),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(R.string.sleep_confirm_message, durationLabel(lastNight.durationMinutes)),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .alpha(0.8f),
                        )
                        PrimaryButton(
                            text = stringResource(R.string.sleep_confirm),
                            onClick = {
                                SleepRepository.confirm(context, lastNight)
                                version++
                                toast.show(context.getString(R.string.sleep_confirmed), ToastKind.SUCCESS)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        SecondaryButton(
                            text = stringResource(R.string.sleep_edit),
                            onClick = { dialog = SleepDialog.Edit(today, lastNight) },
                        )
                    }
                }
            }

            if (lastNight == null) {
                SettingsCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.sleep_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.alpha(0.8f),
                        )
                        PrimaryButton(
                            text = stringResource(R.string.sleep_add),
                            onClick = { dialog = SleepDialog.Edit(today, null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                        )
                    }
                }
            } else {
                Text(stringResource(R.string.sleep_last_night), fontSize = 14.sp, color = StepsyTheme.colors.accent)
                Text(durationLabel(lastNight.durationMinutes), fontSize = 44.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = stringResource(R.string.sleep_range, time(lastNight.startAt), time(lastNight.endAt)),
                    fontSize = 16.sp,
                    modifier = Modifier.alpha(0.8f),
                )
            }

            SleepStatusMessage(week, modifier = Modifier.padding(vertical = 16.dp))

            SettingsCard {
                StatRow(
                    stringResource(R.string.sleep_average),
                    SleepInsights.average(week)?.let { durationLabel(it) } ?: "-",
                )
                SettingsDivider()
                StatRow(
                    stringResource(R.string.sleep_consistency),
                    SleepInsights.bedtimeSpreadMinutes(week)?.let { stringResource(R.string.sleep_consistency_value, it.toInt()) } ?: "-",
                )
                SettingsDivider()
                StatRow(stringResource(R.string.sleep_streak), SleepInsights.streak(week, today).toString())
            }

            Text(
                text = stringResource(R.string.sleep_last_7).uppercase(),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
            )
            Text(
                text = stringResource(R.string.sleep_last_7_caption),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .alpha(0.6f),
            )
            val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
            val byDate = week.associateBy { it.wakeDate }
            StepsBarChart(
                values = days.map { byDate[it]?.minutes?.toInt() ?: 0 },
                labels = days.map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) },
                goal = SleepInsights.HEALTHY_MIN.toInt(),
                valueLabel = { "${it / 60}h${"%02d".format(it % 60)}" },
                showMultiplier = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            )

            if (sessions.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.sleep_nights).uppercase(),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                )
                SettingsCard {
                    sessions.asReversed().forEachIndexed { index, session ->
                        if (index > 0) SettingsDivider()
                        val wake = LocalDate.parse(session.wakeDate)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dialog = SleepDialog.Edit(wake, session) }
                                .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dateFormat.format(Date(Util.dateStringToCalendarMillis(session.wakeDate))), fontSize = 16.sp)
                                val source = stringResource(
                                    when (session.source) {
                                        SleepRepository.SOURCE_GOOGLE -> R.string.sleep_source_google
                                        SleepRepository.SOURCE_PHONE -> R.string.sleep_source_phone
                                        else -> R.string.sleep_source_manual
                                    }
                                )
                                val suffix = if (session.confirmed) "" else ", ${stringResource(R.string.sleep_unconfirmed)}"
                                Text(
                                    text = "${stringResource(R.string.sleep_range, time(session.startAt), time(session.endAt))} · $source$suffix",
                                    fontSize = 13.sp,
                                    modifier = Modifier.alpha(0.6f),
                                )
                            }
                            Text(durationLabel(session.durationMinutes), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = {
                                SleepRepository.delete(context, session)
                                version++
                                toast.show(context.getString(R.string.sleep_deleted), ToastKind.INFO)
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete),
                                    contentDescription = stringResource(R.string.sleep_delete, session.wakeDate),
                                    tint = StepsyTheme.colors.accent,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    (dialog as? SleepDialog.Edit)?.let { edit ->
        SleepEditDialog(
            wakeDate = edit.wakeDate,
            session = edit.session,
            onSave = { start, end ->
                SleepRepository.saveManual(context, start, end)
                dialog = null
                version++
                toast.show(context.getString(R.string.sleep_saved), ToastKind.SUCCESS)
            },
            onDismiss = { dialog = null },
        )
    }
}

@Composable
private fun SleepStatusMessage(week: List<Night>, modifier: Modifier = Modifier) {
    val average = SleepInsights.average(week) ?: return
    val status = SleepInsights.status(average)
    val label = durationLabel(average)
    val (text, color) = when (status) {
        SleepStatus.SHORT -> stringResource(R.string.sleep_status_short, label) to MaterialTheme.colorScheme.error
        SleepStatus.A_BIT_SHORT -> stringResource(R.string.sleep_status_a_bit_short, label) to StepsyTheme.colors.special
        SleepStatus.HEALTHY -> stringResource(R.string.sleep_status_healthy, label) to StepsyTheme.colors.goal
        SleepStatus.LONG -> stringResource(R.string.sleep_status_long, label) to StepsyTheme.colors.special
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(text = text, color = color, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        SleepInsights.bedtimeSpreadMinutes(week)?.takeIf { it > 60 }?.let {
            Text(
                text = stringResource(R.string.sleep_consistency_tip, it.toInt()),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .alpha(0.8f),
            )
        }
    }
}

private enum class SleepTimeField { BED, WAKE }

@Composable
private fun SleepEditDialog(
    wakeDate: LocalDate,
    session: SleepSession?,
    onSave: (start: Long, end: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var bedMinutes by remember { mutableIntStateOf(session?.let { minuteOfDay(it.startAt) } ?: (23 * 60)) }
    var wakeMinutes by remember { mutableIntStateOf(session?.let { minuteOfDay(it.endAt) } ?: (7 * 60)) }
    var picking by remember { mutableStateOf<SleepTimeField?>(null) }
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    val timeFormat = remember { android.text.format.DateFormat.getTimeFormat(context) }

    val end = millisAt(wakeDate, wakeMinutes)
    val start = millisAt(if (bedMinutes > wakeMinutes) wakeDate.minusDays(1) else wakeDate, bedMinutes)
    val hours = (end - start) / 3_600_000.0
    val valid = hours in 1.0..16.0

    fun label(minutes: Int) = timeFormat.format(Date(millisAt(wakeDate, minutes)))

    StepsyDialog(
        title = stringResource(R.string.sleep_edit_title, wakeDate.toString()),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        confirmEnabled = valid,
        onConfirm = { onSave(start, end) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            StatRow(stringResource(R.string.sleep_bedtime), label(bedMinutes), onClick = { picking = SleepTimeField.BED })
            StatRow(stringResource(R.string.sleep_wake), label(wakeMinutes), onClick = { picking = SleepTimeField.WAKE })
            Text(
                text = durationLabel(((end - start) / 60_000L).coerceAtLeast(0)),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.Center,
            )
        }
    }

    picking?.let { field ->
        TimePickerDialog(
            title = stringResource(if (field == SleepTimeField.BED) R.string.sleep_bedtime else R.string.sleep_wake),
            initialHour = (if (field == SleepTimeField.BED) bedMinutes else wakeMinutes) / 60,
            initialMinute = (if (field == SleepTimeField.BED) bedMinutes else wakeMinutes) % 60,
            is24Hour = is24Hour,
            onConfirm = { hour, minute ->
                if (field == SleepTimeField.BED) bedMinutes = hour * 60 + minute else wakeMinutes = hour * 60 + minute
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}
