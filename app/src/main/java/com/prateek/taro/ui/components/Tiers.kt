package com.prateek.taro.ui.components

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import com.prateek.taro.util.Sprees
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.aspectRatio
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import com.prateek.taro.R
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.StepTiers
import com.prateek.taro.util.Util
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Immutable
data class TierStyle(@StringRes val name: Int, val color: Color, val holo: Boolean = false)

val TIER_NAMES = listOf(R.string.tier_goal, R.string.tier_overtime, R.string.tier_heating, R.string.tier_double, R.string.tier_relentless, R.string.tier_mythic, R.string.tier_beyond)

val SPREE_NAMES = listOf(R.string.spree_warming, R.string.spree_roll, R.string.spree_locked, R.string.spree_iron, R.string.spree_habit, R.string.spree_centurion)

@Composable
fun tierStyles(): List<TierStyle> {
    val colors = TaroTheme.colors
    val tints = listOf(colors.goal, colors.mint, colors.special, colors.flame, colors.red, colors.purple, colors.purple)
    return TIER_NAMES.mapIndexed { index, name -> TierStyle(name, tints[index], holo = index == StepTiers.LAST) }
}

@Composable
fun spreeStyles(): List<TierStyle> {
    val tiers = tierStyles()
    return SPREE_NAMES
        .mapIndexed { index, name -> tiers[index + 1].copy(name = name) }
}

@Composable
fun holoColors(): List<Color> {
    val colors = TaroTheme.colors
    return listOf(colors.red, colors.special, colors.mint, colors.sleep, colors.purple)
}

fun tierMultiple(index: Int): String {
    val multiple = StepTiers.MULTIPLES[index]
    return if (multiple % 1.0 == 0.0) multiple.toInt().toString() else multiple.toString()
}

@Composable
fun TierText(style: TierStyle, text: String, textStyle: TextStyle, modifier: Modifier = Modifier, textAlign: TextAlign? = null) {
    if (style.holo) {
        val holo = holoColors()
        val span = with(LocalDensity.current) { textStyle.fontSize.toPx() } * HOLO_SPAN
        val shift = rememberInfiniteTransition(label = "holo").animateFloat(0f, 1f, infiniteRepeatable(tween(HOLO_MS, easing = LinearEasing)), label = "holo shift")
        val brush = Brush.horizontalGradient(holo + holo.first(), startX = shift.value * span, endX = shift.value * span + span, tileMode = TileMode.Repeated)
        Text(text = text, style = textStyle.copy(brush = brush), textAlign = textAlign, modifier = modifier)
    } else {
        Text(text, style = textStyle, color = style.color, textAlign = textAlign, modifier = modifier)
    }
}

@Composable
fun TierMedal(index: Int, modifier: Modifier = Modifier) {
    val style = tierStyles()[index]
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val disc = with(LocalDensity.current) { type.heavy.lineHeight.toDp() } * MEDAL_DISC_SHARE
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.tight),
        modifier = modifier
            .clip(CircleShape)
            .background(style.color.copy(alpha = MEDAL_ALPHA))
            .padding(start = spacing.hair, end = spacing.gap, top = spacing.hair, bottom = spacing.hair),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(disc)
                .clip(CircleShape)
                .background(if (style.holo) Brush.linearGradient(holoColors()) else Brush.linearGradient(listOf(style.color, style.color))),
        ) {
            Text(stringResource(R.string.tier_multiple, tierMultiple(index)), style = type.heavy.copy(fontSize = type.heavy.fontSize * MEDAL_MULTIPLE_SHARE), color = TaroTheme.colors.background)
        }
        TierText(style, stringResource(style.name).uppercase(), type.heavy)
    }
}

@Composable
fun TierBar(steps: Int, goal: Int, modifier: Modifier = Modifier, course: Int? = null) {
    val styles = tierStyles()
    val holo = holoColors()
    val colors = TaroTheme.colors
    val ink = MaterialTheme.colorScheme.onSurface
    val measurer = rememberTextMeasurer()
    val axis = TaroTheme.text.axis
    val goalLabel = stringResource(R.string.tier_goal)
    val labels = StepTiers.MULTIPLES.indices.map { if (it == 0) goalLabel else stringResource(R.string.tier_multiple, tierMultiple(it)) }
    val reached = StepTiers.reached(steps, goal)
    val density = LocalDensity.current
    val barHeight = TaroTheme.spacing.gap + TaroTheme.spacing.hair
    val labelRoom = with(density) { axis.fontSize.toDp() } * LABEL_ROOM_SHARE
    val shown by animateFloatAsState(steps.toFloat(), spring(dampingRatio = 0.85f, stiffness = 200f), label = "tier bar")
    val grow = rememberDrawIn(Unit)
    val animatedTop by animateFloatAsState(tierScale(steps, goal), spring(stiffness = 120f), label = "tier scale")

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(if (reached >= 0) barHeight + labelRoom else barHeight),
    ) {
        val bar = barHeight.toPx()
        val radius = CornerRadius(bar / 2)
        val value = shown * grow.value
        fun x(v: Float) = (v / animatedTop).coerceIn(0f, 1f) * size.width
        drawRoundRect(colors.accentOpaque, size = Size(size.width, bar), cornerRadius = radius)
        val lane = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, bar, radius)) }
        clipPath(lane) {
            if (course != null && course > steps && reached < 0) {
                drawRect(colors.goal.copy(alpha = COURSE_ALPHA), topLeft = Offset(0f, 0f), size = Size(x(course.toFloat()), bar))
            }
            if (goal > 0) {
                drawRect(colors.goal, size = Size(x(minOf(value, goal.toFloat())), bar))
                (1..StepTiers.LAST).forEach { index ->
                    val from = StepTiers.threshold(index - 1, goal).toFloat()
                    val to = StepTiers.threshold(index, goal).toFloat()
                    if (value > from) {
                        val left = x(from)
                        val width = x(minOf(value, if (index == StepTiers.LAST) Float.MAX_VALUE else to)) - left
                        val style = styles[index]
                        if (style.holo) {
                            drawRect(Brush.horizontalGradient(holo, startX = left, endX = left + width.coerceAtLeast(1f)), topLeft = Offset(left, 0f), size = Size(width, bar))
                        } else {
                            drawRect(style.color, topLeft = Offset(left, 0f), size = Size(width, bar))
                        }
                        drawRect(colors.background, topLeft = Offset(left - bar * SEAM_SHARE / 2, 0f), size = Size(bar * SEAM_SHARE, bar))
                    }
                }
            } else {
                drawRoundRect(colors.goal, size = Size(size.width * grow.value, bar), cornerRadius = radius)
            }
        }
        if (reached < 0) return@Canvas
        val placed = mutableListOf<ClosedFloatingPointRange<Float>>()
        val order = listOf(reached + 1, reached, 0) + StepTiers.MULTIPLES.indices
        order.distinct().filter { it in StepTiers.MULTIPLES.indices && StepTiers.threshold(it, goal) <= animatedTop * 1.001f }.forEach { index ->
            val tx = x(StepTiers.threshold(index, goal).toFloat())
            val lit = index <= reached
            val tint = if (lit) ink else colors.accent
            drawLine(tint, Offset(tx, -bar * TICK_OVERHANG), Offset(tx, bar * (1 + TICK_OVERHANG)), strokeWidth = bar * TICK_SHARE)
            val layout = measurer.measure(labels[index], axis.copy(color = tint))
            val from = (tx - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
            val span = from - bar / 2..from + layout.size.width + bar / 2
            if (placed.none { it.start < span.endInclusive && span.start < it.endInclusive }) {
                placed += span
                drawText(layout, topLeft = Offset(from, bar * (1 + TICK_OVERHANG) + bar * LABEL_GAP_SHARE))
            }
        }
    }
}

fun tierScale(steps: Int, goal: Int): Float {
    val reached = StepTiers.reached(steps, goal)
    return when {
        goal <= 0 -> 1f
        reached < 0 -> goal.toFloat()
        reached >= StepTiers.LAST -> maxOf(steps.toFloat(), StepTiers.threshold(StepTiers.LAST, goal).toFloat()) * TOP_HEADROOM
        else -> StepTiers.threshold(reached + 1, goal).toFloat()
    }
}

@Composable
fun TierRing(steps: Int, goal: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val styles = tierStyles()
    val holo = holoColors()
    val colors = TaroTheme.colors
    val reached = StepTiers.reached(steps, goal)
    val shown by animateFloatAsState(steps.toFloat(), spring(dampingRatio = 0.85f, stiffness = 200f), label = "tier ring")
    val top by animateFloatAsState(tierScale(steps, goal), spring(stiffness = 120f), label = "tier ring scale")
    val grow = rememberDrawIn(Unit)
    Box(contentAlignment = Alignment.Center, modifier = modifier.aspectRatio(1f)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * RING_STROKE_SHARE
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            val value = shown * grow.value
            fun sweep(v: Float) = (v / top).coerceIn(0f, 1f) * 360f
            fun arc(from: Float, to: Float, brush: Brush) {
                val start = sweep(from)
                val end = sweep(to)
                if (end > start) drawArc(brush, RING_START + start, end - start, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
            }
            drawArc(colors.accentOpaque, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
            if (goal <= 0) return@Canvas
            arc(0f, minOf(value, goal.toFloat()), SolidColor(colors.goal))
            (1..StepTiers.LAST).forEach { index ->
                val from = StepTiers.threshold(index - 1, goal).toFloat()
                if (value <= from) return@forEach
                val to = if (index == StepTiers.LAST) value else minOf(value, StepTiers.threshold(index, goal).toFloat())
                val style = styles[index]
                arc(from, to, if (style.holo) Brush.sweepGradient(holo + holo.first(), center) else SolidColor(style.color))
            }
            if (reached < 0) return@Canvas
            StepTiers.MULTIPLES.indices.forEach { index ->
                val v = StepTiers.threshold(index, goal).toFloat()
                if (v > value || v > top) return@forEach
                val angle = Math.toRadians((RING_START + sweep(v)).toDouble())
                val radius = arcSize.width / 2
                val inner = radius - stroke / 2
                val outer = radius + stroke / 2
                drawLine(
                    colors.background,
                    Offset(center.x + (cos(angle) * inner).toFloat(), center.y + (sin(angle) * inner).toFloat()),
                    Offset(center.x + (cos(angle) * outer).toFloat(), center.y + (sin(angle) * outer).toFloat()),
                    strokeWidth = stroke * RING_SEAM_SHARE,
                )
            }
        }
        content()
    }
}

@Composable
fun TierLabel(steps: Int, goal: Int, fallback: String, modifier: Modifier = Modifier) {
    val reached = StepTiers.reached(steps, goal)
    if (reached < 0) {
        Text(fallback.uppercase(), style = TaroTheme.text.label, color = TaroTheme.colors.goal, modifier = modifier)
        return
    }
    val style = tierStyles()[reached]
    val text = if (reached == 0) stringResource(style.name) + " ✓" else stringResource(R.string.tier_multiple, tierMultiple(reached)) + " " + stringResource(style.name)
    TierText(style, text.uppercase(), rankLabelStyle(), modifier)
}

@Composable
fun SpreeLabel(days: Int, fallback: String?, modifier: Modifier = Modifier) {
    val spree = Sprees.reached(days)
    if (spree < 0) {
        fallback?.let { Text(it.uppercase(), style = TaroTheme.text.label, color = TaroTheme.colors.special, modifier = modifier) }
        return
    }
    val style = spreeStyles()[spree]
    TierText(style, stringResource(style.name).uppercase(), rankLabelStyle(), modifier)
}

@Composable
private fun rankLabelStyle(): TextStyle = TaroTheme.text.label.copy(fontFamily = TaroTheme.text.heavy.fontFamily, fontWeight = TaroTheme.text.heavy.fontWeight)

@Composable
fun SpreeBanner(index: Int, days: Int, onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val style = spreeStyles()[index]
    val holo = holoColors()
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val haptics = LocalHapticFeedback.current
    val drop = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }
    val sparks = SPARKS_BASE + index * SPARKS_PER_TIER
    val edge = if (style.holo) holo.last() else style.color
    val shape = RoundedCornerShape(spacing.loose)

    LaunchedEffect(Unit) {
        launch { drop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 380f)) }
        delay(BANNER_LAND_MS)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        launch { burst.animateTo(1f, tween(BURST_MS, easing = FastOutSlowInEasing)) }
        delay(BANNER_HOLD_MS)
        exit.animateTo(1f, tween(EXIT_MS, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                translationY = -size.height * BANNER_DROP * (1f - drop.value + exit.value)
                alpha = drop.value.coerceIn(0f, 1f) * (1f - exit.value)
            }
            .clickable(interactionSource = null, indication = null, onClick = onFinished),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawSparks(burst.value, center, size.width * BANNER_SPARK_REACH, sparks, size.height * BANNER_SPARK_SIZE) { i ->
                if (i % 3 == 0) Color.White else if (style.holo) holo[i % holo.size] else style.color
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.gap),
            modifier = Modifier
                .shadow(spacing.tight, shape)
                .clip(shape)
                .background(TaroTheme.colors.dialogSurface)
                .border(spacing.hair / 2, edge.copy(alpha = BANNER_EDGE_ALPHA), shape)
                .padding(horizontal = spacing.loose, vertical = spacing.gap),
        ) {
            TierText(style, days.toString(), type.value.copy(fontFamily = type.heavy.fontFamily, fontWeight = type.heavy.fontWeight))
            Column {
                TierText(style, stringResource(style.name).uppercase(), type.heavy)
                Text(pluralStringResource(R.plurals.spree_days, days, days), style = type.note, color = TaroTheme.colors.accent)
            }
        }
    }
}

fun DrawScope.drawSparks(t: Float, origin: Offset, reach: Float, count: Int, radius: Float, color: (Int) -> Color) {
    repeat(count) { i ->
        val angle = i * 2 * Math.PI / count + i * SPARK_JITTER
        val r = reach * t * (0.55f + (i * 37 % 45) / 100f)
        val center = Offset(origin.x + (cos(angle) * r).toFloat(), origin.y + (sin(angle) * r).toFloat())
        drawCircle(color(i).copy(alpha = (1f - t).coerceIn(0f, 1f)), radius = radius * (1.2f - t * 0.6f), center = center)
    }
}

fun tierGlow(steps: Int, goal: Int, styles: List<TierStyle>): Color {
    val reached = StepTiers.reached(steps, goal)
    return when {
        reached < 1 -> styles[0].color
        styles[reached].holo -> styles[reached - 1].color
        else -> styles[reached].color
    }
}

@Composable
fun TierNumber(text: String, style: TextStyle, steps: Int, goal: Int, modifier: Modifier = Modifier, fit: Boolean = false) {
    val styles = tierStyles()
    val reached = StepTiers.reached(steps, goal)
    val tier = styles.getOrNull(reached)
    val color by animateColorAsState(tier?.takeUnless { it.holo }?.color ?: styles[0].color, tween(NUMBER_FADE_MS), label = "tier number")
    val holo = holoColors()
    val shown = if (tier?.holo == true) style.copy(brush = Brush.linearGradient(holo)) else style.copy(color = color)
    if (fit) FitNumber(text, shown, modifier) else RollingText(text, shown, modifier)
}

@Composable
private fun FitNumber(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    androidx.compose.foundation.text.BasicText(
        text = text,
        style = style,
        maxLines = 1,
        autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(maxFontSize = style.fontSize),
        modifier = modifier,
    )
}

@Composable
fun TierStamp(index: Int, steps: Int, onFinished: () -> Unit) {
    val style = tierStyles()[index]
    val holo = holoColors()
    val type = TaroTheme.text
    val scale = remember { Animatable(STAMP_START_SCALE) }
    val appear = remember { Animatable(0f) }
    val flash = remember { Animatable(FLASH_ALPHA) }
    val burst = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }
    val strength = (index + 1f) / StepTiers.MULTIPLES.size
    val sparks = SPARKS_BASE + index * SPARKS_PER_TIER

    LaunchedEffect(Unit) {
        launch { flash.animateTo(0f, tween(FLASH_MS)) }
        launch { appear.animateTo(1f, tween(APPEAR_MS)) }
        launch { burst.animateTo(1f, tween(BURST_MS, easing = FastOutSlowInEasing)) }
        scale.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 520f))
        delay(HOLD_MS)
        exit.animateTo(1f, tween(EXIT_MS, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val a = flash.value * (0.6f + strength * 0.4f)
            if (style.holo) drawRect(Brush.linearGradient(holo), alpha = a) else drawRect(style.color, alpha = a)
            val t = burst.value
            val origin = Offset(size.width / 2, size.height * STAMP_CENTER)
            val reach = size.minDimension * (SPARK_REACH + strength * SPARK_REACH)
            drawSparks(t, origin, reach, sparks, size.minDimension * SPARK_SIZE) { i ->
                if (i % 3 == 0) Color.White else if (style.holo) holo[i % holo.size] else style.color
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val e = exit.value
                    val s = scale.value * (1f - e * EXIT_SHRINK)
                    scaleX = s
                    scaleY = s
                    rotationZ = STAMP_TILT * (1f - e)
                    alpha = appear.value * (1f - e)
                    translationY = -size.height * (1f - STAMP_CENTER * 2) - e * size.height * EXIT_RISE
                },
        ) {
            val shadow = Shadow(if (style.holo) holo.last() else style.color, blurRadius = STAMP_GLOW)
            TierText(style, stringResource(style.name).uppercase(), type.stamp.copy(shadow = shadow), textAlign = TextAlign.Center)
            Text(
                text = stringResource(R.string.tier_stamp_steps, Util.formatSteps(steps)).uppercase(),
                style = type.heroLabel,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = TaroTheme.spacing.tight),
            )
        }
    }
}

suspend fun Animatable<Float, *>.shake(strength: Float) {
    SHAKE_STEPS.forEach { animateTo(it * strength, tween(SHAKE_STEP_MS)) }
    animateTo(0f, tween(SHAKE_STEP_MS))
}

private val SHAKE_STEPS = listOf(-1f, 0.8f, -0.6f, 0.45f, -0.25f)
private const val SHAKE_STEP_MS = 55
private const val RING_STROKE_SHARE = 0.1f
private const val RING_START = -90f
private const val RING_SEAM_SHARE = 0.18f
private const val HOLO_MS = 2_800
private const val HOLO_SPAN = 6f
private const val MEDAL_ALPHA = 0.18f
private const val MEDAL_DISC_SHARE = 1.75f
private const val MEDAL_MULTIPLE_SHARE = 0.7f
private const val LABEL_ROOM_SHARE = 2.6f
private const val TOP_HEADROOM = 1.05f
private const val COURSE_ALPHA = 0.3f
private const val SEAM_SHARE = 0.18f
private const val TICK_OVERHANG = 0.3f
private const val TICK_SHARE = 0.14f
private const val LABEL_GAP_SHARE = 0.35f
private const val NUMBER_FADE_MS = 600
private const val STAMP_START_SCALE = 3.2f
private const val FLASH_ALPHA = 0.5f
private const val FLASH_MS = 650
private const val APPEAR_MS = 140
private const val BURST_MS = 1_000
private const val HOLD_MS = 900L
private const val EXIT_MS = 420
private const val EXIT_SHRINK = 0.6f
private const val EXIT_RISE = 0.35f
private const val STAMP_TILT = -6f
private const val STAMP_CENTER = 0.4f
private const val STAMP_GLOW = 36f
private const val SPARKS_BASE = 18
private const val SPARKS_PER_TIER = 6
private const val SPARK_REACH = 0.35f
private const val SPARK_JITTER = 0.37
private const val SPARK_SIZE = 0.011f
private const val BANNER_LAND_MS = 280L
private const val BANNER_HOLD_MS = 2_600L
private const val BANNER_DROP = 1.6f
private const val BANNER_SPARK_REACH = 0.6f
private const val BANNER_SPARK_SIZE = 0.05f
private const val BANNER_EDGE_ALPHA = 0.6f
