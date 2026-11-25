package com.prateek.taro.ui

import com.prateek.taro.ui.components.BentoTile
import com.prateek.taro.ui.components.BoneText
import com.prateek.taro.ui.components.Skeleton
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.prateek.taro.ui.components.WeightForecast
import com.prateek.taro.ui.components.WeightTrendChart
import kotlin.math.sqrt
import com.prateek.taro.ui.components.SecondaryButton
import com.prateek.taro.ui.components.PrimaryButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.prateek.taro.energy.UnderLoggedDay
import com.prateek.taro.ui.components.infoOf
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.prateek.taro.R
import kotlin.math.abs
import java.time.format.DateTimeFormatter
import androidx.compose.material3.MaterialTheme
import com.prateek.taro.ui.components.Info
import com.prateek.taro.ui.components.Meter
import com.prateek.taro.ui.components.Fact
import com.prateek.taro.ui.components.BentoNote
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import com.prateek.taro.energy.BodyInsights
import com.prateek.taro.energy.BodyModel
import com.prateek.taro.energy.DailyEnergy
import com.prateek.taro.energy.EnergyFilter
import com.prateek.taro.energy.Metabolism
import com.prateek.taro.energy.TrendPoint
import com.prateek.taro.ui.components.Panel
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val FORECAST_DAYS = 28
private const val LEARNED_SHARE = 0.75

@Composable
fun rememberBodyInsights(vararg keys: Any?): State<BodyInsights?> {
    val context = LocalContext.current
    val dataVersion = rememberDataVersion()
    return produceState<BodyInsights?>(null, dataVersion, *keys) {
        value = withContext(Dispatchers.IO) { DailyEnergy.insights(context) }
        value?.model?.factors?.let { if (it != AppPreferences.energyFactors) AppPreferences.energyFactors = it }
    }
}

fun BodyModel?.tissueTrend(points: List<TrendPoint>): List<TrendPoint> {
    if (this == null) return points
    val tissue = states.associate { it.date to it.tissue }
    return points.map { point -> tissue[point.date]?.let { point.copy(trend = it) } ?: point }
}

private fun percent(factor: Double): String {
    val value = ((factor - 1) * 100).roundToInt()
    return if (value > 0) "+$value%" else "$value%"
}

private fun spread(sd: Double) = "±${(sd * 100).roundToInt()}%"

@Composable
private fun factorValue(factor: Double, sd: Double, prior: Double): String =
    if (sd > prior * LEARNED_SHARE) stringResource(R.string.body_needs_variety) else "${percent(factor)}  ${spread(sd)}"

@Composable
fun UnderLoggedCard(days: List<UnderLoggedDay>, modifier: Modifier = Modifier) {
    val checked by AppPreferences.underLogCheckedFlow().collectAsStateWithLifecycle(emptySet())
    val incomplete by AppPreferences.incompleteFoodDaysFlow().collectAsStateWithLifecycle(emptySet())
    val day = days.firstOrNull { it.date.toString() !in checked && it.date.toString() !in incomplete } ?: return
    val format = remember { SimpleDateFormat("EEE d MMM", Locale.getDefault()) }
    val spacing = TaroTheme.spacing
    BentoTile(color = TaroTheme.colors.special, tinted = true, modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.underlog_title, format.format(Date(Util.dateStringToCalendarMillis(day.date.toString())))),
            style = TaroTheme.text.title,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.underlog_text, Util.formatSteps(day.logged.roundToInt()), Util.formatSteps(day.usual.roundToInt())),
            style = TaroTheme.text.note,
            color = TaroTheme.colors.accent,
            modifier = Modifier.padding(top = spacing.hair),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.gap), modifier = Modifier.padding(top = spacing.loose)) {
            PrimaryButton(
                text = stringResource(R.string.underlog_mark),
                onClick = { AppPreferences.setFoodDayIncomplete(day.date.toString(), true) },
                tint = TaroTheme.colors.special,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = stringResource(R.string.underlog_keep),
                onClick = { AppPreferences.markUnderLogChecked(day.date.toString()) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun BodyFactors(model: BodyModel?, loaded: Boolean, modifier: Modifier = Modifier) {
    val spacing = TaroTheme.spacing
    val colors = TaroTheme.colors
    Panel(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.gap), modifier = Modifier.padding(spacing.tight)) {
            if (model == null && !loaded) {
                Skeleton {
                    repeat(3) {
                        BoneText(TaroTheme.text.meterLabel, 0.4f, Modifier.padding(top = spacing.tight))
                        BoneText(TaroTheme.text.note, 0.8f)
                    }
                }
                return@Column
            }
            if (model == null) {
                BentoNote(stringResource(R.string.body_empty))
                return@Column
            }
            val factors = model.factors
            BentoNote(
                if (model.learning) {
                    stringResource(
                        R.string.body_learning,
                        minOf(model.loggedDays, EnergyFilter.SETTLED_DAYS),
                        EnergyFilter.SETTLED_DAYS,
                        minOf(model.weighIns, EnergyFilter.SETTLED_WEIGH_INS),
                        EnergyFilter.SETTLED_WEIGH_INS,
                    )
                } else {
                    stringResource(R.string.body_settled, model.loggedDays, model.weighIns)
                } + " " + stringResource(R.string.trends_body_note),
            )
            val formulaTotal = Metabolism.withDigestion(model.restingAtTissue + model.recentActive + model.recentWorkouts, factors.digestion)
            FactorMeter(
                label = stringResource(R.string.body_combined),
                factor = factors.need(model.restingAtTissue, model.recentActive, model.recentWorkouts) / formulaTotal,
                sd = factors.needSd(model.restingAtTissue, model.recentActive, model.recentWorkouts) / formulaTotal,
                prior = null,
                info = infoOf(R.string.body_combined, R.string.info_combined),
            )
            FactorMeter(stringResource(R.string.body_resting), factors.resting, factors.restingSd, EnergyFilter.PRIOR_RESTING_SD, infoOf(R.string.body_resting, R.string.info_baseline))
            FactorMeter(stringResource(R.string.body_activity), factors.activity, factors.activitySd, EnergyFilter.PRIOR_ACTIVITY_SD, infoOf(R.string.body_activity, R.string.info_walking))
            if (model.workoutDays > 0) {
                FactorMeter(stringResource(R.string.body_workouts), factors.workout, factors.workoutSd, EnergyFilter.PRIOR_WORKOUT_SD, infoOf(R.string.body_workouts, R.string.info_workouts))
            }
            Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                if (abs(model.latest.water) >= MIN_WATER_KG) Fact(
                    value = Util.formatWeightChange(model.latest.water),
                    label = stringResource(R.string.trends_water),
                    color = PROTEIN_BLUE,
                    info = infoOf(R.string.body_weight_now, R.string.info_weight_now),
                    icon = R.drawable.ic_weight,
                    style = TaroTheme.text.factCompact,
                )
                model.forecastKg(FORECAST_DAYS)?.let { kg ->
                    val date = Util.logicalToday().plusDays(FORECAST_DAYS.toLong())
                    Fact(
                        value = Util.formatWeight(kg),
                        label = stringResource(R.string.trends_forecast, date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))),
                        color = colors.goal,
                        info = infoOf(R.string.info_forecast_title, R.string.info_forecast),
                        icon = R.drawable.ic_trends,
                        style = TaroTheme.text.factCompact,
                    )
                }
            }
        }
    }
}

@Composable
private fun FactorMeter(label: String, factor: Double, sd: Double, prior: Double?, info: Info) {
    val learned = prior == null || sd <= prior * LEARNED_SHARE
    fun at(value: Double) = ((value - 1) / FACTOR_SPAN + 0.5).toFloat()
    Meter(
        label = label,
        value = if (learned) "${percent(factor)}  ${spread(sd)}" else stringResource(R.string.body_needs_variety),
        fraction = 0f,
        color = MaterialTheme.colorScheme.onSurface,
        band = at(factor - sd)..at(factor + sd),
        info = info,
        marker = 0.5f,
    )
}

private const val FACTOR_SPAN = 0.6
private const val MIN_WATER_KG = 0.05

const val WEIGHT_STALE_DAYS = 14L
private const val FORECAST_STEP = 7
private const val FORECAST_WEEKS = 4

@Composable
fun WeightCourse(points: List<TrendPoint>, model: BodyModel?, days: Int, aspect: Float, modifier: Modifier = Modifier) {
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val latest = points.lastOrNull() ?: return
    val sd = model?.latest?.tissueSd
    val today = Util.logicalToday()
    val stale = latest.date.takeIf { it.isBefore(today.minusDays(WEIGHT_STALE_DAYS)) }
    val start = minOf(today, latest.date).minusDays(days.toLong())
    val shown = points.filter { !it.date.isBefore(start) }
    val daily = model?.dailyChangeKg()
    val forecast = if (model != null && daily != null && stale == null) {
        val needSd = model.factors.needSd(model.restingAtTissue, model.recentActive, model.recentWorkouts)
        (1..FORECAST_WEEKS).map { week ->
            val ahead = week * FORECAST_STEP
            WeightForecast(
                date = latest.date.plusDays(ahead.toLong()),
                kg = latest.trend + daily * ahead,
                sd = sqrt((sd ?: 0.0) * (sd ?: 0.0) + (needSd * ahead / Metabolism.KCAL_PER_KG).let { it * it }),
            )
        }
    } else {
        emptyList()
    }
    var selected by remember(shown, forecast) { mutableStateOf<Int?>(null) }
    val day = remember { DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()) }
    Column(modifier = modifier) {
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
                .aspectRatio(aspect)
                .padding(top = spacing.tight),
        )
        BentoNote(
            forecast.lastOrNull()?.let { stringResource(R.string.trends_weight_course, Util.formatWeight(it.kg), it.date.format(day)) }
                ?: stringResource(R.string.trends_forecast_wait),
            Modifier.padding(top = spacing.tight),
        )
        stale?.let { BentoNote(stringResource(R.string.trends_weight_stale, it.format(day)), Modifier.padding(top = spacing.tight)) }
    }
}
