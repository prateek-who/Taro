package com.prateek.taro.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.prateek.taro.ui.theme.Chivo
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.Util
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private val AxisGray = Color(0xFF888888)
private const val STAGGER = 0.07f

private fun barColor(base: Color, value: Int, min: Int, max: Int, dark: Boolean): Color {
    if (max == min) return base
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(base.toArgb(), hsl)
    val factor = (value - min).toFloat() / (max - min)
    val (from, to) = if (dark) 0.3f to 0.75f else 0.75f to 0.3f
    hsl[2] = from + (to - from) * factor
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    val path = Path()
    for (point in 0 until 10) {
        val angle = Math.PI / 5 * point - Math.PI / 2
        val r = if (point % 2 == 0) radius else radius * 0.45f
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (point == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

@Composable
fun StepsBarChart(
    values: List<Int>,
    labels: List<String>,
    goal: Int,
    modifier: Modifier = Modifier,
    highlightGoal: Int = goal,
    valueLabel: (Int) -> String = { it.toString() },
    showMultiplier: Boolean = true,
    appearKey: Any? = Unit,
    selectedIndex: Int? = null,
    onBarClick: ((Int) -> Unit)? = null,
    metColor: Color? = null,
    barTint: Color? = null,
) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(appearKey) {
        appear.snapTo(0f)
        appear.animateTo(1f, tween(900, easing = LinearEasing))
    }
    val animated = values.map {
        animateFloatAsState(it.toFloat(), tween(200, easing = FastOutSlowInEasing), label = "bar").value
    }
    val stagger = if (values.size > 1) minOf(STAGGER, 0.5f / (values.size - 1)) else 0f
    fun growth(index: Int) =
        EaseOutBack.transform(((appear.value - index * stagger) / (1f - stagger * (values.size - 1))).coerceIn(0f, 1f))
    val dataMax = animated.maxOrNull() ?: 0f
    val axisMax = if (goal > 0) max(dataMax, goal * 1.1f) * 1.05f else max(dataMax * 1.05f, 1f)

    val primary = MaterialTheme.colorScheme.primary
    val dark = isSystemInDarkTheme()
    val colors = remember(values, primary, dark) {
        val min = values.minOrNull() ?: 0
        val max = values.maxOrNull() ?: 0
        values.map { barColor(primary, it, min, max, dark) }
    }
    val goalColor = TaroTheme.colors.accent.copy(alpha = 100 / 255f)
    val goalMetColor = metColor ?: TaroTheme.colors.goal
    val starColor = TaroTheme.colors.special
    val anyGoalMet = highlightGoal > 0 && values.any { it >= highlightGoal }
    val measurer = rememberTextMeasurer()
    val valueStyle = TextStyle(color = AxisGray, fontSize = 10.sp, fontFamily = Chivo)
    val labelStyle = valueStyle.copy(fontSize = 12.sp)
    val multiplierStyle = valueStyle.copy(color = goalMetColor, fontWeight = FontWeight.Bold)
    val description = labels.zip(values).joinToString { (label, value) -> "$label $value" }

    val tap = if (onBarClick != null) {
        Modifier.pointerInput(values.size) {
            detectTapGestures { offset ->
                onBarClick((offset.x / (size.width / values.size)).toInt().coerceIn(0, values.size - 1))
            }
        }
    } else {
        Modifier
    }

    Canvas(modifier = modifier.then(tap).semantics { contentDescription = description }) {
        val bottomLabelHeight = 24.dp.toPx()
        val top = (if (anyGoalMet) 44.dp else 20.dp).toPx()
        val baseline = size.height - bottomLabelHeight
        val plotHeight = baseline - top
        val slot = size.width / values.size
        val barWidth = slot * 0.62f
        val corner = CornerRadius(minOf(barWidth / 2, 8.dp.toPx()))
        val gap = 2.dp.toPx()

        if (goal > 0) {
            val y = baseline - goal / axisMax * plotHeight
            drawLine(
                color = goalColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 12f)),
            )
        }

        drawLine(AxisGray, Offset(0f, baseline), Offset(size.width, baseline), strokeWidth = 1.dp.toPx())

        animated.forEachIndexed { index, value ->
            val slotStart = index * slot
            labels.getOrNull(index)?.let { label ->
                val layout = measurer.measure(label, labelStyle)
                drawText(
                    layout,
                    topLeft = Offset(slotStart + (slot - layout.size.width) / 2, baseline + gap * 2),
                )
            }
            val grown = growth(index)
            val height = value / axisMax * plotHeight * grown
            val metGoal = highlightGoal > 0 && values[index] >= highlightGoal
            val dim = if (selectedIndex != null && selectedIndex != index) 0.3f else 1f
            val barTop = baseline - height
            val barLeft = slotStart + (slot - barWidth) / 2
            val top = CornerRadius(minOf(corner.x, height))
            val bar = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(barLeft, barTop, barLeft + barWidth, baseline),
                        topLeft = top,
                        topRight = top,
                        bottomRight = CornerRadius.Zero,
                        bottomLeft = CornerRadius.Zero,
                    ),
                )
            }
            drawPath(
                path = bar,
                brush = if (metGoal) {
                    Brush.verticalGradient(listOf(goalMetColor, goalMetColor.copy(alpha = 0.45f)), startY = barTop, endY = baseline)
                } else if (barTint != null) {
                    Brush.verticalGradient(listOf(barTint, barTint.copy(alpha = 0.45f)), startY = barTop, endY = baseline)
                } else {
                    SolidColor(colors[index])
                },
                alpha = dim,
            )
            if (grown < 0.98f) return@forEachIndexed

            var labelTop = baseline - height - gap
            if (value >= 1f) {
                val layout = measurer.measure(
                    valueLabel(value.toInt()),
                    if (selectedIndex == index) valueStyle.copy(color = starColor, fontWeight = FontWeight.Bold) else valueStyle,
                )
                labelTop -= layout.size.height
                drawText(layout, topLeft = Offset(slotStart + (slot - layout.size.width) / 2, labelTop))
            }

            if (metGoal) {
                val starRadius = 6.dp.toPx()
                val starCenter = Offset(slotStart + slot / 2, labelTop - gap - starRadius)
                drawStar(starCenter, starRadius, starColor)
                Util.goalMultiplier(values[index], highlightGoal)?.takeIf { showMultiplier }?.let { multiplier ->
                    val layout = measurer.measure(multiplier, multiplierStyle)
                    drawText(
                        layout,
                        topLeft = Offset(
                            slotStart + (slot - layout.size.width) / 2,
                            starCenter.y - starRadius - gap - layout.size.height,
                        ),
                    )
                }
            }
        }
    }
}
