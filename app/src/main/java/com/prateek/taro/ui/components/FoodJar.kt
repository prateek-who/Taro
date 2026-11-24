package com.prateek.taro.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
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
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.ui.theme.rememberAnimationsEnabled

private const val JAR_ASPECT = 0.82f
private const val NECK_SHARE = 0.09f
private const val LID_SHARE = 0.52f
private const val BODY_CORNER_SHARE = 0.22f
private const val HEADROOM = 1.06
private const val BAND_ALPHA = 0.16f
private const val FLOOR_STROKE_SHARE = 0.022f
private const val FLOOR_REACH_SHARE = 0.03f
private const val WAVE_AMPLITUDE_SHARE = 0.05f
private const val BACK_WAVE_ALPHA = 0.5f
private const val FRONT_WAVE_MS = 2600
private const val BACK_WAVE_MS = 3800
private const val SHINE_ALPHA = 0.06f
private const val SHINE_X_SHARE = 0.13f
private const val SHINE_WIDTH_SHARE = 0.075f

@Composable
fun FoodJar(parts: List<Pair<Double, Color>>, floor: Double, top: Double, color: Color, overColor: Color, modifier: Modifier = Modifier) {
    val track = TaroTheme.colors.accentOpaque
    val progress = rememberDrawIn(parts.map { it.first })
    val animate = rememberAnimationsEnabled()
    val still = remember { mutableFloatStateOf(0f) }
    val front: State<Float>
    val back: State<Float>
    if (animate) {
        val transition = rememberInfiniteTransition(label = "jar waves")
        front = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(FRONT_WAVE_MS, easing = LinearEasing), RepeatMode.Restart), label = "front wave")
        back = transition.animateFloat(1f, 0f, infiniteRepeatable(tween(BACK_WAVE_MS, easing = LinearEasing), RepeatMode.Restart), label = "back wave")
    } else {
        front = still
        back = still
    }
    Canvas(modifier = modifier.aspectRatio(JAR_ASPECT)) {
        val neck = size.height * NECK_SHARE
        val lid = size.width * LID_SHARE
        val body = RoundRect(0f, neck, size.width, size.height, CornerRadius(size.width * BODY_CORNER_SHARE))
        val shape = Path().apply { addRoundRect(body) }
        drawRoundRect(track, topLeft = Offset((size.width - lid) / 2, 0f), size = Size(lid, neck * 1.4f), cornerRadius = CornerRadius(neck / 2))
        drawPath(shape, track)

        val shown = parts.filter { it.first > 0 }
        val eaten = shown.sumOf { it.first }
        val cap = maxOf(top, floor, eaten) * HEADROOM
        val inner = size.height - neck
        fun level(value: Double) = if (cap <= 0) 0f else (value / cap).toFloat() * inner
        fun y(value: Double) = size.height - level(value)
        val amplitude = size.width * WAVE_AMPLITUDE_SHARE

        clipPath(shape) {
            if (top > floor) drawRect(color.copy(alpha = BAND_ALPHA), topLeft = Offset(0f, y(top)), size = Size(size.width, y(floor) - y(top)))
            val reach = eaten * progress.value
            var at = 0.0
            shown.forEach { (value, tint) ->
                val end = minOf(at + value, reach)
                if (end > at) drawRect(tint, topLeft = Offset(0f, y(end)), size = Size(size.width, level(end) - level(at) + 1f))
                at += value
            }
            if (reach > top && top > 0) drawRect(overColor, topLeft = Offset(0f, y(reach)), size = Size(size.width, y(top) - y(reach)))
            if (reach > 0) {
                val surfaceColor = if (reach > top && top > 0) overColor else shown.last().second
                wave(y(reach), amplitude * 0.7f, back.value, surfaceColor.copy(alpha = surfaceColor.alpha * BACK_WAVE_ALPHA))
                wave(y(reach), amplitude, front.value, surfaceColor)
            }
            drawRoundRect(
                Color.White.copy(alpha = SHINE_ALPHA),
                topLeft = Offset(size.width * SHINE_X_SHARE, neck + size.height * 0.08f),
                size = Size(size.width * SHINE_WIDTH_SHARE, inner * 0.7f),
                cornerRadius = CornerRadius(size.width * SHINE_WIDTH_SHARE / 2),
            )
        }
        if (top > floor) {
            val reach = size.width * FLOOR_REACH_SHARE
            drawLine(color, Offset(-reach, y(floor)), Offset(size.width + reach, y(floor)), strokeWidth = size.width * FLOOR_STROKE_SHARE, cap = StrokeCap.Round)
        }
    }
}

private fun DrawScope.wave(surface: Float, amplitude: Float, phase: Float, color: Color) {
    val length = size.width / 2
    val path = Path()
    var x = -length * 2 + phase * length * 2
    path.moveTo(x, surface)
    var up = true
    while (x < size.width + length) {
        path.quadraticTo(x + length / 4, surface + if (up) -amplitude else amplitude, x + length / 2, surface)
        x += length / 2
        up = !up
    }
    path.lineTo(x, surface + amplitude * 2)
    path.lineTo(-length * 2, surface + amplitude * 2)
    path.close()
    drawPath(path, color)
}
