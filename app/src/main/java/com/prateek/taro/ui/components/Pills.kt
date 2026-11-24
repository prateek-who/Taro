package com.prateek.taro.ui.components

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.border
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prateek.taro.ui.theme.TaroMotion
import com.prateek.taro.ui.theme.TaroTheme
import kotlin.math.roundToInt

@Composable
fun <T> PillSelector(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null,
) {
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val style = LocalTextStyle.current.merge(TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
    val widths = remember(options, style, density) {
        val padding = with(density) { 32.dp.toPx() }
        options.map { measurer.measure(it.second, style).size.width + padding }
    }
    val lefts = remember(widths) { widths.runningFold(0f) { acc, width -> acc + width } }
    val selectedIndex = options.indexOfFirst { it.first == selected }
    val left = animateFloatAsState(lefts.getOrElse(selectedIndex) { 0f }, TaroMotion.snappy(), label = "pill left")
    val width = animateFloatAsState(widths.getOrElse(selectedIndex) { 0f }, TaroMotion.snappy(), label = "pill width")
    val indicator = accent ?: MaterialTheme.colorScheme.onSurface

    LaunchedEffect(selectedIndex, widths) {
        if (selectedIndex >= 0) scroll.animateScrollTo((lefts[selectedIndex] - widths[selectedIndex]).roundToInt().coerceAtLeast(0))
    }

    val edge = MaterialTheme.colorScheme.onSurface.copy(alpha = PILL_BORDER_ALPHA)
    val fade = with(density) { TaroTheme.spacing.loose.toPx() }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(TaroTheme.colors.accentOpaque)
            .border(TaroTheme.spacing.hair / 2, edge, CircleShape)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                if (scroll.canScrollBackward) {
                    drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black), startX = 0f, endX = fade), blendMode = BlendMode.DstIn)
                }
                if (scroll.canScrollForward) {
                    drawRect(Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = size.width - fade, endX = size.width), blendMode = BlendMode.DstIn)
                }
            }
            .horizontalScroll(scroll)
            .padding(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.drawBehind {
                if (selectedIndex >= 0) {
                    drawRoundRect(
                        color = indicator,
                        topLeft = Offset(left.value, 0f),
                        size = Size(width.value, size.height),
                        cornerRadius = CornerRadius(size.height / 2),
                    )
                }
            },
        ) {
            options.forEachIndexed { index, (value, label) ->
                val active = index == selectedIndex
                val color by animateColorAsState(
                    if (active) TaroTheme.colors.background else TaroTheme.colors.accent,
                    label = "pill text",
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(36.dp)
                        .width(with(density) { widths[index].toDp() })
                        .clip(CircleShape)
                        .clickable(role = Role.Tab) { onSelect(value) }
                        .semantics { this.selected = active },
                ) {
                    Text(
                        text = label,
                        style = style,
                        color = color,
                    )
                }
            }
        }
    }
}

@Composable
fun StatPill(icon: Int, text: String, color: Color, lit: Boolean, modifier: Modifier = Modifier, info: Info? = null) {
    val state = rememberInfoState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .infoClickable(state, info)
            .padding(start = 6.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
    ) {
        GlowingIcon(icon = icon, lit = lit, litColor = color, modifier = Modifier.padding(end = 4.dp))
        RollingText(text, TextStyle(fontSize = 16.sp, color = color, fontFamily = MaterialTheme.typography.bodyLarge.fontFamily))
        InfoPopup(state, info, color)
    }
}

private const val PILL_BORDER_ALPHA = 0.16f
