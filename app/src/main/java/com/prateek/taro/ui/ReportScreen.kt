package com.prateek.taro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prateek.taro.R
import com.prateek.taro.report.DayInput
import com.prateek.taro.report.Highlight
import com.prateek.taro.report.Period
import com.prateek.taro.report.PeriodReport
import com.prateek.taro.report.ReportData
import com.prateek.taro.report.ReportKind
import com.prateek.taro.report.StoryData
import com.prateek.taro.report.StoryFacts
import com.prateek.taro.report.WeightVerdict
import com.prateek.taro.sleep.SleepInsights
import com.prateek.taro.ui.components.BentoNote
import com.prateek.taro.ui.components.BentoTile
import com.prateek.taro.ui.components.BoneText
import com.prateek.taro.ui.components.BoneTile
import com.prateek.taro.ui.components.Fact
import com.prateek.taro.ui.components.GridCell
import com.prateek.taro.ui.components.GridDays
import com.prateek.taro.ui.components.GridHeading
import com.prateek.taro.ui.components.GridRow
import com.prateek.taro.ui.components.IconDisc
import com.prateek.taro.ui.components.KeyFact
import com.prateek.taro.ui.components.LinkText
import com.prateek.taro.ui.components.Panel
import com.prateek.taro.ui.components.RowDivider
import com.prateek.taro.ui.components.SectionLabel
import com.prateek.taro.ui.components.Skeleton
import com.prateek.taro.ui.components.StepSwitcher
import com.prateek.taro.ui.components.TextTabs
import com.prateek.taro.ui.components.infoOf
import com.prateek.taro.ui.components.rememberBarGrowth
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Util
import java.time.LocalDate
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

private enum class Area { STEPS, MOVING, EATEN, PROTEIN, SLEEP, WEIGHT }

private data class TotalFact(val value: String, val label: String, val color: Color, val explained: Boolean = false)

private data class ReportView(val report: PeriodReport, val facts: StoryFacts?)

private fun kcal(value: Double) = Util.formatSteps(value.roundToInt())

private fun compact(value: Double): String =
    if (value < 1000) value.roundToInt().toString() else "%.1fk".format(Locale.getDefault(), value / 1000)

private fun clock(minutes: Long) = "%d:%02d".format(Locale.getDefault(), minutes / 60, minutes % 60)

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
    val spacing = TaroTheme.spacing
    val colors = TaroTheme.colors
    var kind by rememberSaveable { mutableStateOf(initial?.kind ?: ReportKind.WEEK) }
    var start by rememberSaveable {
        mutableStateOf((initial ?: ReportData.latestComplete(ReportKind.WEEK)).start.toString())
    }
    val period = Period(kind, LocalDate.parse(start))
    val today = Util.logicalToday()
    val navigator = LocalNavigator.currentOrThrow
    val view = rememberInBackground(period, dataVersion) {
        ReportData.load(context, period)?.let { report ->
            ReportView(report, if (report.steps > 0) runCatching { StoryData.load(context, report) }.getOrNull() else null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        AppOverflowMenu(
            back = true,
            onBack = onBack,
            center = {
                StepSwitcher(
                    label = periodLabel(period),
                    onPrevious = { start = period.previous().start.toString() },
                    onNext = { start = period.next().start.toString() },
                    nextEnabled = !period.contains(today),
                    previousDescription = stringResource(R.string.report_previous),
                    nextDescription = stringResource(R.string.report_next),
                )
            },
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.gap),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.loose, end = spacing.loose, bottom = spacing.loose),
        ) {
            TextTabs(
                options = listOf(ReportKind.WEEK to stringResource(R.string.report_week), ReportKind.MONTH to stringResource(R.string.report_month)),
                selected = kind,
                onSelect = {
                    kind = it
                    start = ReportData.latestComplete(it).start.toString()
                },
                color = colors.goal,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            val current = view?.value
            when {
                view == null -> ReportSkeleton()
                current == null || current.report.steps == 0 && current.report.loggedDays == 0 -> BentoNote(
                    stringResource(R.string.report_empty),
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.wide),
                )
                else -> ReportBody(current, onWatch = { navigator.push(ReportStoryScreen(period.key)) }, onDay = { navigator.push(DayScreen(it.toString())) })
            }
        }
    }
}

@Composable
private fun ReportSkeleton() {
    val spacing = TaroTheme.spacing
    Skeleton {
        BoneTile(Modifier.fillMaxWidth()) {
            BoneText(TaroTheme.text.greeting, 0.6f)
            BoneText(TaroTheme.text.note, 0.9f)
            BoneText(TaroTheme.text.note, 0.4f)
        }
        BoneTile(Modifier.fillMaxWidth().aspectRatio(SKELETON_GRID).padding(top = spacing.gap))
        BoneTile(Modifier.fillMaxWidth().aspectRatio(SKELETON_FACTS).padding(top = spacing.gap))
    }
}

@Composable
private fun ReportBody(view: ReportView, onWatch: () -> Unit, onDay: (LocalDate) -> Unit) {
    val report = view.report
    val facts = view.facts
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val kind = report.inputs.period.kind
    val week = kind == ReportKind.WEEK
    val word = stringResource(if (week) R.string.story_week else R.string.story_month)
    val tint = facts?.let { plotColor(it.plot) } ?: colors.goal

    BentoTile(color = tint, tinted = true, modifier = Modifier.fillMaxWidth()) {
        Text(
            facts?.let { plotTitle(it.plot, word) } ?: stringResource(if (week) R.string.report_card_week else R.string.report_card_month),
            style = TaroTheme.text.greeting,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val lines = report.highlights.take(2).map { highlightText(it, kind) }
        BentoNote(lines.joinToString(" "), Modifier.padding(top = spacing.tight))
        if (report.inProgress) BentoNote(stringResource(R.string.report_so_far), Modifier.padding(top = spacing.hair))
        if (report.steps > 0) {
            LinkText(
                text = stringResource(if (week) R.string.story_play_week else R.string.story_play_month),
                color = tint,
                icon = R.drawable.ic_play,
                onClick = onWatch,
                modifier = Modifier.padding(top = spacing.tight),
            )
        }
    }

    DaysGrid(report, madeUp = facts?.turn?.takeIf { it.madeUp }?.turn?.date, onDay = onDay)

    SectionLabel(stringResource(if (week) R.string.report_the_week else R.string.report_the_month))
    TotalsPanel(report)

    if (report.inputs.badges.isNotEmpty()) {
        SectionLabel(stringResource(R.string.report_badges))
        Panel {
            Column(modifier = Modifier.padding(horizontal = spacing.tight)) {
                report.inputs.badges.forEachIndexed { index, badge ->
                    if (index > 0) RowDivider()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.gap),
                        modifier = Modifier.padding(vertical = spacing.tight),
                    ) {
                        IconDisc(R.drawable.ic_badge_medal, colors.special)
                        Text(stringResource(badge), style = TaroTheme.text.meterLabel, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun areaColor(area: Area): Color = when (area) {
    Area.STEPS -> TaroTheme.colors.goal
    Area.MOVING -> TaroTheme.colors.flame
    Area.EATEN -> TaroTheme.colors.special
    Area.PROTEIN -> PROTEIN_BLUE
    Area.SLEEP -> TaroTheme.colors.sleep
    Area.WEIGHT -> MaterialTheme.colorScheme.onSurface
}

@Composable
private fun areaTitle(area: Area): String = stringResource(
    when (area) {
        Area.STEPS -> R.string.report_area_steps
        Area.MOVING -> R.string.report_area_moving
        Area.EATEN -> R.string.report_area_eaten
        Area.PROTEIN -> R.string.report_area_protein
        Area.SLEEP -> R.string.report_area_sleep
        Area.WEIGHT -> R.string.report_area_weight
    }
)

private fun areas(report: PeriodReport): List<Area> = buildList {
    val inputs = report.inputs
    add(Area.STEPS)
    if (inputs.usualActive != null || inputs.days.any { it.activeKcal > 0 }) add(Area.MOVING)
    if (inputs.days.any { it.eaten != null }) {
        add(Area.EATEN)
        if (inputs.proteinTarget > 0) add(Area.PROTEIN)
    }
    if (inputs.nights.isNotEmpty()) add(Area.SLEEP)
    if (inputs.weighIns.isNotEmpty()) add(Area.WEIGHT)
}

private fun cellFor(area: Area, day: DayInput?, report: PeriodReport, madeUp: LocalDate?): GridCell? {
    day ?: return GridCell()
    val inputs = report.inputs
    return when (area) {
        Area.STEPS -> if (day.steps <= 0) GridCell(missing = day.complete) else GridCell(
            text = compact(day.steps.toDouble()),
            fill = if (day.goal > 0) day.steps.toFloat() / day.goal else null,
            hit = day.goal > 0 && day.steps >= day.goal,
            ring = day.date == madeUp,
        )
        Area.MOVING -> if (day.activeKcal <= 0) GridCell() else GridCell(
            text = day.activeKcal.roundToInt().toString(),
            fill = inputs.usualActive?.takeIf { it > 0 }?.let { (day.activeKcal / it).toFloat() },
            hit = inputs.usualActive?.let { day.activeKcal >= it } ?: true,
        )
        Area.EATEN -> day.eaten?.let { eaten ->
            GridCell(
                text = compact(eaten),
                fill = day.burned.takeIf { it > 0 }?.let { (eaten / it).toFloat() },
                hit = day.burned > 0 && eaten <= day.burned,
                warn = day.burned > 0 && eaten > day.burned * OVER_SHARE,
                missing = !day.fullyLogged,
            )
        } ?: GridCell(missing = day.complete)
        Area.PROTEIN -> if (day.eaten == null) GridCell(missing = day.complete) else GridCell(
            text = day.protein.roundToInt().toString(),
            fill = (day.protein / inputs.proteinTarget).toFloat(),
            hit = day.protein >= inputs.proteinTarget,
            missing = !day.fullyLogged,
        )
        Area.SLEEP -> inputs.nights[day.date]?.let { minutes ->
            GridCell(
                text = clock(minutes),
                fill = minutes.toFloat() / SleepInsights.TARGET_MIN,
                hit = minutes >= SleepInsights.TARGET_MIN * GOOD_NIGHT_SHARE,
            )
        } ?: GridCell(missing = day.complete)
        Area.WEIGHT -> inputs.weighIns[day.date]?.let { GridCell(text = "%.1f".format(Locale.getDefault(), Util.kgToDisplay(it))) } ?: GridCell()
    }
}

@Composable
private fun areaHeading(area: Area, report: PeriodReport): Pair<String, String?> {
    val inputs = report.inputs
    val logged = inputs.days.filter { it.eaten != null && it.fullyLogged }
    return when (area) {
        Area.STEPS -> Util.formatSteps(report.steps) to report.goalDaysPossible.takeIf { it > 0 }?.let {
            stringResource(R.string.report_grid_goal_days, report.goalDays, it)
        }
        Area.MOVING -> stringResource(R.string.energy_kcal, kcal(report.activeKcal)) to inputs.usualActive?.let {
            stringResource(R.string.report_grid_usual, kcal(it))
        }
        Area.EATEN -> (logged.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.report_grid_a_day, kcal(it.sumOf { day -> day.eaten ?: 0.0 } / it.size)) } ?: "") to
            pluralStringResource(R.plurals.report_grid_logged, logged.size, logged.size)
        Area.PROTEIN -> (logged.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.report_grid_grams, it.sumOf { day -> day.protein }.div(it.size).roundToInt()) } ?: "") to
            stringResource(R.string.report_grid_target_g, inputs.proteinTarget.roundToInt())
        Area.SLEEP -> stringResource(R.string.report_grid_night, clock(inputs.nights.values.average().toLong())) to
            stringResource(R.string.report_grid_target, clock(SleepInsights.TARGET_MIN))
        Area.WEIGHT -> (report.actualKg?.let { Util.formatWeightChange(it) } ?: "") to
            pluralStringResource(R.plurals.report_grid_weigh_ins, inputs.weighIns.size, inputs.weighIns.size)
    }
}

@Composable
private fun DaysGrid(report: PeriodReport, madeUp: LocalDate?, onDay: (LocalDate) -> Unit) {
    val spacing = TaroTheme.spacing
    val period = report.inputs.period
    val week = period.kind == ReportKind.WEEK
    val byDate = report.inputs.days.associateBy { it.date }
    val shown = areas(report)
    val firstDay = ReportData.firstDayOfWeek()
    val gridStart = if (week) period.start else period.start.minusDays(((period.start.dayOfWeek.value - firstDay.value + 7) % 7).toLong())
    val rows = generateSequence(gridStart) { it.plusWeeks(1) }.takeWhile { !it.isAfter(period.end) }.toList()
    val fallback = report.bestDay?.date ?: report.inputs.days.lastOrNull()?.date ?: period.start
    var pickedKey by rememberSaveable(period.key) { mutableStateOf(fallback.toString()) }
    val picked = LocalDate.parse(pickedKey)
    var areaName by rememberSaveable { mutableStateOf(Area.STEPS.name) }
    val area = Area.valueOf(areaName).takeIf { it in shown } ?: Area.STEPS
    val growth = rememberBarGrowth(period.key to areaName, if (week) DAYS else rows.size * DAYS)
    val pick: (LocalDate) -> Unit = { if (period.contains(it) && byDate.containsKey(it)) pickedKey = it.toString() }

    Panel {
        Column(modifier = Modifier.padding(horizontal = spacing.hair)) {
            if (week) {
                GridDays(
                    labels = (0 until DAYS).map { offset ->
                        val date = period.start.plusDays(offset.toLong())
                        date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()) to date.dayOfMonth.toString()
                    },
                    selected = (picked.toEpochDay() - period.start.toEpochDay()).toInt(),
                    color = TaroTheme.colors.goal,
                    onPick = { pick(period.start.plusDays(it.toLong())) },
                )
                shown.forEach { row ->
                    val (value, note) = areaHeading(row, report)
                    GridHeading(areaColor(row), areaTitle(row), value, note, Modifier.padding(top = spacing.loose, bottom = spacing.tight))
                    GridRow(
                        cells = (0 until DAYS).map { cellFor(row, byDate[period.start.plusDays(it.toLong())], report, madeUp) },
                        color = areaColor(row),
                        selected = (picked.toEpochDay() - period.start.toEpochDay()).toInt(),
                        progress = growth,
                        onPick = { pick(period.start.plusDays(it.toLong())) },
                    )
                }
            } else {
                TextTabs(
                    options = shown.map { it.name to areaTitle(it) },
                    selected = area.name,
                    onSelect = { areaName = it },
                    color = areaColor(area),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                val (value, note) = areaHeading(area, report)
                GridHeading(areaColor(area), areaTitle(area), value, note, Modifier.padding(top = spacing.gap, bottom = spacing.tight))
                GridDays(
                    labels = (0 until DAYS).map { gridStart.plusDays(it.toLong()).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()) to "" },
                    selected = null,
                    color = areaColor(area),
                )
                rows.forEach { weekStart ->
                    val dates = (0 until DAYS).map { weekStart.plusDays(it.toLong()) }
                    GridRow(
                        cells = dates.map { if (period.contains(it)) cellFor(area, byDate[it], report, madeUp) else null },
                        color = areaColor(area),
                        selected = dates.indexOf(picked).takeIf { it >= 0 },
                        progress = growth,
                        onPick = { pick(dates[it]) },
                        modifier = Modifier.padding(top = spacing.tight),
                    )
                }
            }
            byDate[picked]?.let { day -> PickedDay(day, report, onDay) }
        }
    }
}

@Composable
private fun PickedDay(day: DayInput, report: PeriodReport, onDay: (LocalDate) -> Unit) {
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val inputs = report.inputs
    val format = remember { DateTimeFormatter.ofPattern("EEEE d", Locale.getDefault()) }
    RowDivider(Modifier.padding(top = spacing.loose))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = spacing.gap)) {
        Text(day.date.format(format), style = TaroTheme.text.meterLabel, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        LinkText(stringResource(R.string.report_your_day), colors.accent, onClick = { onDay(day.date) }, trailingIcon = R.drawable.ic_chevron_right)
    }
    val items = buildList {
        add(Triple(colors.goal, Util.formatSteps(day.steps), stringResource(R.string.report_pick_steps)))
        add(Triple(colors.flame, kcal(day.activeKcal), stringResource(R.string.report_pick_moving)))
        day.eaten?.let {
            add(
                Triple(
                    colors.special,
                    kcal(it),
                    if (day.burned > 0) stringResource(R.string.report_pick_eaten_of, kcal(day.burned)) else stringResource(R.string.report_pick_eaten),
                )
            )
            add(Triple(PROTEIN_BLUE, stringResource(R.string.report_pick_grams, day.protein.roundToInt()), stringResource(R.string.report_pick_protein)))
        }
        inputs.nights[day.date]?.let { add(Triple(colors.sleep, minutesText(it), stringResource(R.string.report_pick_slept))) }
        inputs.weighIns[day.date]?.let { add(Triple(MaterialTheme.colorScheme.onSurface, Util.formatWeight(it), stringResource(R.string.report_pick_weighed))) }
    }
    items.chunked(FACTS_PER_ROW).forEach { row ->
        Row(modifier = Modifier.padding(top = spacing.gap)) {
            row.forEach { (color, value, label) -> KeyFact(color, value, label, Modifier.weight(1f)) }
            repeat(FACTS_PER_ROW - row.size) { Box(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun TotalsPanel(report: PeriodReport) {
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val ink = MaterialTheme.colorScheme.onSurface
    val week = report.inputs.period.kind == ReportKind.WEEK
    val last = stringResource(if (week) R.string.report_last_week else R.string.report_last_month)
    val nights = report.inputs.nightMinutes
    val facts = buildList<TotalFact> {
        add(
            TotalFact(
                Util.formatSteps(report.steps),
                report.stepsChange?.let { stringResource(R.string.report_fact_steps_change, (if (it >= 0) "+" else "−") + abs(it) + "%", last) } ?: stringResource(R.string.report_pick_steps),
                colors.goal,
            )
        )
        if (report.goalDaysPossible > 0) add(TotalFact(stringResource(R.string.report_n_of, report.goalDays, report.goalDaysPossible), stringResource(R.string.report_fact_goal_days), colors.goal))
        add(TotalFact("%.1f %s".format(Locale.getDefault(), Util.metersToDistance(report.distanceM), Util.distanceUnit()), stringResource(R.string.report_distance).lowercase(), ink))
        report.expectedKg?.let { add(TotalFact((if (report.balance > 0) "+" else "−") + kcal(abs(report.balance)), stringResource(R.string.report_fact_balance), if (report.balance > 0) colors.flame else colors.goal, explained = true)) }
        report.actualKg?.let { add(TotalFact(Util.formatWeightChange(it), stringResource(R.string.report_fact_trend), ink)) }
        if (nights.isNotEmpty()) add(TotalFact(minutesText(nights.average().toLong()), stringResource(R.string.report_fact_night), colors.sleep))
        report.bestScore?.let { add(TotalFact(Util.formatSteps(it.second), stringResource(R.string.report_fact_score), colors.special)) }
    }
    Panel {
        Column(modifier = Modifier.padding(horizontal = spacing.hair)) {
            facts.chunked(FACTS_PER_ROW).forEachIndexed { index, row ->
                Row(modifier = Modifier.padding(top = if (index > 0) spacing.gap else spacing.hair)) {
                    row.forEach { fact ->
                        Fact(
                            fact.value,
                            fact.label,
                            fact.color,
                            Modifier.weight(1f),
                            info = if (fact.explained) infoOf(R.string.report_energy, R.string.info_report_energy) else null,
                            style = type.factCompact,
                            alignment = Alignment.CenterStart,
                        )
                    }
                    repeat(FACTS_PER_ROW - row.size) { Box(Modifier.weight(1f)) }
                }
            }
            report.verdict?.let { verdict ->
                val (text, color) = when (verdict) {
                    WeightVerdict.MATCHES -> stringResource(R.string.report_verdict_matches) to colors.goal
                    WeightVerdict.LOST_MORE -> stringResource(R.string.report_verdict_more) to colors.special
                    WeightVerdict.LOST_LESS -> stringResource(R.string.report_verdict_less) to colors.flame
                }
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(spacing.tight), modifier = Modifier.padding(top = spacing.loose)) {
                    val dot = with(LocalDensity.current) { type.note.fontSize.toDp() * DOT_SHARE }
                    Box(
                        Modifier
                            .padding(top = spacing.tight)
                            .size(dot)
                            .clip(CircleShape)
                            .drawBehind { drawRect(color) }
                    )
                    Text(text, style = type.note, color = ink)
                }
            }
            if (report.inputs.topFoods.isNotEmpty()) {
                BentoNote(stringResource(R.string.report_top_foods_line, report.inputs.topFoods.joinToString(", ")), Modifier.padding(top = spacing.gap))
            }
            report.inputs.napMinutes.takeIf { it.isNotEmpty() }?.let { naps ->
                BentoNote(pluralStringResource(R.plurals.report_naps_line, naps.size, naps.size, minutesText(naps.sum())), Modifier.padding(top = spacing.hair))
            }
            report.inputs.bedtimeSpread?.let {
                BentoNote(stringResource(R.string.report_spread_line, minutesText(it.toLong())), Modifier.padding(top = spacing.hair))
            }
        }
    }
}

@Composable
fun ReportCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    val rootNavigator = navigator.parent ?: navigator
    val dataVersion = rememberDataVersion()
    val spacing = TaroTheme.spacing
    val colors = TaroTheme.colors
    val seen by AppPreferences.reportsSeenFlow().collectAsStateWithLifecycle(AppPreferences.reportsSeen)
    val due = rememberInBackground(dataVersion, seen) {
        val today = Util.logicalToday()
        val month = ReportData.latestComplete(ReportKind.MONTH, today).takeIf { today.dayOfMonth <= 7 }
        val week = ReportData.latestComplete(ReportKind.WEEK, today)
        listOfNotNull(month, week).firstOrNull { it.key !in seen && ReportData.stepsIn(context, it) > 0 }
            ?.let { it to ReportData.stepsIn(context, it) }
    }?.value ?: return
    val (period, steps) = due
    val week = period.kind == ReportKind.WEEK

    BentoTile(color = colors.goal, tinted = true, modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(if (week) R.string.report_card_week else R.string.report_card_month), style = TaroTheme.text.title, color = MaterialTheme.colorScheme.onSurface)
                Text(periodLabel(period), style = TaroTheme.text.meterValue, color = colors.goal, modifier = Modifier.padding(top = spacing.hair))
                BentoNote(stringResource(R.string.report_card_steps_only, Util.formatSteps(steps)), Modifier.padding(top = spacing.hair))
            }
            IconButton(onClick = { AppPreferences.markReportSeen(period.key) }) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.report_dismiss), tint = colors.accent)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.wide), modifier = Modifier.padding(top = spacing.tight)) {
            LinkText(
                text = stringResource(R.string.report_watch),
                color = colors.goal,
                icon = R.drawable.ic_play,
                onClick = {
                    AppPreferences.markReportSeen(period.key)
                    rootNavigator.push(ReportStoryScreen(period.key))
                },
            )
            LinkText(
                text = stringResource(R.string.report_open),
                color = MaterialTheme.colorScheme.onSurface,
                trailingIcon = R.drawable.ic_chevron_right,
                onClick = {
                    AppPreferences.markReportSeen(period.key)
                    rootNavigator.push(ReportScreen(period.key))
                },
            )
        }
    }
}

private const val DAYS = 7
private const val FACTS_PER_ROW = 3
private const val OVER_SHARE = 1.1
private const val GOOD_NIGHT_SHARE = 0.875
private const val DOT_SHARE = 0.55f
private const val SKELETON_GRID = 0.75f
private const val SKELETON_FACTS = 2.4f
