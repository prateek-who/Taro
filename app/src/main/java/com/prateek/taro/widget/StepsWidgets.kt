package com.prateek.taro.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.prateek.taro.R
import com.prateek.taro.ui.MainActivity
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Util

private val STEPS = intPreferencesKey("steps")
private val REVISION = longPreferencesKey("revision")

abstract class StepsWidget(private val style: WidgetStyle) : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            val state = currentState<Preferences>()
            WidgetContent(style, readWidgetSettings(context, appWidgetId), state[STEPS] ?: AppPreferences.steps)
        }
    }
}

class IconWidget : StepsWidget(WidgetStyle.ICON)
class CompactWidget : StepsWidget(WidgetStyle.COMPACT)
class PlainWidget : StepsWidget(WidgetStyle.PLAIN)

object StepsWidgets {

    private fun widgetFor(style: WidgetStyle): StepsWidget = when (style) {
        WidgetStyle.ICON -> IconWidget()
        WidgetStyle.COMPACT -> CompactWidget()
        WidgetStyle.PLAIN -> PlainWidget()
    }

    private suspend fun push(context: Context, widget: StepsWidget, id: GlanceId, steps: Int) {
        updateAppWidgetState(context, id) {
            it[STEPS] = steps
            it[REVISION] = System.nanoTime()
        }
        widget.update(context, id)
    }

    suspend fun updateAll(context: Context, steps: Int) {
        val manager = GlanceAppWidgetManager(context)
        WidgetStyle.entries.map(::widgetFor).forEach { widget ->
            manager.getGlanceIds(widget.javaClass).forEach { push(context, widget, it, steps) }
        }
    }

    suspend fun update(context: Context, style: WidgetStyle, appWidgetId: Int, steps: Int) {
        val id = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
        push(context, widgetFor(style), id, steps)
    }
}

private class Palette(val background: ColorProvider, val primary: ColorProvider, val secondary: ColorProvider)

private fun palette(context: Context, settings: WidgetSettings): Palette {
    val alpha = settings.opacity / 100f
    fun Color.faded() = copy(alpha = this.alpha * alpha)

    return if (settings.themeMode == "system") {
        val day = context.widgetColors(dark = false, settings.useDynamicColors)
        val night = context.widgetColors(dark = true, settings.useDynamicColors)
        Palette(
            background = ColorProvider(day.background.faded(), night.background.faded()),
            primary = ColorProvider(day.primary, night.primary),
            secondary = ColorProvider(day.secondary, night.secondary),
        )
    } else {
        val colors = context.widgetColors(dark = settings.themeMode == "dark", settings.useDynamicColors)
        Palette(ColorProvider(colors.background.faded()), ColorProvider(colors.primary), ColorProvider(colors.secondary))
    }
}

@Composable
private fun WidgetContent(style: WidgetStyle, settings: WidgetSettings, steps: Int) {
    val context = LocalContext.current
    val palette = palette(context, settings)
    val scale = settings.scale

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = GlanceModifier
            .fillMaxSize()
            .background(palette.background)
            .cornerRadius(R.dimen.widgetRoundness)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        when (style) {
            WidgetStyle.ICON -> {
                Image(
                    provider = ImageProvider(R.drawable.widget_icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(palette.primary),
                    modifier = GlanceModifier.size(36.dp),
                )
                Spacer(GlanceModifier.width(16.dp))
                StepsAndDistance(steps, palette, 24f * scale, 14f * scale)
            }
            WidgetStyle.COMPACT -> StepsAndDistance(steps, palette, 18f * scale, 11f * scale)
            WidgetStyle.PLAIN -> Text(
                text = context.resources.getQuantityString(R.plurals.steps_formatted, steps, steps),
                style = TextStyle(color = palette.primary, fontSize = (22f * scale).sp, textAlign = TextAlign.Center),
            )
        }
    }
}

@Composable
private fun StepsAndDistance(steps: Int, palette: Palette, stepsSize: Float, distanceSize: Float) {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = steps.toString(),
            style = TextStyle(
                color = palette.primary,
                fontSize = stepsSize.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
        )
        Text(
            text = context.getString(R.string.distance_today, Util.stepsToDistance(steps), Util.distanceUnit()),
            style = TextStyle(color = palette.secondary, fontSize = distanceSize.sp, textAlign = TextAlign.Center),
        )
    }
}
