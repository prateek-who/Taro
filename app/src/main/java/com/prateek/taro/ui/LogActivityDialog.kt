package com.prateek.taro.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prateek.taro.R
import com.prateek.taro.data.LoggedActivity
import com.prateek.taro.energy.Activities
import com.prateek.taro.energy.ActivityType
import com.prateek.taro.energy.CustomActivity
import com.prateek.taro.energy.Intensity
import com.prateek.taro.ui.components.BentoNote
import com.prateek.taro.ui.components.BentoTile
import com.prateek.taro.ui.components.BentoValue
import com.prateek.taro.ui.components.EditRow
import com.prateek.taro.ui.components.IconTile
import com.prateek.taro.ui.components.IconTileGrid
import com.prateek.taro.ui.components.LinkText
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.NumberField
import com.prateek.taro.ui.components.PrimaryButton
import com.prateek.taro.ui.components.StepperField
import com.prateek.taro.ui.components.TaroDialog
import com.prateek.taro.ui.components.TaroTextField
import com.prateek.taro.ui.components.TextTabs
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.ui.components.WhenDialog
import com.prateek.taro.ui.components.dayLabel
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.DayClock
import com.prateek.taro.util.Util
import java.time.LocalDate
import java.util.Date
import kotlin.math.roundToInt

private val MINUTES_RANGE = 1..1440
private val KCAL_RANGE = 1.0..5000.0
private const val MINUTE_STEP = 5
private const val DEFAULT_MINUTES = 30
private const val TILE_COLUMNS = 4
private val QUICK_MINUTES = listOf(15, 30, 45, 60, 90)

private sealed interface Pick {
    data class Builtin(val type: ActivityType) : Pick
    data class Saved(val activity: CustomActivity) : Pick
    data object New : Pick
}

@DrawableRes
private fun ActivityType.icon(): Int = when (this) {
    ActivityType.STRENGTH -> R.drawable.ic_act_strength
    ActivityType.CALISTHENICS -> R.drawable.ic_skill_pullup
    ActivityType.CYCLING -> R.drawable.ic_act_cycling
    ActivityType.SWIMMING -> R.drawable.ic_act_swimming
    ActivityType.YOGA -> R.drawable.ic_act_yoga
    ActivityType.ROWING -> R.drawable.ic_act_rowing
    ActivityType.HIIT -> R.drawable.ic_badge_bolt
}

@Composable
private fun ActivityType.color(): Color {
    val colors = TaroTheme.colors
    return when (this) {
        ActivityType.STRENGTH -> colors.flame
        ActivityType.CALISTHENICS -> colors.red
        ActivityType.CYCLING -> colors.mint
        ActivityType.SWIMMING -> PROTEIN_BLUE
        ActivityType.YOGA -> colors.sleep
        ActivityType.ROWING -> colors.goal
        ActivityType.HIIT -> colors.purple
    }
}

@Composable
fun LogActivityDialog(
    weightKg: Double,
    onSave: (name: String, minutes: Int?, kcal: Double, date: LocalDate, finishedAt: Long) -> Unit,
    onDismiss: () -> Unit,
    initialDate: LocalDate = Util.logicalToday(),
    initialType: ActivityType? = null,
    initialMinutes: Int? = null,
    initialFinish: Long? = null,
    editing: LoggedActivity? = null,
) {
    val context = LocalContext.current
    val toast = LocalToast.current
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val saved by AppPreferences.customActivitiesFlow().collectAsStateWithLifecycle(AppPreferences.customActivities)
    val builtinLabels = ActivityType.entries.map { it to stringResource(it.label) }
    var pick by remember {
        mutableStateOf<Pick>(
            editing?.let { old ->
                builtinLabels.firstOrNull { it.second == old.name }?.let { Pick.Builtin(it.first) }
                    ?: saved.firstOrNull { it.name == old.name }?.let { Pick.Saved(it) }
                    ?: Pick.New
            } ?: initialType?.let { Pick.Builtin(it) } ?: saved.lastOrNull()?.let { Pick.Saved(it) } ?: Pick.Builtin(ActivityType.STRENGTH)
        )
    }
    var newName by remember { mutableStateOf(TextFieldValue(editing?.name.orEmpty())) }
    var intensity by remember { mutableStateOf(Intensity.MODERATE) }
    var minutes by remember { mutableStateOf(TextFieldValue((editing?.durationMinutes ?: initialMinutes ?: DEFAULT_MINUTES).toString())) }
    var date by remember { mutableStateOf(editing?.let { LocalDate.parse(it.date) } ?: initialDate) }
    var finished by remember { mutableIntStateOf(DayClock.minuteOfDay(editing?.loggedAt ?: initialFinish ?: System.currentTimeMillis())) }
    var kcalInput by remember { mutableStateOf(editing?.let { TextFieldValue(it.kcal.roundToInt().toString()) }) }
    var editingKcal by remember { mutableStateOf(editing != null) }
    var pickingWhen by remember { mutableStateOf(false) }

    val name = when (val current = pick) {
        is Pick.Builtin -> stringResource(current.type.label)
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
        if (editing == null) {
            kcalInput = null
            editingKcal = false
        }
    }

    fun setMinutes(value: Int) {
        minutes = TextFieldValue(value.coerceIn(MINUTES_RANGE).toString())
        if (!editingKcal) kcalInput = null
    }

    val newLabel = stringResource(R.string.activity_new_tile)
    val tiles: List<Triple<Pick, String, Pair<Int, Color>>> =
        saved.map { Triple(Pick.Saved(it), it.name, R.drawable.ic_act_custom to colors.special) } +
            ActivityType.entries.map { Triple(Pick.Builtin(it), stringResource(it.label), it.icon() to it.color()) } +
            Triple(Pick.New, newLabel, R.drawable.ic_add to colors.accent)

    TaroDialog(
        title = stringResource(if (editing != null) R.string.activity_edit_title else R.string.activity_dialog_title),
        onDismiss = onDismiss,
        dismissText = null,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            IconTileGrid(tiles, TILE_COLUMNS) { (option, label, look), modifier ->
                IconTile(
                    icon = look.first,
                    label = label,
                    color = look.second,
                    selected = pick == option,
                    onClick = { select(option) },
                    onLongClick = (option as? Pick.Saved)?.let { savedPick ->
                        {
                            AppPreferences.customActivities = saved - savedPick.activity
                            if (pick == option) select(Pick.Builtin(ActivityType.STRENGTH))
                            toast.show(context.getString(R.string.activity_removed, savedPick.activity.name), ToastKind.INFO)
                        }
                    },
                    modifier = modifier,
                )
            }

            if (pick == Pick.New) {
                TaroTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = stringResource(R.string.activity_name),
                    modifier = Modifier.fillMaxWidth().padding(top = spacing.loose),
                )
                TextTabs(
                    options = Intensity.entries.map { it to stringResource(it.label) },
                    selected = intensity,
                    onSelect = {
                        intensity = it
                        if (!editingKcal) kcalInput = null
                    },
                    color = colors.flame,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = spacing.gap),
                )
            }

            Text(
                stringResource(R.string.activity_how_long),
                style = type.note,
                color = colors.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = spacing.loose),
            )
            StepperField(
                value = minutes,
                onValueChange = {
                    minutes = it
                    if (!editingKcal) kcalInput = null
                },
                unit = stringResource(R.string.activity_min_unit),
                onMinus = { setMinutes((minutesValue ?: DEFAULT_MINUTES) - MINUTE_STEP) },
                onPlus = { setMinutes((minutesValue ?: 0) + MINUTE_STEP) },
                minusEnabled = (minutesValue ?: 0) > MINUTE_STEP,
                modifier = Modifier.padding(top = spacing.tight),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.gap, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
                QUICK_MINUTES.forEach { value ->
                    LinkText(value.toString(), if (minutesValue == value) colors.flame else colors.accent, onClick = { setMinutes(value) })
                }
            }

            BentoTile(color = colors.flame, tinted = true, modifier = Modifier.fillMaxWidth().padding(top = spacing.gap), onClick = { editingKcal = true }) {
                if (editingKcal) {
                    NumberField(
                        value = kcalField,
                        onValueChange = { kcalInput = it },
                        label = stringResource(R.string.activity_kcal),
                        decimal = true,
                        suffix = "kcal",
                        large = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BentoValue(kcalValue?.roundToInt()?.toString() ?: "-", colors.flame, Modifier.weight(1f), unit = stringResource(R.string.activity_burned_unit))
                        Icon(
                            painterResource(R.drawable.ic_edit),
                            contentDescription = stringResource(R.string.activity_kcal),
                            tint = colors.accent,
                            modifier = Modifier.size(with(LocalDensity.current) { type.meterValue.lineHeight.toDp() }),
                        )
                    }
                }
                BentoNote(
                    when {
                        kcalInput != null -> stringResource(R.string.activity_burn_yours)
                        minutesValue == null -> stringResource(R.string.activity_burn_need_time)
                        else -> stringResource(R.string.activity_burn_note, minutesValue, name.ifEmpty { newLabel }.lowercase(), Util.formatWeight(weightKg))
                    }
                )
            }

            val timeFormat = remember { android.text.format.DateFormat.getTimeFormat(context) }
            EditRow(
                icon = R.drawable.ic_schedule,
                color = colors.accent,
                label = dayLabel(date, Util.logicalToday()),
                value = stringResource(R.string.activity_finished_value, timeFormat.format(Date(Util.momentOf(date, finished)))),
                onClick = { pickingWhen = true },
            )

            PrimaryButton(
                text = stringResource(R.string.action_save),
                enabled = valid,
                tint = colors.flame,
                onClick = {
                    if (kcalValue != null) {
                        if (pick == Pick.New && saved.none { it.name == name }) {
                            AppPreferences.customActivities = Activities.withAdded(saved, CustomActivity(name, intensity.met))
                        }
                        onSave(name, minutesValue, kcalValue, date, Util.momentOf(date, finished))
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = spacing.gap),
            )
            Text(
                stringResource(R.string.activity_tip_short),
                style = type.note,
                color = colors.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = spacing.gap),
            )
        }
    }

    if (pickingWhen) {
        WhenDialog(
            title = stringResource(R.string.activity_when),
            date = date,
            minuteOfDay = finished,
            timeLabel = stringResource(R.string.activity_finished_at),
            onSave = { day, minute ->
                date = day
                finished = minute
                pickingWhen = false
            },
            onDismiss = { pickingWhen = false },
        )
    }
}
