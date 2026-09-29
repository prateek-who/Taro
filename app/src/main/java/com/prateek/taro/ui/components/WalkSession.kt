package com.prateek.taro.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prateek.taro.calibration.CalibrationSession
import kotlinx.coroutines.delay

@Composable
fun rememberWalkSession(): CalibrationSession {
    val context = LocalContext.current
    val session = remember { CalibrationSession(context) }
    val state by session.state.collectAsStateWithLifecycle()

    DisposableEffect(Unit) { onDispose { session.stop() } }

    val view = LocalView.current
    DisposableEffect(state.running) {
        view.keepScreenOn = state.running
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(state.running) {
        while (state.running) {
            delay(1_000)
            session.tick()
        }
    }
    return session
}
