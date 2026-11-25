package com.prateek.taro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prateek.taro.R
import com.prateek.taro.energy.DailyEnergy
import com.prateek.taro.ui.components.BarPart
import com.prateek.taro.ui.components.HourBar
import com.prateek.taro.ui.components.HourlyChart
import com.prateek.taro.ui.components.KeyFact
import com.prateek.taro.ui.components.Panel
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Util
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

private const val HOUR_MS = 3_600_000L
private const val HOUR_CHART_ASPECT = 2.1f
private const val HOUR_PANEL_ASPECT = 1.4f
private const val RESTING_ALPHA = 0.22f
private const val MINUTES_PER_HOUR = 60.0

@Composable
fun restingTone(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = RESTING_ALPHA).compositeOver(TaroTheme.colors.background)

@Composable
private fun rememberHourLabel(): (Long) -> String {
    val context = LocalContext.current
    val format = remember { android.text.format.DateFormat.getTimeFormat(context) }
    return { millis -> format.format(Date(millis)) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BurnByHourPanel(date: LocalDate, usual: DoubleArray?, refreshKey: Any?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dataVersion = rememberDataVersion()
    val now = System.currentTimeMillis()
    val factors by AppPreferences.energyFactorsFlow().collectAsStateWithLifecycle(AppPreferences.energyFactors)
    val timeline = rememberInBackground(date, refreshKey, dataVersion, factors) { DailyEnergy.timeline(context, date, factors, now) }?.value
    var selected by remember(date) { mutableStateOf<Int?>(null) }
    if (timeline == null) {
        Panel(modifier = modifier.fillMaxWidth().aspectRatio(HOUR_PANEL_ASPECT)) {}
        return
    }
    val colors = TaroTheme.colors
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val resting = restingTone()
    val hourLabel = rememberHourLabel()
    val hours = timeline.hours
    val nowAt = now.takeIf { it in timeline.start until timeline.end }
    val nowHour = nowAt?.let { (it - timeline.start).toDouble() / HOUR_MS }

    fun expected(index: Int) = timeline.restingHour + factors.activity * (usual?.getOrNull(index) ?: 0.0)
    fun elapsedMinutes(index: Int) = MINUTES_PER_HOUR * (nowHour?.let { (it - index).coerceIn(0.0, 1.0) } ?: 1.0)
    fun sleepShare(index: Int) = elapsedMinutes(index).takeIf { it > 0 }?.let { (hours[index].sleepMinutes / it).coerceIn(0.0, 1.0) } ?: 0.0
    fun kcal(value: Double) = Util.formatSteps(value.roundToInt())

    val showUsual = nowHour != null && usual != null
    val bars = hours.mapIndexed { index, hour ->
        val share = sleepShare(index)
        HourBar(
            parts = listOf(
                BarPart(hour.resting * share, colors.sleep),
                BarPart(hour.resting * (1 - share), resting),
                BarPart(hour.active, colors.flame),
                BarPart(hour.logged, colors.special),
            ),
            ghost = if (showUsual && index >= floor(nowHour!!)) expected(index) else 0.0,
        )
    }
    val burned = hours.sumOf { it.burn }
    val byMidnight = if (showUsual) {
        burned + hours.indices.sumOf { index -> expected(index) * (index + 1 - maxOf(nowHour!!, index.toDouble())).coerceIn(0.0, 1.0) }
    } else {
        null
    }
    val pick = selected?.let { hours.getOrNull(it) }
    val dayLabel = remember(date) { date.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())) }
    val shown = pick?.let { listOf(it) } ?: hours
    val asleep = shown.sumOf { it.sleepMinutes }.toLong()
    val steps = shown.sumOf { it.steps }

    Panel(modifier = modifier) {
        Column(modifier = Modifier.padding(spacing.tight)) {
            Text(
                text = when {
                    pick != null -> stringResource(R.string.timeline_hour_span, hourLabel(pick.start), hourLabel(pick.start + HOUR_MS))
                    nowAt != null -> stringResource(R.string.timeline_so_far)
                    else -> dayLabel
                },
                style = type.note,
                color = colors.accent,
            )
            Row {
                Text(
                    text = kcal(pick?.burn ?: burned),
                    style = type.fact,
                    color = if (pick != null) colors.flame else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = stringResource(R.string.home_kcal_unit),
                    style = type.unit,
                    color = colors.accent,
                    modifier = Modifier.alignByBaseline().padding(start = spacing.tight),
                )
                Spacer(modifier = Modifier.weight(1f))
                if (pick == null && byMidnight != null) {
                    Text(
                        text = stringResource(R.string.timeline_by_midnight, kcal(byMidnight)),
                        style = type.note,
                        color = colors.accent,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
            HourlyChart(
                bars = bars,
                start = timeline.start,
                selected = selected,
                onSelect = { selected = it },
                now = nowAt,
                appearKey = date,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(HOUR_CHART_ASPECT)
                    .padding(top = spacing.gap),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.loose),
                verticalArrangement = Arrangement.spacedBy(spacing.gap),
                modifier = Modifier.padding(top = spacing.gap),
            ) {
                val moving = if (pick != null && steps > 0) {
                    stringResource(R.string.timeline_moving_steps, Util.formatSteps(steps))
                } else {
                    stringResource(R.string.timeline_moving)
                }
                KeyFact(colors.flame, kcal(shown.sumOf { it.active }), moving)
                shown.sumOf { it.logged }.takeIf { it.roundToInt() > 0 }?.let { KeyFact(colors.special, kcal(it), stringResource(R.string.food_workout)) }
                KeyFact(resting, kcal(shown.sumOf { it.resting }), stringResource(R.string.timeline_resting))
                if (asleep > 0) KeyFact(colors.sleep, minutesText(asleep), stringResource(R.string.timeline_asleep))
            }
        }
    }
}
