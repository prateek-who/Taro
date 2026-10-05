package com.prateek.taro.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.State
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.prateek.taro.ui.theme.rememberAnimationsEnabled
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class Scene { BLOBS, CONFETTI }

private const val TWO_PI = (2 * PI).toFloat()

@Composable
private fun loop(durationMs: Int): State<Float> {
    val enabled = rememberAnimationsEnabled()
    if (!enabled) return remember { mutableFloatStateOf(0.35f) }
    return rememberInfiniteTransition(label = "scene").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = LinearEasing), RepeatMode.Restart),
        label = "scene time",
    )
}

private class Particle(val x: Float, val y: Float, val speed: Float, val size: Float, val phase: Float, val spin: Float)

private fun particles(count: Int, seed: Int) = Random(seed).let { random ->
    List(count) {
        Particle(
            x = random.nextFloat(),
            y = random.nextFloat(),
            speed = 0.6f + random.nextFloat() * 0.8f,
            size = 0.5f + random.nextFloat(),
            phase = random.nextFloat() * TWO_PI,
            spin = random.nextFloat() * 360f,
        )
    }
}

@Composable
fun StoryScene(scene: Scene, ink: Color, seed: Int, modifier: Modifier = Modifier) {
    Blobs(ink, seed, modifier)
    if (scene == Scene.CONFETTI) Confetti(ink, seed, modifier)
}

@Composable
private fun Blobs(ink: Color, seed: Int, modifier: Modifier) {
    val time = loop(14_000)
    val offsets = remember(seed) { Random(seed).let { r -> List(3) { r.nextFloat() * TWO_PI } } }
    Canvas(modifier) {
        val t = time.value * TWO_PI
        offsets.forEachIndexed { i, phase ->
            val center = Offset(
                size.width * (0.2f + 0.6f * ((i * 0.37f) % 1f)) + size.width * 0.12f * sin(t + phase),
                size.height * (0.2f + 0.3f * i) + size.height * 0.06f * cos(t * 0.8f + phase),
            )
            drawCircle(ink.copy(alpha = 0.06f), radius = size.width * (0.32f + 0.06f * i), center = center)
        }
    }
}

@Composable
private fun Confetti(ink: Color, seed: Int, modifier: Modifier) {
    val time = loop(5_000)
    val pieces = remember(seed) { particles(46, seed) }
    val colors = listOf(ink, Color.White, Color(0xFFFF6FAE), Color(0xFF4FC3F7), Color(0xFFFFD54F))
    Canvas(modifier) {
        pieces.forEachIndexed { i, p ->
            val y = ((p.y + time.value * p.speed) % 1.15f - 0.1f) * size.height
            val x = p.x * size.width + sin(time.value * TWO_PI * 2 + p.phase) * 14.dp.toPx()
            rotate(p.spin + time.value * 720f * p.speed, Offset(x, y)) {
                drawRect(
                    colors[i % colors.size].copy(alpha = 0.85f),
                    topLeft = Offset(x - 4.dp.toPx() * p.size, y - 2.dp.toPx() * p.size),
                    size = Size(8.dp.toPx() * p.size, 4.dp.toPx() * p.size),
                )
            }
        }
    }
}
