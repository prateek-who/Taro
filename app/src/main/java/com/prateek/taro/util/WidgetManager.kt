package com.prateek.taro.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.prateek.taro.widget.StepsWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object WidgetManager {
    private val updateHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isUpdateQueued = false

    fun updateAllWidgets(context: Context, steps: Int? = null, immediate: Boolean = false) {
        val appContext = context.applicationContext
        val stepsToUse = steps ?: AppPreferences.steps

        if (immediate) {
            performWidgetUpdates(appContext, stepsToUse)
            return
        }

        synchronized(this) {
            if (!isUpdateQueued) {
                isUpdateQueued = true
                updateHandler.postDelayed({
                    performWidgetUpdates(appContext, stepsToUse)
                    isUpdateQueued = false
                }, 150)
            }
        }
    }

    private fun performWidgetUpdates(context: Context, steps: Int) {
        scope.launch { StepsWidgets.updateAll(context, steps) }
    }
}
