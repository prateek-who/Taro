package com.prateek.taro.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.BackupScheduler
import com.prateek.taro.util.GoalNotificationWorker
import com.prateek.taro.util.Util

internal class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Util.applyTheme(AppPreferences.theme)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        BackupScheduler.ensureBackupScheduled(applicationContext)
        GoalNotificationWorker.createNotificationChannels(this)

        setContent {
            TaroTheme {
                TaroApp()
            }
        }
    }
}
