package com.prateek.taro.ui

import com.prateek.taro.ui.components.TextTabs
import android.content.Context
import com.prateek.taro.ui.components.TimeRow
import androidx.compose.animation.AnimatedVisibility
import com.prateek.taro.ui.components.infoOf
import com.prateek.taro.ui.components.InfoBox
import com.prateek.taro.ui.components.dayLabel
import com.prateek.taro.ui.components.DaySwitcher
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import com.prateek.taro.ui.components.StatRow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prateek.taro.util.Database
import com.prateek.taro.sleep.SleepDiagnostics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.prateek.taro.R
import androidx.compose.foundation.layout.navigationBarsPadding
import cafe.adriel.voyager.core.screen.Screen
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.unit.Dp
import com.prateek.taro.ui.components.NIGHT_ORIGIN_MINUTE
import com.prateek.taro.ui.components.NightWindow
import com.prateek.taro.ui.components.SleepWindows
import com.prateek.taro.ui.components.ChangeOrRemove
import com.prateek.taro.ui.components.EventCard
import com.prateek.taro.ui.components.CardStats
import com.prateek.taro.ui.components.CardHead
import com.prateek.taro.ui.components.CardActions
import com.prateek.taro.ui.components.StripSpan
import com.prateek.taro.ui.components.DayStrip
import com.prateek.taro.ui.components.BentoNote
import com.prateek.taro.ui.components.LinkText
import com.prateek.taro.ui.components.Fact
import com.prateek.taro.data.SleepSession
import com.prateek.taro.sleep.Night
import com.prateek.taro.sleep.SleepDates
import com.prateek.taro.sleep.SleepInsights
import com.prateek.taro.sleep.SleepRepository
import com.prateek.taro.sleep.SleepStatus
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.DateRow
import com.prateek.taro.ui.components.TaroDialog
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.ui.theme.TaroTheme
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
import com.prateek.taro.ui.components.SectionLabel
import com.prateek.taro.ui.components.Panel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size

object SleepPage : Screen {
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
    data class Entry(val date: LocalDate, val session: SleepSession?, val nap: Boolean) : SleepDialog
}

@Composable
private fun SleepContent(tracking: TrackingState) {
    val context = LocalContext.current
    val toast = LocalToast.current
    val colors = TaroTheme.colors
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    var version by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<SleepDialog?>(null) }
    var showDetection by rememberSaveable { mutableStateOf(false) }
    var open by remember { mutableStateOf(setOf<Long>()) }
    val quickLog = rememberQuickLog()
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    val today = SleepDates.today()
    var picked by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val selected = picked?.takeIf { !it.isAfter(today) } ?: today
    fun select(date: LocalDate) {
        picked = date.takeIf { it != today }
    }
    val dataVersion = rememberDataVersion()
    val sessions = rememberRefreshed(version, tracking.refreshKey, dataVersion) { SleepRepository.sessions(context, HISTORY_DAYS) }
    val night = sessions.firstOrNull { it.wakeDate == selected.toString() }
    val naps = rememberRefreshed(version, selected, dataVersion) { SleepRepository.naps(context, selected) }
    val week = sessions.filter { !LocalDate.parse(it.wakeDate).isBefore(today.minusDays(6)) }.map { it.toNight() }
    val usual = sessions.filter { it.wakeDate != selected.toString() }.take(USUAL_NIGHTS).map { it.durationMinutes }.takeIf { it.isNotEmpty() }?.average()?.toLong()
    val timeFormat = remember { android.text.format.DateFormat.getTimeFormat(context) }
    fun time(millis: Long) = timeFormat.format(Date(millis))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        AppOverflowMenu(
            back = true,
            center = { DaySwitcher(date = selected, today = today, onChange = ::select, todayLabel = stringResource(R.string.sleep_last_night)) },
            actions = listOf(
                MenuEntry(R.string.sleep_add, R.string.quick_sleep_hint, R.drawable.ic_sleep, colors.sleep) {
                    dialog = SleepDialog.Entry(selected, null, nap = night != null)
                },
            ) + quickLogActions(quickLog, Util.logicalToday()),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(start = spacing.loose, end = spacing.loose, bottom = spacing.loose),
        ) {
            val minutes = night?.durationMinutes ?: 0L
            HeroRing(
                segments = listOf(RingSegment(minutes.toDouble(), colors.sleep)),
                target = SleepInsights.TARGET_MIN.toDouble(),
                lit = minutes >= SleepInsights.HEALTHY_MIN,
                gradient = true,
                diameter = Dp.Unspecified,
                strokeShare = HERO_STROKE_SHARE,
                modifier = Modifier
                    .fillMaxWidth(HERO_SHARE)
                    .padding(top = spacing.tight),
            ) {
                Text(
                    text = dayLabel(selected, today, stringResource(R.string.sleep_last_night)).uppercase(),
                    style = type.heroLabel,
                    color = colors.accent,
                    maxLines = 1,
                )
                RollingText(
                    text = if (night == null) "0h 00m" else durationLabel(night.durationMinutes),
                    style = type.hero.copy(color = if (night == null) colors.accent else MaterialTheme.colorScheme.onSurface),
                )
                InfoBox(infoOf(R.string.info_sleep_goal_title, R.string.info_sleep_goal), Modifier.clip(RoundedCornerShape(spacing.tight)), color = colors.sleep) {
                    Text(
                        text = night?.let { stringResource(R.string.sleep_range, time(it.startAt), time(it.endAt)) } ?: stringResource(R.string.sleep_nothing_yet),
                        style = type.note,
                        color = colors.accent,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            DayStrip(
                start = millisAt(selected.minusDays(1), STRIP_START_HOUR * 60),
                end = millisAt(selected, STRIP_END_HOUR * 60),
                now = System.currentTimeMillis().takeIf { selected == today },
                spans = (listOfNotNull(night) + naps).map { StripSpan(it.startAt, it.endAt, colors.sleep) },
                dots = emptyList(),
                modifier = Modifier.padding(top = spacing.loose),
            )

            if (night == null) {
                LinkText(
                    text = stringResource(R.string.sleep_add),
                    color = colors.sleep,
                    onClick = { dialog = SleepDialog.Entry(selected, null, nap = false) },
                    icon = R.drawable.ic_add,
                    modifier = Modifier.padding(top = spacing.tight),
                )
            } else if (!night.confirmed && night.source != SleepRepository.SOURCE_MANUAL) {
                BentoNote(
                    text = stringResource(if (night.source == SleepRepository.SOURCE_GOOGLE) R.string.sleep_found_google else R.string.sleep_found_phone),
                    modifier = Modifier.padding(top = spacing.gap),
                )
                CardActions {
                    LinkText(
                        text = stringResource(R.string.day_looks_right),
                        color = colors.goal,
                        onClick = {
                            SleepRepository.confirm(context, night)
                            version++
                            toast.show(context.getString(R.string.sleep_confirmed), ToastKind.SUCCESS)
                        },
                        icon = R.drawable.ic_check,
                    )
                    LinkText(
                        text = stringResource(R.string.day_change),
                        color = MaterialTheme.colorScheme.onSurface,
                        onClick = { dialog = SleepDialog.Entry(selected, night, nap = false) },
                        icon = R.drawable.ic_edit,
                    )
                    LinkText(
                        text = stringResource(R.string.day_remove),
                        color = MaterialTheme.colorScheme.error,
                        onClick = {
                            SleepRepository.delete(context, night)
                            version++
                            toast.show(context.getString(R.string.sleep_deleted), ToastKind.INFO)
                        },
                        icon = R.drawable.ic_delete,
                    )
                }
            }

            val average = SleepInsights.average(week)
            val streak = SleepInsights.streak(week, today)
            val spread = SleepInsights.bedtimeSpreadMinutes(week)
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.loose),
            ) {
                Fact(average?.let { durationLabel(it) } ?: "-", stringResource(R.string.sleep_fact_average), colors.sleep, info = infoOf(R.string.info_sleep_average_title, R.string.info_sleep_average), icon = R.drawable.ic_sleep, style = type.factCompact)
                Fact(streak.toString(), pluralStringResource(R.plurals.sleep_fact_streak, streak), colors.special, info = infoOf(R.string.info_sleep_streak_title, R.string.info_sleep_streak), icon = R.drawable.ic_badge_week, style = type.factCompact)
                Fact(spread?.let { stringResource(R.string.sleep_spread_value, minutesText(it)) } ?: "-", stringResource(R.string.sleep_fact_spread), colors.accent, info = infoOf(R.string.info_sleep_spread_title, R.string.info_sleep_spread), icon = R.drawable.ic_schedule, style = type.factCompact)
            }
            average?.let { SleepVerdict(it, spread, Modifier.padding(top = spacing.loose + spacing.tight, start = spacing.gap, end = spacing.gap)) }

            SectionLabel(stringResource(R.string.sleep_log_title), info = infoOf(R.string.sleep_log_title, R.string.info_naps))
            Column(verticalArrangement = Arrangement.spacedBy(spacing.gap), modifier = Modifier.fillMaxWidth()) {
                (listOfNotNull(night) + naps).forEach { entry ->
                    SleepCard(
                        session = entry,
                        usualMinutes = usual.takeIf { !entry.nap },
                        expanded = entry.id in open,
                        onToggle = { open = if (entry.id in open) open - entry.id else open + entry.id },
                        onConfirm = {
                            SleepRepository.confirm(context, entry)
                            version++
                            toast.show(context.getString(R.string.sleep_confirmed), ToastKind.SUCCESS)
                        },
                        onChange = { dialog = SleepDialog.Entry(selected, entry, entry.nap) },
                        onRemove = {
                            SleepRepository.delete(context, entry)
                            version++
                            toast.show(context.getString(if (entry.nap) R.string.nap_deleted else R.string.sleep_deleted), ToastKind.INFO)
                        },
                    )
                }
                LinkText(
                    text = stringResource(if (night == null) R.string.sleep_add else R.string.day_add_nap),
                    color = colors.sleep,
                    onClick = { dialog = SleepDialog.Entry(selected, null, nap = night != null) },
                    icon = R.drawable.ic_add,
                )
            }

            val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
            val byDate = sessions.associateBy { LocalDate.parse(it.wakeDate) }
            SectionLabel(stringResource(R.string.sleep_last_7), info = infoOf(R.string.info_sleep_week_title, R.string.info_sleep_week))
            Panel {
                Column(modifier = Modifier.padding(spacing.tight)) {
                    SleepWindows(
                        nights = days.map { date ->
                            val entry = byDate[date]
                            val origin = millisAt(date.minusDays(1), NIGHT_ORIGIN_MINUTE)
                            NightWindow(
                                label = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                bedMinute = entry?.let { ((it.startAt - origin) / 60_000L).toInt() },
                                wakeMinute = entry?.let { ((it.endAt - origin) / 60_000L).toInt() },
                                good = entry != null && SleepInsights.inRange(entry.toNight()),
                            )
                        },
                        color = colors.sleep,
                        selected = days.indexOf(selected).takeIf { it >= 0 && selected != today },
                        onSelect = {
                            select(days[it])
                            scope.launch { scroll.animateScrollTo(0) }
                        },
                        modifier = Modifier.aspectRatio(WINDOWS_ASPECT),
                    )
                    average?.let {
                        BentoNote(
                            text = stringResource(R.string.sleep_week_note, durationLabel(it), week.count(SleepInsights::inRange), week.size),
                            modifier = Modifier.padding(top = spacing.gap),
                        )
                    }
                }
            }

            SectionLabel(stringResource(R.string.sleep_diag_title), info = infoOf(R.string.info_sleep_detection_title, R.string.info_sleep_detection))
            LinkText(
                text = stringResource(if (showDetection) R.string.sleep_diag_hide else R.string.sleep_diag_show),
                color = colors.accent,
                onClick = { showDetection = !showDetection },
                icon = if (showDetection) R.drawable.ic_expand_less else R.drawable.ic_expand_more,
                modifier = Modifier.align(Alignment.Start),
            )
            AnimatedVisibility(visible = showDetection) { SleepDetectionCard() }
        }
    }

    (dialog as? SleepDialog.Entry)?.let { edit ->
        SleepEntryDialog(
            initialDate = edit.date,
            session = edit.session,
            initialNap = edit.nap,
            onSave = { start, end, nap ->
                saveSleepEntry(context, edit.session, start, end, nap)
                dialog = null
                version++
                toast.show(context.getString(if (nap) R.string.nap_saved else R.string.sleep_saved), ToastKind.SUCCESS)
            },
            onDismiss = { dialog = null },
        )
    }

    QuickLogDialogs(quickLog, Util.logicalToday())
}

@Composable
private fun SleepVerdict(average: Long, spread: Long?, modifier: Modifier = Modifier) {
    val label = durationLabel(average)
    val (text, color) = when (SleepInsights.status(average)) {
        SleepStatus.SHORT -> stringResource(R.string.sleep_status_short, label) to MaterialTheme.colorScheme.error
        SleepStatus.A_BIT_SHORT -> stringResource(R.string.sleep_status_a_bit_short, label) to TaroTheme.colors.special
        SleepStatus.HEALTHY -> stringResource(R.string.sleep_status_healthy, label) to TaroTheme.colors.goal
        SleepStatus.LONG -> stringResource(R.string.sleep_status_long, label) to TaroTheme.colors.special
    }
    val tip = spread?.takeIf { it > 60 }?.let { stringResource(R.string.sleep_consistency_tip, minutesText(it)) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TaroTheme.spacing.tight), modifier = modifier.fillMaxWidth()) {
        Text(text, style = TaroTheme.text.meterValue, color = color, textAlign = TextAlign.Center)
        tip?.let { Text(it, style = TaroTheme.text.note, color = TaroTheme.colors.accent, textAlign = TextAlign.Center) }
    }
}

@Composable
fun SleepCard(session: SleepSession, usualMinutes: Long?, expanded: Boolean, onToggle: () -> Unit, onConfirm: () -> Unit, onChange: () -> Unit, onRemove: () -> Unit) {
    val context = LocalContext.current
    val format = remember { android.text.format.DateFormat.getTimeFormat(context) }
    val colors = TaroTheme.colors
    val asking = !session.confirmed && session.source != SleepRepository.SOURCE_MANUAL
    EventCard(colors.sleep, onClick = onToggle) {
        Column {
            CardHead(
                title = stringResource(if (session.nap) R.string.day_nap else R.string.day_slept, minutesText(session.durationMinutes)),
                value = stringResource(R.string.day_until, format.format(Date(session.endAt))),
                found = asking,
            )
            if (asking) {
                BentoNote(stringResource(if (session.source == SleepRepository.SOURCE_GOOGLE) R.string.day_sleep_google else R.string.day_sleep_phone))
                CardActions {
                    LinkText(stringResource(R.string.day_looks_right), colors.goal, onClick = onConfirm, icon = R.drawable.ic_check)
                    LinkText(stringResource(R.string.day_change), MaterialTheme.colorScheme.onSurface, onClick = onChange, icon = R.drawable.ic_edit)
                    LinkText(stringResource(R.string.day_remove), MaterialTheme.colorScheme.error, onClick = onRemove, icon = R.drawable.ic_delete)
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    val difference = usualMinutes?.let { session.durationMinutes - it }
                    CardStats(
                        listOfNotNull(
                            Triple(colors.sleep, minutesText(session.durationMinutes), stringResource(R.string.day_stat_asleep)),
                            Triple(colors.sleep, format.format(Date(session.startAt)), stringResource(R.string.day_stat_bed)),
                            Triple(colors.sleep, format.format(Date(session.endAt)), stringResource(R.string.day_stat_woke)),
                            difference?.let {
                                Triple(colors.accent, (if (it < 0) "\u2212" else "+") + minutesText(kotlin.math.abs(it)), stringResource(R.string.day_stat_vs_usual))
                            },
                        ),
                    )
                    if (!asking) ChangeOrRemove(true, onChange, onRemove)
                }
            }
        }
    }
}


private const val HISTORY_DAYS = 120L
private const val STRIP_START_HOUR = 20
private const val STRIP_END_HOUR = 12
private const val USUAL_NIGHTS = 14
private const val HERO_SHARE = 0.72f
private const val HERO_STROKE_SHARE = 0.075f
private const val WINDOWS_ASPECT = 1.5f

fun saveSleepEntry(context: Context, session: SleepSession?, start: Long, end: Long, nap: Boolean) {
    val kept = session?.takeIf { it.nap == nap }
    if (session != null && kept == null) SleepRepository.delete(context, session)
    if (nap) {
        SleepRepository.saveNap(context, start, end, kept?.id ?: 0)
    } else {
        kept?.let { old ->
            if (SleepDates.wakeDateOf(end).toString() != old.wakeDate) SleepRepository.delete(context, old)
        }
        SleepRepository.saveManual(context, start, end)
    }
}

@Composable
fun SleepEntryDialog(
    initialDate: LocalDate,
    session: SleepSession?,
    initialNap: Boolean,
    onSave: (start: Long, end: Long, nap: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val now = remember { minuteOfDay(System.currentTimeMillis()) }
    var nap by remember { mutableStateOf(initialNap) }
    var date by remember { mutableStateOf(initialDate) }
    var startMinutes by remember {
        mutableIntStateOf(session?.let { minuteOfDay(it.startAt) } ?: if (initialNap) (now - 30).coerceAtLeast(0) else 23 * 60)
    }
    var endMinutes by remember { mutableIntStateOf(session?.let { minuteOfDay(it.endAt) } ?: if (initialNap) now else 7 * 60) }

    val span = SleepDates.manualSpan(date, startMinutes, endMinutes)
    val start = span.start
    val end = span.end
    val minutes = (end - start) / 60_000L
    val valid = minutes > 0

    TaroDialog(
        title = stringResource(if (session == null) R.string.sleep_add_title else R.string.sleep_edit_dialog_title),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        confirmEnabled = valid,
        onConfirm = { onSave(start, end, nap) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TaroTheme.spacing.tight)) {
            TextTabs(
                options = listOf(false to stringResource(R.string.sleep_night_label), true to stringResource(R.string.nap_label)),
                selected = nap,
                onSelect = { nap = it },
                color = TaroTheme.colors.sleep,
            )
            DateRow(
                label = stringResource(if (nap) R.string.date_label else R.string.sleep_woke_on),
                date = date,
                today = SleepDates.today(),
                onChange = { date = it },
            )
            TimeRow(label = stringResource(R.string.nap_from), minuteOfDay = startMinutes, onChange = { startMinutes = it })
            TimeRow(label = stringResource(R.string.nap_to), minuteOfDay = endMinutes, onChange = { endMinutes = it })
            Text(
                text = if (valid) durationLabel(minutes.coerceAtLeast(0)) else stringResource(R.string.sleep_invalid),
                style = if (valid) TaroTheme.text.fact else TaroTheme.text.note,
                color = if (valid) TaroTheme.colors.sleep else MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TaroTheme.spacing.tight),
            )
        }
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
        style = TaroTheme.text.note,
        color = TaroTheme.colors.accent,
        modifier = Modifier.padding(start = TaroTheme.spacing.loose, end = TaroTheme.spacing.loose, bottom = TaroTheme.spacing.tight),
    )
}
