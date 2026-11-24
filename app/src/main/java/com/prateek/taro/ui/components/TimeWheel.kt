package com.prateek.taro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.prateek.taro.R
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.DayClock
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.DateFormatSymbols
import java.util.Locale
import kotlin.math.abs

private const val VISIBLE_ROWS = 5
private const val LOOP_TURNS = 200
private const val ROW_SHARE = 1.25f
private const val WHEEL_WIDTH_SHARE = 2.4f
private const val FAR_ALPHA = 0.3f
private const val NEAR_ALPHA = 0.6f
private const val FAR_SCALE = 0.7f
private const val NEAR_SCALE = 0.82f

@Composable
fun TimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
    is24Hour: Boolean = true,
    suggestions: List<Pair<String, Int>>? = null,
) {
    var minuteOfDay by remember { mutableIntStateOf(initialHour * 60 + initialMinute) }
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val nowLabel = stringResource(R.string.time_now)
    val links = suggestions ?: listOf(nowLabel to DayClock.minuteOfDay(System.currentTimeMillis()))
    TaroDialog(
        title = title,
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        onConfirm = { onConfirm(minuteOfDay / 60, minuteOfDay % 60) },
    ) {
        Column {
            TimeWheels(minuteOfDay, is24Hour, onChange = { minuteOfDay = it })
            if (links.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.loose, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth().padding(top = spacing.gap),
                ) {
                    links.forEachIndexed { index, (label, minute) ->
                        LinkText(label, if (index == 0) colors.special else colors.accent, onClick = { minuteOfDay = minute })
                    }
                }
            }
        }
    }
}

@Composable
fun TimeWheels(minuteOfDay: Int, is24Hour: Boolean, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val row = with(LocalDensity.current) { type.value.lineHeight.toDp() } * ROW_SHARE
    val column = with(LocalDensity.current) { type.value.fontSize.toDp() } * WHEEL_WIDTH_SHARE
    val hour = minuteOfDay / 60
    val minute = minuteOfDay % 60
    val periods = remember { DateFormatSymbols.getInstance(Locale.getDefault()).amPmStrings.map { it.lowercase() } }
    val current by rememberUpdatedState(minuteOfDay)

    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(row)
                .background(TaroTheme.colors.accentOpaque, RoundedCornerShape(spacing.loose)),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (is24Hour) {
                Wheel((0..23).map { "%02d".format(it) }, hour, row, column, loop = true) { onChange(it * 60 + current % 60) }
            } else {
                Wheel((1..12).map { it.toString() }, (hour + 11) % 12, row, column, loop = true) { picked ->
                    val pm = current / 60 >= 12
                    onChange(((picked + 1) % 12 + if (pm) 12 else 0) * 60 + current % 60)
                }
            }
            Text(":", style = type.value, color = MaterialTheme.colorScheme.onSurface)
            Wheel((0..59).map { "%02d".format(it) }, minute, row, column, loop = true) { onChange(current / 60 * 60 + it) }
            if (!is24Hour) {
                Wheel(periods, if (hour >= 12) 1 else 0, row, column, loop = false) { picked ->
                    onChange((current / 60 % 12 + picked * 12) * 60 + current % 60)
                }
            }
        }
    }
}

@Composable
private fun Wheel(values: List<String>, selected: Int, row: Dp, width: Dp, loop: Boolean, onSelect: (Int) -> Unit) {
    val type = TaroTheme.text
    val haptics = LocalHapticFeedback.current
    val pad = VISIBLE_ROWS / 2
    val count = if (loop) values.size * LOOP_TURNS else values.size + pad * 2
    fun valueAt(item: Int) = if (loop) values[item % values.size] else values.getOrNull(item - pad)
    fun itemFor(index: Int, near: Int = count / 2): Int =
        if (loop) near - near % values.size + index else index + pad
    val state = rememberLazyListState(initialFirstVisibleItemIndex = itemFor(selected) - pad)
    val rowPx = with(LocalDensity.current) { row.toPx() }
    val center by remember {
        derivedStateOf { state.firstVisibleItemIndex + pad + if (state.firstVisibleItemScrollOffset > rowPx / 2) 1 else 0 }
    }
    val pick by rememberUpdatedState(onSelect)

    LaunchedEffect(state) {
        snapshotFlow { center }.distinctUntilChanged().collect { item ->
            val index = if (loop) item % values.size else (item - pad).coerceIn(values.indices)
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            if (!state.isScrollInProgress) return@collect
            pick(index)
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.distinctUntilChanged().collect { scrolling ->
            if (!scrolling) pick(if (loop) center % values.size else (center - pad).coerceIn(values.indices))
        }
    }
    LaunchedEffect(selected) {
        val shown = if (loop) center % values.size else center - pad
        if (shown != selected && !state.isScrollInProgress) {
            val target = if (loop) {
                val base = center - shown
                listOf(base + selected, base + selected - values.size, base + selected + values.size).minBy { abs(it - center) }
            } else {
                selected + pad
            }
            state.animateScrollToItem(target - pad)
        }
    }

    LazyColumn(
        state = state,
        flingBehavior = rememberSnapFlingBehavior(state),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.height(row * VISIBLE_ROWS).width(width),
    ) {
        items(count) { item ->
            val distance = abs(item - center)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.height(row).fillMaxWidth()) {
                valueAt(item)?.let { text ->
                    Text(
                        text = text,
                        style = type.value,
                        textAlign = TextAlign.Center,
                        color = if (distance == 0) MaterialTheme.colorScheme.onSurface else TaroTheme.colors.accent,
                        modifier = Modifier.graphicsLayer {
                            val scale = when (distance) {
                                0 -> 1f
                                1 -> NEAR_SCALE
                                else -> FAR_SCALE
                            }
                            scaleX = scale
                            scaleY = scale
                            alpha = when (distance) {
                                0 -> 1f
                                1 -> NEAR_ALPHA
                                else -> FAR_ALPHA
                            }
                        },
                    )
                }
            }
        }
    }
}
