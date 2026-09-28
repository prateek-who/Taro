package com.nvllz.stepsy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.nvllz.stepsy.R
import com.nvllz.stepsy.ui.components.LocalToast
import com.nvllz.stepsy.ui.components.ToastKind
import com.nvllz.stepsy.energy.ActivityEnergy
import com.nvllz.stepsy.energy.EnergyModel
import com.nvllz.stepsy.ui.components.NumberField
import com.nvllz.stepsy.ui.components.PrimaryButton
import com.nvllz.stepsy.ui.components.ScrollingColumn
import com.nvllz.stepsy.ui.components.SecondaryButton
import com.nvllz.stepsy.ui.components.SettingsCard
import com.nvllz.stepsy.ui.components.SettingsDivider
import com.nvllz.stepsy.ui.components.StatRow
import com.nvllz.stepsy.ui.components.StepsyScaffold
import com.nvllz.stepsy.ui.components.ToggleGroup
import com.nvllz.stepsy.ui.components.rememberWalkSession
import com.nvllz.stepsy.util.AppPreferences
import com.nvllz.stepsy.util.Util
import java.util.Locale

object AccuracyScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        AccuracyContent(onBack = { navigator.pop() })
    }
}

private const val YARD_M = 0.9144

private fun percentLabel(error: Float) = "%+.1f%%".format(Locale.getDefault(), error)

private fun errorPercent(measured: Double, truth: Double) = ((measured - truth) / truth * 100).toFloat()

@Composable
private fun AccuracyContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val toast = LocalToast.current
    val session = rememberWalkSession()
    val state by session.state.collectAsStateWithLifecycle()
    val results by AppPreferences.accuracyFlow().collectAsStateWithLifecycle(null)
    val imperial = AppPreferences.unitSystem == Util.UnitSystem.IMPERIAL

    var stepMode by rememberSaveable { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }
    var counted by remember { mutableStateOf(TextFieldValue()) }
    var distance by remember { mutableStateOf(TextFieldValue(if (imperial) "440" else "400")) }


    val cadence = if (state.elapsedS > 0) (state.steps * 60 / state.elapsedS).toInt() else 0
    val body = remember { ActivityEnergy.body() }
    val estimatedM = state.steps * EnergyModel.stepLengthM(body, cadence)
    val truthM = distance.text.toDoubleOrNull()?.let { if (imperial) it * YARD_M else it }
    val countedSteps = counted.text.toIntOrNull()

    val error: Float? = when {
        !finished || state.steps == 0 -> null
        stepMode -> countedSteps?.takeIf { it > 0 }?.let { errorPercent(state.steps.toDouble(), it.toDouble()) }
        else -> truthM?.takeIf { it > 0 }?.let { errorPercent(estimatedM, it) }
    }

    StepsyScaffold(title = stringResource(R.string.accuracy_title), onBack = onBack) { padding ->
        ScrollingColumn(padding, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = stringResource(R.string.accuracy_intro),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            SettingsCard {
                val never = stringResource(R.string.accuracy_never)
                StatRow(
                    stringResource(R.string.accuracy_last_steps),
                    results?.stepError?.let { stringResource(R.string.accuracy_result, percentLabel(it), results?.stepDate.orEmpty()) } ?: never,
                )
                SettingsDivider()
                StatRow(
                    stringResource(R.string.accuracy_last_distance),
                    results?.distanceError?.let { stringResource(R.string.accuracy_result, percentLabel(it), results?.distanceDate.orEmpty()) } ?: never,
                )
            }

            ToggleGroup(
                options = listOf(
                    true to stringResource(R.string.accuracy_mode_steps),
                    false to stringResource(R.string.accuracy_mode_distance),
                ),
                selected = stepMode,
                onSelect = { if (!state.running) stepMode = it },
                modifier = Modifier.padding(vertical = 12.dp),
            )

            Text(
                text = stringResource(if (stepMode) R.string.accuracy_steps_help else R.string.accuracy_distance_help),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .alpha(0.8f),
            )

            if (!stepMode) {
                NumberField(
                    value = distance,
                    onValueChange = { distance = it },
                    label = stringResource(R.string.calibration_distance_label, if (imperial) "yd" else "m"),
                    decimal = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                )
            }

            if (state.running || finished) {
                SettingsCard {
                    StatRow(stringResource(R.string.accuracy_counted_by_phone), state.steps.toString())
                    if (!stepMode) {
                        SettingsDivider()
                        StatRow(
                            stringResource(R.string.accuracy_estimated_distance),
                            if (imperial) "%.0f yd".format(Locale.getDefault(), estimatedM / YARD_M)
                            else "%.0f m".format(Locale.getDefault(), estimatedM),
                        )
                    }
                    error?.let {
                        SettingsDivider()
                        StatRow(stringResource(R.string.accuracy_error), percentLabel(it))
                    }
                }
            }

            if (finished && stepMode) {
                NumberField(
                    value = counted,
                    onValueChange = { counted = it },
                    label = stringResource(R.string.accuracy_counted_label),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            ) {
                when {
                    state.running -> PrimaryButton(
                        text = stringResource(R.string.calibration_stop),
                        onClick = {
                            session.stop()
                            finished = true
                        },
                    )
                    finished -> {
                        SecondaryButton(stringResource(R.string.calibration_discard), onClick = { finished = false })
                        PrimaryButton(
                            text = stringResource(R.string.accuracy_save),
                            enabled = error != null,
                            onClick = {
                                val today = Util.todayDateString()
                                error?.let {
                                    if (stepMode) AppPreferences.saveStepAccuracy(it, today)
                                    else AppPreferences.saveDistanceAccuracy(it, today)
                                    toast.show(context.getString(R.string.accuracy_saved), ToastKind.SUCCESS)
                                }
                                finished = false
                            },
                        )
                    }
                    else -> PrimaryButton(
                        text = stringResource(R.string.calibration_start),
                        onClick = {
                            counted = TextFieldValue()
                            finished = false
                            session.start(useGps = false)
                        },
                    )
                }
            }
        }
    }
}
