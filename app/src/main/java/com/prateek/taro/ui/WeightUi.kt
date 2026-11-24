package com.prateek.taro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.prateek.taro.energy.WeightJournal
import com.prateek.taro.ui.components.BentoTile
import com.prateek.taro.ui.components.EditRow
import com.prateek.taro.ui.components.Fact
import com.prateek.taro.ui.components.Sparkline
import com.prateek.taro.ui.components.StepperField
import com.prateek.taro.ui.components.TaroDatePickerDialog
import com.prateek.taro.ui.components.dayLabel
import kotlin.math.roundToInt
import com.prateek.taro.ui.components.TintChip
import java.time.LocalDate
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.prateek.taro.R
import com.prateek.taro.energy.DietGoal
import com.prateek.taro.energy.PaceAssessment
import com.prateek.taro.energy.PaceStatus
import com.prateek.taro.energy.TrendPoint
import com.prateek.taro.ui.components.TaroDialog
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.Util
import java.time.temporal.ChronoUnit
import java.util.Locale

private val WEIGHT_KG_RANGE = 20.0..400.0
private const val WEIGHT_STEP = 0.1
private const val SPARK_POINTS = 13
private const val SPARK_ASPECT = 4f

fun weekChange(points: List<TrendPoint>): Double? {
    val last = points.lastOrNull() ?: return null
    val weekAgo = points.lastOrNull { ChronoUnit.DAYS.between(it.date, last.date) >= 7 } ?: return null
    return last.trend - weekAgo.trend
}

@Composable
fun LogWeightDialog(
    title: String,
    initialKg: Double?,
    onSave: (kg: Double, date: LocalDate) -> Unit,
    onDismiss: () -> Unit,
    initialDate: LocalDate = Util.logicalToday(),
) {
    val context = LocalContext.current
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val today = Util.logicalToday()
    var date by remember { mutableStateOf(initialDate) }
    var pickingDate by remember { mutableStateOf(false) }
    fun display(kg: Double) = "%.1f".format(Locale.getDefault(), Util.kgToDisplay(kg))
    val initialText = initialKg?.let(::display).orEmpty()
    var value by remember { mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length))) }
    val kg = Util.parseMeasure(value.text)?.let(Util::displayToKg)?.takeIf { it in WEIGHT_KG_RANGE }
    val points = rememberInBackground(Unit) { WeightJournal.points(context) }?.value.orEmpty()
    val earlier = points.filter { it.date < date }
    val previous = earlier.lastOrNull()

    fun step(direction: Int) {
        val shown = Util.parseMeasure(value.text) ?: initialKg?.let(Util::kgToDisplay) ?: return
        val next = ((shown / WEIGHT_STEP).roundToInt() + direction) * WEIGHT_STEP
        val text = "%.1f".format(Locale.getDefault(), next)
        value = TextFieldValue(text, TextRange(text.length))
    }

    TaroDialog(
        title = title,
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        confirmEnabled = kg != null,
        onConfirm = { kg?.let { onSave(it, date) } },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.gap), modifier = Modifier.verticalScroll(rememberScrollState())) {
            StepperField(
                value = value,
                onValueChange = { value = it },
                unit = Util.weightUnit(),
                onMinus = { step(-1) },
                onPlus = { step(1) },
                decimal = true,
            )
            if (previous != null) {
                BentoTile(color = colors.goal, tinted = true, modifier = Modifier.fillMaxWidth()) {
                    Sparkline(
                        values = earlier.takeLast(SPARK_POINTS).map { it.kg } + kg,
                        color = colors.goal,
                        modifier = Modifier.fillMaxWidth().aspectRatio(SPARK_ASPECT),
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(top = spacing.gap)) {
                        Fact(Util.formatWeight(previous.kg), dayLabel(previous.date, today), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f), style = type.factCompact)
                        Fact(
                            kg?.let { Util.formatWeightChange(it - previous.kg) } ?: "-",
                            stringResource(R.string.weight_dialog_change),
                            colors.special,
                            Modifier.weight(1f),
                            style = type.factCompact,
                        )
                        Fact(Util.formatWeight(previous.trend), stringResource(R.string.weight_dialog_trend), colors.goal, Modifier.weight(1f), style = type.factCompact)
                    }
                }
            }
            EditRow(
                icon = R.drawable.ic_dateformat,
                color = colors.accent,
                label = stringResource(R.string.date_label),
                value = dayLabel(date, today),
                onClick = { pickingDate = true },
            )
            Text(
                text = stringResource(R.string.weight_dialog_hint),
                style = type.note,
                color = colors.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (pickingDate) {
        TaroDatePickerDialog(
            initial = date,
            latest = today,
            onPick = {
                date = it
                pickingDate = false
            },
            onDismiss = { pickingDate = false },
        )
    }
}

@Composable
fun PaceMessage(assessment: PaceAssessment, goal: DietGoal?, modifier: Modifier = Modifier) {
    val actual = assessment.actualPerWeek?.let(Util::formatWeightChange).orEmpty()
    val expected = Util.formatWeightChange(assessment.expectedPerWeek)
    val cut = goal == DietGoal.CUT
    val (text, color) = when (assessment.status) {
        PaceStatus.NOT_ENOUGH_DATA -> stringResource(R.string.weight_pace_not_enough) to TaroTheme.colors.accent
        PaceStatus.ON_TRACK -> stringResource(R.string.weight_pace_on_track, actual) to TaroTheme.colors.goal
        PaceStatus.SLOWER -> stringResource(R.string.weight_pace_slower, actual, expected) to TaroTheme.colors.special
        PaceStatus.FASTER -> stringResource(R.string.weight_pace_faster, actual, expected) to TaroTheme.colors.special
        PaceStatus.TOO_FAST -> stringResource(
            if (cut) R.string.weight_pace_too_fast_cut else R.string.weight_pace_too_fast_bulk, actual
        ) to MaterialTheme.colorScheme.error
        PaceStatus.WRONG_WAY -> stringResource(
            if (cut) R.string.weight_pace_wrong_way_cut else R.string.weight_pace_wrong_way_bulk, actual
        ) to MaterialTheme.colorScheme.error
        PaceStatus.STEADY -> stringResource(R.string.weight_pace_steady, actual) to TaroTheme.colors.goal
        PaceStatus.DRIFTING -> stringResource(R.string.weight_pace_drifting, actual) to TaroTheme.colors.special
    }
    TintChip(text = text, color = color, modifier = modifier)
}
