package com.prateek.taro.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Database

@Composable
fun rememberDataVersion(): Int {
    val data by Database.changes.collectAsStateWithLifecycle()
    val settings by remember { AppPreferences.settingsVersionFlow() }.collectAsStateWithLifecycle(0)
    return data * 31 + settings
}
