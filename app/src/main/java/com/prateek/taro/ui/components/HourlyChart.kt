package com.prateek.taro.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.Util
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

data class BarPart(val value: Double, val color: Color)

data class HourBar(val parts: List<BarPart>, val ghost: Double = 0.0) {
    val total: Double get() = parts.sumOf { it.value }
}

private const val HOUR_MS = 3_600_000f
private val LABEL_STEPS = listOf(1, 2, 3, 6)
private val GRID_STEPS = listOf(1.0, 2.0, 2.5, 5.0)
private const val MAX_LABELS = 5f
private const val MAX_GRID_LINES = 3
private const val MIN_SPAN = 3f
private const val HEADROOM = 0.92f
private const val BAR_SHARE = 0.78f
private const val RADIUS_SHARE = 0.3f
private const val DIM_ALPHA = 0.3f
private const val GHOST_ALPHA = 0.45f
private const val LINE_SHARE = 0.006f
private const val DOT_SHARE = 0.012f
private const val GAP_SHARE = 0.25f

@Composable
fun HourlyChart(
    bars: List<HourBar>,
    start: Long,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    now: Long? = null,
    appearKey: Any? = null,
    grid: Boolean = true,
) {
    val context = LocalContext.current
    val colors = TaroTheme.colors
    val type = TaroTheme.text
    val haptics = LocalHapticFeedback.current
    val measurer = rememberTextMeasurer()
    val labelStyle = type.axis.copy(color = colors.accent)
    val hourFormat = remember {
        SimpleDateFormat(if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h a", Locale.getDefault())
    }
    val count = bars.size
    val growth = rememberBarGrowth(appearKey, count)
    val scaleMax = (bars.maxOfOrNull { maxOf(it.total, it.ghost) } ?: 0.0).takeIf { it > 0 } ?: 1.0
    val morph = rememberMorph(bars.flatMap { bar -> bar.parts.map { it.value } + bar.ghost } + scaleMax)
    val nowHour = now?.let { ((it - start) / HOUR_MS).toDouble().coerceIn(0.0, count.toDouble()) }
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
        if (index != currentSelected) {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            currentSelect(index)
        }
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
        val values = morph()
        val max = values.last().takeIf { it > 0 } ?: 1.0
        val line = size.minDimension * LINE_SHARE
        val labelHeight = measurer.measure("0", labelStyle).size.height.toFloat()
        val chartHeight = size.height - labelHeight * (1 + GAP_SHARE)
        val gridStep = niceStep(max)
        val gridLabels = if (grid) (1..MAX_GRID_LINES).map { it * gridStep }.filter { it <= max }.map { it to measurer.measure(Util.formatSteps(it.toInt()), labelStyle) } else emptyList()
        val gutter = gridLabels.maxOfOrNull { it.second.size.width.toFloat() }?.let { it + labelHeight * GAP_SHARE * 2 } ?: 0f
        val plotWidth = size.width - gutter
        val viewStart = view.start
        val slot = plotWidth / view.span
        fun xOf(hour: Double) = ((hour - viewStart) * slot).toFloat()
        fun heightOf(value: Double) = (value / max * chartHeight * HEADROOM).toFloat()

        gridLabels.forEach { (value, layout) ->
            val y = chartHeight - heightOf(value)
            drawLine(
                colors.accent.copy(alpha = DIM_ALPHA),
                Offset(0f, y),
                Offset(plotWidth, y),
                strokeWidth = line,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(line * 2, line * 5)),
            )
            drawText(layout, topLeft = Offset(size.width - layout.size.width, y - layout.size.height / 2f))
        }

        val barWidth = slot * BAR_SHARE
        val radius = barWidth * RADIUS_SHARE
        var index = 0
        clipRect(0f, 0f, plotWidth, chartHeight) {
            bars.forEachIndexed { i, bar ->
                val parts = bar.parts.mapIndexed { p, part -> part.copy(value = values[index + p]) }
                val ghost = values[index + bar.parts.size]
                index += bar.parts.size + 1
                val left = xOf(i.toDouble()) + (slot - barWidth) / 2
                if (left > plotWidth || left + barWidth < 0f) return@forEachIndexed
                val grown = growth.bar(i)
                val total = parts.sumOf { it.value }
                if (ghost > total) {
                    val top = chartHeight - heightOf(ghost) * grown
                    drawRoundRect(
                        color = colors.accent.copy(alpha = GHOST_ALPHA),
                        topLeft = Offset(left + line / 2, top + line / 2),
                        size = Size(barWidth - line, chartHeight - top - line),
                        cornerRadius = CornerRadius(radius),
                        style = Stroke(width = line * 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(line * 3, line * 3))),
                    )
                }
                if (total <= 0) return@forEachIndexed
                val alpha = if (selected != null && selected != i) DIM_ALPHA else 1f
                val top = chartHeight - heightOf(total) * grown
                val shape = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = left,
                            top = top,
                            right = left + barWidth,
                            bottom = chartHeight,
                            topLeftCornerRadius = CornerRadius(radius),
                            topRightCornerRadius = CornerRadius(radius),
                        )
                    )
                }
                clipPath(shape) {
                    var base = chartHeight
                    parts.forEach { part ->
                        if (part.value <= 0) return@forEach
                        val height = heightOf(part.value) * grown
                        drawRect(part.color.copy(alpha = part.color.alpha * alpha), Offset(left, base - height), Size(barWidth, height))
                        base -= height
                    }
                }
            }
        }

        drawLine(colors.accent.copy(alpha = DIM_ALPHA), Offset(0f, chartHeight), Offset(plotWidth, chartHeight), strokeWidth = line)

        selected?.let { i ->
            val x = xOf(i + 0.5)
            if (x in 0f..plotWidth) drawLine(colors.accent, Offset(x, 0f), Offset(x, chartHeight), strokeWidth = line * 1.5f)
        }

        nowHour?.takeIf { it < count }?.let {
            val x = xOf(it)
            if (x in 0f..plotWidth) drawCircle(colors.accent, radius = size.minDimension * DOT_SHARE, center = Offset(x, chartHeight))
        }

        val every = LABEL_STEPS.firstOrNull { view.span / it <= MAX_LABELS } ?: LABEL_STEPS.last()
        val firstLabel = (ceil(viewStart / every) * every).toInt()
        for (hour in firstLabel until count step every) {
            if (hour > viewStart + view.span) break
            val text = hourFormat.format(Date(start + hour * HOUR_MS.toLong())).lowercase(Locale.getDefault())
            val layout = measurer.measure(text, labelStyle)
            val x = xOf(hour.toDouble()).coerceIn(0f, plotWidth - layout.size.width)
            drawText(layout, topLeft = Offset(x, chartHeight + labelHeight * GAP_SHARE))
        }
    }
}

private fun niceStep(max: Double): Double {
    val raw = max / MAX_GRID_LINES
    val magnitude = 10.0.pow(floor(log10(raw)))
    return GRID_STEPS.map { it * magnitude }.firstOrNull { it >= raw } ?: (10 * magnitude)
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
