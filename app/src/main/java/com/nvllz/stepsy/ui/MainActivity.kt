package com.nvllz.stepsy.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.ResultReceiver
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.nvllz.stepsy.service.MotionService
import com.nvllz.stepsy.ui.theme.StepsyTheme
import com.nvllz.stepsy.util.AppPreferences
import com.nvllz.stepsy.util.BackupScheduler
import com.nvllz.stepsy.util.GoalNotificationWorker
import com.nvllz.stepsy.util.PauseController
import com.nvllz.stepsy.util.Util

internal class MainActivity : AppCompatActivity() {
    private lateinit var tracking: TrackingState
    private val lastKnownDate = Util.todayDateString()

    override fun onCreate(savedInstanceState: Bundle?) {
        Util.applyTheme(AppPreferences.theme)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        BackupScheduler.ensureBackupScheduled(applicationContext)
        GoalNotificationWorker.createNotificationChannels(this)

        tracking = TrackingState(
            steps = if (AppPreferences.date == Util.todayDateString()) AppPreferences.steps else 0,
            paused = PauseController.isPaused(this),
            showOnboarding = AppPreferences.needsOnboarding(),
        )

        val actions = MainActions(
            onTogglePause = {
                if (tracking.paused) PauseController.resume(this) else PauseController.pause(this)
                tracking.paused = !tracking.paused
            },
            onPauseFor = { minutes, endTime ->
                PauseController.pauseFor(this, minutes, endTime)
                tracking.paused = true
            },
            onPauseIndefinitely = {
                PauseController.pauseIndefinitely(this)
                tracking.paused = true
            },
            onUpdateSteps = { newSteps ->
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, MotionService::class.java).apply {
                        putExtra("MANUAL_STEP_COUNT_CHANGE", true)
                        putExtra(MotionService.KEY_STEPS, newSteps)
                    }
                )
            },
            onOnboardingDone = {
                tracking.showOnboarding = false
                tracking.refreshKey++
                checkPermissions()
            },
        )

        setContent {
            StepsyTheme {
                CompositionLocalProvider(LocalTracking provides tracking, LocalMainActions provides actions) {
                    Navigator(HomeScreen) { SlideTransition(it) }
                }
            }
        }

        if (!tracking.showOnboarding) checkPermissions()
    }

    override fun onResume() {
        super.onResume()

        if (Util.todayDateString() != lastKnownDate) {
            recreate()
            return
        }

        tracking.refreshKey++
        if (isActivityPermissionGranted()) startTracking()
    }

    private fun startTracking() {
        startService(
            Intent(this, MotionService::class.java).apply {
                action = MotionService.ACTION_SUBSCRIBE
                putExtra(RECEIVER_TAG, object : ResultReceiver(Handler(Looper.getMainLooper())) {
                    override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
                        if (resultCode == 0) {
                            tracking.paused = resultData.getBoolean(MotionService.KEY_IS_PAUSED, false)
                            tracking.steps = resultData.getInt(MotionService.KEY_STEPS)
                        }
                    }
                })
            }
        )
        startService(Intent(this, MotionService::class.java).putExtra("FORCE_UPDATE", true))
    }

    private fun checkPermissions() {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER) &&
                !isGranted(Manifest.permission.ACTIVITY_RECOGNITION)
            ) add(Manifest.permission.ACTIVITY_RECOGNITION)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isGranted(Manifest.permission.POST_NOTIFICATIONS)) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !isGranted(Manifest.permission.FOREGROUND_SERVICE_HEALTH)) {
                add(Manifest.permission.FOREGROUND_SERVICE_HEALTH)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST)
        }
        requestIgnoreBatteryOptimization()
    }

    private fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun requestIgnoreBatteryOptimization() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:$packageName".toUri())
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST && isActivityPermissionGranted()) startTracking()
    }

    private fun isActivityPermissionGranted() =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER) &&
            isGranted(Manifest.permission.ACTIVITY_RECOGNITION)

    companion object {
        const val RECEIVER_TAG = "RECEIVER_TAG"
        private const val PERMISSION_REQUEST = 100
    }
}
