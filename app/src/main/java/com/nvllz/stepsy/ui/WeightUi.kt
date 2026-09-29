package com.nvllz.stepsy.ui

import java.time.LocalDate
import com.nvllz.stepsy.ui.components.DateRow
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.nvllz.stepsy.R
import com.nvllz.stepsy.energy.DietGoal
import com.nvllz.stepsy.energy.PaceAssessment
import com.nvllz.stepsy.energy.PaceStatus
import com.nvllz.stepsy.energy.TrendPoint
import com.nvllz.stepsy.ui.components.NumberField
import com.nvllz.stepsy.ui.components.StepsyDialog
import com.nvllz.stepsy.ui.theme.StepsyTheme
import com.nvllz.stepsy.util.Util
import java.time.temporal.ChronoUnit
import java.util.Locale

private val WEIGHT_KG_RANGE = 20.0..400.0

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
    var date by remember { mutableStateOf(initialDate) }
    val initialText = initialKg?.let { "%.1f".format(Locale.getDefault(), Util.kgToDisplay(it)) }.orEmpty()
    var value by remember { mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length))) }
    val kg = Util.parseMeasure(value.text)?.let(Util::displayToKg)?.takeIf { it in WEIGHT_KG_RANGE }

    StepsyDialog(
        title = title,
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        confirmEnabled = kg != null,
        onConfirm = { kg?.let { onSave(it, date) } },
    ) {
        Column {
            DateRow(
                label = stringResource(R.string.date_label),
                date = date,
                today = Util.logicalToday(),
                onChange = { date = it },
                modifier = Modifier.padding(bottom = 12.dp),
            )
            NumberField(
                value = value,
                onValueChange = { value = it },
                label = stringResource(R.string.weight_title),
                decimal = true,
                suffix = Util.weightUnit(),
                large = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.weight_dialog_hint),
                style = MaterialTheme.typography.bodySmall,
                color = StepsyTheme.colors.accent,
                modifier = Modifier.padding(top = 12.dp, start = 4.dp),
            )
        }
    }
}

@Composable
fun PaceMessage(assessment: PaceAssessment, goal: DietGoal?, modifier: Modifier = Modifier) {
    val actual = assessment.actualPerWeek?.let(Util::formatWeightChange).orEmpty()
    val expected = Util.formatWeightChange(assessment.expectedPerWeek)
    val cut = goal == DietGoal.CUT
    val (text, color) = when (assessment.status) {
        PaceStatus.NOT_ENOUGH_DATA -> stringResource(R.string.weight_pace_not_enough) to StepsyTheme.colors.accent
        PaceStatus.ON_TRACK -> stringResource(R.string.weight_pace_on_track, actual) to StepsyTheme.colors.goal
        PaceStatus.SLOWER -> stringResource(R.string.weight_pace_slower, actual, expected) to StepsyTheme.colors.special
        PaceStatus.FASTER -> stringResource(R.string.weight_pace_faster, actual, expected) to StepsyTheme.colors.special
        PaceStatus.TOO_FAST -> stringResource(
            if (cut) R.string.weight_pace_too_fast_cut else R.string.weight_pace_too_fast_bulk, actual
        ) to MaterialTheme.colorScheme.error
        PaceStatus.WRONG_WAY -> stringResource(
            if (cut) R.string.weight_pace_wrong_way_cut else R.string.weight_pace_wrong_way_bulk, actual
        ) to MaterialTheme.colorScheme.error
        PaceStatus.STEADY -> stringResource(R.string.weight_pace_steady, actual) to StepsyTheme.colors.goal
        PaceStatus.DRIFTING -> stringResource(R.string.weight_pace_drifting, actual) to StepsyTheme.colors.special
    }
    Text(text = text, color = color, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
}
