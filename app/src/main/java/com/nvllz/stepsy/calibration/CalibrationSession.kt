package com.nvllz.stepsy.calibration

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SessionState(
    val running: Boolean = false,
    val elapsedS: Long = 0,
    val steps: Int = 0,
    val gpsDistanceM: Double = 0.0,
    val gpsAccuracyM: Float? = null,
    val windows: List<CalibrationWindow> = emptyList(),
    val gpsFixes: Int = 0,
    val usableFixes: Int = 0,
    val cadence: Int? = null,
)

class CalibrationSession(context: Context) {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private var recorder = CalibrationRecorder()
    private var lastCounterValue = -1
    private var startedAt = 0L
    private var lastAccuracy: Float? = null
    private var gpsFixes = 0
    private var usableFixes = 0
    private var previousFix: Location? = null
    private var previousFixTime = 0L

    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val value = event.values[0].toInt()
            if (lastCounterValue >= 0 && value > lastCounterValue) {
                recorder.addStep(SystemClock.elapsedRealtime(), value - lastCounterValue)
            }
            lastCounterValue = value
            publish()
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    private val locationListener = LocationListener { location -> onLocation(location) }

    val hasStepSensor: Boolean
        get() = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    fun hasPreciseLocation(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun isGpsEnabled(): Boolean = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

    @SuppressLint("MissingPermission")
    fun start(useGps: Boolean) {
        stop()
        recorder = CalibrationRecorder()
        lastCounterValue = -1
        lastAccuracy = null
        gpsFixes = 0
        usableFixes = 0
        previousFix = null
        startedAt = SystemClock.elapsedRealtime()

        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let {
            sensorManager.registerListener(stepListener, it, SensorManager.SENSOR_DELAY_FASTEST, 0)
        }
        if (useGps && hasPreciseLocation()) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000L, 0f, locationListener, Looper.getMainLooper())
        }
        _state.value = SessionState(running = true)
    }

    fun stop() {
        sensorManager.unregisterListener(stepListener)
        locationManager.removeUpdates(locationListener)
        _state.value = _state.value.copy(running = false)
    }

    fun tick() {
        if (_state.value.running) publish()
    }

    private fun onLocation(location: Location) {
        val now = SystemClock.elapsedRealtime()
        gpsFixes++
        lastAccuracy = location.accuracy

        val speed = when {
            location.accuracy > MAX_ACCURACY_M -> null
            location.hasSpeed() -> location.speed.toDouble().takeIf { speedAccuracyOk(location) }
            else -> derivedSpeed(location, now)
        }
        if (speed != null) {
            usableFixes++
            recorder.addSpeed(now, speed)
        }

        previousFix = location.takeIf { it.accuracy <= MAX_ACCURACY_M }
        previousFixTime = now
        publish()
    }

    private fun speedAccuracyOk(location: Location) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !location.hasSpeedAccuracy() ||
            location.speedAccuracyMetersPerSecond <= MAX_SPEED_ERROR_MPS

    private fun derivedSpeed(location: Location, now: Long): Double? {
        val previous = previousFix ?: return null
        val seconds = (now - previousFixTime) / 1000.0
        if (seconds <= 0 || seconds > MAX_FIX_GAP_S) return null
        return previous.distanceTo(location) / seconds
    }

    private fun publish() {
        _state.value = SessionState(
            running = _state.value.running,
            elapsedS = (SystemClock.elapsedRealtime() - startedAt) / 1000,
            steps = recorder.totalSteps,
            gpsDistanceM = recorder.gpsDistanceM,
            gpsAccuracyM = lastAccuracy,
            windows = recorder.windows(),
            gpsFixes = gpsFixes,
            usableFixes = usableFixes,
            cadence = recorder.recentCadence(SystemClock.elapsedRealtime()),
        )
    }

    private companion object {
        const val MAX_ACCURACY_M = 25f
        const val MAX_SPEED_ERROR_MPS = 1.5f
        const val MAX_FIX_GAP_S = 5.0
    }
}
