package com.prateek.taro.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.rotate
import androidx.compose.material3.IconButton
import com.prateek.taro.util.DayClock
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prateek.taro.R
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Util
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

@Composable
fun DateRow(label: String, date: LocalDate, today: LocalDate, onChange: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    var picking by remember { mutableStateOf(false) }
    val format = remember { SimpleDateFormat(AppPreferences.dateFormatString, Locale.getDefault()) }
    val value = when (date) {
        today -> stringResource(R.string.header_today)
        today.minusDays(1) -> stringResource(R.string.date_yesterday)
        else -> format.format(Date(Util.dateStringToCalendarMillis(date.toString())))
    }

    PickerRow(icon = R.drawable.ic_dateformat, label = label, value = value, onClick = { picking = true }, modifier = modifier)

    if (picking) {
        TaroDatePickerDialog(
            initial = date,
            latest = today,
            onPick = {
                picking = false
                onChange(it)
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
fun PickerRow(icon: Int, label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier) { EditRow(icon, TaroTheme.colors.accent, label, value, onClick) }
}

@Composable
fun dayLabel(date: LocalDate, today: LocalDate, todayLabel: String? = null): String {
    val format = remember { SimpleDateFormat("EEE d MMM", Locale.getDefault()) }
    return when {
        date == today -> todayLabel ?: stringResource(R.string.header_today)
        date == today.minusDays(1) && todayLabel == null -> stringResource(R.string.date_yesterday)
        else -> format.format(Date(Util.dateStringToCalendarMillis(date.toString())))
    }
}

@Composable
fun DaySwitcher(date: LocalDate, today: LocalDate, onChange: (LocalDate) -> Unit, modifier: Modifier = Modifier, todayLabel: String? = null) {
    var picking by remember { mutableStateOf(false) }
    StepSwitcher(
        label = dayLabel(date, today, todayLabel),
        onPrevious = { onChange(date.minusDays(1)) },
        onNext = { onChange(date.plusDays(1)) },
        nextEnabled = date.isBefore(today),
        previousDescription = stringResource(R.string.day_previous),
        nextDescription = stringResource(R.string.day_next),
        labelColor = if (date == today) TaroTheme.colors.accent else MaterialTheme.colorScheme.onSurface,
        onLabelClick = { picking = true },
        modifier = modifier,
    )
    if (picking) {
        TaroDatePickerDialog(
            initial = date,
            latest = today,
            onPick = {
                picking = false
                onChange(it)
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
fun StepSwitcher(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    nextEnabled: Boolean,
    previousDescription: String,
    nextDescription: String,
    modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    onLabelClick: (() -> Unit)? = null,
) {
    val spacing = TaroTheme.spacing
    val button = spacing.wide + spacing.gap + spacing.hair
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = modifier) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(button)) {
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = previousDescription, tint = TaroTheme.colors.accent, modifier = Modifier.rotate(180f))
        }
        Text(
            text = label.uppercase(),
            style = TaroTheme.text.label,
            color = labelColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .clip(RoundedCornerShape(spacing.gap + spacing.hair))
                .then(if (onLabelClick != null) Modifier.clickable(onClick = onLabelClick) else Modifier)
                .padding(horizontal = spacing.tight, vertical = spacing.tight),
        )
        IconButton(onClick = onNext, enabled = nextEnabled, modifier = Modifier.size(button)) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = nextDescription,
                tint = TaroTheme.colors.accent.copy(alpha = if (nextEnabled) 1f else DISABLED_ALPHA),
            )
        }
    }
}

private const val DISABLED_ALPHA = 0.25f

@Composable
fun TimeRow(label: String, minuteOfDay: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var picking by remember { mutableStateOf(false) }
    val format = remember { android.text.format.DateFormat.getTimeFormat(context) }
    val value = remember(minuteOfDay) {
        format.format(Date(DayClock.momentOf("2000-01-01", minuteOfDay, 0)))
    }
    PickerRow(R.drawable.ic_day_start, label, value, onClick = { picking = true }, modifier = modifier)
    if (picking) {
        TimePickerDialog(
            title = label,
            initialHour = minuteOfDay / 60,
            initialMinute = minuteOfDay % 60,
            is24Hour = android.text.format.DateFormat.is24HourFormat(context),
            onConfirm = { hour, minute ->
                picking = false
                onChange(hour * 60 + minute)
            },
            onDismiss = { picking = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaroDatePickerDialog(initial: LocalDate, latest: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val latestMillis = latest.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        yearRange = (latest.year - 5)..latest.year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= latestMillis
            override fun isSelectableYear(year: Int) = year <= latest.year
        },
    )
    val onSurface = MaterialTheme.colorScheme.onSurface
    val colors = DatePickerDefaults.colors(
        containerColor = TaroTheme.colors.dialogSurface,
        selectedDayContainerColor = onSurface,
        selectedDayContentColor = TaroTheme.colors.background,
        selectedYearContainerColor = onSurface,
        selectedYearContentColor = TaroTheme.colors.background,
        todayDateBorderColor = TaroTheme.colors.accent,
        todayContentColor = onSurface,
        headlineContentColor = onSurface,
        titleContentColor = TaroTheme.colors.accent,
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        colors = colors,
        confirmButton = {
            PrimaryButton(
                text = stringResource(android.R.string.ok),
                onClick = { state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } },
                enabled = state.selectedDateMillis != null,
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
            )
        },
        dismissButton = {
            SecondaryButton(
                text = stringResource(android.R.string.cancel),
                onClick = onDismiss,
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
            )
        },
    ) {
        DatePicker(state = state, colors = colors, showModeToggle = false)
    }
}
