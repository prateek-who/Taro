package com.prateek.taro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TaroTheme.colors.accentOpaque)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = TaroTheme.colors.accent, modifier = Modifier.size(20.dp))
        Text(label, color = TaroTheme.colors.accent, modifier = Modifier.weight(1f))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
