package com.nvllz.stepsy.ui

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.nvllz.stepsy.R
import com.nvllz.stepsy.ui.components.PrimaryButton
import com.nvllz.stepsy.ui.components.SwitchRow
import com.nvllz.stepsy.ui.components.StepsySlider
import com.nvllz.stepsy.ui.components.ToggleGroup
import com.nvllz.stepsy.ui.theme.StepsyTheme
import com.nvllz.stepsy.util.AppPreferences
import com.nvllz.stepsy.widget.StepsWidgets
import com.nvllz.stepsy.widget.WidgetColors
import com.nvllz.stepsy.widget.WidgetStyle
import com.nvllz.stepsy.widget.isDark
import com.nvllz.stepsy.widget.widgetColors
import com.nvllz.stepsy.widget.widgetPrefs
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
            StepsyTheme {
                WidgetConfigureScreen(style, appWidgetId) {
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
private fun WidgetConfigureScreen(style: WidgetStyle, appWidgetId: Int, onSave: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { widgetPrefs(context, appWidgetId) }

    var opacity by remember { mutableFloatStateOf(prefs.getInt("opacity", 100).toFloat()) }
    var textScale by remember { mutableFloatStateOf(prefs.getInt("text_scale", 100).toFloat()) }
    var useDynamicColors by remember { mutableStateOf(prefs.getBoolean("use_dynamic_colors", true)) }
    var themeMode by remember { mutableStateOf(prefs.getString("theme_mode", "system") ?: "system") }
    var inverseBackground by remember { mutableStateOf(false) }

    val colors = remember(themeMode, useDynamicColors) { context.widgetColors(isDark(themeMode), useDynamicColors) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(if (inverseBackground) colors.onSurface else colors.surface)
                .padding(36.dp),
        ) {
            WidgetPreview(style, colors, opacity / 100f, textScale / 100f)
        }

        SwitchRow(
            text = stringResource(R.string.widget_pref_invert_bg),
            checked = inverseBackground,
            onCheckedChange = { inverseBackground = it },
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp),
        )

        HorizontalDivider(
            color = StepsyTheme.colors.accent,
            modifier = Modifier.padding(horizontal = 30.dp, vertical = 30.dp),
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            SwitchRow(
                text = stringResource(R.string.widget_pref_dynamic_colors),
                checked = useDynamicColors,
                onCheckedChange = {
                    useDynamicColors = it
                    prefs.edit { putBoolean("use_dynamic_colors", it) }
                },
            )

            Spacer(Modifier.height(30.dp))

            Label(R.string.theme)
            ToggleGroup(
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
            )

            Spacer(Modifier.height(30.dp))

            Label(R.string.widget_pref_text_scale)
            StepsySlider(
                value = textScale,
                onValueChange = {
                    textScale = it
                    prefs.edit { putInt("text_scale", it.toInt()) }
                },
                valueRange = 75f..150f,
                steps = 14,
            )

            Spacer(Modifier.height(30.dp))

            Label(R.string.widget_pref_bg_opacity)
            StepsySlider(
                value = opacity,
                onValueChange = { opacity = it },
                valueRange = 0f..100f,
            )

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
                    .align(Alignment.End)
                    .padding(top = 54.dp, bottom = 20.dp),
            )
        }
    }
}

@Composable
private fun Label(textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun WidgetPreview(style: WidgetStyle, colors: WidgetColors, opacity: Float, scale: Float) {
    Box(
        modifier = Modifier
            .background(
                color = colors.background.copy(alpha = colors.background.alpha * opacity),
                shape = RoundedCornerShape(dimensionResource(R.dimen.widgetRoundness)),
            )
            .padding(16.dp),
    ) {
        when (style) {
            WidgetStyle.ICON -> Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.widget_icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colors.primary),
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(width = 30.dp, height = 40.dp),
                )
                StepsAndDistance(colors, 24f * scale, 14f * scale)
            }
            WidgetStyle.COMPACT -> StepsAndDistance(colors, 18f * scale, 11f * scale)
            WidgetStyle.PLAIN -> Text(
                text = "12345 steps",
                color = colors.primary,
                fontSize = (22f * scale).sp,
            )
        }
    }
}

@Composable
private fun StepsAndDistance(colors: WidgetColors, stepsSize: Float, distanceSize: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "12345",
            color = colors.primary,
            fontSize = stepsSize.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "12.34 km",
            color = colors.secondary,
            fontSize = distanceSize.sp,
            textAlign = TextAlign.Center,
        )
    }
}
