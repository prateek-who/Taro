package com.nvllz.stepsy.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.annotation.ArrayRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
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
import com.nvllz.stepsy.BuildConfig
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.nvllz.stepsy.R
import com.nvllz.stepsy.service.MotionService
import com.nvllz.stepsy.service.isPlayServicesAvailable
import com.nvllz.stepsy.ui.components.FeetInchesDialog
import com.nvllz.stepsy.ui.components.HtmlDialog
import com.nvllz.stepsy.ui.components.NumberInputDialog
import com.nvllz.stepsy.ui.components.PreferenceDivider
import com.nvllz.stepsy.ui.components.PreferenceRow
import com.nvllz.stepsy.ui.components.ScrollingColumn
import com.nvllz.stepsy.ui.components.SectionHeader
import com.nvllz.stepsy.ui.components.SettingsCard
import com.nvllz.stepsy.ui.components.SingleChoiceDialog
import com.nvllz.stepsy.ui.components.StepsyScaffold
import com.nvllz.stepsy.ui.components.StepsySwitch
import com.nvllz.stepsy.util.AppPreferences
import com.nvllz.stepsy.util.AppPreferences.PreferenceKeys
import com.nvllz.stepsy.util.Util
import com.nvllz.stepsy.util.Util.UnitSystem
import kotlinx.coroutines.launch
import java.text.NumberFormat
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
            onFirstDayChanged = {
                navigator.popUntilRoot()
                activity?.recreate()
            },
        )
    }
}

private enum class SettingsDialog { HEIGHT, LEG_LENGTH, STEP_LENGTH, WEIGHT, LANGUAGE, THEME, UNIT_SYSTEM, DATE_FORMAT, FIRST_DAY, ABOUT }

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

private fun cmToInches(cm: Int) = (cm / 2.54).roundToInt()

private fun heightLabel(cm: Int, imperial: Boolean) = if (imperial) {
    val inches = cmToInches(cm)
    "${inches / 12}′${inches % 12}″"
} else {
    "$cm ${Util.heightUnit()}"
}

private fun legLengthLabel(cm: Int, imperial: Boolean) =
    if (imperial) "${cmToInches(cm)} ${Util.stepLengthUnit()}" else "$cm ${Util.heightUnit()}"

private fun stepLengthLabel(cm: Float, imperial: Boolean) =
    "${twoDecimals.format(if (imperial) cm / 2.54f else cm)} ${Util.stepLengthUnit()}"

private fun weightLabel(kg: Int, imperial: Boolean) =
    "${if (imperial) (kg * 2.20462).roundToInt() else kg} ${Util.weightUnit()}"

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
private fun SettingsContent(onBack: () -> Unit, onFirstDayChanged: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }

    val height by AppPreferences.heightFlow().collectAsStateWithLifecycle(AppPreferences.height)
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
    val isFullBuild = BuildConfig.HAS_PROPRIETARY_LIBRARIES
    val hasPlayServices = remember { isFullBuild && isPlayServicesAvailable(context) }

    val languages = choices(R.array.language_names, R.array.language_values)
    val themes = choices(R.array.theme_entries, R.array.theme_values)
    val units = choices(R.array.unit_system_entries, R.array.unit_system_values)
    val dateFormats = choices(R.array.date_format_options, R.array.date_format_values)
    val weekdays = choices(R.array.weekdays, R.array.weekdays_values)

    fun invalid() {
        scope.launch { snackbar.showSnackbar(context.getString(R.string.enter_valid_value)) }
    }

    fun <T> save(key: Preferences.Key<T>, value: T, afterSave: () -> Unit = {}) {
        scope.launch {
            AppPreferences.dataStore.edit { it[key] = value }
            afterSave()
        }
    }

    fun onVehicleFilterChange(checked: Boolean) {
        if (checked && !isPlayServicesAvailable(context)) {
            scope.launch { snackbar.showSnackbar(context.getString(R.string.vehicle_filter_unavailable)) }
            save(PreferenceKeys.VEHICLE_FILTER_ENABLED, false)
        } else {
            save(PreferenceKeys.VEHICLE_FILTER_ENABLED, checked)
        }
    }

    StepsyScaffold(title = stringResource(R.string.settings), onBack = onBack, snackbarHostState = snackbar) { padding ->
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
                    summary = if (abs(stepLength - estimatedStepLength) < 0.01f) {
                        "~${stepLengthLabel(estimatedStepLength, imperial)}"
                    } else {
                        stepLengthLabel(stepLength, imperial)
                    },
                    onClick = { dialog = SettingsDialog.STEP_LENGTH },
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
            }

            SectionHeader(stringResource(R.string.header_general))
            SettingsCard {
                PreferenceRow(
                    icon = R.drawable.ic_car,
                    title = stringResource(R.string.pref_vehicle_filter_title),
                    summary = stringResource(
                        when {
                            !isFullBuild -> R.string.pref_vehicle_filter_summary_foss
                            !hasPlayServices -> R.string.vehicle_filter_unavailable
                            else -> R.string.pref_vehicle_filter_summary
                        }
                    ),
                    enabled = hasPlayServices,
                    onClick = { onVehicleFilterChange(!vehicleFilter) },
                    showChevron = false,
                ) {
                    StepsySwitch(
                        checked = isFullBuild && hasPlayServices && vehicleFilter,
                        onCheckedChange = ::onVehicleFilterChange,
                        enabled = isFullBuild,
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
                        save(PreferenceKeys.HEIGHT, (total * 2.54).roundToInt().coerceIn(1, 250).toString())
                    } else {
                        invalid()
                    }
                },
                onDismiss = dismiss,
            )
        } else {
            NumberInputDialog(
                title = stringResource(R.string.pref_height),
                initial = height.toString(),
                hint = "${Util.heightUnit()} (1-250)",
                onConfirm = { input ->
                    dialog = null
                    val cm = input.toIntOrNull()
                    if (cm != null && cm in 1..250) save(PreferenceKeys.HEIGHT, cm.toString()) else invalid()
                },
                onDismiss = dismiss,
            )
        }

        SettingsDialog.LEG_LENGTH -> NumberInputDialog(
            title = stringResource(R.string.pref_leg_length),
            initial = legLength?.let { if (imperial) cmToInches(it).toString() else it.toString() }.orEmpty(),
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
            initial = if (imperial) (weight * 2.20462).roundToInt().toString() else weight.toString(),
            hint = "${Util.weightUnit()} (${if (imperial) "1-1100" else "1-500"})",
            onConfirm = { input ->
                dialog = null
                val value = input.toIntOrNull()
                when {
                    value == null -> invalid()
                    imperial && value in 1..1100 ->
                        save(PreferenceKeys.WEIGHT, (value / 2.20462).roundToInt().coerceIn(1, 500).toString())
                    !imperial && value in 1..500 -> save(PreferenceKeys.WEIGHT, value.toString())
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
