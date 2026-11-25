package com.prateek.taro.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.animateFloatAsState
import com.prateek.taro.ui.theme.TaroMotion
import com.prateek.taro.ui.components.selectedAll
import com.prateek.taro.ui.components.RollingText
import com.prateek.taro.ui.components.StepperField
import com.prateek.taro.ui.components.SectionLabel
import com.prateek.taro.ui.components.RowDivider
import com.prateek.taro.ui.components.Panel
import com.prateek.taro.ui.components.NavRow
import com.prateek.taro.ui.components.Meter
import com.prateek.taro.ui.components.LinkText
import com.prateek.taro.ui.components.Fact
import com.prateek.taro.ui.components.BentoNote
import com.prateek.taro.ui.components.BentoTile
import com.prateek.taro.ui.components.TextTabs
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.prateek.taro.calibration.CalibrationRepository
import com.prateek.taro.calibration.CalibrationWindow
import com.prateek.taro.ui.components.PrimaryButton
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
private const val USUAL_SPREAD = 5
private const val CADENCE_MIN = 60
private const val CADENCE_MAX = 200
private const val DISTANCE_STEP = 10.0
private const val STRONG_FIX_M = 10f
private const val FAIR_FIX_M = 25f

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

    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        AppOverflowMenu(
            back = true,
            title = stringResource(test?.title ?: R.string.calibration_title),
            onBack = { if (test != null) leaveTest() else onBack() },
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.gap),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.loose, end = spacing.loose, bottom = spacing.loose),
        ) {
            if (test == null) {
                PaceList(
                    overview = overview,
                    imperial = imperial,
                    walking = calibration?.let { stepLabel(it.walkingStepCm / 100.0, imperial) },
                    samples = calibration?.samples,
                    running = calibration?.runningStepCm?.let { stepLabel(it / 100.0, imperial) },
                    onPick = { pace = it },
                )
                if (calibration != null || overview?.paces?.isNotEmpty() == true) {
                    LinkText(
                        text = stringResource(R.string.calibration_reset),
                        color = colors.accent,
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) { CalibrationRepository.reset(context) }
                                saves++
                            }
                        },
                    )
                }
                return@Column
            }

            TextTabs(
                options = listOf(
                    true to stringResource(R.string.calibration_mode_gps),
                    false to stringResource(R.string.calibration_mode_distance),
                ),
                selected = gpsMode,
                onSelect = { if (!state.running && !finished) gpsMode = it },
                color = colors.goal,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            if (!state.running && !finished) {
                BentoNote(stringResource(test.hint))
                BentoNote(stringResource(if (gpsMode) R.string.calibration_gps_help else R.string.calibration_distance_help))
            }

            if (!gpsMode && !state.running) {
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
                LiveTile(
                    pace = test,
                    steps = state.steps,
                    elapsed = elapsedLabel(state.elapsedS),
                    cadence = state.cadence ?: resultWindows.takeIf { it.isNotEmpty() }?.map { it.cadence }?.average()?.roundToInt(),
                    usualCadence = overview?.paces?.get(CalibrationPace.USUAL)?.cadence?.roundToInt(),
                    samples = resultWindows.size.takeIf { gpsMode },
                    facts = buildList {
                        if (gpsMode) {
                            add(Triple(distanceLabel(state.gpsDistanceM, imperial), stringResource(R.string.calibration_gps_distance_short), MaterialTheme.colorScheme.onSurface))
                            val accuracy = state.gpsAccuracyM
                            add(
                                Triple(
                                    accuracy?.let { "±%.0f m".format(Locale.getDefault(), it) } ?: stringResource(R.string.calibration_waiting_short),
                                    stringResource(R.string.calibration_signal_short),
                                    when {
                                        accuracy == null -> colors.accent
                                        accuracy <= STRONG_FIX_M -> colors.goal
                                        accuracy <= FAIR_FIX_M -> colors.special
                                        else -> colors.flame
                                    },
                                )
                            )
                        }
                        add(Triple(measuredStepM?.let { stepLabel(it, imperial) } ?: "…", stringResource(R.string.calibration_so_far), colors.goal))
                    },
                )
            }

            val problem = when {
                !gpsMode || resultWindows.isNotEmpty() || state.elapsedS < 60 -> null
                state.gpsFixes == 0 -> R.string.calibration_no_fix
                state.usableFixes * 2 < state.gpsFixes -> R.string.calibration_weak_gps
                state.steps * 60 < state.elapsedS * 40 -> R.string.calibration_few_steps
                else -> null
            }
            val warning = when {
                (state.running || finished) && problem != null -> problem
                finished && !canSave -> if (gpsMode) R.string.calibration_need_more else R.string.calibration_too_few_steps
                else -> null
            }
            warning?.let { Text(stringResource(it), style = TaroTheme.text.note, color = MaterialTheme.colorScheme.error) }

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
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LinkText(stringResource(R.string.calibration_discard), colors.accent, onClick = { finished = false }, modifier = Modifier.padding(top = spacing.tight))
                    }
                    else -> {
                        PrimaryButton(text = stringResource(R.string.calibration_start), onClick = ::start, modifier = Modifier.fillMaxWidth())
                        LinkText(stringResource(R.string.calibration_all_paces), colors.accent, onClick = ::leaveTest, modifier = Modifier.padding(top = spacing.tight))
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
    samples: Int?,
    running: String?,
    onPick: (CalibrationPace) -> Unit,
) {
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val tested = overview?.paces?.size ?: 0
    BentoTile(color = colors.goal, label = stringResource(R.string.calibration_your_step), tinted = true, modifier = Modifier.fillMaxWidth()) {
        if (walking == null) {
            BentoNote(stringResource(R.string.calibration_not_calibrated), Modifier.padding(top = spacing.tight))
        } else {
            Row(modifier = Modifier.padding(top = spacing.tight)) {
                Fact(walking, stringResource(R.string.calibration_walking_short), colors.goal, Modifier.weight(1f), alignment = Alignment.CenterStart)
                Fact(running ?: "…", stringResource(R.string.calibration_running_short), if (running != null) colors.goal else colors.accent, Modifier.weight(1f), alignment = Alignment.CenterStart)
            }
            samples?.let { BentoNote(pluralStringResource(R.plurals.calibration_from_samples, it, it), Modifier.padding(top = spacing.tight)) }
        }
        BentoNote(stringResource(R.string.calibration_intro), Modifier.padding(top = spacing.tight))
        Meter(
            label = stringResource(R.string.calibration_variety),
            value = stringResource(R.string.calibration_variety_value, tested, CalibrationPace.entries.size),
            fraction = tested.toFloat() / CalibrationPace.entries.size,
            color = colors.goal,
            modifier = Modifier.padding(top = spacing.gap),
        )
        if (overview != null && overview.paces.isNotEmpty()) {
            BentoNote(stringResource(if (overview.variety) R.string.calibration_pace_ok else R.string.calibration_pace_more), Modifier.padding(top = spacing.tight))
        }
    }
    SectionLabel(stringResource(R.string.calibration_paces))
    Panel {
        Column(modifier = Modifier.padding(horizontal = spacing.tight)) {
            CalibrationPace.entries.forEachIndexed { index, pace ->
                if (index > 0) RowDivider()
                NavRow(
                    icon = R.drawable.ic_steps,
                    color = paceColor(pace),
                    title = stringResource(pace.title),
                    note = overview?.paces?.get(pace)?.let { paceSummary(it, imperial) } ?: stringResource(R.string.calibration_pace_untested),
                    onClick = { onPick(pace) },
                )
            }
        }
    }
    BentoNote(stringResource(R.string.calibration_paces_help))
}

@Composable
private fun paceColor(pace: CalibrationPace) = when (pace) {
    CalibrationPace.SLOW -> TaroTheme.colors.sleep
    CalibrationPace.USUAL -> TaroTheme.colors.goal
    CalibrationPace.BRISK -> TaroTheme.colors.flame
    CalibrationPace.JOG -> TaroTheme.colors.red
}

@Composable
private fun paceSummary(result: PaceResult, imperial: Boolean) = stringResource(
    R.string.calibration_pace_result,
    stepLabel(result.stepLengthM, imperial),
    result.cadence.roundToInt(),
    stringResource(if (result.measured) R.string.calibration_source_measured else R.string.calibration_source_gps),
)

@Composable
private fun LiveTile(
    pace: CalibrationPace,
    steps: Int,
    elapsed: String,
    cadence: Int?,
    usualCadence: Int?,
    samples: Int?,
    facts: List<Triple<String, String, Color>>,
) {
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val color = paceColor(pace)
    val target = when (pace) {
        CalibrationPace.SLOW -> usualCadence?.let { CADENCE_MIN..(it - PACE_GAP) }
        CalibrationPace.USUAL -> usualCadence?.let { (it - USUAL_SPREAD)..(it + USUAL_SPREAD) }
        CalibrationPace.BRISK -> usualCadence?.let { minOf(it + PACE_GAP, BRISK_LIMIT)..CADENCE_MAX }
        CalibrationPace.JOG -> EnergyModel.RUNNING_CADENCE..CADENCE_MAX
    }
    fun share(value: Int) = ((value - CADENCE_MIN).toFloat() / (CADENCE_MAX - CADENCE_MIN)).coerceIn(0f, 1f)
    BentoTile(color = color, label = stringResource(pace.title), tinted = true, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = spacing.tight)) {
            RollingText(Util.formatSteps(steps), type.hero.copy(color = MaterialTheme.colorScheme.onSurface))
            Text(stringResource(R.string.calibration_steps_unit), style = type.unit, color = colors.accent, modifier = Modifier.padding(start = spacing.tight, bottom = spacing.tight).weight(1f))
            Text(elapsed, style = type.fact, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = spacing.tight))
        }
        Meter(
            label = stringResource(R.string.calibration_cadence),
            value = cadence?.let { stringResource(R.string.calibration_cadence_value, it) } ?: stringResource(R.string.calibration_cadence_waiting),
            fraction = cadence?.let(::share) ?: 0f,
            color = color,
            band = target?.let { share(it.first)..share(it.last) },
            modifier = Modifier.padding(top = spacing.gap),
        )
        target?.let {
            BentoNote(
                when (pace) {
                    CalibrationPace.SLOW -> stringResource(R.string.calibration_aim_below, it.last)
                    CalibrationPace.USUAL -> stringResource(R.string.calibration_aim_hold, it.first, it.last)
                    else -> stringResource(R.string.calibration_aim_above, it.first)
                },
                Modifier.padding(top = spacing.tight),
            )
        }
        if (samples != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = spacing.loose)) {
                Text(stringResource(R.string.calibration_samples), style = type.meterLabel, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.calibration_progress, minOf(samples, pace.samples), pace.samples), style = type.meterValue, color = colors.accent)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.tight), modifier = Modifier.padding(top = spacing.tight)) {
                repeat(pace.samples) { index ->
                    val lit by animateFloatAsState(if (index < samples) 1f else 0f, TaroMotion.bouncy(), label = "sample")
                    Box(
                        modifier = Modifier
                            .size(spacing.loose)
                            .graphicsLayer {
                                scaleX = 0.8f + 0.2f * lit
                                scaleY = scaleX
                            }
                            .clip(CircleShape)
                            .background(if (index < samples) color else colors.accentOpaque),
                    )
                }
            }
        }
        Row(modifier = Modifier.padding(top = spacing.loose)) {
            facts.forEach { (value, label, tint) ->
                Fact(value, label, tint, Modifier.weight(1f), style = type.factCompact, alignment = Alignment.CenterStart)
            }
        }
    }
}
