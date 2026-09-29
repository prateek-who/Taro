package com.nvllz.stepsy.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nvllz.stepsy.R
import com.nvllz.stepsy.energy.DietGoal
import com.nvllz.stepsy.energy.Metabolism
import com.nvllz.stepsy.energy.Sex
import com.nvllz.stepsy.ui.components.NumberField
import com.nvllz.stepsy.ui.components.StepsyDialog
import com.nvllz.stepsy.ui.components.ToggleGroup
import com.nvllz.stepsy.util.CalorieGoal
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import java.util.Locale
import com.nvllz.stepsy.ui.components.NumberInputDialog
import com.nvllz.stepsy.ui.components.SingleChoiceDialog

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

    StepsyDialog(
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
