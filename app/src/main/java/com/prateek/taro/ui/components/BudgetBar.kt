package com.prateek.taro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import com.prateek.taro.ui.theme.TaroTheme

private const val HEADROOM = 1.06
private const val BAND_ALPHA = 0.26f
private const val LANE_REST_ALPHA = 0.28f
private const val HATCH_ALPHA = 0.32f
private const val HATCH_STEP = 2.5f
private const val PIN_WIDTH = 1.5f
private const val OUTLINE = 0.75f

@Composable
fun BudgetBar(
    parts: List<Pair<Double, Color>>,
    eaten: Double,
    burned: Double,
    floor: Double,
    top: Double,
    modifier: Modifier = Modifier,
) {
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val track = colors.accentOpaque
    val over = MaterialTheme.colorScheme.error
    val outline = colors.background
    val values = rememberMorph(parts.map { it.first } + listOf(eaten, burned, floor, top))
    val grow = rememberDrawIn(Unit)
    val barHeight = spacing.gap + spacing.hair
    val head = spacing.tight
    val laneGap = spacing.hair * 2
    val lane = spacing.hair * 2
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(head + spacing.tight + barHeight + laneGap + lane),
    ) {
        val now = values()
        val shares = now.take(parts.size)
        val (eat, burn, low, high) = now.drop(parts.size)
        val max = maxOf(high, eat, burn).coerceAtLeast(1.0) * HEADROOM
        fun x(value: Double) = (value / max).toFloat().coerceIn(0f, 1f) * size.width
        val headPx = head.toPx()
        val hair = spacing.hair.toPx()
        val barTop = headPx + spacing.tight.toPx()
        val bar = barHeight.toPx()
        val laneTop = barTop + bar + laneGap.toPx()
        val laneHeight = lane.toPx()
        val radius = CornerRadius(bar / 2)
        val reach = grow.value

        drawRoundRect(track, Offset(0f, barTop), Size(size.width, bar), radius)
        val shape = Path().apply { addRoundRect(RoundRect(0f, barTop, size.width, barTop + bar, radius)) }
        clipPath(shape) {
            if (high > low) {
                drawRect(colors.special.copy(alpha = BAND_ALPHA), Offset(x(low), barTop), Size(x(high) - x(low), bar))
                drawRect(colors.special, Offset(x(low), barTop), Size(hair, bar))
            }
            if (burn > eat) hatch(x(eat), x(burn) * reach, barTop, bar, colors.flame.copy(alpha = HATCH_ALPHA), hair)
            var start = 0f
            shares.forEachIndexed { i, share ->
                val width = (share / max).toFloat() * size.width * reach
                if (width > 0f) drawRect(parts[i].second, Offset(start, barTop), Size(width, bar))
                start += width
            }
            if (eat > high && high > 0) drawRect(over, Offset(x(high), barTop), Size((x(eat) - x(high)) * reach, bar))
        }

        val laneRadius = CornerRadius(laneHeight / 2)
        if (low > burn) drawRoundRect(colors.flame.copy(alpha = LANE_REST_ALPHA), Offset(0f, laneTop), Size(x(low), laneHeight), laneRadius)
        drawRoundRect(colors.flame, Offset(0f, laneTop), Size(x(burn) * reach, laneHeight), laneRadius)

        val pinX = x(burn) * reach
        val pin = hair * PIN_WIDTH
        val pinTop = barTop - hair
        drawLine(outline, Offset(pinX, pinTop), Offset(pinX, laneTop + laneHeight), pin + hair * OUTLINE * 2, StrokeCap.Round)
        drawLine(colors.flame, Offset(pinX, pinTop), Offset(pinX, laneTop + laneHeight), pin, StrokeCap.Round)
        val half = headPx * 0.6f
        val tip = headPx
        val arrow = Path().apply {
            moveTo(pinX - half, tip - headPx * 0.9f)
            lineTo(pinX + half, tip - headPx * 0.9f)
            lineTo(pinX, tip)
            close()
        }
        drawPath(arrow, colors.flame)
    }
}

private fun DrawScope.hatch(from: Float, to: Float, top: Float, height: Float, color: Color, stroke: Float) {
    if (to <= from) return
    val step = stroke * HATCH_STEP
    clipRect(left = from, top = top, right = to, bottom = top + height) {
        var x = from - height
        while (x < to) {
            drawLine(color, Offset(x, top + height), Offset(x + height, top), stroke)
            x += step
        }
    }
}
