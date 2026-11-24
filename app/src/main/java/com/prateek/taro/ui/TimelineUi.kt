package com.prateek.taro.ui

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prateek.taro.util.AppPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.prateek.taro.R
import com.prateek.taro.energy.DailyEnergy
import com.prateek.taro.energy.DayTimeline
import com.prateek.taro.ui.components.HourSeries
import com.prateek.taro.ui.components.HourlyChart
import com.prateek.taro.ui.components.LegendItem
import com.prateek.taro.ui.components.BentoNote
import com.prateek.taro.ui.components.Panel
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.Database
import com.prateek.taro.util.Util
import com.prateek.taro.util.Walks
import java.time.LocalDate
import java.util.Date
import kotlin.math.roundToInt

private const val HOUR_MS = 3_600_000L
private const val HOUR_CHART_ASPECT = 2.3f
private const val HOUR_PANEL_ASPECT = 1.7f

@Composable
private fun rememberHourLabel(): (Long) -> String {
    val context = LocalContext.current
    val format = remember { android.text.format.DateFormat.getTimeFormat(context) }
    return { millis -> format.format(Date(millis)) }
}

private fun DayTimeline.sleepSpans() = sleep.map { it.start to it.end }

private fun DayTimeline.nowOrNull(now: Long) = now.takeIf { it in start until end }

@Composable
private fun Readout(text: String) {
    Text(
        text = text,
        style = TaroTheme.text.meterValue,
        modifier = Modifier.padding(start = TaroTheme.spacing.tight, end = TaroTheme.spacing.tight, top = TaroTheme.spacing.hair, bottom = TaroTheme.spacing.tight),
    )
}

@Composable
fun StepsByHourPanel(date: LocalDate, refreshKey: Any?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dataVersion = rememberDataVersion()
    val now = System.currentTimeMillis()
    val factors by AppPreferences.energyFactorsFlow().collectAsStateWithLifecycle(AppPreferences.energyFactors)
    val loaded = rememberInBackground(date, refreshKey, dataVersion, factors) {
        val day = date.toString()
        DailyEnergy.timeline(context, date, factors, now) to
            Walks.all(Database.getInstance(context).getMinutes(day, day).map { it.minuteStart to it.steps }).maxByOrNull { it.minutes }
    }?.value
    var selected by remember(date) { mutableStateOf<Int?>(null) }
    if (loaded == null) {
        Panel(modifier = modifier.fillMaxWidth().aspectRatio(HOUR_PANEL_ASPECT)) {}
        return
    }
    val (timeline, longest) = loaded
    val hourLabel = rememberHourLabel()
    val steps = timeline.hours.map { it.steps.toDouble() }

    Panel(modifier = modifier) {
        val peak = timeline.hours.maxByOrNull { it.steps }?.takeIf { it.steps > 0 }
        val readout = selected?.let { index ->
            val hour = timeline.hours[index]
            stringResource(R.string.timeline_steps_at, hourLabel(hour.start), hourLabel(hour.start + HOUR_MS), Util.formatSteps(hour.steps))
        } ?: peak?.let {
            val busiest = stringResource(R.string.timeline_busiest, hourLabel(it.start))
            longest?.let { walk -> "$busiest ${stringResource(R.string.timeline_longest_walk, walk.minutes, hourLabel(walk.start))}" } ?: busiest
        } ?: stringResource(R.string.timeline_steps_empty)
        Readout(readout)
        HourlyChart(
            series = listOf(HourSeries(steps, TaroTheme.colors.goal)),
            start = timeline.start,
            sleep = timeline.sleepSpans(),
            selected = selected,
            onSelect = { selected = it },
            area = true,
            now = timeline.nowOrNull(now),
            appearKey = date,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(HOUR_CHART_ASPECT)
                .padding(horizontal = TaroTheme.spacing.tight),
        )
    }
}

@Composable
fun EnergyByHourPanel(date: LocalDate, dayNeed: Double?, refreshKey: Any?, modifier: Modifier = Modifier) {
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
    val hourLabel = rememberHourLabel()
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val burn = timeline.hours.map { it.burn }
    val eaten = timeline.hours.map { it.eaten }

    fun kcal(value: Double) = Util.formatSteps(value.roundToInt())

    Panel(modifier = modifier) {
        HourlyChart(
            series = listOf(HourSeries(burn, colors.flame), HourSeries(eaten, colors.special, events = true)),
            start = timeline.start,
            sleep = timeline.sleepSpans(),
            selected = selected,
            onSelect = { selected = it },
            cumulative = true,
            scaleMax = dayNeed?.let { maxOf(it, eaten.sum()) },
            now = timeline.nowOrNull(now),
            appearKey = date,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(HOUR_CHART_ASPECT)
                .padding(horizontal = spacing.tight, vertical = spacing.tight),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.loose), modifier = Modifier.padding(horizontal = spacing.tight)) {
            LegendItem(colors.flame, stringResource(R.string.timeline_burned), "")
            LegendItem(colors.special, stringResource(R.string.timeline_eaten), "")
            LegendItem(colors.sleep.copy(alpha = 0.5f), stringResource(R.string.timeline_sleep), "")
        }
        BentoNote(
            text = selected?.let { index ->
                stringResource(R.string.timeline_energy_by, hourLabel(timeline.hours[index].start + HOUR_MS), kcal(burn.take(index + 1).sum()), kcal(eaten.take(index + 1).sum()))
            } ?: stringResource(R.string.timeline_energy_total, kcal(burn.sum()), kcal(eaten.sum())),
            modifier = Modifier.padding(start = spacing.tight, end = spacing.tight, top = spacing.tight, bottom = spacing.tight),
        )
    }
}
