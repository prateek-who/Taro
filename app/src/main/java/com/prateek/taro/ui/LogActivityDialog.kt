package com.prateek.taro.ui

import com.prateek.taro.util.DayClock
import com.prateek.taro.ui.components.TimeRow
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prateek.taro.R
import com.prateek.taro.energy.Activities
import com.prateek.taro.energy.ActivityType
import com.prateek.taro.energy.CustomActivity
import com.prateek.taro.energy.Intensity
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.NumberField
import com.prateek.taro.ui.components.PillSelector
import com.prateek.taro.ui.components.RangeChip
import com.prateek.taro.ui.components.TaroDialog
import com.prateek.taro.ui.components.TaroTextField
import com.prateek.taro.ui.components.TintChip
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Util
import com.prateek.taro.ui.components.DateRow
import java.time.LocalDate
import kotlin.math.roundToInt

private val MINUTES_RANGE = 1..1440
private val KCAL_RANGE = 1.0..5000.0

private sealed interface Pick {
    data class Builtin(val type: ActivityType) : Pick
    data class Saved(val activity: CustomActivity) : Pick
    data object New : Pick
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogActivityDialog(
    weightKg: Double,
    onSave: (name: String, minutes: Int?, kcal: Double, date: LocalDate, finishedAt: Long) -> Unit,
    onDismiss: () -> Unit,
    initialDate: LocalDate = Util.logicalToday(),
) {
    val context = LocalContext.current
    val toast = LocalToast.current
    val saved by AppPreferences.customActivitiesFlow().collectAsStateWithLifecycle(AppPreferences.customActivities)
    var pick by remember { mutableStateOf<Pick>(saved.lastOrNull()?.let { Pick.Saved(it) } ?: Pick.Builtin(ActivityType.STRENGTH)) }
    var newName by remember { mutableStateOf(TextFieldValue()) }
    var intensity by remember { mutableStateOf(Intensity.MODERATE) }
    var minutes by remember { mutableStateOf(TextFieldValue()) }
    var date by remember { mutableStateOf(initialDate) }
    var finished by remember { mutableIntStateOf(DayClock.minuteOfDay(System.currentTimeMillis())) }
    var kcalInput by remember { mutableStateOf<TextFieldValue?>(null) }

    val builtinName = (pick as? Pick.Builtin)?.let { stringResource(it.type.label) }
    val name = when (val current = pick) {
        is Pick.Builtin -> builtinName.orEmpty()
        is Pick.Saved -> current.activity.name
        Pick.New -> Activities.cleanName(newName.text)
    }
    val met = when (val current = pick) {
        is Pick.Builtin -> current.type.met
        is Pick.Saved -> current.activity.met
        Pick.New -> intensity.met
    }
    val minutesValue = minutes.text.toIntOrNull()?.takeIf { it in MINUTES_RANGE }
    val estimate = minutesValue?.let { Activities.estimateActiveKcal(met, weightKg, it) }
    val kcalField = kcalInput ?: TextFieldValue(estimate?.roundToInt()?.toString().orEmpty())
    val kcalValue = kcalField.text.replace(',', '.').toDoubleOrNull()?.takeIf { it in KCAL_RANGE }
    val valid = kcalValue != null && name.isNotEmpty() && (minutes.text.isBlank() || minutesValue != null)

    fun select(next: Pick) {
        pick = next
        kcalInput = null
    }

    TaroDialog(
        title = stringResource(R.string.activity_dialog_title),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        confirmEnabled = valid,
        onConfirm = {
            if (kcalValue != null) {
                if (pick == Pick.New) {
                    AppPreferences.customActivities = Activities.withAdded(saved, CustomActivity(name, intensity.met))
                }
                onSave(name, minutesValue, kcalValue, date, Util.momentOf(date, finished))
            }
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            DateRow(
                label = stringResource(R.string.date_label),
                date = date,
                today = Util.logicalToday(),
                onChange = { date = it },
            )
            TimeRow(
                label = stringResource(R.string.activity_finished_at),
                minuteOfDay = finished,
                onChange = { finished = it },
            )

            FlowRow(modifier = Modifier.fillMaxWidth()) {
                saved.forEach { activity ->
                    RangeChip(
                        label = activity.name,
                        selected = pick == Pick.Saved(activity),
                        onClick = { select(Pick.Saved(activity)) },
                        onLongClick = {
                            AppPreferences.customActivities = saved - activity
                            if (pick == Pick.Saved(activity)) select(Pick.Builtin(ActivityType.STRENGTH))
                            toast.show(context.getString(R.string.activity_removed, activity.name), ToastKind.INFO)
                        },
                    )
                }
                ActivityType.entries.forEach { type ->
                    RangeChip(
                        label = stringResource(type.label),
                        selected = pick == Pick.Builtin(type),
                        onClick = { select(Pick.Builtin(type)) },
                    )
                }
                RangeChip(
                    label = stringResource(R.string.activity_new),
                    selected = pick == Pick.New,
                    onClick = { select(Pick.New) },
                )
            }

            if (pick == Pick.New) {
                TaroTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = stringResource(R.string.activity_name),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.activity_intensity),
                    style = MaterialTheme.typography.bodySmall,
                    color = TaroTheme.colors.accent,
                    modifier = Modifier.padding(start = 4.dp),
                )
                PillSelector(
                    options = Intensity.entries.map { it to stringResource(it.label) },
                    selected = intensity,
                    onSelect = {
                        intensity = it
                        kcalInput = null
                    },
                )
                Text(
                    text = stringResource(R.string.activity_saved_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = TaroTheme.colors.accent,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            NumberField(
                value = minutes,
                onValueChange = {
                    minutes = it
                    kcalInput = null
                },
                label = stringResource(R.string.activity_minutes),
                suffix = "min",
                modifier = Modifier.fillMaxWidth(),
            )

            NumberField(
                value = kcalField,
                onValueChange = { kcalInput = it },
                label = stringResource(R.string.activity_kcal),
                decimal = true,
                suffix = "kcal",
                large = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (kcalInput == null && estimate != null) {
                TintChip(text = stringResource(R.string.activity_estimate_note), color = TaroTheme.colors.special)
            }

            Text(
                text = stringResource(R.string.activity_tip),
                style = MaterialTheme.typography.bodySmall,
                color = TaroTheme.colors.accent,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
