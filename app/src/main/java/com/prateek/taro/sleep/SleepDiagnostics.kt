package com.prateek.taro.sleep

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.prateek.taro.util.AppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

data class SleepDiagnosticsState(
    val registeredAt: Long?,
    val registerError: String?,
    val googleAt: Long?,
    val googleSummary: String?,
    val estimateAt: Long?,
    val estimateSummary: String?,
)

object SleepDiagnostics {
    private val REGISTERED_AT = longPreferencesKey("sleep_api_registered_at")
    private val REGISTER_ERROR = stringPreferencesKey("sleep_api_error")
    private val GOOGLE_AT = longPreferencesKey("sleep_api_last_event_at")
    private val GOOGLE_SUMMARY = stringPreferencesKey("sleep_api_last_event")
    private val ESTIMATE_AT = longPreferencesKey("sleep_estimate_last_run_at")
    private val ESTIMATE_SUMMARY = stringPreferencesKey("sleep_estimate_last_result")

    fun flow(): Flow<SleepDiagnosticsState> = AppPreferences.dataStore.data.map { prefs ->
        SleepDiagnosticsState(
            registeredAt = prefs[REGISTERED_AT],
            registerError = prefs[REGISTER_ERROR],
            googleAt = prefs[GOOGLE_AT],
            googleSummary = prefs[GOOGLE_SUMMARY],
            estimateAt = prefs[ESTIMATE_AT],
            estimateSummary = prefs[ESTIMATE_SUMMARY],
        )
    }

    fun registered() = runBlocking {
        AppPreferences.dataStore.edit {
            it[REGISTERED_AT] = System.currentTimeMillis()
            it.remove(REGISTER_ERROR)
        }
    }

    fun registerFailed(error: String) = runBlocking {
        AppPreferences.dataStore.edit {
            it[REGISTERED_AT] = System.currentTimeMillis()
            it[REGISTER_ERROR] = error
        }
    }

    fun googleEvent(summary: String) = runBlocking {
        AppPreferences.dataStore.edit {
            it[GOOGLE_AT] = System.currentTimeMillis()
            it[GOOGLE_SUMMARY] = summary
        }
    }

    fun estimate(summary: String) = runBlocking {
        AppPreferences.dataStore.edit {
            it[ESTIMATE_AT] = System.currentTimeMillis()
            it[ESTIMATE_SUMMARY] = summary
        }
    }
}
