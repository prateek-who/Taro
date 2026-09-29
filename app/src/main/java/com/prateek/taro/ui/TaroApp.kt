package com.prateek.taro.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.res.stringResource
import androidx.core.util.Consumer
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.prateek.taro.R
import com.prateek.taro.energy.WeightJournal
import com.prateek.taro.service.MotionService
import com.prateek.taro.sleep.SleepDetector
import com.prateek.taro.sleep.SleepRepository
import com.prateek.taro.service.StepSnapshot
import com.prateek.taro.service.StepUpdates
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.ToastHost
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.ui.components.rememberToastState
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.PauseController
import com.prateek.taro.util.Util
import com.prateek.taro.util.WeightReminderScheduler
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
fun TaroApp() {
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

    LaunchedEffect(canTrack) { if (canTrack) SleepDetector.start(context) }

    LifecycleResumeEffect(Unit) {
        if (Util.todayDateString() != lastKnownDate) {
            activity?.recreate()
        } else {
            SleepRepository.estimateLastNight(context)
            refreshKey++
        }
        onPauseOrDispose {}
    }

    val paused = pendingPause ?: live.paused
    val toast = rememberToastState()
    val actions = remember {
        MainActions(
            onTogglePause = {
                val nowPaused = pendingPause ?: live.paused
                val message = if (nowPaused) PauseController.resume(context) else PauseController.pause(context)
                toast.show(message, ToastKind.INFO)
                pendingPause = !nowPaused
            },
            onPauseFor = { minutes, endTime ->
                toast.show(PauseController.pauseFor(context, minutes, endTime), ToastKind.INFO)
                pendingPause = true
            },
            onPauseIndefinitely = {
                toast.show(PauseController.pauseIndefinitely(context), ToastKind.INFO)
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

    var weightPrompt by rememberSaveable {
        mutableStateOf(activity?.intent?.getBooleanExtra(WeightReminderScheduler.EXTRA_LOG_WEIGHT, false) == true)
    }
    activity?.intent?.removeExtra(WeightReminderScheduler.EXTRA_LOG_WEIGHT)

    LaunchedEffect(Unit) { WeightReminderScheduler.schedule(context, replace = false) }

    DisposableEffect(activity) {
        val listener = Consumer<Intent> { intent ->
            if (intent.getBooleanExtra(WeightReminderScheduler.EXTRA_LOG_WEIGHT, false)) weightPrompt = true
        }
        (activity as? ComponentActivity)?.addOnNewIntentListener(listener)
        onDispose { (activity as? ComponentActivity)?.removeOnNewIntentListener(listener) }
    }

    CompositionLocalProvider(LocalTracking provides tracking, LocalMainActions provides actions, LocalToast provides toast) {
        Box(modifier = Modifier.fillMaxSize()) {
            Navigator(RootScreen) { SlideTransition(it) }
            if (weightPrompt) {
                LogWeightDialog(
                    title = stringResource(R.string.weight_dialog_title),
                    initialKg = AppPreferences.weight,
                    onSave = { kg, date ->
                        WeightJournal.log(context, date.toString(), kg)
                        WeightReminderScheduler.dismissNotification(context)
                        weightPrompt = false
                        refreshKey++
                        toast.show(context.getString(R.string.weight_saved), ToastKind.SUCCESS)
                    },
                    onDismiss = { weightPrompt = false },
                )
            }
            ToastHost(
                state = toast,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            )
        }
    }
}
