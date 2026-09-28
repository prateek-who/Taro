package com.nvllz.stepsy.util

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import kotlin.math.abs

object BootSession {
    private const val BOOT_TIME_TOLERANCE_MS = 10_000L

    fun bootCount(context: Context): Int =
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)

    fun bootTime(): Long = System.currentTimeMillis() - SystemClock.elapsedRealtime()

    fun isSameBoot(context: Context, savedBootCount: Int, savedBootTime: Long): Boolean {
        val current = bootCount(context)
        return if (current >= 0 && savedBootCount >= 0) {
            current == savedBootCount
        } else {
            abs(bootTime() - savedBootTime) < BOOT_TIME_TOLERANCE_MS
        }
    }
}
