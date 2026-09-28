package com.nvllz.stepsy.ui

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.nvllz.stepsy.widget.CompactWidget
import com.nvllz.stepsy.widget.IconWidget
import com.nvllz.stepsy.widget.PlainWidget

class WidgetIconProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = IconWidget()
}

class WidgetCompactProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactWidget()
}

class WidgetPlainProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PlainWidget()
}
