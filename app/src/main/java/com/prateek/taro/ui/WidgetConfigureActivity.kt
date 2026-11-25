package com.prateek.taro.ui

import kotlin.math.roundToInt
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.prateek.taro.ui.components.TaroSwitch
import com.prateek.taro.ui.components.SectionHeader
import com.prateek.taro.ui.components.PreferenceDivider
import com.prateek.taro.ui.components.PreferenceRow
import com.prateek.taro.ui.components.SettingsCard
import com.prateek.taro.ui.components.Panel
import com.prateek.taro.ui.components.TextTabs
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.edit
import com.prateek.taro.R
import com.prateek.taro.ui.components.PrimaryButton
import com.prateek.taro.ui.components.TaroSlider
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.widget.StepsWidgets
import com.prateek.taro.widget.WidgetColors
import com.prateek.taro.widget.WidgetStyle
import com.prateek.taro.widget.isDark
import com.prateek.taro.widget.widgetColors
import com.prateek.taro.widget.widgetPrefs
import kotlinx.coroutines.launch

abstract class WidgetConfigureActivity : AppCompatActivity() {

    protected abstract val style: WidgetStyle

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            TaroTheme {
                WidgetConfigureScreen(style, appWidgetId, onCancel = ::finish) {
                    lifecycleScope.launch {
                        StepsWidgets.update(this@WidgetConfigureActivity, style, appWidgetId, AppPreferences.steps)
                        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                        finish()
                    }
                }
            }
        }
    }
}

class WidgetIconConfigureActivity : WidgetConfigureActivity() {
    override val style = WidgetStyle.ICON
}

class WidgetCompactConfigureActivity : WidgetConfigureActivity() {
    override val style = WidgetStyle.COMPACT
}

class WidgetPlainConfigureActivity : WidgetConfigureActivity() {
    override val style = WidgetStyle.PLAIN
}

@Composable
private fun WidgetConfigureScreen(style: WidgetStyle, appWidgetId: Int, onCancel: () -> Unit, onSave: () -> Unit) {
    val context = LocalContext.current
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val theme = TaroTheme.colors
    val prefs = remember { widgetPrefs(context, appWidgetId) }

    var opacity by remember { mutableFloatStateOf(prefs.getInt("opacity", 100).toFloat()) }
    var textScale by remember { mutableFloatStateOf(prefs.getInt("text_scale", 100).toFloat()) }
    var useDynamicColors by remember { mutableStateOf(prefs.getBoolean("use_dynamic_colors", true)) }
    var themeMode by remember { mutableStateOf(prefs.getString("theme_mode", "system") ?: "system") }
    var inverseBackground by remember { mutableStateOf(false) }

    val colors = remember(themeMode, useDynamicColors) { context.widgetColors(isDark(themeMode), useDynamicColors) }
    val wallpaper = if (inverseBackground) {
        Brush.linearGradient(listOf(colors.onSurface, colors.onSurface))
    } else {
        Brush.linearGradient(listOf(theme.goal, theme.sleep, theme.purple).map { it.copy(alpha = WALLPAPER_ALPHA).compositeOver(colors.surface) })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .safeDrawingPadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.tight)) {
            IconButton(onClick = onCancel) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(androidx.appcompat.R.string.abc_action_bar_up_description), tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(stringResource(R.string.widget_title), style = type.title, color = MaterialTheme.colorScheme.onSurface)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.loose, end = spacing.loose, bottom = spacing.loose),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(spacing.wide))
                    .background(wallpaper)
                    .padding(vertical = spacing.wide * 2, horizontal = spacing.loose),
            ) {
                WidgetPreview(style, colors, opacity / 100f, textScale / 100f)
            }

            SectionHeader(stringResource(R.string.theme))
            TextTabs(
                options = listOf(
                    "system" to stringResource(R.string.preference_system),
                    "light" to stringResource(R.string.theme_light),
                    "dark" to stringResource(R.string.theme_dark),
                ),
                selected = themeMode,
                onSelect = {
                    themeMode = it
                    prefs.edit { putString("theme_mode", it) }
                },
                color = theme.goal,
                modifier = Modifier.padding(horizontal = spacing.tight),
            )

            Panel(modifier = Modifier.padding(top = spacing.gap)) {
                SliderRow(R.string.widget_pref_text_scale, textScale)
                TaroSlider(
                    value = textScale,
                    onValueChange = {
                        textScale = it
                        prefs.edit { putInt("text_scale", it.toInt()) }
                    },
                    valueRange = 75f..150f,
                    steps = 14,
                )
                SliderRow(R.string.widget_pref_bg_opacity, opacity)
                TaroSlider(value = opacity, onValueChange = { opacity = it }, valueRange = 0f..100f)
            }

            SettingsCard(modifier = Modifier.padding(top = spacing.gap)) {
                PreferenceRow(
                    icon = R.drawable.ic_theme,
                    title = stringResource(R.string.widget_pref_invert_bg),
                    color = theme.accent,
                    showChevron = false,
                    onClick = { inverseBackground = !inverseBackground },
                ) {
                    TaroSwitch(checked = inverseBackground, onCheckedChange = { inverseBackground = it }, color = theme.goal)
                }
                PreferenceDivider()
                PreferenceRow(
                    icon = R.drawable.ic_image,
                    title = stringResource(R.string.widget_pref_dynamic_colors),
                    color = theme.goal,
                    showChevron = false,
                    onClick = {
                        useDynamicColors = !useDynamicColors
                        prefs.edit { putBoolean("use_dynamic_colors", useDynamicColors) }
                    },
                ) {
                    TaroSwitch(
                        checked = useDynamicColors,
                        onCheckedChange = {
                            useDynamicColors = it
                            prefs.edit { putBoolean("use_dynamic_colors", it) }
                        },
                        color = theme.goal,
                    )
                }
            }

            PrimaryButton(
                text = stringResource(R.string.widget_pref_btn_save),
                onClick = {
                    prefs.edit(commit = true) {
                        putInt("opacity", opacity.toInt())
                        putBoolean("use_dynamic_colors", useDynamicColors)
                        putInt("text_scale", textScale.toInt())
                    }
                    onSave()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.wide),
            )
        }
    }
}

@Composable
private fun SliderRow(label: Int, value: Float) {
    val type = TaroTheme.text
    Row(modifier = Modifier.padding(top = TaroTheme.spacing.tight, start = TaroTheme.spacing.hair, end = TaroTheme.spacing.hair)) {
        Text(stringResource(label), style = type.meterLabel, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text("${value.roundToInt()}%", style = type.meterValue, color = TaroTheme.colors.accent)
    }
}

@Composable
private fun WidgetPreview(style: WidgetStyle, colors: WidgetColors, opacity: Float, scale: Float) {
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    Box(
        modifier = Modifier
            .background(
                color = colors.background.copy(alpha = colors.background.alpha * opacity),
                shape = RoundedCornerShape(dimensionResource(R.dimen.widgetRoundness)),
            )
            .padding(spacing.loose),
    ) {
        when (style) {
            WidgetStyle.ICON -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.loose)) {
                Image(
                    painter = painterResource(R.drawable.widget_icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colors.primary),
                    modifier = Modifier.size(with(LocalDensity.current) { (type.fact.lineHeight * scale).toDp() }),
                )
                StepsAndDistance(colors, type.fact.fontSize * scale, type.note.fontSize * scale)
            }
            WidgetStyle.COMPACT -> StepsAndDistance(colors, type.value.fontSize * scale, type.axis.fontSize * scale)
            WidgetStyle.PLAIN -> Text(
                text = stringResource(R.string.widget_sample_plain),
                color = colors.primary,
                fontSize = type.value.fontSize * scale,
            )
        }
    }
}

@Composable
private fun StepsAndDistance(colors: WidgetColors, stepsSize: TextUnit, distanceSize: TextUnit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "12,345",
            color = colors.primary,
            fontSize = stepsSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "9.26 km",
            color = colors.secondary,
            fontSize = distanceSize,
            textAlign = TextAlign.Center,
        )
    }
}

private const val WALLPAPER_ALPHA = 0.35f
