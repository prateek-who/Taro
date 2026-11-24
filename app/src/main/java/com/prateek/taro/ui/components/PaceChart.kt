package com.prateek.taro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.prateek.taro.ui.theme.TaroTheme

@Composable
fun PaceChart(
    today: List<Pair<Double, Double>>,
    usual: List<Pair<Double, Double>>?,
    course: List<Pair<Double, Double>>,
    goal: Int,
    goalLabel: String,
    hourLabels: List<Pair<Double, String>>,
    color: Color,
    selected: Double?,
    onSelect: (Double?) -> Unit,
    modifier: Modifier = Modifier,
    hours: Double = 24.0,
) {
    val accent = TaroTheme.colors.accent
    val background = TaroTheme.colors.background
    val ink = MaterialTheme.colorScheme.onSurface
    val grid = ink.copy(alpha = PACE_GRID_ALPHA)
    val measurer = rememberTextMeasurer()
    val axis = TaroTheme.text.axis.copy(color = accent)
    val reveal = rememberDrawIn(today.lastOrNull())
    val currentSelect by rememberUpdatedState(onSelect)
    val last = today.lastOrNull()?.first ?: 0.0
    fun hourAt(x: Float, width: Float) = (x / width * hours).coerceIn(0.0, maxOf(last, usual?.lastOrNull()?.first ?: 0.0))

    Canvas(
        modifier = modifier
            .pointerInput(hours, last) {
                detectTapGestures { tap -> currentSelect(if (selected != null) null else hourAt(tap.x, size.width.toFloat())) }
            }
            .pointerInput(hours, last) {
                detectHorizontalDragGestures(onDragEnd = {}) { change, _ -> currentSelect(hourAt(change.position.x, size.width.toFloat())) }
            },
    ) {
        if (today.isEmpty()) return@Canvas
        val labelHeight = measurer.measure("0", axis).size.height
        val stroke = size.height * PACE_STROKE_SHARE
        val top = labelHeight * 1.4f
        val bottom = size.height - labelHeight * 1.6f
        val values = today.map { it.second } + course.map { it.second } + usual.orEmpty().map { it.second } + goal.toDouble()
        val max = (values.max() * PACE_HEADROOM).coerceAtLeast(1.0)
        fun x(hour: Double) = (hour / hours * size.width).toFloat()
        fun y(value: Double) = (bottom - (bottom - top) * (value / max)).toFloat()
        fun points(list: List<Pair<Double, Double>>) = list.map { Offset(x(it.first), y(it.second)) }

        (1..PACE_GRID_LINES).forEach { line ->
            val gy = bottom - (bottom - top) * line / (PACE_GRID_LINES + 1)
            drawLine(grid, Offset(0f, gy), Offset(size.width, gy), strokeWidth = stroke * 0.4f)
        }
        drawLine(grid, Offset(0f, bottom), Offset(size.width, bottom), strokeWidth = stroke * 0.4f)
        if (goal > 0) {
            val gy = y(goal.toDouble())
            drawLine(accent, Offset(0f, gy), Offset(size.width, gy), strokeWidth = stroke * 0.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 2, stroke * 2.5f)))
            val label = measurer.measure(goalLabel, axis)
            drawText(label, topLeft = Offset(0f, gy - label.size.height - stroke))
        }

        clipRect(right = size.width * reveal.value) {
            usual?.takeIf { it.size > 1 }?.let {
                drawPath(monotonePath(points(it)), accent, style = Stroke(width = stroke * 0.7f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 0.6f, stroke * 2f))))
            }
            val line = points(today)
            if (line.size > 1) {
                val curve = monotonePath(line)
                val area = Path().apply {
                    addPath(curve)
                    lineTo(line.last().x, bottom)
                    lineTo(line.first().x, bottom)
                    close()
                }
                drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = PACE_FILL_ALPHA), Color.Transparent), startY = top, endY = bottom))
                drawPath(curve, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            if (course.size > 1) {
                drawPath(monotonePath(points(course)), color.copy(alpha = PACE_COURSE_ALPHA), style = Stroke(width = stroke * 0.8f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 2, stroke * 2))))
            }
        }

        if (course.isNotEmpty()) {
            val end = Offset(x(today.last().first), y(today.last().second))
            usual?.let { drawCircle(accent, radius = stroke * 1.2f, center = Offset(end.x, y(interpolate(it, today.last().first)))) }
            drawCircle(background, radius = stroke * 2.4f, center = end)
            drawCircle(color, radius = stroke * 1.7f, center = end)
        }

        selected?.let { hour ->
            val sx = x(hour)
            drawLine(ink.copy(alpha = PACE_SELECT_ALPHA), Offset(sx, top), Offset(sx, bottom), strokeWidth = stroke * 0.5f)
            usual?.let { drawCircle(accent, radius = stroke * 1.3f, center = Offset(sx, y(interpolate(it, hour)))) }
            if (hour <= today.last().first) drawCircle(color, radius = stroke * 1.6f, center = Offset(sx, y(interpolate(today, hour))))
        }

        val placed = mutableListOf<ClosedFloatingPointRange<Float>>()
        hourLabels.forEach { (hour, text) ->
            val layout = measurer.measure(text, axis)
            val from = (x(hour) - if (hour == 0.0) 0f else layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
            val span = from - labelHeight..from + layout.size.width + labelHeight
            if (placed.none { it.start < span.endInclusive && span.start < it.endInclusive }) {
                placed += span
                drawText(layout, topLeft = Offset(from, size.height - layout.size.height))
            }
        }
    }
}

fun interpolate(points: List<Pair<Double, Double>>, hour: Double): Double {
    if (points.isEmpty()) return 0.0
    if (hour <= points.first().first) return points.first().second
    val (a, b) = points.zipWithNext().firstOrNull { hour <= it.second.first } ?: return points.last().second
    return a.second + (b.second - a.second) * (hour - a.first) / (b.first - a.first)
}

private const val PACE_GRID_LINES = 3
private const val PACE_HEADROOM = 1.08
private const val PACE_STROKE_SHARE = 0.014f
private const val PACE_FILL_ALPHA = 0.3f
private const val PACE_COURSE_ALPHA = 0.75f
private const val PACE_GRID_ALPHA = 0.07f
private const val PACE_SELECT_ALPHA = 0.35f
