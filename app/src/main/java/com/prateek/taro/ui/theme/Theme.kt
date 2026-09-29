package com.prateek.taro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.prateek.taro.R

@Immutable
data class TaroColors(
    val accent: Color,
    val accentOpaque: Color,
    val background: Color,
    val dialogSurface: Color,
    val special: Color,
    val goal: Color,
    val flame: Color,
    val sleep: Color,
)

val LocalTaroColors = staticCompositionLocalOf {
    TaroColors(
        accent = Color.Unspecified,
        accentOpaque = Color.Unspecified,
        background = Color.Unspecified,
        dialogSurface = Color.Unspecified,
        special = Color.Unspecified,
        goal = Color.Unspecified,
        flame = Color.Unspecified,
        sleep = Color.Unspecified,
    )
}

object TaroTheme {
    val colors: TaroColors
        @Composable get() = LocalTaroColors.current
}

@Composable
fun TaroTheme(content: @Composable () -> Unit) {
    val primary = colorResource(R.color.colorPrimary)
    val onPrimary = colorResource(R.color.colorOnPrimary)
    val surface = colorResource(R.color.colorSurface)
    val onSurface = colorResource(R.color.colorOnSurface)
    val accent = colorResource(R.color.colorAccent)
    val dialogSurface = colorResource(R.color.colorDialogSurface)

    val taroColors = TaroColors(
        accent = accent,
        accentOpaque = colorResource(R.color.colorAccentOpaque),
        background = colorResource(R.color.colorBackground),
        dialogSurface = dialogSurface,
        special = colorResource(R.color.colorSpecial),
        goal = colorResource(R.color.colorGoal),
        flame = colorResource(R.color.colorFlame),
        sleep = colorResource(R.color.colorSleep),
    )

    val base = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    val colorScheme = base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primary,
        onPrimaryContainer = onPrimary,
        secondary = accent,
        onSecondary = surface,
        tertiary = accent,
        background = taroColors.background,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        onSurfaceVariant = accent,
        surfaceVariant = dialogSurface,
        surfaceContainerLowest = surface,
        surfaceContainerLow = dialogSurface,
        surfaceContainer = dialogSurface,
        surfaceContainerHigh = dialogSurface,
        surfaceContainerHighest = dialogSurface,
        outline = accent,
        outlineVariant = onSurface.copy(alpha = 0.08f),
    )

    CompositionLocalProvider(LocalTaroColors provides taroColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TaroTypography,
            content = content,
        )
    }
}
