package com.nvllz.stepsy.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.nvllz.stepsy.service.MotionService
import com.nvllz.stepsy.service.StepSnapshot
import com.nvllz.stepsy.service.StepUpdates
import com.nvllz.stepsy.ui.components.LocalToast
import com.nvllz.stepsy.ui.components.ToastHost
import com.nvllz.stepsy.ui.components.rememberToastState
import com.nvllz.stepsy.util.AppPreferences
import com.nvllz.stepsy.util.PauseController
import com.nvllz.stepsy.util.Util
import kotlinx.coroutines.flow.flowOf

private fun Context.isGranted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.canTrackSteps() =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER) &&
        isGranted(Manifest.permission.ACTIVITY_RECOGNITION)

private fun Context.missingPermissions(): Array<String> = buildList {
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
}.toTypedArray()

private fun Context.requestBatteryExemption() {
    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
        startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:$packageName".toUri()))
    }
}

@Composable
fun StepsyApp() {
    val context = LocalContext.current
    val activity = LocalActivity.current

    var showOnboarding by rememberSaveable { mutableStateOf(AppPreferences.needsOnboarding()) }
    var canTrack by remember { mutableStateOf(context.canTrackSteps()) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var pendingPause by remember { mutableStateOf<Boolean?>(null) }
    val lastKnownDate = remember { Util.todayDateString() }

    val initial = remember {
        StepSnapshot(
            steps = if (AppPreferences.date == lastKnownDate) AppPreferences.steps else 0,
            paused = PauseController.isPaused(context),
        )
    }
    val stepFlow = remember(canTrack) { if (canTrack) StepUpdates.flow(context) else flowOf(initial) }
    val live by stepFlow.collectAsStateWithLifecycle(initial)
    LaunchedEffect(live) { pendingPause = null }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        canTrack = context.canTrackSteps()
    }

    LaunchedEffect(showOnboarding) {
        if (!showOnboarding) {
            context.missingPermissions().takeIf { it.isNotEmpty() }?.let(permissionLauncher::launch)
            context.requestBatteryExemption()
        }
    }

    LifecycleResumeEffect(Unit) {
        if (Util.todayDateString() != lastKnownDate) activity?.recreate() else refreshKey++
        onPauseOrDispose {}
    }

    val paused = pendingPause ?: live.paused
    val actions = remember {
        MainActions(
            onTogglePause = {
                val nowPaused = pendingPause ?: live.paused
                if (nowPaused) PauseController.resume(context) else PauseController.pause(context)
                pendingPause = !nowPaused
            },
            onPauseFor = { minutes, endTime ->
                PauseController.pauseFor(context, minutes, endTime)
                pendingPause = true
            },
            onPauseIndefinitely = {
                PauseController.pauseIndefinitely(context)
                pendingPause = true
            },
            onUpdateSteps = { newSteps ->
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, MotionService::class.java)
                        .putExtra("MANUAL_STEP_COUNT_CHANGE", true)
                        .putExtra(MotionService.KEY_STEPS, newSteps),
                )
            },
            onOnboardingDone = {
                showOnboarding = false
                refreshKey++
            },
        )
    }

    val tracking = TrackingState(
        steps = live.steps,
        paused = paused,
        refreshKey = refreshKey,
        showOnboarding = showOnboarding,
    )

    val toast = rememberToastState()

    CompositionLocalProvider(LocalTracking provides tracking, LocalMainActions provides actions, LocalToast provides toast) {
        Box(modifier = Modifier.fillMaxSize()) {
            Navigator(RootScreen) { SlideTransition(it) }
            ToastHost(
                state = toast,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            )
        }
    }
}
