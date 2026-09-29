package com.prateek.taro.backup

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.prateek.taro.BuildConfig
import com.prateek.taro.R
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.AppPreferences.PreferenceKeys
import com.prateek.taro.util.Database
import com.prateek.taro.util.Util
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupIO {
    const val FILE_PREFIX = "taro_"
    const val MIME_TYPE = "application/json"
    private val LEGACY_PREFIXES = listOf(FILE_PREFIX, "stepsytoo_", "stepsy_")
    private val EXTENSIONS = listOf(".json", ".csv")

    fun fileName(now: Date = Date()): String =
        "$FILE_PREFIX${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(now)}.json"

    fun timestampOf(name: String): String? {
        val prefix = LEGACY_PREFIXES.firstOrNull { name.startsWith(it) } ?: return null
        val extension = EXTENSIONS.firstOrNull { name.endsWith(it) } ?: return null
        return name.removePrefix(prefix).removeSuffix(extension)
    }

    suspend fun export(context: Context): String {
        val prefs = AppPreferences.dataStore.data.first()
        return FullBackup.encode(
            FullBackupFile(
                app = context.getString(R.string.app_name),
                appVersion = BuildConfig.VERSION_NAME,
                exportedAt = System.currentTimeMillis(),
                data = Database.getInstance(context).snapshot(),
                preferences = FullBackup.exportPreferences(prefs),
            )
        )
    }

    suspend fun restore(context: Context, file: FullBackupFile): Int {
        Database.getInstance(context).restore(file.data)
        AppPreferences.dataStore.edit { FullBackup.importPreferences(it, file.preferences) }
        val today = Util.todayDateString()
        val todaySteps = file.data.days.firstOrNull { it.date == today }?.steps ?: 0
        AppPreferences.dataStore.edit {
            it[PreferenceKeys.STEPS] = todaySteps
            it[PreferenceKeys.DATE] = today
        }
        return todaySteps
    }
}
