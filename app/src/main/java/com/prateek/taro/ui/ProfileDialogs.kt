package com.prateek.taro.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.prateek.taro.ui.components.StepperButton
import kotlinx.coroutines.launch
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.ui.components.RollingText
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.LinkText
import com.prateek.taro.service.MotionService
import androidx.datastore.preferences.core.edit
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.prateek.taro.R
import com.prateek.taro.util.Util
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.energy.DietGoal
import com.prateek.taro.energy.Metabolism
import com.prateek.taro.energy.Sex
import com.prateek.taro.ui.components.NumberField
import com.prateek.taro.ui.components.TaroDialog
import com.prateek.taro.ui.components.ToggleGroup
import com.prateek.taro.util.CalorieGoal
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import java.util.Locale
import com.prateek.taro.ui.components.NumberInputDialog
import com.prateek.taro.ui.components.SingleChoiceDialog

val AGE_RANGE = 13..110

@Composable
fun sexLabel(sex: Sex): String = stringResource(
    when (sex) {
        Sex.MALE -> R.string.sex_male
        Sex.FEMALE -> R.string.sex_female
        Sex.UNSPECIFIED -> R.string.sex_unspecified
    }
)

@Composable
fun AgeDialog(current: Int?, onConfirm: (Int) -> Unit, onInvalid: () -> Unit, onDismiss: () -> Unit) {
    NumberInputDialog(
        title = stringResource(R.string.pref_age),
        initial = current?.toString().orEmpty(),
        hint = "${stringResource(R.string.pref_age)} (${AGE_RANGE.first}-${AGE_RANGE.last})",
        onConfirm = { input ->
            val age = input.toIntOrNull()
            if (age != null && age in AGE_RANGE) onConfirm(age) else onInvalid()
        },
        onDismiss = onDismiss,
    )
}

@Composable
fun SexDialog(current: Sex?, onSelect: (Sex) -> Unit, onDismiss: () -> Unit) {
    SingleChoiceDialog(
        title = stringResource(R.string.pref_sex),
        entries = Sex.entries.map { sexLabel(it) },
        selectedIndex = current?.let { Sex.entries.indexOf(it) } ?: -1,
        onSelect = { onSelect(Sex.entries[it]) },
        onDismiss = onDismiss,
    )
}

val ADJUSTMENT_RANGE = 50..1500

@Composable
fun calorieGoalSummary(goal: CalorieGoal?): String = when (goal?.goal) {
    null -> stringResource(R.string.calorie_goal_not_set)
    DietGoal.CUT -> stringResource(R.string.calorie_goal_cut_summary, goal.adjustment.toString())
    DietGoal.MAINTAIN -> stringResource(R.string.calorie_goal_maintain_summary)
    DietGoal.BULK -> stringResource(R.string.calorie_goal_bulk_summary, goal.adjustment.toString())
}

@Composable
fun CalorieGoalDialog(current: CalorieGoal?, onSave: (CalorieGoal) -> Unit, onDismiss: () -> Unit) {
    var goal by remember { mutableStateOf(current?.goal ?: DietGoal.MAINTAIN) }
    var adjustment by remember {
        mutableStateOf(TextFieldValue(current?.adjustment?.takeIf { it > 0 }?.toString() ?: "500"))
    }
    val adjustmentValue = adjustment.text.toIntOrNull()?.takeIf { it in ADJUSTMENT_RANGE }
    val valid = goal == DietGoal.MAINTAIN || adjustmentValue != null

    TaroDialog(
        title = stringResource(R.string.calorie_goal),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        confirmEnabled = valid,
        onConfirm = { onSave(CalorieGoal(goal, if (goal == DietGoal.MAINTAIN) 0 else adjustmentValue ?: 0)) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ToggleGroup(
                options = listOf(
                    DietGoal.CUT to stringResource(R.string.calorie_goal_cut),
                    DietGoal.MAINTAIN to stringResource(R.string.calorie_goal_maintain),
                    DietGoal.BULK to stringResource(R.string.calorie_goal_bulk),
                ),
                selected = goal,
                onSelect = { goal = it },
            )
            if (goal != DietGoal.MAINTAIN) {
                NumberField(
                    value = adjustment,
                    onValueChange = { adjustment = it },
                    label = stringResource(
                        if (goal == DietGoal.CUT) R.string.calorie_goal_adjustment_cut else R.string.calorie_goal_adjustment_bulk
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                adjustmentValue?.let {
                    Text(
                        text = stringResource(
                            R.string.calorie_goal_weekly,
                            "%+.2f".format(Locale.getDefault(), Metabolism.weeklyChangeKg(goal, it)),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = stringResource(R.string.calorie_goal_help),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.alpha(0.7f),
                )
            }
        }
    }
}

@Composable
fun ProteinTargetDialog(onDismiss: () -> Unit) {
    NumberInputDialog(
        title = stringResource(R.string.food_protein_dialog),
        initial = Util.formatMeasure(AppPreferences.proteinPerKg.toDouble()),
        hint = "g / kg",
        decimal = true,
        supportingText = stringResource(R.string.food_protein_hint),
        onConfirm = { input ->
            onDismiss()
            Util.parseMeasure(input)?.let { AppPreferences.proteinPerKg = it.toFloat() }
        },
        onDismiss = onDismiss,
    )
}

@Composable
fun StepGoalDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = TaroTheme.colors
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    val current = remember { AppPreferences.dailyGoalTarget }
    var goal by remember { mutableIntStateOf(current.takeIf { it > 0 } ?: DEFAULT_GOAL) }
    val today = Util.logicalToday()
    val suggestions = rememberInBackground(today) { goalSuggestions(context, today) }?.value

    TaroDialog(
        title = stringResource(R.string.steps_goal_title),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        onConfirm = {
            val target = goal
            onDismiss()
            scope.launch {
                AppPreferences.dataStore.edit { AppPreferences.setGoal(it, target, today) }
                context.startService(
                    Intent(context, MotionService::class.java).apply {
                        action = "UPDATE_NOTIFICATION"
                        putExtra("show_progressbar", AppPreferences.dailyGoalNotificationProgressbar)
                        putExtra("daily_target", target)
                    }
                )
                toast.show(context.getString(R.string.daily_goals_saved), ToastKind.SUCCESS)
            }
        },
    ) {
        val stepper = with(LocalDensity.current) { type.value.lineHeight.toDp() }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.loose)) {
                StepperButton(R.drawable.ic_remove, stringResource(R.string.steps_goal_lower), enabled = goal > GOAL_STEP, size = stepper) { goal = (goal - GOAL_STEP).coerceAtLeast(GOAL_STEP) }
                RollingText(Util.formatSteps(goal), type.value.copy(color = colors.goal))
                StepperButton(R.drawable.ic_add, stringResource(R.string.steps_goal_raise), enabled = goal < MAX_GOAL, size = stepper) { goal = (goal + GOAL_STEP).coerceAtMost(MAX_GOAL) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.gap), modifier = Modifier.padding(top = spacing.gap)) {
                suggestions?.average?.let { LinkText(stringResource(R.string.steps_goal_average, Util.formatSteps(it)), colors.goal, onClick = { goal = it }) }
                suggestions?.bestWeek?.takeIf { it != suggestions.average }?.let { LinkText(stringResource(R.string.steps_goal_best_week, Util.formatSteps(it)), colors.goal, onClick = { goal = it }) }
            }
            Text(
                text = stringResource(R.string.steps_goal_note),
                style = type.note,
                color = colors.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = spacing.tight),
            )
        }
    }
}


private const val DEFAULT_GOAL = 10_000
private const val MAX_GOAL = 100_000
