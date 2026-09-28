package com.nvllz.stepsy.api

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.nvllz.stepsy.service.MotionService

class StepsyControlReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val serviceIntent = Intent(context, MotionService::class.java).apply {
            action = when (intent.action) {
                StepsyApi.ACTION_PAUSE -> MotionService.ACTION_PAUSE_COUNTING
                StepsyApi.ACTION_RESUME -> MotionService.ACTION_RESUME_COUNTING
                else -> return
            }

            if (intent.hasExtra(TIMED_PAUSE)) putExtra(TIMED_PAUSE, intent.getBooleanExtra(TIMED_PAUSE, false))
            if (intent.hasExtra(END_TIME)) putExtra(END_TIME, intent.getLongExtra(END_TIME, 0L))
            if (intent.hasExtra(DURATION_MINUTES)) putExtra(DURATION_MINUTES, intent.getIntExtra(DURATION_MINUTES, 0))
        }

        ContextCompat.startForegroundService(context, serviceIntent)
    }

    private companion object {
        const val TIMED_PAUSE = "TIMED_PAUSE"
        const val END_TIME = "END_TIME"
        const val DURATION_MINUTES = "DURATION_MINUTES"
    }
}
