package com.prateek.taro.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

object TaroMotion {
    fun <T> gentle(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessVeryLow)
    fun <T> snappy(): SpringSpec<T> = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
    fun <T> bouncy(): SpringSpec<T> = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
}

@Composable
fun rememberAnimationsEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
}
