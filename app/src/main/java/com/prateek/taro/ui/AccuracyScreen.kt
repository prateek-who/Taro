package com.prateek.taro.ui

import com.prateek.taro.ui.theme.TaroTheme
import kotlin.math.abs
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Color
import com.prateek.taro.ui.components.selectedAll
import com.prateek.taro.ui.components.RollingText
import com.prateek.taro.ui.components.StepperField
import com.prateek.taro.ui.components.LinkText
import com.prateek.taro.ui.components.Fact
import com.prateek.taro.ui.components.BentoNote
import com.prateek.taro.ui.components.BentoTile
import com.prateek.taro.ui.components.TextTabs
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prateek.taro.R
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.energy.ActivityEnergy
import com.prateek.taro.energy.EnergyModel
import com.prateek.taro.ui.components.PrimaryButton
import com.prateek.taro.ui.components.rememberWalkSession
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Util
import java.util.Locale

object AccuracyScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        AccuracyContent(onBack = { navigator.pop() })
    }
}

private const val YARD_M = 0.9144
private const val DISTANCE_STEP = 10.0
private const val GOOD_ERROR = 3f
private const val FAIR_ERROR = 6f

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

    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val dayFormat = remember { DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()) }
    fun day(date: String?) = date?.let { runCatching { LocalDate.parse(it).format(dayFormat) }.getOrNull() }.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        AppOverflowMenu(back = true, title = stringResource(R.string.accuracy_title), onBack = onBack)
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.gap),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.loose, end = spacing.loose, bottom = spacing.loose),
        ) {
            BentoTile(color = colors.goal, label = stringResource(R.string.accuracy_last_results), tinted = true, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(top = spacing.tight)) {
                    val stepError = results?.stepError
                    val distanceError = results?.distanceError
                    Fact(
                        stepError?.let(::percentLabel) ?: "…",
                        if (stepError != null) stringResource(R.string.accuracy_steps_off, day(results?.stepDate)) else stringResource(R.string.accuracy_steps_untested),
                        errorColor(stepError),
                        Modifier.weight(1f),
                        alignment = Alignment.CenterStart,
                    )
                    Fact(
                        distanceError?.let(::percentLabel) ?: "…",
                        if (distanceError != null) stringResource(R.string.accuracy_distance_off, day(results?.distanceDate)) else stringResource(R.string.accuracy_distance_untested),
                        errorColor(distanceError),
                        Modifier.weight(1f),
                        alignment = Alignment.CenterStart,
                    )
                }
                BentoNote(stringResource(R.string.accuracy_intro), Modifier.padding(top = spacing.tight))
            }

            TextTabs(
                options = listOf(
                    true to stringResource(R.string.accuracy_mode_steps),
                    false to stringResource(R.string.accuracy_mode_distance),
                ),
                selected = stepMode,
                onSelect = { if (!state.running && !finished) stepMode = it },
                color = colors.goal,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            if (!state.running && !finished) {
                BentoNote(stringResource(if (stepMode) R.string.accuracy_steps_help else R.string.accuracy_distance_help))
            }

            if (!stepMode && !state.running) {
                BentoTile(color = colors.accent, label = stringResource(R.string.calibration_distance_label, if (imperial) "yd" else "m"), modifier = Modifier.fillMaxWidth()) {
                    StepperField(
                        value = distance,
                        onValueChange = { distance = it },
                        unit = if (imperial) "yd" else "m",
                        onMinus = { distance = Util.formatMeasure(((Util.parseMeasure(distance.text) ?: 0.0) - DISTANCE_STEP).coerceAtLeast(DISTANCE_STEP)).selectedAll() },
                        onPlus = { distance = Util.formatMeasure((Util.parseMeasure(distance.text) ?: 0.0) + DISTANCE_STEP).selectedAll() },
                        decimal = true,
                        modifier = Modifier.padding(top = spacing.tight),
                    )
                }
            }

            if (state.running || finished) {
                BentoTile(color = errorColor(error), tinted = true, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        RollingText(Util.formatSteps(state.steps), type.hero.copy(color = MaterialTheme.colorScheme.onSurface))
                        Text(stringResource(R.string.accuracy_counted_by_phone_short), style = type.unit, color = colors.accent, modifier = Modifier.padding(start = spacing.tight, bottom = spacing.tight).weight(1f))
                    }
                    Row(modifier = Modifier.padding(top = spacing.gap)) {
                        if (!stepMode) {
                            Fact(
                                if (imperial) "%.0f yd".format(Locale.getDefault(), estimatedM / YARD_M) else "%.0f m".format(Locale.getDefault(), estimatedM),
                                stringResource(R.string.accuracy_estimated_distance_short),
                                MaterialTheme.colorScheme.onSurface,
                                Modifier.weight(1f),
                                style = type.factCompact,
                                alignment = Alignment.CenterStart,
                            )
                        }
                        error?.let {
                            Fact(percentLabel(it), stringResource(R.string.accuracy_off), errorColor(it), Modifier.weight(1f), style = type.factCompact, alignment = Alignment.CenterStart)
                        }
                    }
                    if (finished && stepMode) {
                        Text(stringResource(R.string.accuracy_counted_label), style = type.meterLabel, color = colors.accent, modifier = Modifier.padding(top = spacing.loose))
                        StepperField(
                            value = counted,
                            onValueChange = { counted = it },
                            unit = "",
                            onMinus = { counted = ((countedSteps ?: state.steps) - 1).coerceAtLeast(1).toString().selectedAll() },
                            onPlus = { counted = ((countedSteps ?: state.steps) + 1).toString().selectedAll() },
                            modifier = Modifier.padding(top = spacing.tight),
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = spacing.tight)) {
                when {
                    state.running -> PrimaryButton(
                        text = stringResource(R.string.calibration_stop),
                        tint = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            session.stop()
                            finished = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    finished -> {
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
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LinkText(stringResource(R.string.calibration_discard), colors.accent, onClick = { finished = false }, modifier = Modifier.padding(top = spacing.tight))
                    }
                    else -> PrimaryButton(
                        text = stringResource(R.string.calibration_start),
                        onClick = {
                            counted = TextFieldValue()
                            finished = false
                            session.start(useGps = false)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun errorColor(error: Float?): Color = when {
    error == null -> TaroTheme.colors.goal
    abs(error) < GOOD_ERROR -> TaroTheme.colors.goal
    abs(error) < FAIR_ERROR -> TaroTheme.colors.special
    else -> TaroTheme.colors.flame
}
