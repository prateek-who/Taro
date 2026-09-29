package com.prateek.taro.sleep

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.SleepSegmentEvent
import com.google.android.gms.location.SleepSegmentRequest

object SleepDetector {
    const val USES_GOOGLE = true
    private const val TAG = "SleepDetector"
    private const val REQUEST_CODE = 7314

    @SuppressLint("MissingPermission")
    fun start(context: Context) {
        val available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        if (!available || !granted) return

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, SleepSegmentReceiver::class.java),
            flags,
        )
        ActivityRecognition.getClient(context)
            .requestSleepSegmentUpdates(pendingIntent, SleepSegmentRequest(SleepSegmentRequest.SEGMENT_EVENTS_ONLY))
            .addOnFailureListener { Log.w(TAG, "Sleep API unavailable", it) }
    }
}

class SleepSegmentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!SleepSegmentEvent.hasEvents(intent)) return
        SleepSegmentEvent.extractEvents(intent)
            .filter { it.status == SleepSegmentEvent.STATUS_SUCCESSFUL }
            .forEach { SleepRepository.saveDetected(context, it.startTimeMillis, it.endTimeMillis, SleepRepository.SOURCE_GOOGLE) }
    }
}
