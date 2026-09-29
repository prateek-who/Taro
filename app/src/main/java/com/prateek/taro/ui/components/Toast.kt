package com.prateek.taro.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prateek.taro.R
import com.prateek.taro.ui.theme.TaroTheme
import kotlinx.coroutines.delay

enum class ToastKind { SUCCESS, ERROR, INFO }

data class ToastMessage(val text: String, val kind: ToastKind, val id: Long)

@Stable
class ToastState {
    var current by mutableStateOf<ToastMessage?>(null)
        private set

    var bottomOffset by mutableStateOf(0.dp)

    fun show(text: String, kind: ToastKind = ToastKind.INFO) {
        current = ToastMessage(text, kind, System.nanoTime())
    }

    fun dismiss() {
        current = null
    }
}

val LocalToast = staticCompositionLocalOf<ToastState> { error("ToastState not provided") }

@Composable
fun rememberToastState() = remember { ToastState() }

private const val TOAST_DURATION_MS = 2_500L

@Composable
fun ToastHost(state: ToastState, modifier: Modifier = Modifier) {
    val message = state.current
    var lastMessage by remember { mutableStateOf(message) }
    if (message != null) lastMessage = message

    LaunchedEffect(message?.id) {
        if (message != null) {
            delay(TOAST_DURATION_MS)
            if (state.current?.id == message.id) state.dismiss()
        }
    }

    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(tween(250)) { it } + fadeIn(tween(250)),
        exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(200)),
        modifier = modifier.padding(bottom = state.bottomOffset),
    ) {
        lastMessage?.let { ToastPill(it, onClick = state::dismiss) }
    }
}

@Composable
private fun ToastPill(message: ToastMessage, onClick: () -> Unit, maxWidth: Dp = 420.dp) {
    val (icon, tint) = when (message.kind) {
        ToastKind.SUCCESS -> R.drawable.ic_toast_success to TaroTheme.colors.goal
        ToastKind.ERROR -> R.drawable.ic_toast_error to MaterialTheme.colorScheme.error
        ToastKind.INFO -> R.drawable.ic_info to TaroTheme.colors.accent
    }
    val shape = RoundedCornerShape(24.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .widthIn(max = maxWidth)
            .shadow(8.dp, shape)
            .background(TaroTheme.colors.dialogSurface, shape)
            .clickable(onClick = onClick)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(start = 14.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .padding(end = 10.dp)
                .size(20.dp),
        )
        Text(message.text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
