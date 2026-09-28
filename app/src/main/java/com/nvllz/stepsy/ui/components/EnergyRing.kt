package com.nvllz.stepsy.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nvllz.stepsy.ui.theme.StepsyTheme

data class RingSegment(val value: Double, val color: Color)

@Composable
fun EnergyRing(
    segments: List<RingSegment>,
    target: Double,
    modifier: Modifier = Modifier,
    diameter: Dp = 220.dp,
    content: @Composable () -> Unit,
) {
    val scale = maxOf(target, segments.sumOf { it.value }, 1.0)
    val sweeps = segments.map {
        animateFloatAsState((it.value / scale * 360).toFloat(), tween(800), label = "segment sweep").value
    }
    val track = StepsyTheme.colors.accentOpaque

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(diameter)) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(track, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
            var start = -90f
            segments.forEachIndexed { index, segment ->
                drawArc(segment.color, start, sweeps[index], useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                start += sweeps[index]
            }
        }
        content()
    }
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
