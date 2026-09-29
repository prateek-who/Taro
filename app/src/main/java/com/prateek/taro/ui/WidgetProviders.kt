package com.prateek.taro.ui

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.prateek.taro.widget.CompactWidget
import com.prateek.taro.widget.IconWidget
import com.prateek.taro.widget.PlainWidget

class WidgetIconProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = IconWidget()
}

class WidgetCompactProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactWidget()
}

class WidgetPlainProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PlainWidget()
}
