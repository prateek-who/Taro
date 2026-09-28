package com.nvllz.stepsy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.preferences.core.edit
import com.nvllz.stepsy.R
import com.nvllz.stepsy.energy.Sex
import com.nvllz.stepsy.ui.components.NumberField
import com.nvllz.stepsy.ui.components.StepsyDialog
import com.nvllz.stepsy.ui.components.ToggleGroup
import com.nvllz.stepsy.ui.theme.StepsyTheme
import com.nvllz.stepsy.util.AppPreferences
import com.nvllz.stepsy.util.AppPreferences.PreferenceKeys
import com.nvllz.stepsy.util.Util
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

private fun TextFieldValue.intIn(range: IntRange) = text.toIntOrNull()?.takeIf { it in range }

private fun TextFieldValue.measureIn(range: ClosedFloatingPointRange<Double>) = Util.parseMeasure(text)?.takeIf { it in range }

private fun inchesToCm(inches: Int) = (inches * 2.54).roundToInt()

@Composable
fun OnboardingDialog(onDone: () -> Unit) {
    var imperial by remember { mutableStateOf(AppPreferences.unitSystem == Util.UnitSystem.IMPERIAL) }
    var heightCm by remember { mutableStateOf(TextFieldValue()) }
    var heightFt by remember { mutableStateOf(TextFieldValue()) }
    var heightIn by remember { mutableStateOf(TextFieldValue()) }
    var weight by remember { mutableStateOf(TextFieldValue()) }
    var leg by remember { mutableStateOf(TextFieldValue()) }
    var ageText by remember { mutableStateOf(TextFieldValue()) }
    var sex by remember { mutableStateOf<Sex?>(null) }

    val height = if (imperial) {
        val totalInches = (heightFt.text.toIntOrNull() ?: 0) * 12 + (heightIn.text.toIntOrNull() ?: 0)
        totalInches.takeIf { it in 12..98 }?.let { inchesToCm(it).toDouble() }
    } else {
        heightCm.measureIn(1.0..250.0)
    }
    val weightKg = if (imperial) weight.measureIn(1.0..1100.0)?.let { (it / 2.20462).coerceIn(1.0, 500.0) } else weight.measureIn(1.0..500.0)
    val legCm = if (imperial) leg.intIn(12..60)?.let(::inchesToCm) else leg.intIn(30..150)
    val age = ageText.intIn(AGE_RANGE)
    val valid = height != null && weightKg != null &&
        (leg.text.isBlank() || legCm != null) &&
        (ageText.text.isBlank() || age != null)

    val unitEntries = stringArrayResource(R.array.unit_system_entries).toList()
    val unitValues = stringArrayResource(R.array.unit_system_values).toList()

    fun finish(save: Boolean) {
        runBlocking {
            AppPreferences.dataStore.edit { prefs ->
                if (save && height != null && weightKg != null) {
                    prefs[PreferenceKeys.UNIT_SYSTEM] = if (imperial) "imperial" else "metric"
                    prefs[PreferenceKeys.HEIGHT] = height.toString()
                    prefs[PreferenceKeys.WEIGHT] = weightKg.toString()
                    legCm?.let { prefs[PreferenceKeys.LEG_LENGTH] = it.toString() }
                    age?.let { prefs[PreferenceKeys.BIRTH_YEAR] = Calendar.getInstance().get(Calendar.YEAR) - it }
                    sex?.let { prefs[PreferenceKeys.SEX] = it.key }
                }
                prefs[PreferenceKeys.ONBOARDING_DONE] = true
            }
        }
        onDone()
    }

    StepsyDialog(
        title = stringResource(R.string.onboarding_title),
        onDismiss = { finish(save = false) },
        dismissText = stringResource(R.string.onboarding_skip),
        confirmText = stringResource(R.string.onboarding_save),
        confirmEnabled = valid,
        onConfirm = { finish(save = true) },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            Text(stringResource(R.string.onboarding_message), style = MaterialTheme.typography.bodyMedium)

            ToggleGroup(
                options = unitValues.zip(unitEntries),
                selected = if (imperial) "imperial" else "metric",
                onSelect = { imperial = it == "imperial" },
            )

            if (imperial) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(heightFt, { heightFt = it }, "${stringResource(R.string.pref_height)} (ft)", Modifier.weight(1f))
                    NumberField(heightIn, { heightIn = it }, "in", Modifier.weight(1f))
                }
            } else {
                NumberField(heightCm, { heightCm = it }, "${stringResource(R.string.pref_height)} (cm)", Modifier.fillMaxWidth(), decimal = true)
            }

            NumberField(
                value = weight,
                onValueChange = { weight = it },
                label = "${stringResource(R.string.pref_weight)} (${if (imperial) "lbs" else "kg"})",
                decimal = true,
                modifier = Modifier.fillMaxWidth(),
            )

            NumberField(
                value = ageText,
                onValueChange = { ageText = it },
                label = stringResource(R.string.age_optional),
                modifier = Modifier.fillMaxWidth(),
            )

            ToggleGroup(
                options = Sex.entries.map { it to sexLabel(it) },
                selected = sex,
                onSelect = { sex = it },
            )

            NumberField(
                value = leg,
                onValueChange = { leg = it },
                label = "${stringResource(R.string.leg_length_optional)} (${if (imperial) "in" else "cm"})",
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.leg_length_hint),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.alpha(0.7f),
            )

            height?.let {
                val stepCm = Util.estimateStepLength(it, legCm)
                val label = if (imperial) "%.1f in".format(Locale.getDefault(), stepCm / 2.54f)
                else "%.1f cm".format(Locale.getDefault(), stepCm)
                Text(
                    text = stringResource(R.string.onboarding_estimated_step, label),
                    color = StepsyTheme.colors.accent,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
