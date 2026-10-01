package com.prateek.taro.ui.components

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prateek.taro.ui.theme.TaroMotion
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.ui.theme.rememberAnimationsEnabled
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

data class RingSegment(val value: Double, val color: Color)

@Composable
fun HeroRing(
    segments: List<RingSegment>,
    target: Double,
    modifier: Modifier = Modifier,
    lit: Boolean = false,
    gradient: Boolean = false,
    diameter: Dp = 264.dp,
    strokeWidth: Dp = 18.dp,
    overflow: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val total = segments.sumOf { it.value }
    val lap = when {
        target <= 0 -> maxOf(total, 1.0)
        overflow -> target
        else -> maxOf(target, total)
    }
    val fractions = segments.map { (it.value / lap).toFloat().coerceAtLeast(0f) }
    val progress = fractions.sum().coerceIn(0f, MAX_LAPS.toFloat())

    val reveal = remember { Animatable(0f) }
    val active = LocalTabActive.current
    LaunchedEffect(progress, active) {
        if (active) reveal.animateTo(progress, TaroMotion.gentle()) else reveal.snapTo(0f)
    }
    val glow = if (lit && rememberAnimationsEnabled()) {
        rememberInfiniteTransition(label = "ring glow")
            .animateFloat(0.12f, 0.32f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "glow alpha")
    } else {
        null
    }

    val track = TaroTheme.colors.accentOpaque
    val bonus = TaroTheme.colors.special
    val fade = TaroTheme.colors.background
    val first = segments.firstOrNull()?.color ?: bonus
    val lapPalette = listOf(bonus, TaroTheme.colors.flame, TaroTheme.colors.sleep, LAP_PINK)
    fun lapColor(index: Int): Color = if (index <= 0) first else lapPalette[(index - 1) % lapPalette.size]

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(diameter)
            .semantics { contentDescription = "${(progress * 100).toInt()}%" },
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = strokeWidth.toPx()
            val radius = size.minDimension / 2 - stroke * 1.4f
            drawArcAt(SolidColor(track), 0f, 360f, radius, stroke, StrokeCap.Butt)

            val shown = reveal.value
            val firstLap = shown.coerceIn(0f, 1f)
            if (firstLap <= 0f) return@Canvas

            rotate(-90f) {
                val full = firstLap >= 0.999f
                val glowAlpha = glow?.value ?: if (lit) 0.2f else 0f
                if (glowAlpha > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val topLap = floor(shown).toInt().let { if (shown - it > 0.002f) it else it - 1 }.coerceAtLeast(0)
                    drawGlow(lapColor(topLap).copy(alpha = (glowAlpha * 2.2f).coerceAtMost(0.7f)), firstLap * 360f, radius, stroke)
                }

                if (gradient && segments.size == 1) {
                    val color = segments.first().color
                    if (full) {
                        drawArcAt(SolidColor(color), 0f, 360f, radius, stroke, StrokeCap.Butt)
                    } else {
                        val dim = lerp(color, fade, 0.55f * (firstLap / GRADIENT_RAMP).coerceAtMost(1f))
                        val brush = sweep(dim, color, firstLap, capFraction(stroke, radius))
                        drawArcAt(brush, 0f, firstLap * 360f, radius, stroke, StrokeCap.Round)
                    }
                } else {
                    val parts = mutableListOf<Triple<Float, Float, Color>>()
                    var cursor = 0f
                    segments.forEachIndexed { index, segment ->
                        val sweep = minOf(fractions[index], firstLap - cursor)
                        if (sweep > 0f) parts += Triple(cursor, cursor + sweep, segment.color)
                        cursor += fractions[index]
                    }
                    if (full) {
                        parts.forEach { (from, to, color) -> drawArcAt(SolidColor(color), from * 360f, (to - from) * 360f, radius, stroke, StrokeCap.Butt) }
                    } else if (parts.isNotEmpty()) {
                        val drawnTo = parts.last().second
                        val cap = capFraction(stroke, radius)
                        absorbSlivers(parts, cap * SOLID_BELOW_CAPS / 2)
                        val brush = if (drawnTo < cap * SOLID_BELOW_CAPS || parts.size == 1) {
                            SolidColor(parts.maxBy { it.second - it.first }.third)
                        } else {
                            segmentSweep(parts, drawnTo, cap)
                        }
                        drawArcAt(brush, 0f, drawnTo * 360f, radius, stroke, StrokeCap.Round)
                    }
                }

                val laps = floor(shown).toInt()
                if (laps >= 1) {
                    val base = lapColor(laps - 1)
                    if (laps >= 2) drawArcAt(SolidColor(base), 0f, 360f, radius, stroke, StrokeCap.Butt)
                    val partial = shown - laps
                    if (partial > 0.002f) {
                        val shadow = 0.55f * (partial / SHADOW_RAMP).coerceAtMost(1f)
                        if (shadow > 0.01f) {
                            val tip = pointAt(partial * 360f + 2f, radius)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(Color.Black.copy(alpha = shadow), Color.Transparent),
                                    center = tip,
                                    radius = stroke * 0.9f,
                                ),
                                radius = stroke * 0.9f,
                                center = tip,
                            )
                        }
                        val next = lapColor(laps)
                        val brush = sweep(base, next, partial, capFraction(stroke, radius))
                        drawArcAt(brush, 0f, partial * 360f, radius, stroke, StrokeCap.Round)
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

private fun DrawScope.drawGlow(color: Color, sweep: Float, radius: Float, stroke: Float) {
    val paint = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = stroke * 1.3f
        strokeCap = android.graphics.Paint.Cap.ROUND
        this.color = color.toArgb()
        maskFilter = BlurMaskFilter(stroke * 0.8f, BlurMaskFilter.Blur.NORMAL)
    }
    drawIntoCanvas {
        it.nativeCanvas.drawArc(center.x - radius, center.y - radius, center.x + radius, center.y + radius, 0f, sweep, false, paint)
    }
}

private const val MAX_LAPS = 10
private const val GRADIENT_RAMP = 0.5f
private const val SHADOW_RAMP = 0.12f
private const val SOLID_BELOW_CAPS = 4f

private fun capFraction(stroke: Float, radius: Float): Float = (stroke / 2 / radius / (2 * Math.PI)).toFloat()

private fun DrawScope.sweep(from: Color, to: Color, tip: Float, cap: Float): Brush {
    val stops = buildList {
        add(0f to from)
        add(tip to to)
        if (tip + cap < 1f) add(tip + cap to to)
        add(1f to from)
    }
    return Brush.sweepGradient(*stops.toTypedArray(), center = center)
}
private fun absorbSlivers(parts: MutableList<Triple<Float, Float, Color>>, minimum: Float) {
    var index = 0
    while (parts.size > 1 && index < parts.size) {
        val (from, to, _) = parts[index]
        if (to - from >= minimum) {
            index++
            continue
        }
        if (index > 0) {
            parts[index - 1] = parts[index - 1].copy(second = to)
        } else {
            parts[1] = parts[1].copy(first = from)
        }
        parts.removeAt(index)
    }
}

private fun DrawScope.segmentSweep(parts: List<Triple<Float, Float, Color>>, drawnTo: Float, cap: Float): Brush {
    val first = parts.first().third
    val last = parts.last().third
    val stops = buildList {
        add(0f to first)
        parts.zipWithNext().forEach { (before, after) ->
            add(before.second to before.third)
            add(after.first to after.third)
        }
        add(drawnTo to last)
        if (drawnTo + cap < 1f) {
            add(drawnTo + cap to last)
            add(drawnTo + cap to first)
        }
        add(1f to first)
    }
    return Brush.sweepGradient(*stops.toTypedArray(), center = center)
}

private val LAP_PINK = Color(0xFFFF6FAE)

private fun DrawScope.pointAt(degrees: Float, radius: Float): Offset {
    val angle = Math.toRadians(degrees.toDouble())
    return Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
}

private fun DrawScope.drawArcAt(brush: Brush, start: Float, sweep: Float, radius: Float, stroke: Float, cap: StrokeCap) {
    drawArc(
        brush = brush,
        startAngle = start,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(stroke, cap = cap),
    )
}

@Composable
fun LegendItem(color: Color, label: String, value: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(10.dp)
                .background(color, CircleShape),
        )
        Text("$label  $value", fontSize = 15.sp)
    }
}
