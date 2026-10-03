package com.prateek.taro.ui

import com.prateek.taro.ui.components.TileRow
import com.prateek.taro.ui.components.PrimaryButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prateek.taro.R
import com.prateek.taro.report.Highlight
import com.prateek.taro.report.Period
import com.prateek.taro.report.PeriodReport
import com.prateek.taro.report.ReportData
import com.prateek.taro.report.ReportKind
import com.prateek.taro.report.WeightVerdict
import com.prateek.taro.ui.components.Panel
import com.prateek.taro.ui.components.RollingText
import com.prateek.taro.ui.components.ScrollingColumn
import com.prateek.taro.ui.components.SectionLabel
import com.prateek.taro.ui.components.StatRow
import com.prateek.taro.ui.components.StatTile
import com.prateek.taro.ui.components.StepsBarChart
import com.prateek.taro.ui.components.TaroScaffold
import com.prateek.taro.ui.components.TintChip
import com.prateek.taro.ui.components.ToggleGroup
import com.prateek.taro.ui.components.infoOf
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Util
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class ReportScreen(private val initial: String? = null) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        ReportContent(Period.parse(initial), onBack = { navigator.pop() })
    }
}

private fun kcal(value: Double) = Util.formatSteps(value.roundToInt())

@Composable
private fun periodLabel(period: Period): String {
    val day = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    return if (period.kind == ReportKind.WEEK) {
        stringResource(R.string.report_week_range, period.start.format(day), period.end.format(day))
    } else {
        "${period.start.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${period.start.year}"
    }
}

@Composable
private fun highlightText(highlight: Highlight, kind: ReportKind): String {
    val unit = stringResource(if (kind == ReportKind.WEEK) R.string.report_last_week else R.string.report_last_month)
    return when (highlight) {
        is Highlight.MoreSteps -> stringResource(R.string.report_more_steps, highlight.percent, unit)
        is Highlight.FewerSteps -> stringResource(R.string.report_fewer_steps, highlight.percent, unit)
        is Highlight.Deficit -> stringResource(R.string.report_deficit, kcal(highlight.kcal), "%.2f".format(Locale.getDefault(), highlight.kg))
        is Highlight.Surplus -> stringResource(R.string.report_surplus, kcal(highlight.kcal), "%.2f".format(Locale.getDefault(), highlight.kg))
        is Highlight.GoalEveryDay -> stringResource(R.string.report_goal_every_day, highlight.days)
        Highlight.WeightOnTrack -> stringResource(R.string.report_weight_on_track)
        is Highlight.Steps -> stringResource(R.string.report_steps_only, Util.formatSteps(highlight.steps))
    }
}

@Composable
private fun ReportContent(initial: Period?, onBack: () -> Unit) {
    val context = LocalContext.current
    val dataVersion = rememberDataVersion()
    var kind by rememberSaveable { mutableStateOf(initial?.kind ?: ReportKind.WEEK) }
    var start by rememberSaveable {
        mutableStateOf((initial ?: Period.containing(ReportKind.WEEK, Util.logicalToday(), ReportData.firstDayOfWeek())).start.toString())
    }
    val period = Period(kind, java.time.LocalDate.parse(start))
    val today = Util.logicalToday()
    val navigator = LocalNavigator.currentOrThrow
    val report = rememberInBackground(period, dataVersion) { ReportData.load(context, period) }

    TaroScaffold(title = stringResource(R.string.report_title), onBack = onBack) { padding ->
        ScrollingColumn(padding, modifier = Modifier.padding(horizontal = 16.dp)) {
            ToggleGroup(
                options = listOf(ReportKind.WEEK to stringResource(R.string.report_week), ReportKind.MONTH to stringResource(R.string.report_month)),
                selected = kind,
                onSelect = {
                    kind = it
                    start = Period.containing(it, Util.logicalToday(), ReportData.firstDayOfWeek()).start.toString()
                },
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                IconButton(onClick = { start = period.previous().start.toString() }) {
                    Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.report_previous), tint = TaroTheme.colors.accent, modifier = Modifier.rotate(180f))
                }
                Text(
                    text = periodLabel(period).uppercase(),
                    fontSize = 13.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                val canNext = !period.contains(today)
                IconButton(onClick = { start = period.next().start.toString() }, enabled = canNext) {
                    Icon(
                        painterResource(R.drawable.ic_chevron_right),
                        stringResource(R.string.report_next),
                        tint = TaroTheme.colors.accent.copy(alpha = if (canNext) 1f else 0.25f),
                    )
                }
            }

            val current = report?.value
            if (current != null && current.steps > 0) {
                PrimaryButton(
                    text = stringResource(if (kind == ReportKind.WEEK) R.string.story_play_week else R.string.story_play_month),
                    onClick = { navigator.push(ReportStoryScreen(period.key)) },
                    icon = R.drawable.ic_play,
                    tint = TaroTheme.colors.goal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
            if (report == null) {
                Box(modifier = Modifier.height(400.dp))
            } else if (current == null || current.steps == 0 && current.loggedDays == 0) {
                Text(
                    text = stringResource(R.string.report_empty),
                    color = TaroTheme.colors.accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                )
            } else {
                ReportBody(current)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportBody(report: PeriodReport) {
    val colors = TaroTheme.colors
    val kind = report.inputs.period.kind
    val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

    Panel(modifier = Modifier.padding(top = 12.dp)) {
        if (report.inProgress) {
            TintChip(stringResource(R.string.report_so_far), colors.accent, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
        }
        Text(
            text = report.highlights.take(2).map { highlightText(it, kind) }.joinToString(" "),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(12.dp),
        )
    }

    SectionLabel(stringResource(R.string.report_movement))
    Panel {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
            RollingText(
                text = Util.formatSteps(report.steps),
                style = MaterialTheme.typography.displaySmall.copy(color = MaterialTheme.colorScheme.onSurface),
            )
            Text(
                text = stringResource(R.string.report_steps_word),
                color = colors.accent,
                modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
            )
        }
        report.stepsChange?.let { change ->
            TintChip(
                text = stringResource(
                    if (change >= 0) R.string.report_change_up else R.string.report_change_down,
                    abs(change),
                    stringResource(if (kind == ReportKind.WEEK) R.string.report_last_week else R.string.report_last_month),
                ),
                color = if (change >= 0) colors.goal else colors.flame,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
            )
        }
        TileRow(modifier = Modifier.padding(top = 12.dp)) {
            StatTile(
                label = stringResource(R.string.report_distance),
                value = "%.1f %s".format(Locale.getDefault(), Util.metersToDistance(report.distanceM), Util.distanceUnit()),
                color = colors.goal,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.report_active),
                value = stringResource(R.string.energy_kcal, kcal(report.activeKcal)),
                color = colors.flame,
                modifier = Modifier.weight(1f),
            )
        }
        StatRow(stringResource(R.string.report_average), Util.formatSteps(report.averageSteps))
        if (report.goalDaysPossible > 0) {
            StatRow(stringResource(R.string.report_goal_days), "${report.goalDays} / ${report.goalDaysPossible}")
        }
        report.bestDay?.let { StatRow(stringResource(R.string.report_best_day), "${it.date.format(dayFormat)} · ${Util.formatSteps(it.steps)}") }
        val days = report.inputs.days
        StepsBarChart(
            values = days.map { it.steps },
            labels = days.mapIndexed { index, day ->
                if (kind == ReportKind.WEEK) day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                else if (index % 7 == 0) day.date.dayOfMonth.toString() else ""
            },
            goal = 0,
            goals = days.map { it.goal },
            showMultiplier = kind == ReportKind.WEEK,
            valueLabel = { if (kind == ReportKind.WEEK) Util.formatSteps(it) else "" },
            appearKey = report.inputs.period,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(top = 8.dp),
        )
    }

    SectionLabel(stringResource(R.string.report_energy), info = infoOf(R.string.report_energy, R.string.info_report_energy))
    Panel {
        if (report.expectedKg == null) {
            Text(
                text = stringResource(R.string.report_energy_empty),
                fontSize = 13.sp,
                color = colors.accent,
                modifier = Modifier.padding(8.dp),
            )
        } else {
            Text(
                text = stringResource(R.string.report_logged_days, report.loggedDays, report.inputs.days.size),
                fontSize = 12.sp,
                color = colors.accent,
                modifier = Modifier.padding(start = 20.dp, top = 8.dp),
            )
            StatRow(stringResource(R.string.report_burned), stringResource(R.string.energy_kcal, kcal(report.burned)))
            StatRow(stringResource(R.string.report_eaten), stringResource(R.string.energy_kcal, kcal(report.eaten)))
            StatRow(
                stringResource(R.string.report_balance),
                (if (report.balance > 0) "+" else "") + stringResource(R.string.energy_kcal, kcal(report.balance)),
            )
            StatRow(stringResource(R.string.report_expected), Util.formatWeightChange(report.expectedKg))
        }
    }

    SectionLabel(stringResource(R.string.report_weight), info = infoOf(R.string.report_weight, R.string.info_report_weight))
    Panel {
        val start = report.inputs.weightStart
        val end = report.inputs.weightEnd
        if (start == null || end == null || report.actualKg == null) {
            Text(
                text = stringResource(R.string.report_weight_empty),
                fontSize = 13.sp,
                color = colors.accent,
                modifier = Modifier.padding(8.dp),
            )
        } else {
            StatRow(stringResource(R.string.report_weight_trend), "${Util.formatWeight(start)} → ${Util.formatWeight(end)}")
            StatRow(stringResource(R.string.report_weight_change), Util.formatWeightChange(report.actualKg))
            report.verdict?.let { verdict ->
                val (text, color) = when (verdict) {
                    WeightVerdict.MATCHES -> stringResource(R.string.report_verdict_matches) to colors.goal
                    WeightVerdict.LOST_MORE -> stringResource(R.string.report_verdict_more) to colors.special
                    WeightVerdict.LOST_LESS -> stringResource(R.string.report_verdict_less) to colors.flame
                }
                Text(text, fontSize = 13.sp, color = color, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp))
            }
        }
    }

    if (report.averageEaten != null || report.inputs.topFoods.isNotEmpty()) {
        SectionLabel(stringResource(R.string.report_food))
        Panel {
            report.averageEaten?.let { StatRow(stringResource(R.string.report_average_eaten), stringResource(R.string.energy_kcal, kcal(it))) }
            if (report.inputs.proteinTarget > 0) {
                StatRow(stringResource(R.string.report_protein_days), "${report.proteinDays} / ${report.inputs.days.size}")
            }
            if (report.inputs.topFoods.isNotEmpty()) {
                StatRow(stringResource(R.string.report_top_foods), report.inputs.topFoods.joinToString(", "))
            }
        }
    }

    val nights = report.inputs.nightMinutes
    if (nights.isNotEmpty() || report.inputs.napMinutes.isNotEmpty()) {
        SectionLabel(stringResource(R.string.report_sleep))
        Panel {
            if (nights.isNotEmpty()) {
                val average = nights.average().toLong()
                StatRow(stringResource(R.string.report_average_night), stringResource(R.string.sleep_duration, (average / 60).toInt(), (average % 60).toInt()))
            }
            report.inputs.bedtimeSpread?.let { StatRow(stringResource(R.string.sleep_spread_short), stringResource(R.string.sleep_spread_value, it.toInt())) }
            if (report.inputs.napMinutes.isNotEmpty()) {
                val total = report.inputs.napMinutes.sum()
                StatRow(
                    stringResource(R.string.report_naps),
                    "${report.inputs.napMinutes.size} · " + stringResource(R.string.sleep_duration, (total / 60).toInt(), (total % 60).toInt()),
                )
            }
        }
    }

    if (report.inputs.badges.isNotEmpty()) {
        SectionLabel(stringResource(R.string.report_badges))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            report.inputs.badges.forEach { TintChip(stringResource(it), colors.special) }
        }
    }
    Box(modifier = Modifier.height(32.dp))
}

@Composable
fun ReportCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    val rootNavigator = navigator.parent ?: navigator
    val dataVersion = rememberDataVersion()
    val seen by AppPreferences.reportsSeenFlow().collectAsStateWithLifecycle(AppPreferences.reportsSeen)
    val due = rememberInBackground(dataVersion, seen) {
        val today = Util.logicalToday()
        val month = ReportData.latestComplete(ReportKind.MONTH, today).takeIf { today.dayOfMonth <= 7 }
        val week = ReportData.latestComplete(ReportKind.WEEK, today)
        listOfNotNull(month, week).firstOrNull { it.key !in seen && ReportData.stepsIn(context, it) > 0 }
            ?.let { it to ReportData.stepsIn(context, it) }
    }?.value ?: return
    val (period, steps) = due

    Panel(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    AppPreferences.markReportSeen(period.key)
                    rootNavigator.push(ReportStoryScreen(period.key))
                }
                .padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(if (period.kind == ReportKind.WEEK) R.string.report_card_week else R.string.report_card_month).uppercase(),
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TaroTheme.colors.goal,
                )
                Text(periodLabel(period), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
                Text(
                    text = stringResource(R.string.report_card_steps, Util.formatSteps(steps)),
                    fontSize = 13.sp,
                    color = TaroTheme.colors.accent,
                )
            }
            IconButton(onClick = { AppPreferences.markReportSeen(period.key) }) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.report_dismiss), tint = TaroTheme.colors.accent, modifier = Modifier.size(20.dp))
            }
        }
    }
}
