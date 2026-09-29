package com.prateek.taro.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.annotation.ArrayRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prateek.taro.BuildConfig
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prateek.taro.R
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.service.MidnightResetReceiver
import com.prateek.taro.service.MotionService
import com.prateek.taro.ui.components.TimePickerDialog
import com.prateek.taro.service.isPlayServicesAvailable
import com.prateek.taro.ui.components.FeetInchesDialog
import com.prateek.taro.ui.components.HtmlDialog
import com.prateek.taro.ui.components.NumberInputDialog
import com.prateek.taro.ui.components.PreferenceDivider
import com.prateek.taro.ui.components.PreferenceRow
import com.prateek.taro.ui.components.ScrollingColumn
import com.prateek.taro.ui.components.SectionHeader
import com.prateek.taro.ui.components.SettingsCard
import com.prateek.taro.ui.components.SingleChoiceDialog
import com.prateek.taro.ui.components.TaroScaffold
import com.prateek.taro.ui.components.TaroSwitch
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.AppPreferences.PreferenceKeys
import com.prateek.taro.util.Util
import com.prateek.taro.util.Util.UnitSystem
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object SettingsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val activity = LocalActivity.current
        SettingsContent(
            onBack = { navigator.pop() },
            onOpenCalibration = { navigator.push(CalibrationScreen) },
            onOpenAccuracy = { navigator.push(AccuracyScreen) },
            onFirstDayChanged = {
                navigator.popUntilRoot()
                activity?.recreate()
            },
        )
    }
}

private enum class SettingsDialog { DAY_START, HEIGHT, AGE, SEX, CALORIE_GOAL, LEG_LENGTH, STEP_LENGTH, WEIGHT, LANGUAGE, THEME, UNIT_SYSTEM, DATE_FORMAT, FIRST_DAY, ABOUT }

private class Choices(val entries: List<String>, val values: List<String>) {
    fun label(value: String) = entries.getOrNull(values.indexOf(value)) ?: value
    fun index(value: String) = values.indexOf(value).coerceAtLeast(0)
}

@Composable
private fun choices(@ArrayRes entries: Int, @ArrayRes values: Int) =
    Choices(stringArrayResource(entries).toList(), stringArrayResource(values).toList())

private val twoDecimals: NumberFormat
    get() = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
        isGroupingUsed = false
    }

private fun cmToInches(cm: Double) = (cm / 2.54).roundToInt()

private const val LBS_PER_KG = 2.20462

private fun heightLabel(cm: Double, imperial: Boolean) = if (imperial) {
    val inches = cmToInches(cm)
    "${inches / 12}′${inches % 12}″"
} else {
    "${Util.formatMeasure(cm)} ${Util.heightUnit()}"
}

private fun legLengthLabel(cm: Int, imperial: Boolean) =
    if (imperial) "${cmToInches(cm.toDouble())} ${Util.stepLengthUnit()}" else "$cm ${Util.heightUnit()}"

private fun stepLengthLabel(cm: Float, imperial: Boolean) =
    "${twoDecimals.format(if (imperial) cm / 2.54f else cm)} ${Util.stepLengthUnit()}"

private fun weightLabel(kg: Double, imperial: Boolean) =
    "${Util.formatMeasure(if (imperial) roundOne(kg * LBS_PER_KG) else kg)} ${Util.weightUnit()}"

private fun roundOne(value: Double) = (value * 10).roundToInt() / 10.0

private fun currentLanguage(): String {
    val locales = AppCompatDelegate.getApplicationLocales()
    return if (locales.isEmpty) "system" else locales[0]?.language ?: "system"
}

private fun restartMotionService(context: Context) {
    val intent = Intent(context, MotionService::class.java)
    context.stopService(intent)
    ContextCompat.startForegroundService(context, intent)
}

@Composable
private fun SettingsContent(
    onBack: () -> Unit,
    onOpenCalibration: () -> Unit,
    onOpenAccuracy: () -> Unit,
    onFirstDayChanged: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toast = LocalToast.current
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }

    val height by AppPreferences.heightFlow().collectAsStateWithLifecycle(AppPreferences.height)
    val calibration by AppPreferences.stepCalibrationFlow().collectAsStateWithLifecycle(AppPreferences.stepCalibration)
    val manualStepLength = AppPreferences.manualStepLength
    val age by AppPreferences.ageFlow().collectAsStateWithLifecycle(AppPreferences.age)
    val sex by AppPreferences.sexFlow().collectAsStateWithLifecycle(AppPreferences.sex)
    val calorieGoal by AppPreferences.calorieGoalFlow().collectAsStateWithLifecycle(AppPreferences.calorieGoal)
    val dayStart by AppPreferences.dayStartMinutesFlow().collectAsStateWithLifecycle(AppPreferences.dayStartMinutes)
    val dayStartLabel = remember(dayStart) {
        val time = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, dayStart / 60)
            set(Calendar.MINUTE, dayStart % 60)
        }
        android.text.format.DateFormat.getTimeFormat(context).format(time.time)
    }
    val legLength by AppPreferences.legLengthFlow().collectAsStateWithLifecycle(AppPreferences.legLength)
    val weight by AppPreferences.weightFlow().collectAsStateWithLifecycle(AppPreferences.weight)
    val stepLength by AppPreferences.stepLengthFlow().collectAsStateWithLifecycle(AppPreferences.stepLength)
    val unitSystem by AppPreferences.unitSystemFlow().collectAsStateWithLifecycle(AppPreferences.unitSystem)
    val theme by AppPreferences.themeFlow().collectAsStateWithLifecycle(AppPreferences.theme)
    val dateFormat by AppPreferences.dateFormatStringFlow().collectAsStateWithLifecycle(AppPreferences.dateFormatString)
    val firstDay by AppPreferences.firstDayOfWeekFlow().collectAsStateWithLifecycle(AppPreferences.firstDayOfWeek)
    val vehicleFilter by AppPreferences.vehicleFilterEnabledFlow().collectAsStateWithLifecycle(AppPreferences.vehicleFilterEnabled)

    val imperial = unitSystem == UnitSystem.IMPERIAL
    val unitValue = if (imperial) "imperial" else "metric"
    val estimatedStepLength = Util.estimateStepLength(height, legLength)
    val hasPlayServices = remember { isPlayServicesAvailable(context) }

    val languages = choices(R.array.language_names, R.array.language_values)
    val themes = choices(R.array.theme_entries, R.array.theme_values)
    val units = choices(R.array.unit_system_entries, R.array.unit_system_values)
    val dateFormats = choices(R.array.date_format_options, R.array.date_format_values)
    val weekdays = choices(R.array.weekdays, R.array.weekdays_values)

    fun invalid() {
        toast.show(context.getString(R.string.enter_valid_value), ToastKind.ERROR)
    }

    fun <T> save(key: Preferences.Key<T>, value: T, afterSave: () -> Unit = {}) {
        scope.launch {
            AppPreferences.dataStore.edit { it[key] = value }
            afterSave()
        }
    }

    fun onVehicleFilterChange(checked: Boolean) {
        if (checked && !isPlayServicesAvailable(context)) {
            toast.show(context.getString(R.string.vehicle_filter_unavailable), ToastKind.ERROR)
            save(PreferenceKeys.VEHICLE_FILTER_ENABLED, false)
        } else {
            save(PreferenceKeys.VEHICLE_FILTER_ENABLED, checked)
        }
    }

    TaroScaffold(title = stringResource(R.string.settings), onBack = onBack) { padding ->
        ScrollingColumn(padding) {
            SectionHeader(stringResource(R.string.header_personal_data))
            SettingsCard {
                PreferenceRow(
                    icon = R.drawable.ic_height,
                    title = stringResource(R.string.pref_height),
                    summary = heightLabel(height, imperial),
                    onClick = { dialog = SettingsDialog.HEIGHT },
                    showChevron = false,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = R.drawable.ic_weight,
                    title = stringResource(R.string.pref_weight),
                    summary = weightLabel(weight, imperial),
                    onClick = { dialog = SettingsDialog.WEIGHT },
                    showChevron = false,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = R.drawable.ic_age,
                    title = stringResource(R.string.pref_age),
                    summary = age?.toString() ?: stringResource(R.string.pref_not_set),
                    onClick = { dialog = SettingsDialog.AGE },
                    showChevron = false,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = R.drawable.ic_sex,
                    title = stringResource(R.string.pref_sex),
                    summary = sex?.let { sexLabel(it) } ?: stringResource(R.string.pref_not_set),
                    onClick = { dialog = SettingsDialog.SEX },
                    showChevron = false,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = R.drawable.ic_calorie_goal,
                    title = stringResource(R.string.calorie_goal),
                    summary = calorieGoalSummary(calorieGoal),
                    onClick = { dialog = SettingsDialog.CALORIE_GOAL },
                    showChevron = false,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = R.drawable.ic_leg_length,
                    title = stringResource(R.string.pref_leg_length),
                    summary = legLength?.let { legLengthLabel(it, imperial) } ?: stringResource(R.string.pref_leg_length_not_set),
                    onClick = { dialog = SettingsDialog.LEG_LENGTH },
                    showChevron = false,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = R.drawable.ic_step_length,
                    title = stringResource(R.string.pref_step_length),
                    summary = when {
                        manualStepLength == null && calibration != null ->
                            "${stepLengthLabel(stepLength, imperial)} · ${stringResource(R.string.pref_step_length_calibrated)}"
                        abs(stepLength - estimatedStepLength) < 0.01f -> "~${stepLengthLabel(estimatedStepLength, imperial)}"
                        else -> stepLengthLabel(stepLength, imperial)
                    },
                    onClick = { dialog = SettingsDialog.STEP_LENGTH },
                    showChevron = false,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = R.drawable.ic_calibrate,
                    title = stringResource(R.string.calibration_title),
                    summary = calibration?.let {
                        stringResource(R.string.calibration_status, stepLengthLabel(it.walkingStepCm, imperial), it.samples)
                    } ?: stringResource(R.string.calibration_not_calibrated),
                    onClick = onOpenCalibration,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = R.drawable.ic_accuracy,
                    title = stringResource(R.string.accuracy_title),
                    summary = stringResource(R.string.accuracy_summary),
                    onClick = onOpenAccuracy,
                )
            }

            SectionHeader(stringResource(R.string.header_general))
            SettingsCard {
                PreferenceRow(
                    icon = R.drawable.ic_car,
                    title = stringResource(R.string.pref_vehicle_filter_title),
                    summary = stringResource(
                        if (hasPlayServices) R.string.pref_vehicle_filter_summary else R.string.vehicle_filter_unavailable
                    ),
                    enabled = hasPlayServices,
                    onClick = { onVehicleFilterChange(!vehicleFilter) },
                    showChevron = false,
                ) {
                    TaroSwitch(
                        checked = hasPlayServices && vehicleFilter,
                        onCheckedChange = ::onVehicleFilterChange,
                        enabled = hasPlayServices,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
                PreferenceDivider()
                PreferenceRow(
                    icon = R.drawable.ic_unit,
                    title = stringResource(R.string.unit_system),
                    summary = units.label(unitValue),
                    onClick = { dialog = SettingsDialog.UNIT_SYSTEM },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = R.drawable.ic_dateformat,
                    title = stringResource(R.string.date_format),
                    summary = dateFormat,
                    onClick = { dialog = SettingsDialog.DATE_FORMAT },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = R.drawable.ic_firstday,
                    title = stringResource(R.string.first_day_of_the_week),
                    summary = weekdays.label(firstDay.toString()),
                    onClick = { dialog = SettingsDialog.FIRST_DAY },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = R.drawable.ic_day_start,
                    title = stringResource(R.string.pref_day_start),
                    summary = stringResource(R.string.pref_day_start_summary, dayStartLabel),
                    onClick = { dialog = SettingsDialog.DAY_START },
                )
            }

            SectionHeader(stringResource(R.string.header_appearance))
            SettingsCard {
                PreferenceRow(
                    icon = R.drawable.ic_language,
                    title = stringResource(R.string.app_language),
                    summary = languages.label(currentLanguage()),
                    onClick = { dialog = SettingsDialog.LANGUAGE },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = R.drawable.ic_theme,
                    title = stringResource(R.string.theme),
                    summary = themes.label(theme),
                    onClick = { dialog = SettingsDialog.THEME },
                )
            }

            SectionHeader(stringResource(R.string.header_about))
            SettingsCard {
                PreferenceRow(
                    icon = R.drawable.ic_info,
                    title = stringResource(R.string.about_stepsy),
                    summary = "${stringResource(R.string.about_version)}: ${BuildConfig.VERSION_NAME}  •  GPL-3.0",
                    onClick = { dialog = SettingsDialog.ABOUT },
                )
            }
        }
    }

    val dismiss = { dialog = null }

    @Composable
    fun Choice(titleRes: Int, choices: Choices, current: String, onChosen: (String) -> Unit) {
        SingleChoiceDialog(
            title = stringResource(titleRes),
            entries = choices.entries,
            selectedIndex = choices.index(current),
            onSelect = {
                dialog = null
                onChosen(choices.values[it])
            },
            onDismiss = dismiss,
        )
    }

    when (dialog) {
        SettingsDialog.HEIGHT -> if (imperial) {
            val inches = cmToInches(height)
            FeetInchesDialog(
                title = stringResource(R.string.pref_height),
                initialFeet = inches / 12,
                initialInches = inches % 12,
                onConfirm = { feet, extraInches ->
                    dialog = null
                    val total = feet * 12 + extraInches
                    if (total in 12..98) {
                        save(PreferenceKeys.HEIGHT, roundOne(total * 2.54).coerceIn(1.0, 250.0).toString())
                    } else {
                        invalid()
                    }
                },
                onDismiss = dismiss,
            )
        } else {
            NumberInputDialog(
                title = stringResource(R.string.pref_height),
                initial = Util.formatMeasure(height),
                hint = "${Util.heightUnit()} (1-250)",
                decimal = true,
                onConfirm = { input ->
                    dialog = null
                    val cm = Util.parseMeasure(input)?.let(::roundOne)
                    if (cm != null && cm in 1.0..250.0) save(PreferenceKeys.HEIGHT, cm.toString()) else invalid()
                },
                onDismiss = dismiss,
            )
        }

        SettingsDialog.AGE -> AgeDialog(
            current = age,
            onConfirm = {
                dialog = null
                AppPreferences.age = it
            },
            onInvalid = {
                dialog = null
                invalid()
            },
            onDismiss = dismiss,
        )

        SettingsDialog.DAY_START -> TimePickerDialog(
            title = stringResource(R.string.pref_day_start),
            initialHour = dayStart / 60,
            initialMinute = dayStart % 60,
            is24Hour = android.text.format.DateFormat.is24HourFormat(context),
            onConfirm = { hour, minute ->
                dialog = null
                AppPreferences.dayStartMinutes = hour * 60 + minute
                MidnightResetReceiver.scheduleNextMidnightAlarm(context)
                context.startService(Intent(context, MotionService::class.java).putExtra("FORCE_UPDATE", true))
                toast.show(context.getString(R.string.pref_day_start_saved), ToastKind.SUCCESS)
            },
            onDismiss = dismiss,
        )

        SettingsDialog.CALORIE_GOAL -> CalorieGoalDialog(
            current = calorieGoal,
            onSave = {
                dialog = null
                AppPreferences.calorieGoal = it
                toast.show(context.getString(R.string.calorie_goal_saved), ToastKind.SUCCESS)
            },
            onDismiss = dismiss,
        )

        SettingsDialog.SEX -> SexDialog(
            current = sex,
            onSelect = {
                dialog = null
                AppPreferences.sex = it
            },
            onDismiss = dismiss,
        )

        SettingsDialog.LEG_LENGTH -> NumberInputDialog(
            title = stringResource(R.string.pref_leg_length),
            initial = legLength?.let { if (imperial) cmToInches(it.toDouble()).toString() else it.toString() }.orEmpty(),
            hint = if (imperial) "${Util.stepLengthUnit()} (12-60)" else "${Util.heightUnit()} (30-150)",
            supportingText = stringResource(R.string.leg_length_hint),
            onConfirm = { input ->
                dialog = null
                val value = input.toIntOrNull()
                val cm = value?.let { if (imperial) (it * 2.54).roundToInt() else it }
                when {
                    input.isBlank() -> AppPreferences.legLength = null
                    cm != null && cm in 30..150 -> save(PreferenceKeys.LEG_LENGTH, cm.toString())
                    else -> invalid()
                }
            },
            onDismiss = dismiss,
        )

        SettingsDialog.STEP_LENGTH -> {
            val factor = if (imperial) 2.54f else 1f
            val range = if (imperial) 0.5f..60f else 1f..150f
            NumberInputDialog(
                title = stringResource(R.string.pref_step_length),
                initial = if (imperial) twoDecimals.format(stepLength / factor) else stepLength.toString(),
                hint = "~${twoDecimals.format(estimatedStepLength / factor)} ${Util.stepLengthUnit()}",
                decimal = true,
                onConfirm = { input ->
                    dialog = null
                    val value = input.replace(',', '.').toFloatOrNull()
                    when {
                        input.isBlank() -> AppPreferences.resetStepLength()
                        value != null && value in range -> save(PreferenceKeys.STEP_LENGTH, value * factor)
                        else -> invalid()
                    }
                },
                onDismiss = dismiss,
            )
        }

        SettingsDialog.WEIGHT -> NumberInputDialog(
            title = stringResource(R.string.pref_weight),
            initial = Util.formatMeasure(if (imperial) roundOne(weight * LBS_PER_KG) else weight),
            hint = "${Util.weightUnit()} (${if (imperial) "1-1100" else "1-500"})",
            decimal = true,
            onConfirm = { input ->
                dialog = null
                val value = Util.parseMeasure(input)?.let(::roundOne)
                when {
                    value == null -> invalid()
                    imperial && value in 1.0..1100.0 ->
                        save(PreferenceKeys.WEIGHT, (value / LBS_PER_KG).coerceIn(1.0, 500.0).toString())
                    !imperial && value in 1.0..500.0 -> save(PreferenceKeys.WEIGHT, value.toString())
                    else -> invalid()
                }
            },
            onDismiss = dismiss,
        )

        SettingsDialog.LANGUAGE -> Choice(R.string.app_language, languages, currentLanguage()) { chosen ->
            AppCompatDelegate.setApplicationLocales(
                if (chosen == "system") LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.create(Locale.forLanguageTag(chosen))
            )
        }

        SettingsDialog.THEME -> Choice(R.string.theme, themes, theme) { chosen ->
            save(PreferenceKeys.THEME, chosen) { Util.applyTheme(chosen) }
        }

        SettingsDialog.UNIT_SYSTEM -> Choice(R.string.unit_system, units, unitValue) { chosen ->
            save(PreferenceKeys.UNIT_SYSTEM, chosen)
            restartMotionService(context)
        }

        SettingsDialog.DATE_FORMAT -> Choice(R.string.date_format, dateFormats, dateFormat) { chosen ->
            save(PreferenceKeys.DATE_FORMAT, chosen)
        }

        SettingsDialog.FIRST_DAY -> Choice(R.string.first_day_of_the_week, weekdays, firstDay.toString()) { chosen ->
            save(PreferenceKeys.FIRST_DAY_OF_WEEK, chosen) { onFirstDayChanged() }
        }

        SettingsDialog.ABOUT -> HtmlDialog(
            title = stringResource(R.string.about_stepsy),
            html = stringResource(R.string.about_html, BuildConfig.VERSION_NAME),
            onDismiss = dismiss,
        )

        null -> Unit
    }
}
