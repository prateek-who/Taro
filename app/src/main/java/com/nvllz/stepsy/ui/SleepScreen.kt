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
import com.nvllz.stepsy.sleep.SleepDates
import com.nvllz.stepsy.sleep.SleepInsights
import com.nvllz.stepsy.sleep.SleepRepository
import com.nvllz.stepsy.sleep.SleepStatus
import com.nvllz.stepsy.ui.components.LocalToast
import com.nvllz.stepsy.ui.components.PrimaryButton
import com.nvllz.stepsy.ui.components.SecondaryButton
import com.nvllz.stepsy.ui.components.StepsBarChart
import com.nvllz.stepsy.ui.components.DateRow
import com.nvllz.stepsy.ui.components.PickerRow
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
import com.nvllz.stepsy.ui.components.HeroRing
import com.nvllz.stepsy.ui.components.RingSegment
import com.nvllz.stepsy.ui.components.RollingText
import com.nvllz.stepsy.ui.components.StatPill
import com.nvllz.stepsy.ui.components.StatTile
import com.nvllz.stepsy.ui.components.SectionLabel
import com.nvllz.stepsy.ui.components.TintChip
import com.nvllz.stepsy.ui.components.Panel
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer

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

private const val SLEEP_TARGET_MIN = 8 * 60L

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

    val today = SleepDates.today()
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
            val sleepColor = StepsyTheme.colors.sleep
            val minutes = lastNight?.durationMinutes ?: 0L
            HeroRing(
                segments = listOf(RingSegment(minutes.toDouble(), sleepColor)),
                target = SLEEP_TARGET_MIN.toDouble(),
                lit = minutes >= SleepInsights.HEALTHY_MIN,
                gradient = true,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.sleep_last_night).uppercase(),
                    fontSize = 13.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StepsyTheme.colors.accent,
                )
                if (lastNight == null) {
                    Text(
                        text = stringResource(R.string.sleep_nothing_yet),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    RollingText(
                        text = durationLabel(lastNight.durationMinutes),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.sleep_range, time(lastNight.startAt), time(lastNight.endAt)),
                        fontSize = 14.sp,
                        color = StepsyTheme.colors.accent,
                    )
                }
                Text(
                    text = stringResource(R.string.sleep_of_goal, durationLabel(SLEEP_TARGET_MIN)),
                    fontSize = 12.sp,
                    color = StepsyTheme.colors.accent,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            val streak = SleepInsights.streak(week, today)
            if (streak > 0) {
                StatPill(
                    icon = R.drawable.ic_sleep,
                    text = stringResource(R.string.sleep_streak_chip, streak),
                    color = sleepColor,
                    lit = streak >= 3,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            when {
                lastNight == null -> PrimaryButton(
                    text = stringResource(R.string.sleep_add),
                    onClick = { dialog = SleepDialog.Edit(today, null) },
                    icon = R.drawable.ic_sleep,
                    modifier = Modifier.padding(top = 12.dp),
                )
                !lastNight.confirmed -> Panel(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = stringResource(R.string.sleep_confirm_short),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 10.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        PrimaryButton(
                            text = stringResource(R.string.sleep_confirm),
                            onClick = {
                                SleepRepository.confirm(context, lastNight)
                                version++
                                toast.show(context.getString(R.string.sleep_confirmed), ToastKind.SUCCESS)
                            },
                            icon = R.drawable.ic_tick,
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(
                            text = stringResource(R.string.sleep_edit),
                            onClick = { dialog = SleepDialog.Edit(today, lastNight) },
                        )
                    }
                }
            }

            SleepStatusMessage(week, modifier = Modifier.padding(top = 16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                StatTile(
                    label = stringResource(R.string.sleep_average_short),
                    value = SleepInsights.average(week)?.let { durationLabel(it) } ?: "-",
                    color = sleepColor,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.sleep_spread_short),
                    value = SleepInsights.bedtimeSpreadMinutes(week)?.let { stringResource(R.string.sleep_spread_value, it.toInt()) } ?: "-",
                    modifier = Modifier.weight(1f),
                )
            }

            val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
            val byDate = week.associateBy { it.wakeDate }
            var selectedBar by remember { mutableStateOf<Int?>(null) }
            SectionLabel(stringResource(R.string.sleep_last_7))
            Panel(modifier = Modifier.height(260.dp)) {
                Text(
                    text = stringResource(R.string.sleep_last_7_caption),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = StepsyTheme.colors.accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
                StepsBarChart(
                    values = days.map { byDate[it]?.minutes?.toInt() ?: 0 },
                    labels = days.map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) },
                    goal = SleepInsights.HEALTHY_MIN.toInt(),
                    valueLabel = { "${it / 60}h${"%02d".format(it % 60)}" },
                    showMultiplier = false,
                    metColor = sleepColor,
                    selectedIndex = selectedBar,
                    onBarClick = { selectedBar = if (selectedBar == it) null else it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }

            if (sessions.isNotEmpty()) {
                SectionLabel(stringResource(R.string.sleep_nights))
                Panel {
                    sessions.asReversed().forEach { session ->
                        val wake = LocalDate.parse(session.wakeDate)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 3.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(sleepColor.copy(alpha = 0.08f))
                                .clickable { dialog = SleepDialog.Edit(wake, session) }
                                .padding(start = 14.dp, top = 6.dp, bottom = 6.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dateFormat.format(Date(Util.dateStringToCalendarMillis(session.wakeDate))),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
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
                                    color = StepsyTheme.colors.accent,
                                )
                            }
                            Text(durationLabel(session.durationMinutes), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = sleepColor)
                            IconButton(onClick = {
                                SleepRepository.delete(context, session)
                                version++
                                toast.show(context.getString(R.string.sleep_deleted), ToastKind.INFO)
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete),
                                    contentDescription = stringResource(R.string.sleep_delete, session.wakeDate),
                                    tint = StepsyTheme.colors.accent,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    (dialog as? SleepDialog.Edit)?.let { edit ->
        SleepEditDialog(
            initialWakeDate = edit.wakeDate,
            session = edit.session,
            onSave = { start, end ->
                edit.session?.let { old ->
                    if (SleepDates.wakeDateOf(end).toString() != old.wakeDate) SleepRepository.delete(context, old)
                }
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
        TintChip(text = text, color = color)
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
    initialWakeDate: LocalDate,
    session: SleepSession?,
    onSave: (start: Long, end: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var wakeDate by remember { mutableStateOf(initialWakeDate) }
    var bedMinutes by remember { mutableIntStateOf(session?.let { minuteOfDay(it.startAt) } ?: (23 * 60)) }
    var wakeMinutes by remember { mutableIntStateOf(session?.let { minuteOfDay(it.endAt) } ?: (7 * 60)) }
    var picking by remember { mutableStateOf<SleepTimeField?>(null) }
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    val timeFormat = remember { android.text.format.DateFormat.getTimeFormat(context) }

    val span = SleepDates.manualSpan(wakeDate, bedMinutes, wakeMinutes)
    val end = span.end
    val start = span.start
    val hours = (end - start) / 3_600_000.0
    val valid = hours in 1.0..16.0

    fun label(minutes: Int) = timeFormat.format(Date(millisAt(wakeDate, minutes)))

    StepsyDialog(
        title = stringResource(if (session == null) R.string.sleep_add_title else R.string.sleep_edit_dialog_title),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        confirmEnabled = valid,
        onConfirm = { onSave(start, end) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DateRow(
                label = stringResource(R.string.sleep_woke_on),
                date = wakeDate,
                today = SleepDates.today(),
                onChange = { wakeDate = it },
            )
            PickerRow(R.drawable.ic_sleep, stringResource(R.string.sleep_bedtime), label(bedMinutes), onClick = { picking = SleepTimeField.BED })
            PickerRow(R.drawable.ic_day_start, stringResource(R.string.sleep_wake), label(wakeMinutes), onClick = { picking = SleepTimeField.WAKE })
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
