package com.nvllz.stepsy.ui.components

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
import androidx.compose.ui.graphics.SolidColor
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
import com.nvllz.stepsy.ui.theme.StepsyMotion
import com.nvllz.stepsy.ui.theme.StepsyTheme
import com.nvllz.stepsy.ui.theme.rememberAnimationsEnabled

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
    content: @Composable ColumnScope.() -> Unit,
) {
    val total = segments.sumOf { it.value }
    val lap = if (target > 0) target else maxOf(total, 1.0)
    val fractions = segments.map { (it.value / lap).toFloat().coerceAtLeast(0f) }
    val progress = fractions.sum().coerceIn(0f, 2f)

    val reveal = remember { Animatable(0f) }
    val active = LocalTabActive.current
    LaunchedEffect(progress, active) {
        if (active) reveal.animateTo(progress, StepsyMotion.gentle()) else reveal.snapTo(0f)
    }
    val glow = if (lit && rememberAnimationsEnabled()) {
        rememberInfiniteTransition(label = "ring glow")
            .animateFloat(0.12f, 0.32f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "glow alpha")
    } else {
        null
    }

    val track = StepsyTheme.colors.accentOpaque
    val bonus = StepsyTheme.colors.special
    val fade = StepsyTheme.colors.background
    val glowColor = segments.firstOrNull()?.color ?: bonus

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
                val glowAlpha = glow?.value ?: if (lit) 0.2f else 0f
                if (glowAlpha > 0f) {
                    drawArcAt(SolidColor(glowColor.copy(alpha = glowAlpha)), 0f, firstLap * 360f, radius, stroke * 2.2f, StrokeCap.Round)
                }

                if (gradient && segments.size == 1) {
                    val color = segments.first().color
                    val brush = Brush.sweepGradient(
                        0f to lerp(color, fade, 0.6f),
                        firstLap to color,
                        1f to lerp(color, fade, 0.6f),
                        center = center,
                    )
                    drawArcAt(brush, 0f, firstLap * 360f, radius, stroke, StrokeCap.Round)
                } else {
                    var cursor = 0f
                    segments.forEachIndexed { index, segment ->
                        val sweep = minOf(fractions[index], firstLap - cursor)
                        if (sweep > 0f) drawArcAt(SolidColor(segment.color), cursor * 360f, sweep * 360f, radius, stroke, StrokeCap.Butt)
                        cursor += fractions[index]
                    }
                }

                val secondLap = (shown - 1f).coerceIn(0f, 1f)
                if (secondLap > 0f) drawArcAt(SolidColor(bonus), 0f, secondLap * 360f, radius, stroke * 0.55f, StrokeCap.Round)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
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
