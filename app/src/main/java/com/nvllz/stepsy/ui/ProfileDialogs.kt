package com.nvllz.stepsy.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nvllz.stepsy.R
import com.nvllz.stepsy.energy.Sex
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
