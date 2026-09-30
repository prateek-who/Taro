package com.prateek.taro.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prateek.taro.ui.theme.Chivo
import com.prateek.taro.ui.theme.TaroTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HourSeries(val values: List<Double>, val color: Color)

private const val HOUR_MS = 3_600_000f
private const val LABEL_EVERY = 6

@Composable
fun HourlyChart(
    series: List<HourSeries>,
    start: Long,
    sleep: List<Pair<Long, Long>>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    cumulative: Boolean = false,
    now: Long? = null,
    appearKey: Any? = null,
) {
    val context = LocalContext.current
    val colors = TaroTheme.colors
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontFamily = Chivo, fontSize = 11.sp, color = colors.accent)
    val hourFormat = remember {
        SimpleDateFormat(if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h a", Locale.getDefault())
    }
    val count = series.firstOrNull()?.values?.size ?: 0
    val appear = remember { Animatable(0f) }
    LaunchedEffect(appearKey, cumulative) {
        appear.snapTo(0f)
        appear.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }
    val currentSelect by rememberUpdatedState(onSelect)
    val currentCount by rememberUpdatedState(count)

    fun indexAt(x: Float, width: Float): Int? =
        if (currentCount == 0) null else (x / width * currentCount).toInt().coerceIn(0, currentCount - 1)

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val index = indexAt(offset.x, size.width.toFloat())
                    currentSelect(index)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    currentSelect(indexAt(change.position.x, size.width.toFloat()))
                }
            },
    ) {
        if (count == 0) return@Canvas
        val labelHeight = 20.dp.toPx()
        val chartHeight = size.height - labelHeight
        val slot = size.width / count
        val progress = appear.value

        sleep.forEach { (from, to) ->
            val left = ((from - start) / HOUR_MS * slot).coerceIn(0f, size.width)
            val right = ((to - start) / HOUR_MS * slot).coerceIn(0f, size.width)
            if (right > left) {
                drawRoundRect(
                    color = colors.sleep.copy(alpha = 0.16f),
                    topLeft = Offset(left, 0f),
                    size = Size(right - left, chartHeight),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                )
            }
        }

        drawLine(colors.accentOpaque, Offset(0f, chartHeight), Offset(size.width, chartHeight), strokeWidth = 1.dp.toPx())

        if (cumulative) {
            val lastHour = now?.let { ((it - start) / HOUR_MS).coerceIn(0f, count.toFloat()) } ?: count.toFloat()
            val totals = series.map { line -> line.values.runningFold(0.0) { sum, value -> sum + value } }
            val max = totals.maxOf { it.maxOrNull() ?: 0.0 }.takeIf { it > 0 } ?: 1.0
            series.forEachIndexed { s, line ->
                val points = totals[s]
                val path = Path()
                val limit = (lastHour * progress)
                points.forEachIndexed { i, value ->
                    if (i > limit + 1) return@forEachIndexed
                    val x = minOf(i.toFloat(), limit) * slot
                    val y = chartHeight - (value / max * chartHeight * 0.92).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, line.color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                selected?.let { index ->
                    val value = points[index + 1]
                    val x = (index + 1) * slot
                    drawCircle(line.color, radius = 5.dp.toPx(), center = Offset(x, chartHeight - (value / max * chartHeight * 0.92).toFloat()))
                }
            }
            selected?.let { index ->
                val x = (index + 1) * slot
                drawLine(colors.accent.copy(alpha = 0.5f), Offset(x, 0f), Offset(x, chartHeight), strokeWidth = 1.dp.toPx())
            }
        } else {
            val max = series.maxOf { it.values.maxOrNull() ?: 0.0 }.takeIf { it > 0 } ?: 1.0
            val groupWidth = slot * 0.72f
            val barWidth = groupWidth / series.size
            val radius = minOf(barWidth / 2, 4.dp.toPx())
            for (i in 0 until count) {
                val dim = selected != null && selected != i
                series.forEachIndexed { s, line ->
                    val value = line.values[i]
                    if (value <= 0) return@forEachIndexed
                    val height = maxOf((value / max * chartHeight * 0.92).toFloat() * progress, 2.dp.toPx())
                    val left = i * slot + (slot - groupWidth) / 2 + s * barWidth
                    val path = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = left,
                                top = chartHeight - height,
                                right = left + barWidth * 0.9f,
                                bottom = chartHeight,
                                topLeftCornerRadius = CornerRadius(radius),
                                topRightCornerRadius = CornerRadius(radius),
                            )
                        )
                    }
                    drawPath(path, line.color.copy(alpha = if (dim) 0.3f else 1f))
                }
            }
        }

        now?.let {
            val x = (it - start) / HOUR_MS * slot
            if (x in 0f..size.width) {
                drawCircle(colors.accent, radius = 3.dp.toPx(), center = Offset(x, chartHeight))
            }
        }

        for (hour in 0..count step LABEL_EVERY) {
            if (hour >= count) break
            val text = hourFormat.format(Date(start + hour * HOUR_MS.toLong())).lowercase(Locale.getDefault())
            val layout = measurer.measure(text, labelStyle)
            val x = (hour * slot).coerceIn(0f, size.width - layout.size.width)
            drawText(layout, topLeft = Offset(x, chartHeight + 4.dp.toPx()))
        }
    }
}
