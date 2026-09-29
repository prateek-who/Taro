package com.prateek.taro.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.ResultReceiver
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.prateek.taro.R
import com.prateek.taro.ui.MainActivity
import com.prateek.taro.util.Database
import com.prateek.taro.util.Util
import java.util.*
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.GoalNotificationWorker
import com.prateek.taro.util.MinuteRecorder
import com.prateek.taro.energy.ActivityEnergy
import com.prateek.taro.energy.ActivityTotals
import com.prateek.taro.energy.AscentTracker
import com.prateek.taro.energy.EnergyModel
import kotlin.math.roundToInt
import com.prateek.taro.util.TimedPauseManager
import com.prateek.taro.util.Util.distanceUnit
import com.prateek.taro.util.WidgetManager
import java.text.NumberFormat

internal class MotionService : Service() {
    private var mTodaysSteps: Int = 0
    private var mLastSteps = -1
    private var mCurrentDate: String = ""
    private var mCachedDailyTarget: Int = 0
    private var mCachedShowProgressbar: Boolean = false
    private var receiver: ResultReceiver? = null
    private lateinit var mListener: SensorEventListener
    private lateinit var mNotificationManager: NotificationManager
    private var isCountingPaused = false
    private var goalReachedToday = false
    private lateinit var activityRecognitionManager: ActivityRecognitionManager
    private var timedPauseHandler = Handler(Looper.getMainLooper())
    private var timedPauseRunnable: Runnable? = null

    private val pauseChannelId = "com.prateek.taro.PAUSE_CHANNEL_ID"
    private val pauseNotificationId = 3844
    private val notificationUpdateInterval: Long
        get() = if (isBatterySavingEnabled(this)) 5_000L else 2_500L
    private var lastNotificationUpdateTime: Long = 0

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        Log.d(TAG, "Creating MotionService")
        startService()

        MidnightResetReceiver.scheduleNextMidnightAlarm(this)
        checkForExistingTimedPause()

        mCurrentDate = AppPreferences.date
        mTodaysSteps = AppPreferences.steps
        mCachedDailyTarget = AppPreferences.dailyGoalTarget
        mCachedShowProgressbar = AppPreferences.dailyGoalNotificationProgressbar
        isCountingPaused = getSharedPreferences("TaroPrefs", MODE_PRIVATE).getBoolean(KEY_IS_PAUSED, false)
        goalReachedToday = AppPreferences.dailyGoalNotification
                && AppPreferences.dailyGoalTarget > 0
                && mTodaysSteps >= AppPreferences.dailyGoalTarget

        if (mCurrentDate.isEmpty()) {
            mCurrentDate = Util.todayDateString()
            AppPreferences.date = mCurrentDate
        }

        mLastSteps = if (isCountingPaused) -1 else AppPreferences.restoreSensorBaseline(this) ?: -1

        val mSensorManager = getSystemService(SENSOR_SERVICE) as? SensorManager
            ?: throw IllegalStateException("Could not get sensor service")

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER) &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED)
        ) {
            Log.d(TAG, "Using step counter sensor")
            val mStepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

            if (mStepSensor == null) {
                Toast.makeText(this, getString(R.string.no_sensor), Toast.LENGTH_LONG).show()
                stopSelf()
                return
            }

            mListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    handleEvent(event.values[0].toInt(), eventWallTime(event))
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
            }

            mSensorManager.registerListener(mListener, mStepSensor, SensorManager.SENSOR_DELAY_UI, 1000000)

            mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)?.let { barometer ->
                mSensorManager.registerListener(pressureListener, barometer, BAROMETER_PERIOD_US, BAROMETER_BATCH_US)
            }
        } else {
            Toast.makeText(this, getString(R.string.no_activity_permission), Toast.LENGTH_LONG).show()
            stopSelf()
        }

        activityRecognitionManager = ActivityRecognitionManager(this)
        activityRecognitionManager.start()

        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })
        val interactive = (getSystemService(POWER_SERVICE) as PowerManager).isInteractive
        Database.getInstance(this).recordScreen(System.currentTimeMillis(), interactive)
        Database.getInstance(this).pruneScreenEvents(System.currentTimeMillis() - SCREEN_EVENT_RETENTION_MS)
    }

    private val handler = Handler(Looper.getMainLooper())
    private val delayedWriteRunnable = Runnable {
        handleStepUpdate(delayedTrigger = true)
    }

    private fun handleEvent(value: Int, eventTimeMs: Long) {
        val previousEventTimeMs = lastEventTimeMs
        lastEventTimeMs = eventTimeMs

        if (!isCountingPaused) {
            if (mLastSteps == -1 || value < mLastSteps) {
                mLastSteps = value
                return
            }

            val delta = value - mLastSteps
            mLastSteps = value
            if (AppPreferences.vehicleFilterEnabled && activityRecognitionManager.isInVehicle) {
                return
            }
            mTodaysSteps += delta
            minuteRecorder.record(delta, previousEventTimeMs, eventTimeMs)

            val target = AppPreferences.dailyGoalTarget
            if (target > 0 && mTodaysSteps >= target && !goalReachedToday) {
                goalReachedToday = true
                GoalNotificationWorker.showDailyGoalNotification(this, target)
            }

            val encouragingNotifications = AppPreferences.encouragingNotifications
            if (encouragingNotifications && !goalReachedToday && target > 0) {
                GoalNotificationWorker.showEncouragingNotification(this, target, mTodaysSteps)
            }

            handleStepUpdate()

            handler.removeCallbacks(delayedWriteRunnable)
            handler.postDelayed(delayedWriteRunnable, dbWriteInterval)
        } else {
            mLastSteps = value
        }
    }

    private fun eventWallTime(event: SensorEvent): Long {
        val ageMs = (SystemClock.elapsedRealtimeNanos() - event.timestamp) / 1_000_000L
        val now = System.currentTimeMillis()
        return if (ageMs in 0..MAX_SENSOR_BATCH_AGE_MS) now - ageMs else now
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val screenOn = intent.action == Intent.ACTION_SCREEN_ON
            Database.getInstance(context).recordScreen(System.currentTimeMillis(), screenOn)
        }
    }

    private val minuteRecorder = MinuteRecorder()
    private val ascentTracker = AscentTracker()
    private var lastEventTimeMs: Long? = null

    private val pressureListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, event.values[0])
            ascentTracker.addAltitude(eventWallTime(event), altitude.toDouble())
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    private fun flushMinutes() {
        val database = Database.getInstance(this)
        val ascent = ascentTracker.drain()
        val steps = minuteRecorder.drain()
        (steps.keys + ascent.keys).forEach { minute ->
            val newSteps = steps[minute] ?: 0
            val stepsInMinute = newSteps + if (minute in steps) 0 else database.stepsInMinute(minute)
            val climbed = AscentTracker.credited(ascent[minute] ?: 0.0, stepsInMinute)
            if (newSteps > 0 || climbed > 0.0) {
                database.addMinuteSteps(minute, Util.millisToDateString(minute), newSteps, climbed.toFloat())
            }
        }
    }

    private var lastSharedPrefsWriteTime: Long = 0
    private var lastDbWriteTime: Long = 0
    private var lastWidgetUpdateTime: Long = 0

    private val dataStoreWriteInterval: Long
        get() = if (isBatterySavingEnabled(this)) 15_000L else 7_500L
    private val dbWriteInterval: Long
        get() = if (isBatterySavingEnabled(this)) 60_000L else 30_000L
    private val widgetsUpdateInterval: Long
        get() = if (isBatterySavingEnabled(this)) 15_000L else 7_500L

    private fun handleStepUpdate(manualStepCountChange: Boolean = false, delayedTrigger: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        val todayStr = Util.todayDateString()

        if (todayStr != mCurrentDate) {
            flushMinutes()
            Database.getInstance(this).addEntry(mCurrentDate, mTodaysSteps)

            val existingSteps = Database.getInstance(this).getSumSteps(todayStr, todayStr)
            val isNewDay = existingSteps == 0

            mTodaysSteps = existingSteps

            if (todayStr > mCurrentDate) {
                if (isNewDay) {
                    goalReachedToday = false
                    GoalNotificationWorker.resetEncouragingNotificationFlags()
                } else {
                    val target = AppPreferences.dailyGoalTarget
                    goalReachedToday = target > 0 && mTodaysSteps >= target
                }
            } else {
                val target = AppPreferences.dailyGoalTarget
                goalReachedToday = target > 0 && mTodaysSteps >= target
            }

            mCurrentDate = todayStr
            persistState()
            lastSharedPrefsWriteTime = currentTime.also { lastDbWriteTime = it }
        }

        if (currentTime - lastSharedPrefsWriteTime >= dataStoreWriteInterval && !manualStepCountChange) {
            persistState()
            lastSharedPrefsWriteTime = currentTime
        }

        if (currentTime - lastDbWriteTime >= dbWriteInterval || manualStepCountChange || delayedTrigger) {
            flushMinutes()
            Database.getInstance(this).addEntry(mCurrentDate, mTodaysSteps)
            lastDbWriteTime = currentTime
        }

        if (currentTime - lastWidgetUpdateTime >= widgetsUpdateInterval || delayedTrigger || manualStepCountChange) {
            updateAllWidgets()
            lastWidgetUpdateTime = currentTime
        }

        sendUpdate()
    }

    private var cachedEnergy: ActivityTotals? = null
    private var cachedEnergySteps = -1
    private var cachedEnergyTime = 0L

    private fun todayEnergy(): ActivityTotals {
        val now = System.currentTimeMillis()
        val cached = cachedEnergy
        if (cached != null && cachedEnergySteps == mTodaysSteps) return cached
        if (cached != null && now - cachedEnergyTime < ENERGY_REFRESH_MS) {
            val extraSteps = mTodaysSteps - cachedEnergySteps
            val body = ActivityEnergy.body()
            return ActivityTotals(
                steps = mTodaysSteps,
                activeKcal = cached.activeKcal + EnergyModel.walkingStepsKcal(body, extraSteps),
                distanceM = cached.distanceM + extraSteps.coerceAtLeast(0) * body.walkingStepM,
            )
        }
        return ActivityEnergy.day(this, mCurrentDate.ifEmpty { Util.todayDateString() }, liveTodaySteps = mTodaysSteps).also {
            cachedEnergy = it
            cachedEnergySteps = mTodaysSteps
            cachedEnergyTime = now
        }
    }

    private fun persistState() {
        AppPreferences.saveStepState(this, mTodaysSteps, mCurrentDate, mLastSteps)
    }

    private fun updateAllWidgets() {
        WidgetManager.updateAllWidgets(
            context = applicationContext,
            steps = mTodaysSteps,
            immediate = true
        )
    }

    private fun sendUpdate() {
        sendBroadcast(Intent("com.prateek.taro.STATE_UPDATE"))

        if (isCountingPaused) {
            sendPauseNotification()
            sendBundleUpdate(true)
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNotificationUpdateTime >= notificationUpdateInterval) {
            val builder = createStepsNotification(mCachedShowProgressbar, mCachedDailyTarget)
            startForeground(FOREGROUND_ID, builder.build())
            lastNotificationUpdateTime = currentTime
        }

        sendBundleUpdate(false)
    }

    private fun createStepsNotification(
        showProgressbar: Boolean = AppPreferences.dailyGoalNotificationProgressbar,
        dailyTarget: Int = AppPreferences.dailyGoalTarget
    ): NotificationCompat.Builder {

        val stepsText = Util.stepsPlural(this, mTodaysSteps)
        val energy = todayEnergy()
        val stats = getString(R.string.notification_stats, Util.metersToDistance(energy.distanceM), distanceUnit(), energy.activeKcal.roundToInt())
        val hasGoal = showProgressbar && dailyTarget > 0
        val goalMet = hasGoal && mTodaysSteps >= dailyTarget
        val progress = if (hasGoal) (mTodaysSteps.toLong() * 100 / dailyTarget).toInt() else 0

        val title = when {
            goalMet -> Util.goalMultiplier(mTodaysSteps, dailyTarget)?.let { getString(R.string.notification_title_goal_met, stepsText, it) } ?: stepsText
            hasGoal -> getString(R.string.notification_title_progress, stepsText, progress)
            else -> stepsText
        }
        val text = when {
            goalMet -> "$stats · ${getString(R.string.notification_step_goal_completed)}"
            hasGoal -> "$stats · ${getString(R.string.notification_to_go, Util.formatSteps(dailyTarget - mTodaysSteps))}"
            else -> stats
        }

        val pausePendingIntent = PendingIntent.getService(
            this, 1,
            Intent(this, MotionService::class.java).apply { action = ACTION_PAUSE_COUNTING },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationPendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, STEP_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(notificationPendingIntent)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_notification, getString(R.string.action_pause), pausePendingIntent)
            .setContentTitle(title)
            .setContentText(text)
            .apply {
                if (hasGoal) {
                    setProgress(100, progress.coerceIn(0, 100), false)
                    if (goalMet) setColor(ContextCompat.getColor(this@MotionService, R.color.colorGoal))
                }
            }
    }

    private fun sendBundleUpdate(paused: Boolean = false) {
        receiver?.let {
            val bundle = Bundle().apply {
                putInt(KEY_STEPS, mTodaysSteps)
                if (paused) putBoolean(KEY_IS_PAUSED, true)
            }
            it.send(0, bundle)
        }
    }

    private fun sendPauseNotification() {
        val pauseNotification = createPauseNotificationBuilder().build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(pauseNotificationId, pauseNotification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(pauseNotificationId, pauseNotification)
        }
    }

    fun isBatterySavingEnabled(context: Context): Boolean {
        val powerManager = context.getSystemService(POWER_SERVICE) as PowerManager
        return powerManager.isPowerSaveMode
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Received start id $startId: $intent")

        intent?.let {
            when (it.action) {
                ACTION_SUBSCRIBE -> receiver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    it.getParcelableExtra(EXTRA_RECEIVER, ResultReceiver::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    it.getParcelableExtra(EXTRA_RECEIVER)
                }
                ACTION_PAUSE_COUNTING -> {
                    isCountingPaused = true

                    val isTimedPause = it.getBooleanExtra("TIMED_PAUSE", false)
                    if (isTimedPause) {
                        val endTime = it.getLongExtra("END_TIME", 0L)
                        val durationMinutes = it.getIntExtra("DURATION_MINUTES", 0)

                        if (endTime > System.currentTimeMillis()) {
                            TimedPauseManager.setPauseEndTime(this, endTime, durationMinutes)
                            startTimedPauseMonitoring()
                        } else {
                            stopTimedPauseMonitoring()
                            TimedPauseManager.clearPauseEndTime(this)
                        }
                    } else {
                        stopTimedPauseMonitoring()
                        TimedPauseManager.clearPauseEndTime(this)
                    }
                }
                ACTION_RESUME_COUNTING -> {
                    isCountingPaused = false
                    stopTimedPauseMonitoring()
                    TimedPauseManager.clearPauseEndTime(this)
                }
                "UPDATE_NOTIFICATION" -> {
                    mCachedShowProgressbar = intent.getBooleanExtra("show_progressbar", mCachedShowProgressbar)
                    mCachedDailyTarget = intent.getIntExtra("daily_target", mCachedDailyTarget)

                    if (!isCountingPaused) {
                        val builder = createStepsNotification(mCachedShowProgressbar, mCachedDailyTarget)
                        startForeground(FOREGROUND_ID, builder.build())
                    }
                    return START_STICKY
                }
            }

            getSharedPreferences("TaroPrefs", MODE_PRIVATE).edit {
                putBoolean(KEY_IS_PAUSED, isCountingPaused)
            }

            if (it.hasExtra("FORCE_UPDATE")) {
                mTodaysSteps = it.getIntExtra(KEY_STEPS, mTodaysSteps)
                val dateExtra = it.getStringExtra(KEY_DATE)
                if (!dateExtra.isNullOrEmpty()) mCurrentDate = dateExtra
                persistState()
                handleStepUpdate()
            }

            if (it.hasExtra("MANUAL_STEP_COUNT_CHANGE")) {
                mTodaysSteps = it.getIntExtra(KEY_STEPS, mTodaysSteps)
                persistState()
                handleStepUpdate(manualStepCountChange = true)
            }

            sendUpdate()
        }

        return START_STICKY
    }

    private fun startService() {
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
            ?: throw IllegalStateException("Could not get notification service")

        createStepNotificationChannel()
        createPauseNotificationChannel()

        if (isCountingPaused) {
            val pauseNotification = createPauseNotificationBuilder().build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(pauseNotificationId, pauseNotification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
            } else {
                startForeground(pauseNotificationId, pauseNotification)
            }
        } else {
            val stepNotification = createStepsNotification(mCachedShowProgressbar, mCachedDailyTarget).build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(FOREGROUND_ID, stepNotification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
            } else {
                startForeground(FOREGROUND_ID, stepNotification)
            }
        }
    }

    private fun createPauseNotificationBuilder(): NotificationCompat.Builder {
        val resumeIntent = Intent(this, MotionService::class.java).apply {
            action = ACTION_RESUME_COUNTING
        }

        val resumePendingIntent = PendingIntent.getService(
            this, 0, resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationText = if (TimedPauseManager.isTimedPauseActive(this)) {
            TimedPauseManager.getRemainingTimeText(this) ?: getString(R.string.notification_step_counting_paused)
        } else {
            getString(R.string.notification_step_counting_paused)
        }

        return NotificationCompat.Builder(this, pauseChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .addAction(R.drawable.ic_notification, getString(R.string.action_resume), resumePendingIntent)
    }

    private fun createStepNotificationChannel() {
        if (mNotificationManager.getNotificationChannel(STEP_CHANNEL_ID) == null) {
            val stepNotificationChannel = NotificationChannel(
                STEP_CHANNEL_ID,
                getString(R.string.notification_category_steps_day),
                NotificationManager.IMPORTANCE_MIN
            )
            stepNotificationChannel.description = getString(R.string.notification_description_steps_day)
            mNotificationManager.createNotificationChannel(stepNotificationChannel)
        }
    }

    private fun createPauseNotificationChannel() {
        if (mNotificationManager.getNotificationChannel(pauseChannelId) == null) {
            val pauseNotificationChannel = NotificationChannel(
                pauseChannelId,
                getString(R.string.notification_category_counting_paused),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            pauseNotificationChannel.description = getString(R.string.notification_description_paused)
            mNotificationManager.createNotificationChannel(pauseNotificationChannel)
        }
    }

    private fun startTimedPauseMonitoring() {
        stopTimedPauseMonitoring()

        val endTime = TimedPauseManager.getPauseEndTime(this)
        val now = System.currentTimeMillis()

        if (endTime <= now) {
            resumeCountingAutomatically()
            return
        }

        val delayMillis = endTime - now
        timedPauseRunnable = Runnable { resumeCountingAutomatically() }
        timedPauseHandler.postDelayed(timedPauseRunnable!!, delayMillis)

        timedPauseHandler.postDelayed({
            if (TimedPauseManager.isTimedPauseActive(this@MotionService)) {
                Log.w(TAG, "Safety check: pause should have ended but didn't - forcing resume")
                resumeCountingAutomatically()
            }
        }, delayMillis + 30000L)
    }

    private fun resumeCountingAutomatically() {
        TimedPauseManager.clearPauseEndTime(this@MotionService)
        isCountingPaused = false

        getSharedPreferences("TaroPrefs", MODE_PRIVATE).edit {
            putBoolean(KEY_IS_PAUSED, false)
        }

        sendUpdate()
        Toast.makeText(this@MotionService, R.string.step_counting_resumed_auto, Toast.LENGTH_SHORT).show()

        stopTimedPauseMonitoring()
    }

    private fun stopTimedPauseMonitoring() {
        timedPauseRunnable?.let { runnable ->
            timedPauseHandler.removeCallbacks(runnable)
            timedPauseRunnable = null
        }
        timedPauseHandler.removeCallbacksAndMessages(null)
    }

    private fun checkForExistingTimedPause() {
        if (TimedPauseManager.isTimedPauseActive(this)) {
            isCountingPaused = true
            startTimedPauseMonitoring()
        } else if (TimedPauseManager.shouldResumeCounting(this)) {
            resumeCountingAutomatically()
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        if (::mListener.isInitialized) {
            (getSystemService(SENSOR_SERVICE) as? SensorManager)?.run {
                unregisterListener(mListener)
                unregisterListener(pressureListener)
            }
        }
        flushMinutes()
        Database.getInstance(this).addEntry(mCurrentDate, mTodaysSteps)
        persistState()
        stopTimedPauseMonitoring()
        MidnightResetReceiver.cancelMidnightAlarm(this)
        if (::activityRecognitionManager.isInitialized) activityRecognitionManager.stop()
        super.onDestroy()
    }

    companion object {
        private val TAG = MotionService::class.java.simpleName
        internal const val ACTION_SUBSCRIBE = "ACTION_SUBSCRIBE"
        internal const val EXTRA_RECEIVER = "RECEIVER_TAG"
        internal const val KEY_STEPS = "STEPS"
        internal const val KEY_DATE = "DATE"
        internal const val KEY_IS_PAUSED = "IS_PAUSED"
        internal const val ACTION_PAUSE_COUNTING = "com.prateek.taro.action.PAUSE_COUNTING"
        internal const val ACTION_RESUME_COUNTING = "com.prateek.taro.action.RESUME_COUNTING"
        private const val FOREGROUND_ID = 3843
        private const val BAROMETER_PERIOD_US = 1_000_000
        private const val ENERGY_REFRESH_MS = 15_000L
        private const val SCREEN_EVENT_RETENTION_MS = 8L * 24 * 60 * 60 * 1000
        private const val MAX_SENSOR_BATCH_AGE_MS = 10 * 60_000L
        private const val BAROMETER_BATCH_US = 30_000_000
        private const val STEP_CHANNEL_ID = "com.prateek.taro.STEP_CHANNEL_ID"
    }
}