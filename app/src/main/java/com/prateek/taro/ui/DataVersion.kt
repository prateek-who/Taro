package com.prateek.taro.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.produceState
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

class Loaded<T>(val value: T)

@Composable
fun <T> rememberInBackground(vararg keys: Any?, compute: () -> T): Loaded<T>? {
    val state = produceState<Loaded<T>?>(null, *keys) { value = Loaded(withContext(Dispatchers.IO) { compute() }) }
    return state.value
}

@Composable
fun <T> rememberRefreshed(vararg keys: Any?, compute: () -> T): T {
    val latest by rememberUpdatedState(compute)
    val state = remember { mutableStateOf(compute()) }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(*keys) {
        if (first[0]) {
            first[0] = false
            return@LaunchedEffect
        }
        state.value = withContext(Dispatchers.IO) { latest() }
    }
    return state.value
}
