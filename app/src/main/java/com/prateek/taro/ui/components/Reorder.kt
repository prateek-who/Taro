package com.prateek.taro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import com.prateek.taro.R
import com.prateek.taro.ui.theme.TaroTheme

private const val HANDLE_SHARE = 0.8f

@Composable
fun <T> ReorderColumn(
    items: List<T>,
    keyOf: (T) -> Any,
    rowHeight: Dp,
    handleDescription: String,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.(T) -> Unit,
) {
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val density = LocalDensity.current
    val handle = rowHeight * HANDLE_SHARE
    val rowPx = with(density) { rowHeight.toPx() }
    val handlePx = with(density) { handle.toPx() }
    val current by rememberUpdatedState(items)
    val keys by rememberUpdatedState(keyOf)
    val move by rememberUpdatedState(onMove)
    var dragKey by remember { mutableStateOf<Any?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    fun endDrag() {
        dragKey = null
        dragOffset = 0f
    }

    Column(
        modifier = modifier.pointerInput(rowPx, handlePx) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (down.position.x > handlePx) return@awaitEachGesture
                val picked = current.getOrNull((down.position.y / rowPx).toInt()) ?: return@awaitEachGesture
                val dragged = keys(picked)
                dragKey = dragged
                dragOffset = 0f
                drag(down.id) { change ->
                    change.consume()
                    dragOffset += change.positionChange().y
                    val index = current.indexOfFirst { keys(it) == dragged }
                    if (dragOffset > rowPx / 2 && index in 0 until current.lastIndex) {
                        move(index, index + 1)
                        dragOffset -= rowPx
                    } else if (dragOffset < -rowPx / 2 && index > 0) {
                        move(index, index - 1)
                        dragOffset += rowPx
                    }
                }
                endDrag()
            }
        },
    ) {
        items.forEach { item ->
            key(keyOf(item)) {
                val dragging = dragKey == keyOf(item)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (dragging) dragOffset else 0f
                            shadowElevation = if (dragging) spacing.tight.toPx() else 0f
                            shape = RoundedCornerShape(spacing.gap)
                            clip = dragging
                        }
                        .background(if (dragging) colors.accentOpaque else Color.Transparent),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_drag_handle),
                        contentDescription = handleDescription,
                        tint = colors.accent,
                        modifier = Modifier
                            .size(handle)
                            .padding(spacing.gap),
                    )
                    content(item)
                }
            }
        }
    }
}
