package com.prateek.taro.ui.components

import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.prateek.taro.ui.theme.TaroMotion
import com.prateek.taro.ui.theme.TaroTheme

data class Info(val title: String, val text: String, val color: Color = Color.Unspecified, @DrawableRes val icon: Int? = null) {
    fun withIcon(@DrawableRes fallback: Int?): Info = if (icon == null && fallback != null) copy(icon = fallback) else this
}

@Composable
fun infoOf(title: Int, text: Int, color: Color = Color.Unspecified) = Info(stringResource(title), stringResource(text), color)

@Stable
class InfoState {
    var open by mutableStateOf(false)
}

@Composable
fun rememberInfoState() = remember { InfoState() }

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.infoClickable(state: InfoState, info: Info?, onClick: (() -> Unit)? = null): Modifier = when {
    info == null && onClick == null -> this
    info == null -> clickable(onClick = onClick!!)
    onClick == null -> clickable { state.open = true }
    else -> combinedClickable(onClick = onClick, onLongClick = { state.open = true })
}

@Composable
fun InfoPopup(state: InfoState, info: Info?, fallbackColor: Color) {
    if (state.open && info != null) {
        InfoBubble(info, if (info.color.isSpecified) info.color else fallbackColor, onDismiss = { state.open = false })
    }
}

@Composable
fun InfoBox(info: Info?, modifier: Modifier = Modifier, color: Color = TaroTheme.colors.accent, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val state = rememberInfoState()
    Box(modifier = modifier.infoClickable(state, info, onClick)) {
        content()
        InfoPopup(state, info, color)
    }
}

private class BubbleShape(private val tailX: Float, private val below: Boolean, private val tail: Float, private val tailWidth: Float, private val corner: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val top = if (below) tail else 0f
        val bottom = if (below) size.height else size.height - tail
        val half = tailWidth / 2
        val x = tailX.coerceIn(corner + half, size.width - corner - half)
        val body = Path().apply { addRoundRect(RoundRect(0f, top, size.width, bottom, corner, corner)) }
        val pointer = Path().apply {
            moveTo(x - half, if (below) top + 1 else bottom - 1)
            quadraticTo(x - half * 0.25f, if (below) top else bottom, x, if (below) 0f else size.height)
            quadraticTo(x + half * 0.25f, if (below) top else bottom, x + half, if (below) top + 1 else bottom - 1)
            close()
        }
        return Outline.Generic(Path.combine(PathOperation.Union, body, pointer))
    }
}

private class Spot(val anchor: IntRect, val window: IntSize)

private class FullWindow(private val onPlaced: (Spot) -> Unit) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        onPlaced(Spot(anchorBounds, windowSize))
        return IntOffset.Zero
    }
}

@Composable
private fun InfoBubble(info: Info, tint: Color, onDismiss: () -> Unit) {
    val colors = TaroTheme.colors
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val density = LocalDensity.current
    var spot by remember { mutableStateOf<Spot?>(null) }
    val position = remember { FullWindow { spot = it } }
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, TaroMotion.snappy()) }
    val scrim = colors.background.copy(alpha = SCRIM_ALPHA)
    val tail = with(density) { spacing.tight.toPx() }
    val tailWidth = with(density) { spacing.loose.toPx() }
    val corner = with(density) { spacing.loose.toPx() }
    val margin = with(density) { spacing.loose.roundToPx() }
    val gap = with(density) { spacing.hair.roundToPx() }
    val halo = with(density) { spacing.hair.toPx() }
    val holeCorner = with(density) { spacing.gap.toPx() }
    val iconSize = with(density) { type.title.lineHeight.toDp() }
    val appView = LocalView.current

    Popup(popupPositionProvider = position, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        val popupView = LocalView.current
        var shift by remember { mutableStateOf(IntOffset.Zero) }
        val current = spot?.let { Spot(it.anchor.translate(-shift.x, -shift.y), it.window) }
        var tailX by remember { mutableFloatStateOf(0f) }
        var below by remember { mutableStateOf(true) }
        Layout(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    val popupAt = IntArray(2).also(popupView::getLocationOnScreen)
                    val appAt = IntArray(2).also(appView::getLocationOnScreen)
                    shift = IntOffset(popupAt[0] - appAt[0], popupAt[1] - appAt[1])
                }
                .drawBehind {
                    if (current == null) return@drawBehind
                    val a = current.anchor
                    val hole = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(Rect(0f, 0f, size.width, size.height))
                        addRoundRect(RoundRect(a.left - halo, a.top - halo, a.right + halo, a.bottom + halo, holeCorner, holeCorner))
                    }
                    drawPath(hole, scrim, alpha = entrance.value.coerceIn(0f, 1f))
                }
                .clickable(interactionSource = null, indication = null, onClick = onDismiss),
            content = {
                val shape = BubbleShape(tailX, below, tail, tailWidth, corner)
                Column(
                    modifier = Modifier
                        .graphicsLayer {
                            val value = entrance.value
                            scaleX = ENTER_SCALE + (1 - ENTER_SCALE) * value
                            scaleY = ENTER_SCALE + (1 - ENTER_SCALE) * value
                            alpha = value.coerceIn(0f, 1f)
                            transformOrigin = TransformOrigin(if (size.width > 0) tailX / size.width else 0.5f, if (below) 0f else 1f)
                        }
                        .shadow(spacing.loose, shape)
                        .clip(shape)
                        .background(colors.dialogSurface)
                        .cornerGlow(tint)
                        .border(spacing.hair / 2, MaterialTheme.colorScheme.onSurface.copy(alpha = EDGE_ALPHA), shape)
                        .clickable(onClick = onDismiss)
                        .semantics { contentDescription = "${info.title}. ${info.text}" }
                        .padding(
                            start = spacing.loose,
                            end = spacing.loose,
                            top = spacing.gap + if (below) spacing.tight else 0.dp,
                            bottom = spacing.loose + if (below) 0.dp else spacing.tight,
                        ),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.gap)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(iconSize * DISC_SHARE)
                                .clip(CircleShape)
                                .background(tint.copy(alpha = DISC_ALPHA)),
                        ) {
                            if (info.icon != null) {
                                Icon(painterResource(info.icon), contentDescription = null, tint = tint, modifier = Modifier.size(iconSize * ICON_SHARE))
                            } else {
                                Box(Modifier.size(iconSize * DOT_SHARE).clip(CircleShape).background(tint))
                            }
                        }
                        Text(info.title, style = type.title, color = tint)
                    }
                    Text(
                        text = info.text,
                        style = type.note,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = TEXT_ALPHA),
                        modifier = Modifier.padding(top = spacing.tight),
                    )
                }
            },
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val bubble = measurables.first().measure(Constraints(maxWidth = (width * WIDTH_SHARE).toInt().coerceAtMost(width - margin * 2)))
            layout(width, height) {
                val anchor = current?.anchor ?: return@layout
                val centerX = anchor.left + anchor.width / 2
                val x = (centerX - bubble.width / 2).coerceIn(margin, maxOf(margin, width - bubble.width - margin))
                val fits = anchor.bottom + gap + bubble.height + margin <= height
                val y = if (fits) anchor.bottom + gap else anchor.top - gap - bubble.height
                tailX = (centerX - x).toFloat()
                below = fits
                bubble.place(x, y)
            }
        }
    }
}

private const val SCRIM_ALPHA = 0.6f
private const val ENTER_SCALE = 0.85f
private const val WIDTH_SHARE = 0.78f
private const val EDGE_ALPHA = 0.08f
private const val TEXT_ALPHA = 0.85f
private const val DISC_SHARE = 1.4f
private const val DISC_ALPHA = 0.2f
private const val ICON_SHARE = 0.8f
private const val DOT_SHARE = 0.4f
