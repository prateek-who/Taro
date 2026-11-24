package com.prateek.taro.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.Color
import com.prateek.taro.energy.DailyEnergy
import com.prateek.taro.ui.components.BentoNote
import com.prateek.taro.ui.components.BentoTile
import com.prateek.taro.ui.components.BentoValue
import com.prateek.taro.ui.components.Fact
import com.prateek.taro.ui.components.MeterBar
import com.prateek.taro.ui.components.StepperValue
import com.prateek.taro.ui.components.TaroSlider
import com.prateek.taro.ui.components.dayLabel
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.ui.components.RollingText
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.LinkText
import com.prateek.taro.service.MotionService
import androidx.datastore.preferences.core.edit
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.prateek.taro.R
import com.prateek.taro.util.Util
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.energy.DietGoal
import com.prateek.taro.energy.Metabolism
import com.prateek.taro.energy.Sex
import com.prateek.taro.ui.components.TaroDialog
import com.prateek.taro.util.CalorieGoal
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import java.util.Locale
import com.prateek.taro.ui.components.NumberInputDialog
import com.prateek.taro.ui.components.SingleChoiceDialog

val AGE_RANGE = 13..110

@Composable
fun sexLabel(sex: Sex): String = stringResource(
    when (sex) {
        Sex.MALE -> R.string.sex_male
        Sex.FEMALE -> R.string.sex_female
        Sex.UNSPECIFIED -> R.string.sex_unspecified
    }
)

@Composable
fun AgeDialog(current: Int?, onConfirm: (Int) -> Unit, onInvalid: () -> Unit, onDismiss: () -> Unit) {
    NumberInputDialog(
        title = stringResource(R.string.pref_age),
        initial = current?.toString().orEmpty(),
        hint = "${stringResource(R.string.pref_age)} (${AGE_RANGE.first}-${AGE_RANGE.last})",
        onConfirm = { input ->
            val age = input.toIntOrNull()
            if (age != null && age in AGE_RANGE) onConfirm(age) else onInvalid()
        },
        onDismiss = onDismiss,
    )
}

@Composable
fun SexDialog(current: Sex?, onSelect: (Sex) -> Unit, onDismiss: () -> Unit) {
    SingleChoiceDialog(
        title = stringResource(R.string.pref_sex),
        entries = Sex.entries.map { sexLabel(it) },
        selectedIndex = current?.let { Sex.entries.indexOf(it) } ?: -1,
        onSelect = { onSelect(Sex.entries[it]) },
        onDismiss = onDismiss,
    )
}

val ADJUSTMENT_RANGE = 50..1500

@Composable
fun calorieGoalSummary(goal: CalorieGoal?): String = when (goal?.goal) {
    null -> stringResource(R.string.calorie_goal_not_set)
    DietGoal.CUT -> stringResource(R.string.calorie_goal_cut_summary, goal.adjustment.toString())
    DietGoal.MAINTAIN -> stringResource(R.string.calorie_goal_maintain_summary)
    DietGoal.BULK -> stringResource(R.string.calorie_goal_bulk_summary, goal.adjustment.toString())
}

private const val ADJUST_STEP = 50
private const val SLIDER_LIMIT = 1000
private const val PROTEIN_STEP = 0.1
private val PROTEIN_RANGE = 0.4..3.0
private const val PROTEIN_SCALE = 2.4
private val PROTEIN_PICKS = listOf(
    R.string.protein_minimum to 0.8,
    R.string.protein_active to 1.2,
    R.string.protein_training to 1.6,
    R.string.protein_max to 2.2,
)
private const val PROTEIN_BAND_LOW = 1.2
private const val PROTEIN_BAND_HIGH = 2.2

private fun signed(goal: CalorieGoal?): Int = when (goal?.goal) {
    DietGoal.CUT -> -goal.adjustment
    DietGoal.BULK -> goal.adjustment
    else -> 0
}

@Composable
private fun calorieZone(delta: Int): Pair<String, Color> {
    val colors = TaroTheme.colors
    return when {
        delta == 0 -> stringResource(R.string.calorie_zone_maintain) to colors.mint
        delta >= -300 && delta < 0 -> stringResource(R.string.calorie_zone_gentle_cut) to colors.goal
        delta >= -650 && delta < 0 -> stringResource(R.string.calorie_zone_steady_cut) to colors.goal
        delta < 0 -> stringResource(R.string.calorie_zone_hard_cut) to colors.red
        delta <= 250 -> stringResource(R.string.calorie_zone_lean_bulk) to colors.flame
        delta <= 500 -> stringResource(R.string.calorie_zone_steady_bulk) to colors.flame
        else -> stringResource(R.string.calorie_zone_hard_bulk) to colors.red
    }
}

@Composable
fun CalorieGoalDialog(current: CalorieGoal?, onSave: (CalorieGoal) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    var delta by remember { mutableIntStateOf(signed(current)) }
    val history = rememberInBackground(Unit) { DailyEnergy.history(context, AppPreferences.energyFactors) }?.value
    val (zone, color) = calorieZone(delta)
    val limit = ADJUSTMENT_RANGE.last

    TaroDialog(
        title = stringResource(R.string.calorie_goal),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        onConfirm = {
            onSave(
                when {
                    delta < 0 -> CalorieGoal(DietGoal.CUT, -delta)
                    delta > 0 -> CalorieGoal(DietGoal.BULK, delta)
                    else -> CalorieGoal(DietGoal.MAINTAIN, 0)
                }
            )
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(zone.uppercase(), style = type.meterLabel, color = color)
            StepperValue(
                text = if (delta > 0) "+$delta" else if (delta < 0) "−${-delta}" else "0",
                color = MaterialTheme.colorScheme.onSurface,
                unit = stringResource(R.string.calorie_goal_per_day),
                onMinus = { delta = (delta - ADJUST_STEP).coerceAtLeast(-limit) },
                onPlus = { delta = (delta + ADJUST_STEP).coerceAtMost(limit) },
                minusEnabled = delta > -limit,
                plusEnabled = delta < limit,
            )
            TaroSlider(
                value = delta.coerceIn(-SLIDER_LIMIT, SLIDER_LIMIT).toFloat(),
                onValueChange = { delta = (it / ADJUST_STEP).roundToInt() * ADJUST_STEP },
                valueRange = -SLIDER_LIMIT.toFloat()..SLIDER_LIMIT.toFloat(),
                modifier = Modifier.fillMaxWidth().padding(top = spacing.gap),
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                listOf(R.string.calorie_zone_hard_cut, R.string.calorie_goal_cut, R.string.calorie_goal_maintain, R.string.calorie_goal_bulk, R.string.calorie_zone_hard_bulk).forEach {
                    Text(stringResource(it), style = type.axis, color = colors.accent)
                }
            }
            history?.let { energy ->
                val need = energy.dailyNeed
                val eat = need + delta
                val weekly = Metabolism.weeklyChangeKg(if (delta < 0) DietGoal.CUT else DietGoal.BULK, kotlin.math.abs(delta))
                BentoTile(color = color, tinted = true, modifier = Modifier.fillMaxWidth().padding(top = spacing.gap)) {
                    BentoValue(Util.formatSteps(eat.roundToInt()), color, unit = stringResource(R.string.calorie_goal_per_day))
                    BentoNote(stringResource(R.string.calorie_goal_need_note, Util.formatSteps(need.roundToInt()), Util.formatSteps(energy.restingPerDay.roundToInt())))
                    if (delta != 0) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = spacing.gap)) {
                            Fact(Util.formatWeightChange(weekly), stringResource(R.string.calorie_goal_a_week), color, Modifier.weight(1f), style = type.factCompact)
                            nextWhole(AppPreferences.weight, weekly)?.let { (target, date) ->
                                Fact(
                                    Util.formatWeight(target),
                                    stringResource(R.string.calorie_goal_by, dayLabel(date, Util.logicalToday())),
                                    MaterialTheme.colorScheme.onSurface,
                                    Modifier.weight(1f),
                                    style = type.factCompact,
                                )
                            }
                            Fact(
                                "%+d%%".format(Locale.getDefault(), (delta / need * 100).roundToInt()),
                                stringResource(R.string.calorie_goal_of_need),
                                MaterialTheme.colorScheme.onSurface,
                                Modifier.weight(1f),
                                style = type.factCompact,
                            )
                        }
                    }
                    if (eat < energy.restingPerDay) {
                        BentoNote(stringResource(R.string.calorie_goal_below_resting), Modifier.padding(top = spacing.tight), color = colors.red)
                    }
                }
            }
        }
    }
}

private fun nextWhole(kg: Double, weeklyKg: Double): Pair<Double, LocalDate>? {
    if (weeklyKg == 0.0) return null
    val shown = Util.kgToDisplay(kg)
    val target = if (weeklyKg < 0) ceil(shown) - 1 else floor(shown) + 1
    val targetKg = Util.displayToKg(target)
    val days = ((targetKg - kg) / (weeklyKg / 7)).roundToInt().takeIf { it > 0 } ?: return null
    return targetKg to Util.logicalToday().plusDays(days.toLong())
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProteinTargetDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    var perKg by remember { mutableStateOf(AppPreferences.proteinPerKg.toDouble()) }
    val weight = AppPreferences.weight
    val grams = (perKg * weight).roundToInt()
    val meals = remember { AppPreferences.meals(context).count { it.daily }.coerceAtLeast(1) }
    fun set(value: Double) {
        perKg = (value.coerceIn(PROTEIN_RANGE) * 10).roundToInt() / 10.0
    }

    TaroDialog(
        title = stringResource(R.string.food_protein_dialog),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        onConfirm = {
            AppPreferences.proteinPerKg = perKg.toFloat()
            onDismiss()
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.Bottom) {
                RollingText(grams.toString(), type.hero.copy(color = PROTEIN_BLUE))
                Text(stringResource(R.string.protein_g_day), style = type.unit, color = colors.accent, modifier = Modifier.padding(start = spacing.hair, bottom = spacing.tight))
            }
            StepperValue(
                text = Util.formatMeasure(perKg),
                color = MaterialTheme.colorScheme.onSurface,
                unit = stringResource(R.string.protein_per_kg),
                onMinus = { set(perKg - PROTEIN_STEP) },
                onPlus = { set(perKg + PROTEIN_STEP) },
                minusEnabled = perKg > PROTEIN_RANGE.start,
                plusEnabled = perKg < PROTEIN_RANGE.endInclusive,
                style = type.fact,
            )
            MeterBar(
                fraction = (perKg / PROTEIN_SCALE).toFloat(),
                color = PROTEIN_BLUE,
                band = (PROTEIN_BAND_LOW / PROTEIN_SCALE).toFloat()..(PROTEIN_BAND_HIGH / PROTEIN_SCALE).toFloat(),
                modifier = Modifier.fillMaxWidth().padding(top = spacing.gap),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.gap, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth().padding(top = spacing.tight)) {
                PROTEIN_PICKS.forEach { (label, value) ->
                    LinkText(stringResource(label, Util.formatMeasure(value)), if (perKg == value) PROTEIN_BLUE else colors.accent, onClick = { set(value) })
                }
            }
            BentoTile(color = PROTEIN_BLUE, tinted = true, modifier = Modifier.fillMaxWidth().padding(top = spacing.gap)) {
                Text(stringResource(R.string.protein_at_weight, Util.formatWeight(weight), grams), style = type.title, color = MaterialTheme.colorScheme.onSurface)
                if (meals > 1) BentoNote(stringResource(R.string.protein_per_meal, grams / meals, meals))
                BentoNote(stringResource(R.string.protein_max_note))
            }
        }
    }
}

@Composable
fun StepGoalDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = TaroTheme.colors
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    val current = remember { AppPreferences.dailyGoalTarget }
    var goal by remember { mutableIntStateOf(current.takeIf { it > 0 } ?: DEFAULT_GOAL) }
    val today = Util.logicalToday()
    val suggestions = rememberInBackground(today) { goalSuggestions(context, today) }?.value

    TaroDialog(
        title = stringResource(R.string.steps_goal_title),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        onConfirm = {
            val target = goal
            onDismiss()
            scope.launch {
                AppPreferences.dataStore.edit { AppPreferences.setGoal(it, target, today) }
                context.startService(
                    Intent(context, MotionService::class.java).apply {
                        action = "UPDATE_NOTIFICATION"
                        putExtra("show_progressbar", AppPreferences.dailyGoalNotificationProgressbar)
                        putExtra("daily_target", target)
                    }
                )
                toast.show(context.getString(R.string.daily_goals_saved), ToastKind.SUCCESS)
            }
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            StepperValue(
                text = Util.formatSteps(goal),
                color = colors.goal,
                onMinus = { goal = (goal - GOAL_STEP).coerceAtLeast(GOAL_STEP) },
                onPlus = { goal = (goal + GOAL_STEP).coerceAtMost(MAX_GOAL) },
                minusEnabled = goal > GOAL_STEP,
                plusEnabled = goal < MAX_GOAL,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.gap), modifier = Modifier.padding(top = spacing.gap)) {
                suggestions?.average?.let { LinkText(stringResource(R.string.steps_goal_average, Util.formatSteps(it)), colors.goal, onClick = { goal = it }) }
                suggestions?.bestWeek?.takeIf { it != suggestions.average }?.let { LinkText(stringResource(R.string.steps_goal_best_week, Util.formatSteps(it)), colors.goal, onClick = { goal = it }) }
            }
            Text(
                text = stringResource(R.string.steps_goal_note),
                style = type.note,
                color = colors.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = spacing.tight),
            )
        }
    }
}


private const val DEFAULT_GOAL = 10_000
private const val MAX_GOAL = 100_000
