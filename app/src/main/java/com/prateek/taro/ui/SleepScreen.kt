package com.prateek.taro.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.Canvas
import androidx.compose.animation.AnimatedVisibility
import com.prateek.taro.ui.components.ActionChip
import com.prateek.taro.ui.components.infoOf
import com.prateek.taro.ui.components.InfoBox
import com.prateek.taro.ui.components.dayLabel
import com.prateek.taro.ui.components.DaySwitcher
import com.prateek.taro.ui.components.LocalTabActive
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import com.prateek.taro.ui.components.StatRow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prateek.taro.util.Database
import com.prateek.taro.sleep.SleepDiagnostics
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
import com.prateek.taro.R
import com.prateek.taro.data.SleepSession
import com.prateek.taro.sleep.Night
import com.prateek.taro.sleep.SleepDates
import com.prateek.taro.sleep.SleepInsights
import com.prateek.taro.sleep.SleepRepository
import com.prateek.taro.sleep.SleepStatus
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.PrimaryButton
import com.prateek.taro.ui.components.SecondaryButton
import com.prateek.taro.ui.components.StepsBarChart
import com.prateek.taro.ui.components.DateRow
import com.prateek.taro.ui.components.PickerRow
import com.prateek.taro.ui.components.TaroDialog
import com.prateek.taro.ui.components.TimePickerDialog
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Util
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.prateek.taro.ui.components.HeroRing
import com.prateek.taro.ui.components.RingSegment
import com.prateek.taro.ui.components.RollingText
import com.prateek.taro.ui.components.StatPill
import com.prateek.taro.ui.components.StatTile
import com.prateek.taro.ui.components.SectionLabel
import com.prateek.taro.ui.components.TintChip
import com.prateek.taro.ui.components.Panel
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
    var showDetection by rememberSaveable { mutableStateOf(false) }
    val quickLog = rememberQuickLog()
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    val today = SleepDates.today()
    val tabActive = LocalTabActive.current
    LaunchedEffect(tabActive) { if (!tabActive) SleepTabState.date = null }
    val selected = SleepTabState.date?.takeIf { !it.isAfter(today) } ?: today
    fun select(date: LocalDate) {
        SleepTabState.date = date.takeIf { it != today }
    }
    val dataVersion = rememberDataVersion()
    val sessions = rememberRefreshed(version, tracking.refreshKey, dataVersion) { SleepRepository.sessions(context, HISTORY_DAYS) }
    val lastNight = sessions.firstOrNull { it.wakeDate == selected.toString() }
    val nightLabel = dayLabel(selected, today, stringResource(R.string.sleep_last_night))
    val week = sessions.filter { !LocalDate.parse(it.wakeDate).isBefore(today.minusDays(6)) }.map { it.toNight() }
    val timeFormat = remember { android.text.format.DateFormat.getTimeFormat(context) }
    val dateFormat = remember { SimpleDateFormat(AppPreferences.dateFormatString, Locale.getDefault()) }

    fun time(millis: Long) = timeFormat.format(Date(millis))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        AppOverflowMenu(
            center = { DaySwitcher(date = selected, today = today, onChange = ::select, todayLabel = stringResource(R.string.sleep_last_night)) },
            actions = listOf(
                MenuEntry(R.string.sleep_add, R.string.quick_sleep_hint, R.drawable.ic_sleep, TaroTheme.colors.sleep) { dialog = SleepDialog.Edit(selected, lastNight) },
            ) + quickLogActions(quickLog, Util.logicalToday()),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        ) {
            val sleepColor = TaroTheme.colors.sleep
            val minutes = lastNight?.durationMinutes ?: 0L
            HeroRing(
                segments = listOf(RingSegment(minutes.toDouble(), sleepColor)),
                target = SLEEP_TARGET_MIN.toDouble(),
                lit = minutes >= SleepInsights.HEALTHY_MIN,
                gradient = true,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(
                    text = nightLabel.uppercase(),
                    fontSize = 13.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TaroTheme.colors.accent,
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
                        color = TaroTheme.colors.accent,
                    )
                }
                InfoBox(
                    info = infoOf(R.string.info_sleep_goal_title, R.string.info_sleep_goal),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = sleepColor,
                ) {
                    Text(
                        text = stringResource(R.string.sleep_of_goal, durationLabel(SLEEP_TARGET_MIN)),
                        fontSize = 12.sp,
                        color = TaroTheme.colors.accent,
                    )
                }
            }

            if (lastNight != null) {
                NightBar(lastNight.startAt, lastNight.endAt, selected, sleepColor, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
            }

            val streak = SleepInsights.streak(week, today)
            if (streak > 0) {
                StatPill(
                    icon = R.drawable.ic_sleep,
                    text = stringResource(R.string.sleep_streak_chip, streak),
                    color = sleepColor,
                    lit = streak >= 3,
                    modifier = Modifier.padding(top = 4.dp),
                    info = infoOf(R.string.info_sleep_streak_title, R.string.info_sleep_streak),
                )
            }

            when {
                lastNight == null -> PrimaryButton(
                    text = stringResource(R.string.sleep_add),
                    onClick = { dialog = SleepDialog.Edit(selected, null) },
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
                            onClick = { dialog = SleepDialog.Edit(selected, lastNight) },
                            modifier = Modifier.weight(1f),
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
                    info = infoOf(R.string.info_sleep_average_title, R.string.info_sleep_average),
                )
                StatTile(
                    label = stringResource(R.string.sleep_spread_short),
                    value = SleepInsights.bedtimeSpreadMinutes(week)?.let { stringResource(R.string.sleep_spread_value, it.toInt()) } ?: "-",
                    modifier = Modifier.weight(1f),
                    info = infoOf(R.string.info_sleep_spread_title, R.string.info_sleep_spread),
                )
            }

            val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
            val byDate = week.associateBy { it.wakeDate }
            SectionLabel(stringResource(R.string.sleep_last_7), info = infoOf(R.string.info_sleep_week_title, R.string.info_sleep_week))
            Panel(modifier = Modifier.height(260.dp)) {
                Text(
                    text = stringResource(R.string.sleep_last_7_caption),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = TaroTheme.colors.accent,
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
                    selectedIndex = days.indexOf(selected).takeIf { it >= 0 && selected != today },
                    onBarClick = {
                        select(days[it])
                        scope.launch { scroll.animateScrollTo(0) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }

            val recent = sessions.filter { !LocalDate.parse(it.wakeDate).isBefore(today.minusDays(LIST_DAYS)) }
            if (recent.isNotEmpty()) {
                SectionLabel(stringResource(R.string.sleep_nights), info = infoOf(R.string.info_sleep_nights_title, R.string.info_sleep_nights))
                Panel {
                    recent.asReversed().forEach { session ->
                        val wake = LocalDate.parse(session.wakeDate)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { dialog = SleepDialog.Edit(wake, session) }
                                .padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
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
                                    color = TaroTheme.colors.accent,
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
                                    tint = TaroTheme.colors.accent,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
            SectionLabel(
                text = stringResource(R.string.sleep_diag_title),
                info = infoOf(R.string.info_sleep_detection_title, R.string.info_sleep_detection),
                action = {
                    ActionChip(
                        text = stringResource(if (showDetection) R.string.sleep_diag_hide else R.string.sleep_diag_show),
                        color = MaterialTheme.colorScheme.onSurface,
                        onClick = { showDetection = !showDetection },
                        trailingIcon = if (showDetection) R.drawable.ic_expand_less else R.drawable.ic_expand_more,
                    )
                },
            )
            AnimatedVisibility(visible = showDetection) { SleepDetectionCard() }
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

    QuickLogDialogs(quickLog, Util.logicalToday())
}

private object SleepTabState {
    var date by mutableStateOf<LocalDate?>(null)
}

private const val HISTORY_DAYS = 120L
private const val LIST_DAYS = 14L
private const val BAR_START_HOUR = 18
private const val BAR_HOURS = 20

@Composable
private fun NightBar(startAt: Long, endAt: Long, wakeDate: LocalDate, color: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val origin = millisAt(wakeDate.minusDays(1), BAR_START_HOUR * 60)
    val span = BAR_HOURS * 3_600_000f
    val track = TaroTheme.colors.accentOpaque
    val format = remember { SimpleDateFormat(if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h a", Locale.getDefault()) }
    Column(modifier = modifier.fillMaxWidth(0.86f)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp),
        ) {
            val radius = CornerRadius(size.height / 2)
            drawRoundRect(track, cornerRadius = radius)
            val left = ((startAt - origin) / span).coerceIn(0f, 1f) * size.width
            val right = ((endAt - origin) / span).coerceIn(0f, 1f) * size.width
            if (right > left) drawRoundRect(color, topLeft = Offset(left, 0f), size = Size(right - left, size.height), cornerRadius = radius)
        }
        Row(modifier = Modifier.padding(top = 4.dp)) {
            (0..BAR_HOURS step 6).forEachIndexed { index, hour ->
                Text(
                    text = format.format(Date(origin + hour * 3_600_000L)).lowercase(Locale.getDefault()),
                    fontSize = 11.sp,
                    color = TaroTheme.colors.accent,
                    textAlign = if (index == 0) TextAlign.Start else TextAlign.Center,
                    modifier = Modifier.weight(if (index == 0) 0.5f else 1f),
                )
            }
        }
    }
}

@Composable
private fun SleepStatusMessage(week: List<Night>, modifier: Modifier = Modifier) {
    val average = SleepInsights.average(week) ?: return
    val status = SleepInsights.status(average)
    val label = durationLabel(average)
    val (text, color) = when (status) {
        SleepStatus.SHORT -> stringResource(R.string.sleep_status_short, label) to MaterialTheme.colorScheme.error
        SleepStatus.A_BIT_SHORT -> stringResource(R.string.sleep_status_a_bit_short, label) to TaroTheme.colors.special
        SleepStatus.HEALTHY -> stringResource(R.string.sleep_status_healthy, label) to TaroTheme.colors.goal
        SleepStatus.LONG -> stringResource(R.string.sleep_status_long, label) to TaroTheme.colors.special
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

    TaroDialog(
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

@Composable
private fun SleepDetectionCard() {
    val context = LocalContext.current
    val state by remember { SleepDiagnostics.flow() }.collectAsStateWithLifecycle(null)
    val dataVersion = rememberDataVersion()
    val screenEvents = rememberRefreshed(dataVersion, state) {
        Database.getInstance(context).screenEventCount(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
    }
    val format = remember { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()) }
    val never = stringResource(R.string.sleep_diag_never)
    fun time(at: Long?) = at?.let { format.format(Date(it)) } ?: never
    val current = state ?: return

    Panel {
        StatRow(stringResource(R.string.sleep_diag_registered), current.registerError?.let { "${time(current.registeredAt)}, $it" } ?: time(current.registeredAt))
        StatRow(stringResource(R.string.sleep_diag_google), time(current.googleAt))
        current.googleSummary?.let { DiagnosticNote(it) }
        StatRow(stringResource(R.string.sleep_diag_estimate), time(current.estimateAt))
        current.estimateSummary?.let { DiagnosticNote(it) }
        StatRow(stringResource(R.string.sleep_diag_screen), screenEvents.toString())
    }
}

@Composable
private fun DiagnosticNote(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = TaroTheme.colors.accent,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
    )
}
