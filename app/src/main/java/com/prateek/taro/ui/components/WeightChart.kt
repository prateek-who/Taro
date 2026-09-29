package com.prateek.taro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prateek.taro.energy.TrendPoint
import com.prateek.taro.ui.theme.Chivo
import com.prateek.taro.ui.theme.TaroTheme
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun WeightChart(
    points: List<TrendPoint>,
    toDisplay: (Double) -> Double,
    modifier: Modifier = Modifier,
    description: String = "",
) {
    val dotColor = TaroTheme.colors.accent.copy(alpha = 0.45f)
    val lineColor = MaterialTheme.colorScheme.onSurface
    val labelColor = Color(0xFF888888)
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = labelColor, fontSize = 10.sp, fontFamily = Chivo)

    Canvas(modifier = modifier.semantics { contentDescription = description }) {
        if (points.isEmpty()) return@Canvas
        val values = points.flatMap { listOf(toDisplay(it.kg), toDisplay(it.trend)) }
        val low = values.min()
        val high = values.max()
        val padding = maxOf((high - low) * 0.15, 0.5)
        val minY = low - padding
        val maxY = high + padding

        val labelWidth = 36.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 8.dp.toPx()
        val left = labelWidth
        val right = size.width
        val first = points.first().date
        val span = maxOf(ChronoUnit.DAYS.between(first, points.last().date), 1L).toFloat()

        fun x(point: TrendPoint) = left + (right - left) * ChronoUnit.DAYS.between(first, point.date) / span
        fun y(value: Double) = (bottom - (bottom - top) * ((value - minY) / (maxY - minY))).toFloat()

        listOf(maxY - padding, minY + padding).forEach { value ->
            val layout = measurer.measure("%.1f".format(Locale.getDefault(), value), labelStyle)
            drawText(layout, topLeft = Offset(0f, y(value) - layout.size.height / 2))
        }

        points.forEach { drawCircle(dotColor, radius = 3.dp.toPx(), center = Offset(x(it), y(toDisplay(it.kg)))) }

        if (points.size > 1) {
            val path = Path()
            points.forEachIndexed { index, point ->
                val offset = Offset(x(point), y(toDisplay(point.trend)))
                if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
            }
            drawPath(path, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        } else {
            drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(x(points.first()), y(toDisplay(points.first().trend))))
        }
    }
}
