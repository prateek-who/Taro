package com.prateek.taro.ui.components

import com.prateek.taro.ui.theme.TaroMotion
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import com.prateek.taro.ui.theme.rememberAnimationsEnabled

val DrawInEasing = CubicBezierEasing(0.22f, 0.8f, 0.24f, 1f)
val SettleEasing = CubicBezierEasing(0.34f, 1.45f, 0.64f, 1f)

private const val DRAW_IN_MS = 1100
private const val BAR_GROW_MS = 650f
private const val BAR_STAGGER_MS = 40f
private const val BAR_SPREAD_MS = 480f

class ChartProgress internal constructor(private val clock: Animatable<Float, *>, private val total: Float, private val stagger: Float) {
    val value: Float get() = clock.value

    fun bar(index: Int): Float =
        if (total <= 0f) 1f else SettleEasing.transform(((clock.value * total - index * stagger) / BAR_GROW_MS).coerceIn(0f, 1f))

    fun fill(): Float = ((clock.value - 0.25f) / 0.75f).coerceIn(0f, 1f)
}

@Composable
fun rememberDrawIn(key: Any?): ChartProgress {
    val enabled = rememberAnimationsEnabled()
    val clock = remember { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(key, enabled) {
        if (!enabled) {
            clock.snapTo(1f)
            return@LaunchedEffect
        }
        clock.snapTo(0f)
        clock.animateTo(1f, tween(DRAW_IN_MS, easing = DrawInEasing))
    }
    return remember(clock) { ChartProgress(clock, 0f, 0f) }
}

@Composable
fun rememberBarGrowth(key: Any?, count: Int): ChartProgress {
    val enabled = rememberAnimationsEnabled()
    val stagger = if (count > 1) minOf(BAR_STAGGER_MS, BAR_SPREAD_MS / (count - 1)) else 0f
    val total = BAR_GROW_MS + stagger * (count - 1).coerceAtLeast(0)
    val clock = remember { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(key, enabled) {
        if (!enabled) {
            clock.snapTo(1f)
            return@LaunchedEffect
        }
        clock.snapTo(0f)
        clock.animateTo(1f, tween(total.toInt(), easing = LinearEasing))
    }
    return remember(clock, total, stagger) { ChartProgress(clock, total, stagger) }
}

@Composable
fun rememberMorph(values: List<Double>): () -> List<Double> {
    val enabled = rememberAnimationsEnabled()
    val holder = remember { arrayOf(values, values) }
    val mix = remember { Animatable(1f) }
    LaunchedEffect(values, enabled) {
        val (from, to) = holder
        holder[0] = if (from.size == to.size) from.zip(to) { a, b -> a + (b - a) * mix.value } else to
        holder[1] = values
        if (!enabled || holder[0].size != values.size) {
            mix.snapTo(1f)
            return@LaunchedEffect
        }
        mix.snapTo(0f)
        mix.animateTo(1f, TaroMotion.snappy())
    }
    return {
        val (from, to) = holder
        val t = mix.value.toDouble()
        if (from.size != to.size || t >= 1.0) to else from.zip(to) { a, b -> a + (b - a) * t }
    }
}

fun monotonePath(points: List<Offset>, path: Path = Path()): Path {
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) return path
    val n = points.size
    val dx = FloatArray(n - 1) { points[it + 1].x - points[it].x }
    val slope = FloatArray(n - 1) { if (dx[it] == 0f) 0f else (points[it + 1].y - points[it].y) / dx[it] }
    val tangent = FloatArray(n)
    tangent[0] = slope[0]
    tangent[n - 1] = slope[n - 2]
    for (i in 1 until n - 1) {
        tangent[i] = if (slope[i - 1] * slope[i] <= 0f) {
            0f
        } else {
            3f * (dx[i - 1] + dx[i]) / ((2f * dx[i] + dx[i - 1]) / slope[i - 1] + (dx[i] + 2f * dx[i - 1]) / slope[i])
        }
    }
    for (i in 0 until n - 1) {
        val third = dx[i] / 3f
        val from = points[i]
        val to = points[i + 1]
        path.cubicTo(from.x + third, from.y + tangent[i] * third, to.x - third, to.y - tangent[i + 1] * third, to.x, to.y)
    }
    return path
}

fun Path.partial(fraction: Float): Path {
    if (fraction >= 1f) return this
    val measure = PathMeasure().apply { setPath(this@partial, false) }
    return Path().also { measure.getSegment(0f, measure.length * fraction.coerceAtLeast(0f), it, true) }
}

private fun smootherstep(x: Double): Double = when {
    x <= 0.0 -> 0.0
    x >= 1.0 -> 1.0
    else -> x * x * x * (x * (x * 6 - 15) + 10)
}

fun runningTotals(perHour: List<Double>, steps: Int, spreadHours: Double, leadHours: Double): List<Double> =
    (0..perHour.size * steps).map { index ->
        val hour = index.toDouble() / steps
        perHour.withIndex().sumOf { (slot, amount) -> amount * smootherstep((hour - slot - leadHours) / spreadHours) }
    }
