package com.nvllz.stepsy.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.nvllz.stepsy.ui.theme.Chivo
import com.nvllz.stepsy.ui.theme.StepsyTheme
import com.nvllz.stepsy.util.Util
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private val AxisGray = Color(0xFF888888)

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
) {
    val animated = values.map {
        animateFloatAsState(it.toFloat(), tween(200, easing = FastOutSlowInEasing), label = "bar").value
    }
    val dataMax = animated.maxOrNull() ?: 0f
    val axisMax = if (goal > 0) max(dataMax, goal * 1.1f) * 1.05f else max(dataMax * 1.05f, 1f)

    val primary = MaterialTheme.colorScheme.primary
    val dark = isSystemInDarkTheme()
    val colors = remember(values, primary, dark) {
        val min = values.minOrNull() ?: 0
        val max = values.maxOrNull() ?: 0
        values.map { barColor(primary, it, min, max, dark) }
    }
    val goalColor = StepsyTheme.colors.accent.copy(alpha = 100 / 255f)
    val goalMetColor = StepsyTheme.colors.goal
    val starColor = StepsyTheme.colors.special
    val anyGoalMet = highlightGoal > 0 && values.any { it >= highlightGoal }
    val measurer = rememberTextMeasurer()
    val valueStyle = TextStyle(color = AxisGray, fontSize = 10.sp, fontFamily = Chivo)
    val labelStyle = valueStyle.copy(fontSize = 12.sp)
    val multiplierStyle = valueStyle.copy(color = goalMetColor, fontWeight = FontWeight.Bold)
    val description = labels.zip(values).joinToString { (label, value) -> "$label $value" }

    Canvas(modifier = modifier.semantics { contentDescription = description }) {
        val bottomLabelHeight = 24.dp.toPx()
        val top = (if (anyGoalMet) 44.dp else 20.dp).toPx()
        val baseline = size.height - bottomLabelHeight
        val plotHeight = baseline - top
        val slot = size.width / values.size
        val barWidth = slot * 0.92f
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
            val height = value / axisMax * plotHeight
            val metGoal = highlightGoal > 0 && values[index] >= highlightGoal
            drawRect(
                color = if (metGoal) goalMetColor else colors[index],
                topLeft = Offset(slotStart + (slot - barWidth) / 2, baseline - height),
                size = Size(barWidth, height),
            )

            var labelTop = baseline - height - gap
            if (value >= 1f) {
                val layout = measurer.measure(value.toInt().toString(), valueStyle)
                labelTop -= layout.size.height
                drawText(layout, topLeft = Offset(slotStart + (slot - layout.size.width) / 2, labelTop))
            }

            if (metGoal) {
                val starRadius = 6.dp.toPx()
                val starCenter = Offset(slotStart + slot / 2, labelTop - gap - starRadius)
                drawStar(starCenter, starRadius, starColor)
                Util.goalMultiplier(values[index], highlightGoal)?.let { multiplier ->
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

            labels.getOrNull(index)?.let { label ->
                val layout = measurer.measure(label, labelStyle)
                drawText(
                    layout,
                    topLeft = Offset(slotStart + (slot - layout.size.width) / 2, baseline + gap * 2),
                )
            }
        }
    }
}
