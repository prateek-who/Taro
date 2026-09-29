package com.prateek.taro.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import com.prateek.taro.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prateek.taro.energy.TrendPoint
import com.prateek.taro.ui.theme.Chivo
import com.prateek.taro.ui.theme.TaroTheme
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

private const val MIN_SPAN_KG = 2.0

@Composable
fun WeightChart(
    points: List<TrendPoint>,
    toDisplay: (Double) -> Double,
    modifier: Modifier = Modifier,
    showWeighIns: Boolean = true,
    showTrend: Boolean = true,
    description: String = "",
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val accent = TaroTheme.colors.accent
    val grid = TaroTheme.colors.accentOpaque
    val highlight = TaroTheme.colors.special
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = accent, fontSize = 10.sp, fontFamily = Chivo)
    val latestStyle = TextStyle(color = onSurface, fontSize = 11.sp, fontFamily = Chivo, fontWeight = FontWeight.SemiBold)
    val dateFormat = remember { DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()) }
    val monthFormat = remember { DateTimeFormatter.ofPattern("MMM", Locale.getDefault()) }
    val pillSurface = TaroTheme.colors.dialogSurface
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(points) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(900))
    }

    Canvas(modifier = modifier.semantics { contentDescription = description }) {
        if (points.isEmpty()) return@Canvas
        val weighIns = showWeighIns || !showTrend
        val kg = points.map { toDisplay(it.kg) }
        val trend = points.map { toDisplay(it.trend) }
        val visible = (if (weighIns) kg else emptyList()) + (if (showTrend) trend else emptyList())
        val low = visible.min()
        val high = visible.max()
        val middle = (low + high) / 2
        val half = maxOf((high - low) / 2 * 1.25, MIN_SPAN_KG / 2)
        val minY = floor((middle - half) * 2) / 2
        val maxY = ceil((middle + half) * 2) / 2

        val labelWidth = 34.dp.toPx()
        val dotRadius = 4.dp.toPx()
        val left = labelWidth + dotRadius * 2
        val right = size.width - dotRadius * 3
        val top = dotRadius * 3
        val bottom = size.height - 20.dp.toPx()
        val first = points.first().date
        val last = points.last().date
        val span = ChronoUnit.DAYS.between(first, last).toFloat()
        val spacing = if (points.size > 1) (right - left) / (points.size - 1) else right - left
        val pointRadius = (spacing * 0.3f).coerceIn(1.6.dp.toPx(), dotRadius * 0.75f)

        fun xOf(date: java.time.LocalDate): Float =
            if (span <= 0f) (left + right) / 2 else left + (right - left) * ChronoUnit.DAYS.between(first, date) / span
        fun x(index: Int): Float = xOf(points[index].date)
        fun y(value: Double) = (bottom - (bottom - top) * ((value - minY) / (maxY - minY))).toFloat()
        fun path(values: List<Double>) = Path().apply {
            values.indices.forEach { index -> if (index == 0) moveTo(x(index), y(values[index])) else lineTo(x(index), y(values[index])) }
        }

        listOf(maxY, (minY + maxY) / 2, minY).forEach { value ->
            val gy = y(value)
            drawLine(grid, Offset(left - dotRadius, gy), Offset(size.width, gy), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            val layout = measurer.measure("%.1f".format(Locale.getDefault(), value), labelStyle)
            drawText(layout, topLeft = Offset(0f, gy - layout.size.height / 2))
        }

        val firstLabel = measurer.measure(first.format(dateFormat), labelStyle)
        drawText(firstLabel, topLeft = Offset(if (span <= 0f) x(0) - firstLabel.size.width / 2 else left - dotRadius, size.height - firstLabel.size.height))
        if (span > 0f) {
            val lastLabel = measurer.measure(last.format(dateFormat), labelStyle)
            drawText(lastLabel, topLeft = Offset(size.width - lastLabel.size.width, size.height - lastLabel.size.height))
        }

        if (span > 45f) {
            var month = first.withDayOfMonth(1).plusMonths(1)
            while (month.isBefore(last)) {
                val mx = xOf(month)
                if (mx - left > 40.dp.toPx() && right - mx > 40.dp.toPx()) {
                    drawLine(grid, Offset(mx, top), Offset(mx, bottom), strokeWidth = 1.dp.toPx())
                    val layout = measurer.measure(month.format(monthFormat), labelStyle)
                    drawText(layout, topLeft = Offset(mx - layout.size.width / 2, size.height - layout.size.height))
                }
                month = month.plusMonths(1)
            }
        }

        val primary = if (weighIns) kg else trend
        val revealX = left + (right - left) * reveal.value + dotRadius * 3
        clipRect(right = if (span <= 0f) size.width else revealX) {
            if (points.size > 1) {
                val area = path(primary).apply {
                    lineTo(x(points.lastIndex), bottom)
                    lineTo(x(0), bottom)
                    close()
                }
                drawPath(area, Brush.verticalGradient(listOf(onSurface.copy(alpha = 0.12f), onSurface.copy(alpha = 0f)), startY = top, endY = bottom))
                if (showTrend) {
                    val dashed = weighIns
                    drawPath(
                        path(trend),
                        if (dashed) accent else onSurface,
                        style = Stroke(
                            width = (if (dashed) 2.dp else 2.5.dp).toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(14f, 10f)) else null,
                        ),
                    )
                }
                if (weighIns) {
                    drawPath(path(kg), onSurface, style = Stroke(width = (if (points.size > 40) 1.5.dp else 2.5.dp).toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            if (weighIns) {
                points.indices.forEach { index -> drawCircle(onSurface, radius = pointRadius, center = Offset(x(index), y(kg[index]))) }
            }
        }

        val lastValue = primary.last()
        val lastX = x(points.lastIndex)
        val lastY = y(lastValue)
        drawCircle(highlight.copy(alpha = 0.25f * reveal.value), radius = dotRadius * 2.4f, center = Offset(lastX, lastY))
        drawCircle(highlight, radius = dotRadius * reveal.value, center = Offset(lastX, lastY))
        val latest = measurer.measure("%.1f".format(Locale.getDefault(), lastValue), latestStyle)
        val pad = 6.dp.toPx()
        val pillHeight = latest.size.height + pad
        val pillWidth = latest.size.width + pad * 2
        val pillX = (lastX - pillWidth / 2).coerceIn(left, size.width - pillWidth)
        val above = lastY - dotRadius * 3 - pillHeight
        val pillY = if (above > 0) above else lastY + dotRadius * 3
        drawRoundRect(pillSurface.copy(alpha = reveal.value), topLeft = Offset(pillX, pillY), size = Size(pillWidth, pillHeight), cornerRadius = CornerRadius(pillHeight / 2))
        drawRoundRect(highlight.copy(alpha = 0.22f * reveal.value), topLeft = Offset(pillX, pillY), size = Size(pillWidth, pillHeight), cornerRadius = CornerRadius(pillHeight / 2))
        drawText(latest, topLeft = Offset(pillX + pad, pillY + pad / 2), alpha = reveal.value)
    }
}

@Composable
fun WeightSeriesToggles(
    showWeighIns: Boolean,
    showTrend: Boolean,
    onChange: (weighIns: Boolean, trend: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val both = showWeighIns && showTrend
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        SeriesChip(
            label = stringResource(R.string.weight_legend_weigh_ins),
            selected = showWeighIns,
            dashed = false,
            color = onSurface,
            onClick = { if (showTrend || !showWeighIns) onChange(!showWeighIns, showTrend) },
        )
        SeriesChip(
            label = stringResource(R.string.weight_legend_trend),
            selected = showTrend,
            dashed = both,
            color = if (both) TaroTheme.colors.accent else onSurface,
            onClick = { if (showWeighIns || !showTrend) onChange(showWeighIns, !showTrend) },
        )
    }
}

@Composable
private fun SeriesChip(label: String, selected: Boolean, dashed: Boolean, color: Color, onClick: () -> Unit) {
    val alpha by animateFloatAsState(if (selected) 1f else 0.4f, label = "series")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) TaroTheme.colors.accentOpaque else Color.Transparent)
            .selectable(selected = selected, role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .alpha(alpha),
    ) {
        Canvas(modifier = Modifier.size(width = 20.dp, height = 10.dp)) {
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 7f)) else null,
            )
        }
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 6.dp))
    }
}
