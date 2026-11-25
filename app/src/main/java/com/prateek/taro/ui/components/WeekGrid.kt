package com.prateek.taro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.prateek.taro.ui.theme.TaroTheme

@Immutable
data class GridCell(
    val text: String? = null,
    val fill: Float? = null,
    val hit: Boolean = false,
    val warn: Boolean = false,
    val ring: Boolean = false,
    val missing: Boolean = false,
)

@Composable
fun GridHeading(color: Color, title: String, value: String, note: String?, modifier: Modifier = Modifier) {
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val dot = with(LocalDensity.current) { type.note.fontSize.toDp() * DOT_SHARE }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.tight), modifier = modifier.fillMaxWidth()) {
        Box(Modifier.size(dot).clip(CircleShape).drawBehind { drawRect(color) })
        Text(title, style = type.meterLabel, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.tight, Alignment.End), modifier = Modifier.weight(1f)) {
            Text(value, style = type.meterValue, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            note?.let { Text(it, style = type.axis, color = TaroTheme.colors.accent, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

@Composable
fun GridDays(labels: List<Pair<String, String>?>, selected: Int?, color: Color, modifier: Modifier = Modifier, onPick: ((Int) -> Unit)? = null) {
    val type = TaroTheme.text
    Row(horizontalArrangement = Arrangement.spacedBy(TaroTheme.spacing.tight), modifier = modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(TaroTheme.spacing.tight))
                    .then(if (onPick != null && label != null) Modifier.clickable { onPick(index) } else Modifier),
            ) {
                if (label != null) {
                    Text(label.first, style = type.axis, color = TaroTheme.colors.accent, maxLines = 1)
                    Text(label.second, style = type.meterValue, color = if (index == selected) color else MaterialTheme.colorScheme.onSurface, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun GridRow(
    cells: List<GridCell?>,
    color: Color,
    selected: Int?,
    progress: ChartProgress,
    modifier: Modifier = Modifier,
    onPick: ((Int) -> Unit)? = null,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(TaroTheme.spacing.tight), modifier = modifier.fillMaxWidth()) {
        cells.forEachIndexed { index, cell ->
            Box(Modifier.weight(1f)) {
                if (cell != null) GridCellBox(cell, color, index == selected, { progress.bar(index) }, onClick = onPick?.let { { it(index) } })
            }
        }
    }
}

@Composable
fun GridCellBox(cell: GridCell, color: Color, selected: Boolean, growth: () -> Float, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val ink = MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(spacing.gap)
    val tone = when {
        cell.warn -> colors.flame
        cell.hit -> color
        else -> color.copy(alpha = MISS_ALPHA)
    }
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(CELL_RATIO)
            .then(if (selected) Modifier.border(spacing.hair * SELECT_SHARE, ink.copy(alpha = SELECT_ALPHA), shape) else Modifier)
            .padding(spacing.hair)
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val corner = CornerRadius(spacing.gap.toPx())
            drawRoundRect(colors.accentOpaque, cornerRadius = corner)
            if (cell.missing) {
                val step = spacing.tight.toPx()
                val stroke = spacing.hair.toPx() * HATCH_SHARE
                clipRect {
                    var x = -size.height
                    while (x < size.width) {
                        drawLine(colors.accent.copy(alpha = HATCH_ALPHA), Offset(x, size.height), Offset(x + size.height, 0f), stroke)
                        x += step
                    }
                }
            }
            cell.fill?.let { share ->
                val height = size.height * (share * TARGET_AT).coerceIn(0f, 1f) * growth()
                clipRect { drawRect(tone, topLeft = Offset(0f, size.height - height), size = Size(size.width, height)) }
                val line = size.height * (1 - TARGET_AT)
                val inset = spacing.hair.toPx() * 2
                drawLine(
                    ink.copy(alpha = TARGET_ALPHA),
                    Offset(inset, line),
                    Offset(size.width - inset, line),
                    spacing.hair.toPx() * HATCH_SHARE,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(spacing.hair.toPx() * 2, spacing.hair.toPx() * 2)),
                )
            }
            if (cell.ring) {
                val width = spacing.hair.toPx()
                drawRoundRect(colors.special, Offset(width / 2, width / 2), Size(size.width - width, size.height - width), corner, style = Stroke(width))
            }
        }
        cell.text?.let {
            Text(
                text = it,
                style = TaroTheme.text.axis,
                color = ink,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.tight)
                    .then(if (cell.fill == null) Modifier.padding(top = spacing.gap) else Modifier),
            )
        }
    }
}

private const val CELL_RATIO = 0.78f
private const val TARGET_AT = 0.55f
private const val MISS_ALPHA = 0.38f
private const val TARGET_ALPHA = 0.6f
private const val HATCH_ALPHA = 0.28f
private const val HATCH_SHARE = 0.75f
private const val SELECT_ALPHA = 0.55f
private const val SELECT_SHARE = 0.75f
private const val DOT_SHARE = 0.55f
