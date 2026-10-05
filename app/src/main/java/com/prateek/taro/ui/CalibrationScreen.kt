package com.prateek.taro.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.prateek.taro.calibration.CalibrationOverview
import com.prateek.taro.calibration.CalibrationPace
import com.prateek.taro.energy.EnergyModel
import com.prateek.taro.calibration.PaceResult
import com.prateek.taro.ui.theme.TaroTheme
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
import com.prateek.taro.R
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.calibration.CalibrationRepository
import com.prateek.taro.calibration.CalibrationWindow
import com.prateek.taro.calibration.StepLengthModel
import com.prateek.taro.ui.components.NumberField
import com.prateek.taro.ui.components.PreferenceRow
import com.prateek.taro.ui.components.PrimaryButton
import com.prateek.taro.ui.components.ScrollingColumn
import com.prateek.taro.ui.components.SecondaryButton
import com.prateek.taro.ui.components.SettingsCard
import com.prateek.taro.ui.components.SettingsDivider
import com.prateek.taro.ui.components.StatRow
import com.prateek.taro.ui.components.TaroScaffold
import com.prateek.taro.ui.components.ToggleGroup
import com.prateek.taro.ui.components.rememberWalkSession
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Util
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
private const val PACE_GAP = 15
private const val BRISK_LIMIT = EnergyModel.RUNNING_CADENCE - 5

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

    var pace by rememberSaveable { mutableStateOf<CalibrationPace?>(null) }
    var gpsMode by rememberSaveable { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }
    var distance by remember { mutableStateOf(TextFieldValue(if (imperial) "440" else "400")) }
    var saves by remember { mutableIntStateOf(0) }
    val overview = rememberInBackground(saves, calibration) { CalibrationRepository.overview(context) }?.value
    val haptics = LocalHapticFeedback.current
    val test = pace

    fun message(textRes: Int, kind: ToastKind = ToastKind.ERROR) {
        toast.show(context.getString(textRes), kind)
    }

    fun leaveTest() {
        session.stop()
        finished = false
        pace = null
    }

    BackHandler(enabled = test != null, onBack = ::leaveTest)

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        when {
            granted[Manifest.permission.ACCESS_FINE_LOCATION] != true -> message(R.string.calibration_location_needed)
            !session.isGpsEnabled() -> message(R.string.calibration_gps_off)
            else -> session.start(useGps = true)
        }
    }

    fun start() {
        finished = false
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
    val measuredSteps = resultWindows.sumOf { it.steps }
    val measuredStepM = if (measuredSteps > 0) resultWindows.sumOf { it.distanceM } / measuredSteps else null
    val canSave = measuredStepM != null && if (gpsMode) resultWindows.size >= MIN_WINDOWS else resultWindows.isNotEmpty()

    LaunchedEffect(resultWindows.size) {
        if (state.running && gpsMode && test != null && resultWindows.size >= test.samples) {
            session.stop()
            finished = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    TaroScaffold(
        title = stringResource(test?.title ?: R.string.calibration_title),
        onBack = { if (test != null) leaveTest() else onBack() },
    ) { padding ->
        ScrollingColumn(padding, modifier = Modifier.padding(horizontal = 16.dp)) {
            if (test == null) {
                PaceList(
                    overview = overview,
                    imperial = imperial,
                    walking = calibration?.let { stringResource(R.string.calibration_status, stepLabel(it.walkingStepCm / 100.0, imperial), it.samples) },
                    running = calibration?.runningStepCm?.let { stepLabel(it / 100.0, imperial) },
                    onPick = { pace = it },
                )
                if (calibration != null || overview?.paces?.isNotEmpty() == true) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        SecondaryButton(
                            text = stringResource(R.string.calibration_reset),
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) { CalibrationRepository.reset(context) }
                                    saves++
                                }
                            },
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                }
                return@ScrollingColumn
            }

            Text(
                text = stringResource(test.hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            ToggleGroup(
                options = listOf(
                    true to stringResource(R.string.calibration_mode_gps),
                    false to stringResource(R.string.calibration_mode_distance),
                ),
                selected = gpsMode,
                onSelect = { if (!state.running && !finished) gpsMode = it },
                modifier = Modifier.padding(bottom = 12.dp),
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

            if (state.running) {
                PaceCard(
                    pace = test,
                    samples = resultWindows.size.takeIf { gpsMode },
                    cadence = state.cadence,
                    usualCadence = overview?.paces?.get(CalibrationPace.USUAL)?.cadence?.roundToInt(),
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
                    }
                    if (resultWindows.isNotEmpty()) {
                        SettingsDivider()
                        StatRow(
                            stringResource(R.string.calibration_cadence),
                            stringResource(R.string.calibration_cadence_value, resultWindows.map { it.cadence }.average().roundToInt()),
                        )
                    }
                    measuredStepM?.let {
                        SettingsDivider()
                        StatRow(stringResource(R.string.calibration_estimate), stepLabel(it, imperial))
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
                                    val accepted = withContext(Dispatchers.IO) { CalibrationRepository.save(context, resultWindows, source, test) }
                                    saves++
                                    finished = false
                                    pace = null
                                    if (accepted) message(R.string.calibration_saved, ToastKind.SUCCESS) else message(R.string.calibration_rejected)
                                }
                            },
                        )
                    }
                    else -> {
                        SecondaryButton(stringResource(R.string.calibration_all_paces), onClick = ::leaveTest)
                        PrimaryButton(text = stringResource(R.string.calibration_start), onClick = ::start)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaceList(
    overview: CalibrationOverview?,
    imperial: Boolean,
    walking: String?,
    running: String?,
    onPick: (CalibrationPace) -> Unit,
) {
    val notCalibrated = stringResource(R.string.calibration_not_calibrated)
    Text(
        text = stringResource(R.string.calibration_intro),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 12.dp),
    )
    SettingsCard {
        StatRow(label = stringResource(R.string.calibration_walking), value = walking ?: notCalibrated)
        SettingsDivider()
        StatRow(label = stringResource(R.string.calibration_running), value = running ?: notCalibrated)
    }
    Text(
        text = stringResource(R.string.calibration_paces_help),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .padding(vertical = 12.dp)
            .alpha(0.8f),
    )
    SettingsCard {
        CalibrationPace.entries.forEachIndexed { index, pace ->
            if (index > 0) SettingsDivider()
            PreferenceRow(
                icon = R.drawable.ic_steps,
                title = stringResource(pace.title),
                summary = overview?.paces?.get(pace)?.let { paceSummary(it, imperial) } ?: stringResource(R.string.calibration_pace_untested),
                onClick = { onPick(pace) },
            )
        }
    }
    if (overview != null && overview.paces.isNotEmpty()) {
        Text(
            text = stringResource(if (overview.variety) R.string.calibration_pace_ok else R.string.calibration_pace_more),
            style = MaterialTheme.typography.bodySmall,
            color = if (overview.variety) TaroTheme.colors.goal else TaroTheme.colors.accent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun paceSummary(result: PaceResult, imperial: Boolean) = stringResource(
    R.string.calibration_pace_result,
    stepLabel(result.stepLengthM, imperial),
    result.cadence.roundToInt(),
    stringResource(if (result.measured) R.string.calibration_source_measured else R.string.calibration_source_gps),
)

@Composable
private fun PaceCard(pace: CalibrationPace, samples: Int?, cadence: Int?, usualCadence: Int?) {
    val aim = when (pace) {
        CalibrationPace.SLOW -> usualCadence?.let { stringResource(R.string.calibration_aim_below, it - PACE_GAP) }
        CalibrationPace.USUAL -> null
        CalibrationPace.BRISK -> usualCadence?.let { stringResource(R.string.calibration_aim_above, minOf(it + PACE_GAP, BRISK_LIMIT)) }
        CalibrationPace.JOG -> stringResource(R.string.calibration_aim_above, EnergyModel.RUNNING_CADENCE)
    }
    SettingsCard {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            if (samples != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    repeat(pace.samples) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (index < samples) TaroTheme.colors.goal else TaroTheme.colors.accentOpaque),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.calibration_progress, minOf(samples, pace.samples), pace.samples).uppercase(),
                    fontSize = 12.sp,
                    color = TaroTheme.colors.accent,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            Text(
                text = cadence?.let { stringResource(R.string.calibration_cadence_value, it) } ?: stringResource(R.string.calibration_cadence_waiting),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (aim != null) {
                Text(
                    text = aim,
                    fontSize = 16.sp,
                    color = TaroTheme.colors.flame,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
