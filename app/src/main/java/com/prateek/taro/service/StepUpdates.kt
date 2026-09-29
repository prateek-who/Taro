package com.prateek.taro.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class StepSnapshot(val steps: Int, val paused: Boolean)

object StepUpdates {

    fun flow(context: Context): Flow<StepSnapshot> = callbackFlow {
        val receiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
                if (resultCode == 0) {
                    trySend(
                        StepSnapshot(
                            steps = resultData.getInt(MotionService.KEY_STEPS),
                            paused = resultData.getBoolean(MotionService.KEY_IS_PAUSED, false),
                        )
                    )
                }
            }
        }
        context.startService(
            Intent(context, MotionService::class.java)
                .setAction(MotionService.ACTION_SUBSCRIBE)
                .putExtra(MotionService.EXTRA_RECEIVER, receiver)
        )
        context.startService(Intent(context, MotionService::class.java).putExtra("FORCE_UPDATE", true))
        awaitClose()
    }
}
