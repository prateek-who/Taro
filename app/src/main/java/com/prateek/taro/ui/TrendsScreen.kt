package com.prateek.taro.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.prateek.taro.R
import androidx.compose.runtime.remember
import com.prateek.taro.energy.WeightTrend
import com.prateek.taro.report.Period
import com.prateek.taro.report.ReportData
import com.prateek.taro.report.ReportKind
import com.prateek.taro.ui.components.BentoNote
import com.prateek.taro.ui.components.BentoTile
import com.prateek.taro.ui.components.BentoValue
import com.prateek.taro.ui.components.Fact
import com.prateek.taro.ui.components.LinkText
import com.prateek.taro.ui.components.MeterBar
import com.prateek.taro.ui.components.PairedBars
import com.prateek.taro.ui.components.Panel
import com.prateek.taro.ui.components.PillSelector
import com.prateek.taro.ui.components.SectionLabel
import com.prateek.taro.ui.components.Sparkline
import com.prateek.taro.ui.components.WeightForecast
import com.prateek.taro.ui.components.WeightTrendChart
import com.prateek.taro.ui.components.infoOf
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.Util
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

object TrendsTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 1u,
            title = stringResource(R.string.tab_trends),
            icon = painterResource(R.drawable.ic_trends),
        )

    @Composable
    override fun Content() {
        TrendsContent()
    }
}

private const val SPARK_ASPECT = 2.8f
private const val BALANCE_SHARE = 0.42f
private const val BALANCE_ASPECT = 1.9f
private const val WEIGHT_ASPECT = 2.2f
private const val STEADY_KG = 0.2
private const val EVEN_KCAL = 50
private const val SAME_SHARE = 0.03
private const val SAME_SLEEP_MIN = 10
private const val FORECAST_STEP = 7
private const val FORECAST_WEEKS = 4
private const val STALE_DAYS = 14L
private const val PLACEHOLDER_ASPECT = 0.9f
private const val DIVIDER_ALPHA = 0.08f
private const val BEST_COLUMNS = 2

private fun kcal(value: Double) = Util.formatSteps(value.roundToInt())

@Composable
private fun TrendsContent() {
    val context = LocalContext.current
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val colors = TaroTheme.colors
    val navigator = LocalNavigator.currentOrThrow
    val root = navigator.parent ?: navigator
    val quickLog = rememberQuickLog()
    val today = Util.logicalToday()
    var range by rememberSaveable { mutableStateOf(TrendRange.MONTH) }
    val dataVersion = rememberDataVersion()
    val summary = rememberInBackground(range, today, dataVersion) { TrendsData.load(context, range, today) }?.value
    val insights by rememberBodyInsights(today)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        AppOverflowMenu(title = stringResource(R.string.tab_trends), actions = quickLogActions(quickLog, today))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.loose, end = spacing.loose, bottom = spacing.loose),
        ) {
            PillSelector(
                options = TrendRange.entries.map { it to stringResource(it.label) },
                selected = range,
                onSelect = { range = it },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            if (summary == null || summary.range != range) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(PLACEHOLDER_ASPECT))
                return@Column
            }
            Text(
                text = sentence(summary),
                style = type.title,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = spacing.hair, end = spacing.hair, top = spacing.loose, bottom = spacing.gap),
            )
            WeightTile(summary, insights?.model, onClick = { root.push(WeightScreen) }, onLog = { quickLog.weight = true })
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.gap),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(top = spacing.gap),
            ) {
                StepsTrendTile(summary, Modifier.weight(1f).fillMaxHeight()) { root.push(StepsPage) }
                SleepTrendTile(summary, Modifier.weight(1f).fillMaxHeight()) { root.push(SleepPage) }
            }
            BalanceTile(summary, Modifier.padding(top = spacing.gap)) { root.push(FoodPage) }
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.gap),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(top = spacing.gap),
            ) {
                ProteinTrendTile(summary, Modifier.weight(1f).fillMaxHeight()) { root.push(FoodPage) }
                MovingTrendTile(summary, Modifier.weight(1f).fillMaxHeight()) { root.push(StepsPage) }
            }

            SectionLabel(stringResource(R.string.body_title), info = infoOf(R.string.info_body_title, R.string.info_body))
            BodyFactors(insights?.model, loaded = insights != null)

            SectionLabel(stringResource(R.string.trends_bests, stringResource(range.span)))
            BestsPanel(summary)

            SectionLabel(stringResource(R.string.report_title))
            ReportsPanel(today, onWatch = { root.push(ReportStoryScreen(it)) }, onOpen = { root.push(ReportScreen(it)) })
        }
    }

    QuickLogDialogs(quickLog, today)
}

@Composable
private fun sentence(summary: TrendSummary): AnnotatedString {
    val colors = TaroTheme.colors
    val span = stringResource(summary.range.span)
    val change = summary.weightChange
    val balance = summary.balance
    val parts = mutableListOf<Pair<String, Color>>()
    val first = when {
        change == null -> null
        abs(change) < STEADY_KG -> stringResource(R.string.trends_say_steady, span)
        else -> {
            val amount = Util.formatWeight(abs(change))
            parts += amount to colors.goal
            stringResource(if (change < 0) R.string.trends_say_down else R.string.trends_say_up, amount, span)
        }
    }
    val text = when {
        first != null && balance != null -> first + when {
            abs(balance) < EVEN_KCAL -> stringResource(R.string.trends_say_even)
            else -> {
                val amount = kcal(abs(balance))
                parts += amount to colors.special
                stringResource(if (balance < 0) R.string.trends_say_under else R.string.trends_say_over, amount)
            }
        }
        first != null -> "$first."
        summary.steps != null -> {
            val amount = Util.formatSteps(summary.steps)
            parts += amount to colors.goal
            stringResource(R.string.trends_say_steps, amount, span)
        }
        else -> stringResource(R.string.trends_say_empty)
    }
    return buildAnnotatedString {
        append(text)
        parts.forEach { (part, color) ->
            val at = text.indexOf(part)
            if (at >= 0) addStyle(SpanStyle(color = color), at, at + part.length)
        }
    }
}

@Composable
private fun WeightTile(summary: TrendSummary, model: com.prateek.taro.energy.BodyModel?, onClick: () -> Unit, onLog: () -> Unit) {
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    BentoTile(colors.goal, label = stringResource(R.string.weight_title), tinted = true, onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        val points = model.tissueTrend(summary.weights)
        if (points.isEmpty()) {
            BentoNote(stringResource(R.string.trends_weight_empty))
            LinkText(stringResource(R.string.weight_log), colors.goal, onClick = onLog, icon = R.drawable.ic_add)
            return@BentoTile
        }
        val latest = points.last()
        val sd = model?.latest?.tissueSd
        val today = Util.logicalToday()
        val stale = latest.date.takeIf { it.isBefore(today.minusDays(STALE_DAYS)) }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                BentoValue(Util.formatWeight(latest.trend), MaterialTheme.colorScheme.onSurface)
                sd?.let { BentoNote(stringResource(R.string.trends_weight_likely, Util.formatWeight(latest.trend - it), Util.formatWeight(latest.trend + it))) }
            }
            WeightTrend.weeklyRate(points)?.takeIf { stale == null }?.let { rate ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(Util.formatWeightChange(rate), style = type.fact, color = colors.goal)
                    BentoNote(stringResource(R.string.trends_weight_week))
                }
            }
        }
        val start = minOf(today, latest.date).minusDays(summary.range.days.toLong())
        val shown = points.filter { !it.date.isBefore(start) }
        val daily = model?.dailyChangeKg()
        val forecast = if (model != null && daily != null && stale == null) {
            val needSd = model.factors.needSd(model.restingAtTissue, model.recentActive, model.recentWorkouts)
            (1..FORECAST_WEEKS).map { week ->
                val days = week * FORECAST_STEP
                WeightForecast(
                    date = latest.date.plusDays(days.toLong()),
                    kg = latest.trend + daily * days,
                    sd = sqrt((sd ?: 0.0) * (sd ?: 0.0) + (needSd * days / com.prateek.taro.energy.Metabolism.KCAL_PER_KG).let { it * it }),
                )
            }
        } else {
            emptyList()
        }
        var selected by remember(shown, forecast) { mutableStateOf<Int?>(null) }
        val day = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
        Text(
            text = selected?.let { index ->
                shown.getOrNull(index)?.let { stringResource(R.string.trends_weight_point, it.date.format(day), Util.formatWeight(it.kg), Util.formatWeight(it.trend)) }
                    ?: forecast[index - shown.size].let {
                        stringResource(R.string.trends_weight_forecast_point, it.date.format(day), Util.formatWeight(it.kg), Util.formatWeight(it.kg - it.sd), Util.formatWeight(it.kg + it.sd))
                    }
            } ?: stringResource(R.string.trends_weight_hint),
            style = type.meterValue,
            color = if (selected == null) colors.accent else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = spacing.gap),
        )
        WeightTrendChart(
            points = shown,
            forecast = forecast,
            color = colors.goal,
            todayLabel = stringResource(R.string.trends_today),
            toDisplay = Util::kgToDisplay,
            selected = selected,
            onSelect = { selected = it },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(WEIGHT_ASPECT)
                .padding(top = spacing.tight),
        )
        BentoNote(
            forecast.lastOrNull()?.let { stringResource(R.string.trends_weight_course, Util.formatWeight(it.kg), it.date.format(day)) }
                ?: stringResource(R.string.trends_forecast_wait),
            Modifier.padding(top = spacing.tight),
        )
        stale?.let {
            BentoNote(stringResource(R.string.trends_weight_stale, it.format(day)), Modifier.padding(top = spacing.tight))
            LinkText(stringResource(R.string.weight_log), colors.goal, onClick = onLog, icon = R.drawable.ic_add)
        }
    }
}

@Composable
private fun change(now: Int?, before: Int?, span: String): Pair<String, Color>? {
    if (now == null || before == null || before == 0) return null
    val difference = now - before
    return when {
        abs(difference) <= before * SAME_SHARE -> stringResource(R.string.trends_same, span) to TaroTheme.colors.accent
        difference > 0 -> stringResource(R.string.trends_steps_up, Util.formatSteps(difference), span) to TaroTheme.colors.goal
        else -> stringResource(R.string.trends_steps_down, Util.formatSteps(-difference), span) to TaroTheme.colors.accent
    }
}

@Composable
private fun StepsTrendTile(summary: TrendSummary, modifier: Modifier, onClick: () -> Unit) {
    val colors = TaroTheme.colors
    val span = stringResource(summary.range.span)
    BentoTile(colors.goal, modifier = modifier, label = stringResource(R.string.tab_steps), tinted = true, onClick = onClick) {
        BentoValue(summary.steps?.let { Util.formatSteps(it) } ?: "-", MaterialTheme.colorScheme.onSurface, unit = stringResource(R.string.trends_a_day))
        change(summary.steps, summary.stepsBefore, span)?.let { (text, color) -> BentoNote(text, color = color) }
        Spacer()
        PairedBars(
            values = summary.stepsSeries.map { (it ?: 0.0) to null },
            first = colors.goal,
            second = colors.goal,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(SPARK_ASPECT)
                .padding(top = TaroTheme.spacing.gap),
        )
    }
}

@Composable
private fun SleepTrendTile(summary: TrendSummary, modifier: Modifier, onClick: () -> Unit) {
    val colors = TaroTheme.colors
    val span = stringResource(summary.range.span)
    BentoTile(colors.sleep, modifier = modifier, label = stringResource(R.string.tab_sleep), tinted = true, onClick = onClick) {
        BentoValue(summary.sleep?.let { minutesText(it) } ?: "-", MaterialTheme.colorScheme.onSurface, unit = stringResource(R.string.trends_a_night))
        val now = summary.sleep
        val before = summary.sleepBefore
        if (now != null && before != null) {
            val difference = now - before
            BentoNote(
                when {
                    abs(difference) < SAME_SLEEP_MIN -> stringResource(R.string.trends_same, span)
                    difference > 0 -> stringResource(R.string.trends_sleep_up, minutesText(difference), span)
                    else -> stringResource(R.string.trends_sleep_down, minutesText(-difference), span)
                },
            )
        }
        Spacer()
        Sparkline(
            values = summary.sleepSeries,
            color = colors.sleep,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(SPARK_ASPECT)
                .padding(top = TaroTheme.spacing.gap),
        )
    }
}

@Composable
private fun BalanceTile(summary: TrendSummary, modifier: Modifier, onClick: () -> Unit) {
    val colors = TaroTheme.colors
    BentoTile(colors.special, modifier = modifier.fillMaxWidth(), label = stringResource(R.string.trends_balance), tinted = true, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                val balance = summary.balance
                if (balance == null) {
                    BentoNote(stringResource(R.string.trends_balance_empty, summary.loggedDays, summary.days))
                } else {
                    val rounded = balance.roundToInt()
                    BentoValue(
                        value = (if (rounded > 0) "+" else if (rounded < 0) "−" else "") + Util.formatSteps(abs(rounded)),
                        color = if (rounded <= 0) colors.goal else colors.special,
                        unit = stringResource(R.string.trends_kcal_a_day),
                    )
                    BentoNote(stringResource(R.string.trends_balance_days, summary.loggedDays, summary.days))
                }
            }
            if (summary.balance != null) PairedBars(
                values = summary.balanceBars,
                first = colors.flame,
                second = colors.special,
                modifier = Modifier
                    .fillMaxWidth(BALANCE_SHARE)
                    .aspectRatio(BALANCE_ASPECT),
            )
        }
    }
}

@Composable
private fun ProteinTrendTile(summary: TrendSummary, modifier: Modifier, onClick: () -> Unit) {
    BentoTile(PROTEIN_BLUE, modifier = modifier, label = stringResource(R.string.food_protein), tinted = true, onClick = onClick) {
        BentoValue(summary.protein?.let { stringResource(R.string.food_macro_value, kcal(it)) } ?: "-", MaterialTheme.colorScheme.onSurface, unit = stringResource(R.string.trends_a_day))
        Spacer()
        if (summary.protein == null) {
            BentoNote(stringResource(R.string.trends_protein_empty))
        } else if (summary.proteinTarget > 0) {
            MeterBar(
                fraction = ((summary.protein ?: 0.0) / summary.proteinTarget).toFloat(),
                color = PROTEIN_BLUE,
                modifier = Modifier.padding(top = TaroTheme.spacing.gap),
            )
            BentoNote(stringResource(R.string.trends_protein_note, kcal(summary.proteinTarget), summary.proteinDays), Modifier.padding(top = TaroTheme.spacing.tight))
        }
    }
}

@Composable
private fun MovingTrendTile(summary: TrendSummary, modifier: Modifier, onClick: () -> Unit) {
    val colors = TaroTheme.colors
    BentoTile(colors.flame, modifier = modifier, label = stringResource(R.string.trends_moving), tinted = true, onClick = onClick) {
        BentoValue(summary.movingKcal?.let { kcal(it) } ?: "-", MaterialTheme.colorScheme.onSurface, unit = stringResource(R.string.trends_kcal_a_day))
        summary.movingKm?.let { BentoNote(stringResource(R.string.trends_moving_km, "%.1f".format(Locale.getDefault(), it))) }
        Spacer()
        Sparkline(
            values = summary.movingSeries,
            color = colors.flame,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(SPARK_ASPECT)
                .padding(top = TaroTheme.spacing.gap),
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Spacer() {
    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
}

@Composable
private fun BestsPanel(summary: TrendSummary) {
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val style = TaroTheme.text.factCompact
    val bests = summary.bests
    val cells: List<@Composable (Modifier) -> Unit> = listOf(
        { Fact(bests.steps?.let { Util.formatSteps(it) } ?: "-", stringResource(R.string.trends_best_day), colors.goal, it, icon = R.drawable.ic_steps, style = style, alignment = Alignment.CenterStart) },
        { Fact(bests.score?.let { Util.formatSteps(it) } ?: "-", stringResource(R.string.trends_best_score), colors.red, it, info = infoOf(R.string.info_score_title, R.string.info_score), icon = R.drawable.ic_badge_star, style = style, alignment = Alignment.CenterStart) },
        { Fact(bests.weekScore?.let { Util.formatSteps(it) } ?: "-", stringResource(R.string.trends_best_week_score), colors.red, it, info = infoOf(R.string.info_score_title, R.string.info_score), icon = R.drawable.ic_badge_medal, style = style, alignment = Alignment.CenterStart) },
        { Fact(bests.goalDays.toString(), pluralStringResource(R.plurals.trends_goal_days, bests.goalDays), colors.special, it, icon = R.drawable.ic_badge_week, style = style, alignment = Alignment.CenterStart) },
        { Fact(bests.nightMinutes?.let { minutesText(it) } ?: "-", stringResource(R.string.trends_longest_night), colors.sleep, it, icon = R.drawable.ic_sleep, style = style, alignment = Alignment.CenterStart) },
        { Fact(if (bests.walkMinutes > 0) minutesText(bests.walkMinutes.toLong()) else "-", stringResource(R.string.trends_longest_walk), colors.goal, it, icon = R.drawable.ic_distance, style = style, alignment = Alignment.CenterStart) },
        { Fact(stringResource(R.string.trends_climb_value, bests.climbM.roundToInt()), stringResource(R.string.trends_most_climb), MaterialTheme.colorScheme.onSurface, it, icon = R.drawable.ic_badge_climb, style = style, alignment = Alignment.CenterStart) },
        { Fact(bests.workouts.toString(), pluralStringResource(R.plurals.trends_workouts, bests.workouts), colors.flame, it, icon = R.drawable.ic_badge_bolt, style = style, alignment = Alignment.CenterStart) },
    )
    Panel {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.gap), modifier = Modifier.padding(vertical = spacing.tight)) {
            cells.chunked(BEST_COLUMNS).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.tight), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { cell -> cell(Modifier.weight(1f)) }
                    repeat(BEST_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ReportsPanel(today: LocalDate, onWatch: (String) -> Unit, onOpen: (String) -> Unit) {
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val week = ReportData.latestComplete(ReportKind.WEEK, today)
    val month = ReportData.latestComplete(ReportKind.MONTH, today)
    val day = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    val monthName = DateTimeFormatter.ofPattern("LLLL", Locale.getDefault())
    Panel {
        Column(modifier = Modifier.padding(horizontal = spacing.tight)) {
            ReportRow(stringResource(R.string.trends_week_story, week.start.format(day)), stringResource(R.string.trends_week_note)) {
                LinkText(stringResource(R.string.trends_watch), colors.goal, onClick = { onWatch(week.key) }, icon = R.drawable.ic_play)
            }
            HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.colorScheme.onSurface.copy(alpha = DIVIDER_ALPHA))
            ReportRow(month.start.format(monthName).replaceFirstChar { it.titlecase(Locale.getDefault()) }, stringResource(R.string.trends_month_note)) {
                LinkText(stringResource(R.string.trends_open), MaterialTheme.colorScheme.onSurface, onClick = { onOpen(month.key) }, trailingIcon = R.drawable.ic_chevron_right)
            }
            HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.colorScheme.onSurface.copy(alpha = DIVIDER_ALPHA))
            LinkText(stringResource(R.string.trends_all_reports), colors.accent, onClick = { onOpen(Period.containing(ReportKind.WEEK, today, ReportData.firstDayOfWeek()).key) }, trailingIcon = R.drawable.ic_chevron_right)
        }
    }
}

@Composable
private fun ReportRow(title: String, note: String, action: @Composable () -> Unit) {
    val spacing = TaroTheme.spacing
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = spacing.tight)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = TaroTheme.text.title, color = MaterialTheme.colorScheme.onSurface)
            BentoNote(note)
        }
        action()
    }
}
