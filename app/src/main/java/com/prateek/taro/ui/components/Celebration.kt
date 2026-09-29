package com.prateek.taro.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.prateek.taro.ui.theme.TaroTheme
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class Particle(
    val angle: Double,
    val speed: Float,
    val color: Color,
    val size: Float,
    val spin: Float,
)

@Composable
fun ConfettiBurst(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val palette = listOf(TaroTheme.colors.goal, TaroTheme.colors.special, TaroTheme.colors.flame, TaroTheme.colors.accent)
    val particles = remember {
        List(90) {
            Particle(
                angle = Random.nextDouble(-Math.PI * 0.95, -Math.PI * 0.05),
                speed = Random.nextFloat() * 0.9f + 0.5f,
                color = palette[it % palette.size],
                size = Random.nextFloat() * 10f + 8f,
                spin = Random.nextFloat() * 720f - 360f,
            )
        }
    }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(2_400, easing = LinearEasing))
        onFinished()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = progress.value
        val origin = Offset(size.width / 2, size.height * 0.28f)
        val reach = size.minDimension * 0.75f
        val gravity = size.height * 0.9f
        particles.forEach { particle ->
            val x = origin.x + (cos(particle.angle) * particle.speed * reach * t).toFloat()
            val y = origin.y + (sin(particle.angle) * particle.speed * reach * t).toFloat() + gravity * t * t
            rotate(particle.spin * t, pivot = Offset(x, y)) {
                drawRect(
                    color = particle.color.copy(alpha = (1f - t).coerceIn(0f, 1f)),
                    topLeft = Offset(x - particle.size / 2, y - particle.size / 4),
                    size = Size(particle.size, particle.size / 2),
                )
            }
        }
    }
}
