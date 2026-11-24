package com.prateek.taro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.prateek.taro.ui.theme.TaroTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

data class NightWindow(val label: String, val bedMinute: Int?, val wakeMinute: Int?, val good: Boolean)

@Composable
fun SleepWindows(nights: List<NightWindow>, color: Color, selected: Int?, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val measurer = rememberTextMeasurer()
    val axis = TaroTheme.text.axis.copy(color = TaroTheme.colors.accent)
    val strong = axis.copy(color = MaterialTheme.colorScheme.onSurface)
    val grid = MaterialTheme.colorScheme.onSurface.copy(alpha = GRID_ALPHA)
    val growth = rememberBarGrowth(nights, nights.size)
    val format = remember {
        SimpleDateFormat(if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h a", Locale.getDefault())
    }
    val known = nights.filter { it.bedMinute != null && it.wakeMinute != null }
    val top = (known.minOfOrNull { it.bedMinute!! } ?: DEFAULT_BED).let { floor(it / 60.0).toInt() * 60 }
    val bottom = (known.maxOfOrNull { it.wakeMinute!! } ?: DEFAULT_WAKE).let { ceil(it / 60.0).toInt() * 60 }.coerceAtLeast(top + 60)
    val step = if (bottom - top > LONG_SPAN) 180 else 120
    val labels = remember(top, bottom, step) {
        (top..bottom step step).map { minute ->
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, ((ORIGIN_MINUTE + minute) / 60) % 24)
                set(Calendar.MINUTE, 0)
            }
            minute to format.format(calendar.time).lowercase(Locale.getDefault())
        }
    }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(nights.size) {
                detectTapGestures { tap ->
                    val gutter = labels.maxOf { measurer.measure(it.second, axis).size.width } + measurer.measure(GAP_SAMPLE, axis).size.width
                    val slot = (size.width - gutter).toFloat() / nights.size.coerceAtLeast(1)
                    val index = ((tap.x - gutter) / slot).toInt()
                    if (index in nights.indices) onSelect(index)
                }
            },
    ) {
        val gutter = (labels.maxOf { measurer.measure(it.second, axis).size.width } + measurer.measure(GAP_SAMPLE, axis).size.width).toFloat()
        val dayHeight = measurer.measure("M", axis).size.height
        val chartTop = dayHeight / 2f
        val chartBottom = size.height - dayHeight * 1.6f
        fun y(minute: Int) = chartTop + (minute - top).toFloat() / (bottom - top) * (chartBottom - chartTop)
        labels.forEach { (minute, text) ->
            val layout = measurer.measure(text, axis)
            drawText(layout, topLeft = Offset(0f, y(minute) - layout.size.height / 2f))
            drawLine(grid, Offset(gutter, y(minute)), Offset(size.width, y(minute)), strokeWidth = size.height * GRID_SHARE)
        }
        val slot = (size.width - gutter) / nights.size.coerceAtLeast(1)
        val bar = slot * BAR_SHARE
        nights.forEachIndexed { index, night ->
            val center = gutter + slot * (index + 0.5f)
            val day = measurer.measure(night.label, if (index == selected) strong else axis)
            drawText(day, topLeft = Offset(center - day.size.width / 2f, size.height - day.size.height))
            val bed = night.bedMinute ?: return@forEachIndexed
            val wake = night.wakeMinute ?: return@forEachIndexed
            val from = y(bed)
            val to = from + (y(wake) - from) * growth.bar(index)
            if (to <= from) return@forEachIndexed
            val alpha = when {
                selected != null && index != selected -> DIM_ALPHA
                night.good -> 1f
                else -> SHORT_ALPHA
            }
            drawRoundRect(color.copy(alpha = alpha), topLeft = Offset(center - bar / 2, from), size = Size(bar, to - from), cornerRadius = CornerRadius(bar / 2))
        }
    }
}

const val NIGHT_ORIGIN_MINUTE = 18 * 60
private const val ORIGIN_MINUTE = NIGHT_ORIGIN_MINUTE
private const val DEFAULT_BED = 4 * 60
private const val DEFAULT_WAKE = 14 * 60
private const val LONG_SPAN = 12 * 60
private const val BAR_SHARE = 0.42f
private const val GRID_ALPHA = 0.07f
private const val GRID_SHARE = 0.004f
private const val DIM_ALPHA = 0.35f
private const val SHORT_ALPHA = 0.55f
private const val GAP_SAMPLE = "00"
