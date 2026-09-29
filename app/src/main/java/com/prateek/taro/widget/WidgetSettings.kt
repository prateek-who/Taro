package com.prateek.taro.widget

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.annotation.ColorRes
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.prateek.taro.R

enum class WidgetStyle { ICON, COMPACT, PLAIN }

data class WidgetSettings(
    val opacity: Int,
    val textScale: Int,
    val useDynamicColors: Boolean,
    val themeMode: String,
) {
    val scale: Float get() = textScale / 100f
}

class WidgetColors(
    val background: Color,
    val primary: Color,
    val secondary: Color,
    val surface: Color,
    val onSurface: Color,
)

fun widgetPrefs(context: Context, appWidgetId: Int) =
    context.getSharedPreferences("widget_prefs_$appWidgetId", Context.MODE_PRIVATE)

fun readWidgetSettings(context: Context, appWidgetId: Int): WidgetSettings {
    val prefs = widgetPrefs(context, appWidgetId)
    return WidgetSettings(
        opacity = prefs.getInt("opacity", 100),
        textScale = prefs.getInt("text_scale", 100),
        useDynamicColors = prefs.getBoolean("use_dynamic_colors", Build.VERSION.SDK_INT >= 31),
        themeMode = prefs.getString("theme_mode", "system") ?: "system",
    )
}

fun systemIsDark(): Boolean =
    Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

fun isDark(themeMode: String) = when (themeMode) {
    "dark" -> true
    "light" -> false
    else -> systemIsDark()
}

fun Context.widgetColors(dark: Boolean, useDynamicColors: Boolean): WidgetColors {
    val config = Configuration(resources.configuration).apply {
        val nightMode = if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
    }
    val themed = createConfigurationContext(config)
    val dynamic = useDynamicColors && Build.VERSION.SDK_INT >= 31

    fun color(@ColorRes dynamicRes: Int, @ColorRes defaultRes: Int) =
        Color(ContextCompat.getColor(themed, if (dynamic) dynamicRes else defaultRes))

    return WidgetColors(
        background = color(R.color.widgetBackground, R.color.widgetBackground_default),
        primary = color(R.color.widgetPrimary, R.color.widgetPrimary_default),
        secondary = color(R.color.widgetSecondary, R.color.widgetSecondary_default),
        surface = color(R.color.colorSurface, R.color.colorSurface),
        onSurface = color(R.color.colorOnSurface, R.color.colorOnSurface),
    )
}
