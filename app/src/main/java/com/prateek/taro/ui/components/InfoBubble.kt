package com.prateek.taro.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.prateek.taro.ui.theme.TaroMotion
import com.prateek.taro.ui.theme.TaroTheme

data class Info(val title: String, val text: String, val color: Color = Color.Unspecified)

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

private val TailHeight = 9.dp
private val TailWidth = 18.dp
private val Corner = 20.dp

private class BubbleShape(private val tailX: Float, private val below: Boolean, private val tail: Float, private val corner: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val top = if (below) tail else 0f
        val bottom = if (below) size.height else size.height - tail
        val half = with(density) { TailWidth.toPx() } / 2
        val x = tailX.coerceIn(corner + half, size.width - corner - half)
        val body = Path().apply { addRoundRect(RoundRect(0f, top, size.width, bottom, corner, corner)) }
        val pointer = Path().apply {
            moveTo(x - half, if (below) top + 1 else bottom - 1)
            quadraticTo(x - half * 0.25f, if (below) top else bottom, x, if (below) 0f else size.height)
            quadraticTo(x + half * 0.25f, if (below) top else bottom, x + half, if (below) top + 1 else bottom - 1)
            close()
        }
        return Outline.Generic(Path.combine(androidx.compose.ui.graphics.PathOperation.Union, body, pointer))
    }
}

private class BubblePosition(private val margin: Int, private val gap: Int, private val onPlaced: (tailX: Float, below: Boolean) -> Unit) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val centerX = anchorBounds.left + anchorBounds.width / 2
        val x = (centerX - popupContentSize.width / 2).coerceIn(margin, maxOf(margin, windowSize.width - popupContentSize.width - margin))
        val below = anchorBounds.bottom + popupContentSize.height + gap + margin <= windowSize.height
        val y = if (below) anchorBounds.bottom + gap else anchorBounds.top - popupContentSize.height - gap
        onPlaced((centerX - x).toFloat(), below)
        return IntOffset(x, y)
    }
}

@Composable
private fun InfoBubble(info: Info, tint: Color, onDismiss: () -> Unit) {
    val density = LocalDensity.current
    var tailX by remember { mutableFloatStateOf(0f) }
    var below by remember { mutableStateOf(true) }
    val position = remember(density) {
        BubblePosition(with(density) { 16.dp.roundToPx() }, with(density) { 4.dp.roundToPx() }) { x, isBelow ->
            tailX = x
            below = isBelow
        }
    }
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, TaroMotion.snappy()) }
    val shape = BubbleShape(tailX, below, with(density) { TailHeight.toPx() }, with(density) { Corner.toPx() })
    val surface = tint.copy(alpha = 0.1f).compositeOver(TaroTheme.colors.dialogSurface)

    Popup(popupPositionProvider = position, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        Column(
            modifier = Modifier
                .graphicsLayer {
                    val value = entrance.value
                    scaleX = 0.8f + 0.2f * value
                    scaleY = 0.8f + 0.2f * value
                    alpha = value.coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(if (size.width > 0) tailX / size.width else 0.5f, if (below) 0f else 1f)
                }
                .widthIn(max = 300.dp)
                .shadow(16.dp, shape, ambientColor = tint, spotColor = tint)
                .background(surface, shape)
                .border(1.dp, tint.copy(alpha = 0.35f), shape)
                .clickable(onClick = onDismiss)
                .semantics { contentDescription = "${info.title}. ${info.text}" }
                .padding(start = 18.dp, end = 18.dp, top = if (below) 14.dp + TailHeight else 14.dp, bottom = if (below) 14.dp else 14.dp + TailHeight),
        ) {
            Text(
                text = info.title.uppercase(),
                color = tint,
                fontSize = 12.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = info.text,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
