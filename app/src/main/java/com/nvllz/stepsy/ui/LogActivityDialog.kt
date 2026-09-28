package com.nvllz.stepsy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.nvllz.stepsy.R
import com.nvllz.stepsy.energy.Activities
import com.nvllz.stepsy.energy.ActivityType
import com.nvllz.stepsy.ui.components.NumberField
import com.nvllz.stepsy.ui.components.RangeChip
import com.nvllz.stepsy.ui.components.StepsyDialog
import com.nvllz.stepsy.ui.components.StepsyTextField
import kotlin.math.roundToInt

private val MINUTES_RANGE = 1..1440
private val KCAL_RANGE = 1.0..5000.0

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogActivityDialog(
    weightKg: Double,
    onSave: (name: String, minutes: Int?, kcal: Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var type by remember { mutableStateOf(ActivityType.STRENGTH) }
    var customName by remember { mutableStateOf(TextFieldValue()) }
    var minutes by remember { mutableStateOf(TextFieldValue()) }
    var kcalInput by remember { mutableStateOf<TextFieldValue?>(null) }

    val minutesValue = minutes.text.toIntOrNull()?.takeIf { it in MINUTES_RANGE }
    val estimate = type.met?.let { met -> minutesValue?.let { Activities.estimateActiveKcal(met, weightKg, it) } }
    val kcalField = kcalInput ?: TextFieldValue(estimate?.roundToInt()?.toString().orEmpty())
    val kcalValue = kcalField.text.replace(',', '.').toDoubleOrNull()?.takeIf { it in KCAL_RANGE }
    val typeName = stringResource(type.label)
    val name = if (type == ActivityType.OTHER) customName.text.trim() else typeName
    val valid = kcalValue != null && name.isNotEmpty() && (minutes.text.isBlank() || minutesValue != null)

    StepsyDialog(
        title = stringResource(R.string.activity_dialog_title),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        confirmEnabled = valid,
        onConfirm = { if (kcalValue != null) onSave(name, minutesValue, kcalValue) },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                ActivityType.entries.forEach { option ->
                    RangeChip(
                        label = stringResource(option.label),
                        selected = option == type,
                        onClick = { type = option },
                    )
                }
            }

            if (type == ActivityType.OTHER) {
                StepsyTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = stringResource(R.string.activity_name),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            NumberField(
                value = minutes,
                onValueChange = { minutes = it },
                label = stringResource(R.string.activity_minutes),
                modifier = Modifier.fillMaxWidth(),
            )

            NumberField(
                value = kcalField,
                onValueChange = { kcalInput = it },
                label = stringResource(R.string.activity_kcal),
                decimal = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (kcalInput == null && estimate != null) {
                Text(
                    text = stringResource(R.string.activity_estimate_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.alpha(0.7f),
                )
            }

            Text(
                text = stringResource(R.string.activity_tip),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.alpha(0.7f),
            )
        }
    }
}
