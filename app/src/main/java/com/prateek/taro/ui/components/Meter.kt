package com.prateek.taro.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.prateek.taro.R
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.prateek.taro.ui.theme.TaroTheme

@Composable
fun MeterBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    band: ClosedFloatingPointRange<Float>? = null,
    height: Dp = TaroTheme.spacing.tight + TaroTheme.spacing.hair,
    marker: Float? = null,
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val track = TaroTheme.colors.accentOpaque
    val grow = rememberDrawIn(Unit)
    val fill by animateFloatAsState(fraction.coerceIn(0f, 1f), spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), label = "meter")
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = radius)
        band?.let {
            val from = it.start.coerceIn(0f, 1f) * size.width
            val to = it.endInclusive.coerceIn(0f, 1f) * size.width
            if (to > from) drawRoundRect(color.copy(alpha = 0.25f), topLeft = Offset(from, 0f), size = Size(to - from, size.height), cornerRadius = radius)
        }
        val width = size.width * fill * grow.value
        if (width > 0f) drawRoundRect(color, size = Size(maxOf(width, size.height), size.height), cornerRadius = radius)
        marker?.let {
            val x = it.coerceIn(0f, 1f) * size.width
            drawLine(ink, Offset(x, 0f), Offset(x, size.height), strokeWidth = size.height * MARKER_SHARE)
        }
    }
}

@Composable
fun Meter(
    label: String,
    value: String,
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    band: ClosedFloatingPointRange<Float>? = null,
    info: Info? = null,
    onEdit: (() -> Unit)? = null,
    marker: Float? = null,
) {
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val touchable = info != null || onEdit != null
    InfoBox(
        info = info,
        modifier = modifier
            .fillMaxWidth()
            .then(if (touchable) Modifier.clip(RoundedCornerShape(spacing.gap)) else Modifier),
        color = color,
        onClick = onEdit,
    ) {
        Column(modifier = if (touchable) Modifier.padding(horizontal = spacing.tight, vertical = spacing.tight) else Modifier) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(label, style = type.meterLabel, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text(value, style = type.meterValue, color = TaroTheme.colors.accent)
                if (onEdit != null) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = stringResource(R.string.meter_edit, label),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(start = spacing.tight)
                            .size(with(LocalDensity.current) { type.meterValue.lineHeight.toDp() }),
                    )
                }
            }
            MeterBar(fraction, color, band = band, marker = marker, modifier = Modifier.padding(top = spacing.tight))
        }
    }
}

@Composable
fun StackBar(parts: List<Pair<Double, Color>>, modifier: Modifier = Modifier, height: Dp = TaroTheme.spacing.gap) {
    val progress = rememberDrawIn(parts.size)
    val values = rememberMorph(parts.map { it.first })
    val gap = with(LocalDensity.current) { TaroTheme.spacing.hair.toPx() }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2)),
    ) {
        val now = values().zip(parts) { value, part -> value to part.second }
        val total = now.sumOf { it.first.coerceAtLeast(0.0) }.takeIf { it > 0 } ?: return@Canvas
        val shown = now.filter { it.first > 0 }
        val room = size.width - gap * (shown.size - 1).coerceAtLeast(0)
        val reach = size.width * progress.value
        var x = 0f
        shown.forEach { (value, color) ->
            val width = (value / total).toFloat() * room
            val visible = (reach - x).coerceIn(0f, width)
            if (visible > 0f) drawRect(color, topLeft = Offset(x, 0f), size = Size(visible, size.height))
            x += width + gap
        }
    }
}

private const val MARKER_SHARE = 0.25f
