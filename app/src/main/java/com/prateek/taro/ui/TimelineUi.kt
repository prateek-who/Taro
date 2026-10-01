package com.prateek.taro.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.prateek.taro.ui.components.infoOf
import com.prateek.taro.ui.components.InfoBox
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prateek.taro.util.AppPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prateek.taro.R
import com.prateek.taro.energy.DailyEnergy
import com.prateek.taro.energy.DayTimeline
import com.prateek.taro.ui.components.HourSeries
import com.prateek.taro.ui.components.HourlyChart
import com.prateek.taro.ui.components.LegendItem
import com.prateek.taro.ui.components.Panel
import com.prateek.taro.ui.components.ToggleGroup
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.Util
import java.time.LocalDate
import java.util.Date
import kotlin.math.roundToInt

private const val HOUR_MS = 3_600_000L
private val PLACEHOLDER_HEIGHT = 260.dp

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
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
    )
}

@Composable
fun StepsByHourPanel(date: LocalDate, refreshKey: Any?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dataVersion = rememberDataVersion()
    val now = System.currentTimeMillis()
    val factors by AppPreferences.energyFactorsFlow().collectAsStateWithLifecycle(AppPreferences.energyFactors)
    val timeline = rememberInBackground(date, refreshKey, dataVersion, factors) { DailyEnergy.timeline(context, date, factors, now) }?.value
    var selected by remember(date) { mutableStateOf<Int?>(null) }
    if (timeline == null) {
        Panel(modifier = modifier.height(PLACEHOLDER_HEIGHT)) {}
        return
    }
    val hourLabel = rememberHourLabel()
    val steps = timeline.hours.map { it.steps.toDouble() }

    Panel(modifier = modifier) {
        val readout = selected?.let { index ->
            val hour = timeline.hours[index]
            stringResource(R.string.timeline_steps_at, hourLabel(hour.start), hourLabel(hour.start + HOUR_MS), Util.formatSteps(hour.steps))
        } ?: timeline.hours.maxByOrNull { it.steps }?.takeIf { it.steps > 0 }?.let {
            stringResource(R.string.timeline_steps_peak, hourLabel(it.start), Util.formatSteps(it.steps))
        } ?: stringResource(R.string.timeline_steps_empty)
        InfoBox(
            info = infoOf(R.string.timeline_steps_title, R.string.info_steps_hour),
            modifier = Modifier
                .padding(start = 8.dp, top = 4.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = TaroTheme.colors.goal,
        ) {
            Text(
                text = stringResource(R.string.timeline_steps_title).uppercase(),
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold,
                color = TaroTheme.colors.accent,
            )
        }
        Readout(readout)
        HourlyChart(
            series = listOf(HourSeries(steps, TaroTheme.colors.goal)),
            start = timeline.start,
            sleep = timeline.sleepSpans(),
            selected = selected,
            onSelect = { selected = if (it == selected) null else it },
            now = timeline.nowOrNull(now),
            appearKey = date,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(horizontal = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(8.dp)) {
            LegendItem(TaroTheme.colors.goal, stringResource(R.string.timeline_steps), "")
            LegendItem(TaroTheme.colors.sleep.copy(alpha = 0.5f), stringResource(R.string.timeline_sleep), "")
        }
    }
}

private enum class EnergyView { HOURLY, RUNNING }

@Composable
fun EnergyByHourPanel(date: LocalDate, dayNeed: Double?, refreshKey: Any?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dataVersion = rememberDataVersion()
    var view by rememberSaveable { mutableStateOf(EnergyView.HOURLY) }
    val now = System.currentTimeMillis()
    val factors by AppPreferences.energyFactorsFlow().collectAsStateWithLifecycle(AppPreferences.energyFactors)
    val timeline = rememberInBackground(date, refreshKey, dataVersion, factors) { DailyEnergy.timeline(context, date, factors, now) }?.value
    var selected by remember(date, view) { mutableStateOf<Int?>(null) }
    if (timeline == null) {
        Panel(modifier = modifier.height(PLACEHOLDER_HEIGHT)) {}
        return
    }
    val hourLabel = rememberHourLabel()
    val colors = TaroTheme.colors
    val burn = timeline.hours.map { it.burn }
    val eaten = timeline.hours.map { it.eaten }

    fun kcal(value: Double) = Util.formatSteps(value.roundToInt())

    Panel(modifier = modifier) {
        ToggleGroup(
            options = listOf(
                EnergyView.HOURLY to stringResource(R.string.timeline_per_hour),
                EnergyView.RUNNING to stringResource(R.string.timeline_running),
            ),
            selected = view,
            onSelect = { view = it },
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp),
        )
        val readout = selected?.let { index ->
            val hour = timeline.hours[index]
            if (view == EnergyView.HOURLY) {
                stringResource(R.string.timeline_energy_at, hourLabel(hour.start), hourLabel(hour.start + HOUR_MS), kcal(hour.burn), kcal(hour.eaten))
            } else {
                stringResource(
                    R.string.timeline_energy_by,
                    hourLabel(hour.start + HOUR_MS),
                    kcal(burn.take(index + 1).sum()),
                    kcal(eaten.take(index + 1).sum()),
                )
            }
        } ?: stringResource(R.string.timeline_energy_total, kcal(burn.sum()), kcal(eaten.sum()))
        Readout(readout)
        val incompleteDays by AppPreferences.incompleteFoodDaysFlow().collectAsStateWithLifecycle(AppPreferences.incompleteFoodDays)
        if (eaten.sum() > 0) FoodDayChip(date.toString(), incompleteDays, Modifier.padding(start = 8.dp, bottom = 8.dp))
        HourlyChart(
            series = listOf(HourSeries(burn, colors.flame), HourSeries(eaten, colors.goal)),
            start = timeline.start,
            sleep = timeline.sleepSpans(),
            selected = selected,
            onSelect = { selected = if (it == selected) null else it },
            cumulative = view == EnergyView.RUNNING,
            scaleMax = dayNeed?.let { maxOf(it, eaten.sum()) },
            now = timeline.nowOrNull(now),
            appearKey = date,
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .padding(horizontal = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(8.dp)) {
            LegendItem(colors.flame, stringResource(R.string.timeline_burned), "")
            LegendItem(colors.goal, stringResource(R.string.timeline_eaten), "")
            LegendItem(colors.sleep.copy(alpha = 0.5f), stringResource(R.string.timeline_sleep), "")
        }
    }
}
