package com.prateek.taro.ui

import com.prateek.taro.ui.components.SecondaryButton
import com.prateek.taro.ui.components.PrimaryButton
import com.prateek.taro.ui.components.TileRow
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    Panel(modifier = modifier) {
        Text(
            text = stringResource(R.string.underlog_title, format.format(Date(Util.dateStringToCalendarMillis(day.date.toString())))),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 6.dp),
        )
        Text(
            text = stringResource(R.string.underlog_text, Util.formatSteps(day.logged.roundToInt()), Util.formatSteps(day.usual.roundToInt())),
            fontSize = 13.sp,
            color = TaroTheme.colors.accent,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp),
        )
        TileRow(modifier = Modifier.padding(8.dp)) {
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
            if (model == null) {
                BentoNote(stringResource(if (loaded) R.string.body_empty else R.string.body_loading))
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
