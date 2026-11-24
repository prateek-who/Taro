package com.prateek.taro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.positionChange
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
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

data class HourSeries(val values: List<Double>, val color: Color, val events: Boolean = false)

private const val HOUR_MS = 3_600_000f
private val LABEL_STEPS = listOf(1, 2, 3, 6)
private const val MAX_LABELS = 5f
private const val MIN_SPAN = 3f
private const val STEPS_PER_HOUR = 6
private const val HEADROOM = 0.92f
private const val FILL_ALPHA = 0.24f

@Composable
fun HourlyChart(
    series: List<HourSeries>,
    start: Long,
    sleep: List<Pair<Long, Long>>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    cumulative: Boolean = false,
    area: Boolean = false,
    now: Long? = null,
    appearKey: Any? = null,
    scaleMax: Double? = null,
) {
    val context = LocalContext.current
    val colors = TaroTheme.colors
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontFamily = Chivo, fontSize = 11.sp, color = colors.accent)
    val hourFormat = remember {
        SimpleDateFormat(if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h a", Locale.getDefault())
    }
    val count = series.firstOrNull()?.values?.size ?: 0
    val drawIn = rememberDrawIn(appearKey to cumulative)
    val growth = rememberBarGrowth(appearKey to cumulative, count)
    val nowHour = now?.let { ((it - start) / HOUR_MS).toDouble().coerceIn(0.0, count.toDouble()) }
    val lines = remember(series, cumulative, area, nowHour) {
        when {
            cumulative -> series.map { cumulativePoints(it, nowHour) }
            area -> series.map { areaPoints(it, nowHour) }
            else -> emptyList()
        }
    }
    val lastSelectable = nowHour?.let { floor(it).toInt().coerceIn(0, count - 1) } ?: (count - 1)
    val currentSelect by rememberUpdatedState(onSelect)
    val currentSelected by rememberUpdatedState(selected)
    val currentCount by rememberUpdatedState(count)
    val currentLast by rememberUpdatedState(lastSelectable)
    val view = remember(appearKey, count) { ZoomWindow(count.toFloat()) }
    val scope = rememberCoroutineScope()

    fun indexAt(x: Float, width: Float): Int? =
        if (currentCount == 0) null else floor(view.hourAt(x, width)).toInt().coerceIn(0, currentLast.coerceAtLeast(0))

    fun follow(x: Float, width: Float) {
        val index = indexAt(x, width)
        if (index != currentSelected) currentSelect(index)
    }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val index = indexAt(offset.x, size.width.toFloat())
                    currentSelect(if (index == currentSelected) null else index)
                }
            }
            .pointerInput(view) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val width = size.width.toFloat()
                    var pinched = false
                    var dragging = false
                    var moved = Offset.Zero
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.count { it.pressed }
                        if (pressed >= 2) {
                            pinched = true
                            view.transform(event.calculateCentroid(useCurrent = true).x, event.calculateZoom(), event.calculatePan().x, width)
                            event.changes.forEach { it.consume() }
                        } else if (!pinched) {
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!dragging) {
                                moved += change.positionChange()
                                if (abs(moved.y) > viewConfiguration.touchSlop && abs(moved.y) > abs(moved.x)) break
                                if (abs(moved.x) > viewConfiguration.touchSlop) dragging = true
                            }
                            if (dragging) {
                                change.consume()
                                follow(change.position.x, width)
                            }
                        }
                    } while (event.changes.any { it.pressed })
                    if (pinched) scope.launch { view.settle() }
                }
            },
    ) {
        if (count == 0) return@Canvas
        val labelHeight = 20.dp.toPx()
        val chartHeight = size.height - labelHeight
        val viewStart = view.start
        val slot = size.width / view.span
        fun xOf(hour: Double) = ((hour - viewStart) * slot).toFloat()

        sleep.forEach { (from, to) ->
            val left = xOf((from - start) / HOUR_MS.toDouble()).coerceIn(0f, size.width)
            val right = xOf((to - start) / HOUR_MS.toDouble()).coerceIn(0f, size.width)
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

        if (cumulative || area) {
            val max = maxOf(scaleMax ?: 0.0, lines.maxOf { line -> line.maxOfOrNull { it.second } ?: 0.0 }).takeIf { it > 0 } ?: 1.0
            fun y(value: Double) = chartHeight - (value / max * chartHeight * HEADROOM).toFloat()
            clipRect(0f, 0f, size.width, chartHeight) {
                series.forEachIndexed { s, line ->
                    val points = lines[s].map { (hour, value) -> Offset(xOf(hour), y(value)) }
                    if (points.size < 2) return@forEachIndexed
                    drawCurve(points, line.color, chartHeight, drawIn)
                }
            }
            selected?.let { index ->
                val x = xOf(if (cumulative) index + 1.0 else index + 0.5)
                if (x !in 0f..size.width) return@let
                drawLine(colors.accent.copy(alpha = 0.5f), Offset(x, 0f), Offset(x, chartHeight), strokeWidth = 1.dp.toPx())
                series.forEach { line ->
                    val value = if (cumulative) line.values.take(index + 1).sum() else line.values[index]
                    drawCircle(colors.background, radius = 6.dp.toPx(), center = Offset(x, y(value)))
                    drawCircle(line.color, radius = 4.5.dp.toPx(), center = Offset(x, y(value)))
                }
            }
        } else {
            val max = series.maxOf { it.values.maxOrNull() ?: 0.0 }.takeIf { it > 0 } ?: 1.0
            val groupWidth = slot * 0.72f
            val barWidth = groupWidth / series.size
            val radius = minOf(barWidth / 2, 4.dp.toPx())
            clipRect(0f, 0f, size.width, chartHeight) {
                for (i in 0 until count) {
                    val dim = selected != null && selected != i
                    val grown = growth.bar(i)
                    series.forEachIndexed { s, line ->
                        val value = line.values[i]
                        if (value <= 0) return@forEachIndexed
                        val height = maxOf((value / max * chartHeight * HEADROOM).toFloat() * grown, 2.dp.toPx() * minOf(grown, 1f))
                        val left = xOf(i.toDouble()) + (slot - groupWidth) / 2 + s * barWidth
                        if (left > size.width || left + barWidth < 0f) return@forEachIndexed
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
        }

        nowHour?.let {
            val x = xOf(it)
            if (x in 0f..size.width) drawCircle(colors.accent, radius = 3.dp.toPx(), center = Offset(x, chartHeight))
        }

        val every = LABEL_STEPS.firstOrNull { view.span / it <= MAX_LABELS } ?: LABEL_STEPS.last()
        val firstLabel = (ceil(viewStart / every) * every).toInt()
        for (hour in firstLabel until count step every) {
            if (hour > viewStart + view.span) break
            val text = hourFormat.format(Date(start + hour * HOUR_MS.toLong())).lowercase(Locale.getDefault())
            val layout = measurer.measure(text, labelStyle)
            val x = xOf(hour.toDouble()).coerceIn(0f, size.width - layout.size.width)
            drawText(layout, topLeft = Offset(x, chartHeight + 4.dp.toPx()))
        }
    }
}

private class ZoomWindow(private val hours: Float) {
    var start by mutableFloatStateOf(0f)
        private set
    var span by mutableFloatStateOf(hours)
        private set

    fun hourAt(x: Float, width: Float): Float = start + x / width * span

    fun transform(centroid: Float, zoom: Float, pan: Float, width: Float) {
        if (hours <= 0f || width <= 0f) return
        val anchor = hourAt(centroid, width)
        val next = (span / zoom).coerceIn(minOf(MIN_SPAN, hours), hours)
        span = next
        start = (anchor - centroid / width * next - pan / width * next).coerceIn(0f, hours - next)
    }

    suspend fun settle() {
        if (span < hours * 0.9f) return
        val fromStart = start
        val fromSpan = span
        animate(0f, 1f, animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) { t, _ ->
            span = (fromSpan + (hours - fromSpan) * t).coerceAtMost(hours)
            start = (fromStart * (1f - t)).coerceIn(0f, hours - span)
        }
    }
}

private fun DrawScope.drawCurve(points: List<Offset>, color: Color, baseline: Float, progress: ChartProgress) {
    val line = monotonePath(points)
    val fill = progress.fill()
    if (fill > 0f) {
        val area = monotonePath(points).apply {
            lineTo(points.last().x, baseline)
            lineTo(points.first().x, baseline)
            close()
        }
        val top = points.minOf { it.y }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = FILL_ALPHA * fill), color.copy(alpha = 0f)), startY = top, endY = baseline))
    }
    drawPath(
        line.partial(progress.value),
        color,
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun cumulativePoints(series: HourSeries, nowHour: Double?): List<Pair<Double, Double>> {
    val end = nowHour ?: series.values.size.toDouble()
    if (series.events) {
        val totals = runningTotals(series.values, STEPS_PER_HOUR, spreadHours = 1.0, leadHours = 0.0)
        val last = floor(end * STEPS_PER_HOUR).toInt().coerceAtMost(totals.lastIndex)
        val points = (0..last).map { it.toDouble() / STEPS_PER_HOUR to totals[it] }
        val total = series.values.sum()
        return if (end > points.last().first) points + (end to maxOf(points.last().second, total)) else points
    }
    val totals = series.values.runningFold(0.0) { sum, value -> sum + value }
    val whole = floor(end).toInt()
    val points = (0..whole).map { it.toDouble() to totals[it] }
    return if (end > whole) points + (end to totals[minOf(whole + 1, totals.lastIndex)]) else points
}

private fun areaPoints(series: HourSeries, nowHour: Double?): List<Pair<Double, Double>> {
    val values = series.values
    val last = nowHour?.let { floor(it).toInt().coerceAtMost(values.lastIndex) } ?: values.lastIndex
    val centers = (0..last).map { it + 0.5 to values[it] }
    val end = nowHour ?: values.size.toDouble()
    return listOf(0.0 to values.first()) + centers + (end to values[last])
}
