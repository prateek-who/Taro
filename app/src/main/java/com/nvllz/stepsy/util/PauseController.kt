package com.nvllz.stepsy.util

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.core.content.edit
import com.nvllz.stepsy.R
import com.nvllz.stepsy.service.MotionService
import java.util.Date

object PauseController {

    private const val STATE_PREFS = "StepsyPrefs"
    private const val STATE_UPDATE = "com.nvllz.stepsy.STATE_UPDATE"

    fun isPaused(context: Context): Boolean =
        context.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE).getBoolean(MotionService.KEY_IS_PAUSED, false)

    fun pause(context: Context): String {
        TimedPauseManager.clearPauseEndTime(context)
        send(context, paused = true) { action = MotionService.ACTION_PAUSE_COUNTING }
        return context.getString(R.string.step_counting_paused)
    }

    fun resume(context: Context): String {
        TimedPauseManager.clearPauseEndTime(context)
        send(context, paused = false) { action = MotionService.ACTION_RESUME_COUNTING }
        return context.getString(R.string.step_counting_resumed)
    }

    fun pauseIndefinitely(context: Context): String {
        send(context, paused = true) {
            action = MotionService.ACTION_PAUSE_COUNTING
            putExtra("TIMED_PAUSE", false)
        }
        return context.getString(R.string.step_counting_paused)
    }

    fun pauseFor(context: Context, durationMinutes: Int, specificEndTime: Long = 0L): String {
        val endTime = if (specificEndTime > 0L) specificEndTime
        else System.currentTimeMillis() + durationMinutes * 60_000L

        send(context, paused = true) {
            action = MotionService.ACTION_PAUSE_COUNTING
            putExtra("TIMED_PAUSE", true)
            putExtra("END_TIME", endTime)
            putExtra("DURATION_MINUTES", durationMinutes)
        }

        val formatted = DateFormat.getTimeFormat(context).format(Date(endTime))
        return context.getString(R.string.step_counting_paused_until, formatted)
    }

    private fun send(context: Context, paused: Boolean, configure: Intent.() -> Unit) {
        context.startService(Intent(context.applicationContext, MotionService::class.java).apply(configure))
        context.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE).edit {
            putBoolean(MotionService.KEY_IS_PAUSED, paused)
        }
        context.sendBroadcast(Intent(STATE_UPDATE))
    }
}
