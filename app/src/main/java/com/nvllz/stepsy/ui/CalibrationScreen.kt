package com.nvllz.stepsy.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.nvllz.stepsy.calibration.GuidedPlan
import com.nvllz.stepsy.energy.EnergyModel
import com.nvllz.stepsy.calibration.GuidedProgress
import com.nvllz.stepsy.ui.theme.StepsyTheme
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.nvllz.stepsy.calibration.CalibrationRepository
import com.nvllz.stepsy.calibration.CalibrationSample
import com.nvllz.stepsy.calibration.CalibrationWindow
import com.nvllz.stepsy.calibration.StepLengthModel
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

object CalibrationScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        CalibrationContent(onBack = { navigator.pop() })
    }
}

private const val MIN_WINDOWS = 3
private const val MIN_KNOWN_DISTANCE_STEPS = 50
private const val YARD_M = 0.9144

private fun stepLabel(meters: Double, imperial: Boolean) =
    if (imperial) "%.1f in".format(Locale.getDefault(), meters * 100 / 2.54)
    else "%.1f cm".format(Locale.getDefault(), meters * 100)

private fun distanceLabel(meters: Double, imperial: Boolean) =
    if (imperial) "%.0f yd".format(Locale.getDefault(), meters / YARD_M)
    else "%.0f m".format(Locale.getDefault(), meters)

private fun elapsedLabel(seconds: Long) = "%d:%02d".format(Locale.getDefault(), seconds / 60, seconds % 60)

@Composable
private fun CalibrationContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toast = LocalToast.current
    val session = rememberWalkSession()
    val state by session.state.collectAsStateWithLifecycle()
    val calibration by AppPreferences.stepCalibrationFlow().collectAsStateWithLifecycle(AppPreferences.stepCalibration)
    val imperial = AppPreferences.unitSystem == Util.UnitSystem.IMPERIAL

    var gpsMode by rememberSaveable { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }
    var distance by remember { mutableStateOf(TextFieldValue(if (imperial) "440" else "400")) }
    val skips = remember { mutableStateListOf<Long>() }
    val guided = GuidedPlan.progress(state.elapsedS, skips)
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(guided.index) {
        if (state.running && guided.index > 0) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun message(textRes: Int, kind: ToastKind = ToastKind.ERROR) {
        toast.show(context.getString(textRes), kind)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        when {
            granted[Manifest.permission.ACCESS_FINE_LOCATION] != true -> message(R.string.calibration_location_needed)
            !session.isGpsEnabled() -> message(R.string.calibration_gps_off)
            else -> session.start(useGps = true)
        }
    }

    fun start() {
        finished = false
        skips.clear()
        when {
            !gpsMode -> session.start(useGps = false)
            !session.hasPreciseLocation() -> permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
            !session.isGpsEnabled() -> message(R.string.calibration_gps_off)
            else -> session.start(useGps = true)
        }
    }

    val knownDistanceM = distance.text.toDoubleOrNull()?.let { if (imperial) it * YARD_M else it }
    val resultWindows: List<CalibrationWindow> = if (gpsMode) {
        state.windows
    } else {
        listOfNotNull(
            knownDistanceM?.takeIf { state.steps >= MIN_KNOWN_DISTANCE_STEPS && state.elapsedS > 0 }?.let {
                CalibrationWindow(state.steps, it, state.elapsedS.toInt())
            }
        )
    }
    val preview = StepLengthModel.fit(resultWindows.map { CalibrationSample(it.cadence, it.stepLengthM, it.distanceM) })
    val samples = resultWindows.map { CalibrationSample(it.cadence, it.stepLengthM, it.distanceM) }
    val walkingCadences = resultWindows.map { it.cadence }.filter { it < EnergyModel.RUNNING_CADENCE }
    val hasFit = preview.walkingStepM != null || preview.runningStepM != null
    val canSave = hasFit && if (gpsMode) resultWindows.size >= MIN_WINDOWS else resultWindows.isNotEmpty()

    StepsyScaffold(title = stringResource(R.string.calibration_title), onBack = onBack) { padding ->
        ScrollingColumn(padding, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = stringResource(R.string.calibration_intro),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            SettingsCard {
                val notCalibrated = stringResource(R.string.calibration_not_calibrated)
                StatRow(
                    label = stringResource(R.string.calibration_walking),
                    value = calibration?.let {
                        stringResource(R.string.calibration_status, stepLabel(it.walkingStepCm / 100.0, imperial), it.samples)
                    } ?: notCalibrated,
                )
                SettingsDivider()
                StatRow(
                    label = stringResource(R.string.calibration_running),
                    value = calibration?.runningStepCm?.let { stepLabel(it / 100.0, imperial) } ?: notCalibrated,
                )
            }

            ToggleGroup(
                options = listOf(
                    true to stringResource(R.string.calibration_mode_gps),
                    false to stringResource(R.string.calibration_mode_distance),
                ),
                selected = gpsMode,
                onSelect = { if (!state.running) gpsMode = it },
                modifier = Modifier.padding(vertical = 12.dp),
            )

            Text(
                text = stringResource(if (gpsMode) R.string.calibration_gps_help else R.string.calibration_distance_help),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .alpha(0.8f),
            )

            if (!gpsMode) {
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

            if (gpsMode && state.running) {
                GuidedPhaseCard(
                    progress = guided,
                    cadence = state.cadence,
                    onNext = { skips.add(state.elapsedS) },
                )
            }

            if (state.running || finished) {
                SettingsCard {
                    StatRow(stringResource(R.string.calibration_elapsed), elapsedLabel(state.elapsedS))
                    SettingsDivider()
                    StatRow(stringResource(R.string.calibration_steps), state.steps.toString())
                    if (gpsMode) {
                        SettingsDivider()
                        StatRow(stringResource(R.string.calibration_gps_distance), distanceLabel(state.gpsDistanceM, imperial))
                        SettingsDivider()
                        StatRow(
                            stringResource(R.string.calibration_signal),
                            state.gpsAccuracyM?.let { "±%.0f m".format(Locale.getDefault(), it) }
                                ?: stringResource(R.string.calibration_waiting_gps),
                        )
                        SettingsDivider()
                        StatRow(
                            stringResource(R.string.calibration_fixes),
                            stringResource(R.string.calibration_fixes_value, state.usableFixes, state.gpsFixes),
                        )
                        SettingsDivider()
                        StatRow(stringResource(R.string.calibration_samples), resultWindows.size.toString())
                        if (walkingCadences.isNotEmpty()) {
                            SettingsDivider()
                            StatRow(
                                stringResource(R.string.calibration_pace_range),
                                stringResource(
                                    R.string.calibration_pace_range_value,
                                    walkingCadences.min().roundToInt(),
                                    walkingCadences.max().roundToInt(),
                                ),
                            )
                            Text(
                                text = stringResource(
                                    if (StepLengthModel.coversPaceRange(samples)) R.string.calibration_pace_ok else R.string.calibration_pace_more
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (StepLengthModel.coversPaceRange(samples)) StepsyTheme.colors.goal else StepsyTheme.colors.accent,
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                            )
                        }
                    }
                    preview.walkingStepM?.let {
                        SettingsDivider()
                        StatRow(stringResource(R.string.calibration_estimate), stepLabel(it, imperial))
                    }
                    preview.runningStepM?.let {
                        SettingsDivider()
                        StatRow(stringResource(R.string.calibration_running), stepLabel(it, imperial))
                    }
                }
            }

            val problem = when {
                !gpsMode || resultWindows.isNotEmpty() || state.elapsedS < 60 -> null
                state.gpsFixes == 0 -> R.string.calibration_no_fix
                state.usableFixes * 2 < state.gpsFixes -> R.string.calibration_weak_gps
                state.steps * 60 < state.elapsedS * 40 -> R.string.calibration_few_steps
                else -> null
            }
            if ((state.running || finished) && problem != null) {
                Text(
                    text = stringResource(problem),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            if (finished && !canSave && problem == null) {
                Text(
                    text = stringResource(if (gpsMode) R.string.calibration_need_more else R.string.calibration_too_few_steps),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
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
                            text = stringResource(R.string.calibration_save),
                            enabled = canSave,
                            onClick = {
                                val source = if (gpsMode) CalibrationRepository.SOURCE_GPS else CalibrationRepository.SOURCE_KNOWN_DISTANCE
                                scope.launch {
                                    withContext(Dispatchers.IO) { CalibrationRepository.save(context, resultWindows, source) }
                                    finished = false
                                    message(R.string.calibration_saved, ToastKind.SUCCESS)
                                }
                            },
                        )
                    }
                    else -> PrimaryButton(text = stringResource(R.string.calibration_start), onClick = ::start)
                }
            }

            if (calibration != null && !state.running) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    SecondaryButton(
                        text = stringResource(R.string.calibration_reset),
                        onClick = { scope.launch { withContext(Dispatchers.IO) { CalibrationRepository.reset(context) } } },
                    )
                }
            }
        }
    }
}

@Composable
private fun GuidedPhaseCard(progress: GuidedProgress, cadence: Int?, onNext: () -> Unit) {
    val phase = progress.phase
    SettingsCard {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                GuidedPlan.phases.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (index <= progress.index) StepsyTheme.colors.goal else StepsyTheme.colors.accentOpaque),
                    )
                }
            }
            Text(
                text = if (phase != null) {
                    stringResource(R.string.calibration_phase_step, progress.index + 1, GuidedPlan.phases.size).uppercase()
                } else {
                    ""
                },
                fontSize = 12.sp,
                color = StepsyTheme.colors.accent,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(phase?.title ?: R.string.calibration_phase_done_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(phase?.hint ?: R.string.calibration_phase_done_hint),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .alpha(0.8f),
            )
            if (phase != null) {
                Text(
                    text = stringResource(R.string.calibration_phase_left, elapsedLabel(progress.secondsLeft.toLong())),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            Text(
                text = cadence?.let { stringResource(R.string.calibration_cadence_value, it) } ?: "",
                fontSize = 16.sp,
                color = StepsyTheme.colors.flame,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (phase != null) {
                SecondaryButton(stringResource(R.string.calibration_next), onClick = onNext, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
