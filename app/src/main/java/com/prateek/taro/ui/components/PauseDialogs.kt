package com.prateek.taro.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.prateek.taro.R
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

@Composable
private fun ResumeTimeDialog(
    onConfirm: (durationMinutes: Int, specificEndTime: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val now = remember { Calendar.getInstance() }
    TimePickerDialog(
        title = stringResource(R.string.resume_at_time),
        initialHour = now.get(Calendar.HOUR_OF_DAY),
        initialMinute = (now.get(Calendar.MINUTE) + 1).coerceAtMost(59),
        onConfirm = { hour, minute ->
            val resumeTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
            }
            val durationMinutes = ((resumeTime.timeInMillis - now.timeInMillis) / 60_000L).toInt()
            onConfirm(durationMinutes, resumeTime.timeInMillis)
        },
        onDismiss = onDismiss,
    )
}
