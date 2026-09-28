package com.nvllz.stepsy.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.nvllz.stepsy.R
import java.util.Calendar

@Composable
fun PauseDialogs(
    onPauseFor: (durationMinutes: Int, specificEndTime: Long) -> Unit,
    onPauseIndefinitely: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showTimePicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        ResumeTimeDialog(onConfirm = onPauseFor, onDismiss = onDismiss)
        return
    }

    val options = listOf(
        R.string.pause_30_minutes to { onPauseFor(30, 0L) },
        R.string.pause_1_hour to { onPauseFor(60, 0L) },
        R.string.pause_2_hours to { onPauseFor(120, 0L) },
        R.string.pause_custom_time to { showTimePicker = true },
        R.string.pause_indefinitely to onPauseIndefinitely,
    )

    SingleChoiceDialog(
        title = stringResource(R.string.pause_step_counting),
        entries = options.map { stringResource(it.first) },
        selectedIndex = -1,
        onSelect = { options[it].second() },
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResumeTimeDialog(
    onConfirm: (durationMinutes: Int, specificEndTime: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val now = remember { Calendar.getInstance() }
    val state = rememberTimePickerState(
        initialHour = now.get(Calendar.HOUR_OF_DAY),
        initialMinute = (now.get(Calendar.MINUTE) + 1).coerceAtMost(59),
        is24Hour = true,
    )

    StepsyDialog(
        title = stringResource(R.string.resume_at_time),
        onDismiss = onDismiss,
        confirmText = stringResource(android.R.string.ok),
        onConfirm = {
            val resumeTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, state.hour)
                set(Calendar.MINUTE, state.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
            }
            val durationMinutes = ((resumeTime.timeInMillis - now.timeInMillis) / 60_000L).toInt()
            onConfirm(durationMinutes, resumeTime.timeInMillis)
        },
    ) {
        TimePicker(
            state = state,
            colors = TimePickerDefaults.colors(
                clockDialColor = MaterialTheme.colorScheme.surface,
                selectorColor = MaterialTheme.colorScheme.primary,
                timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
    }
}
