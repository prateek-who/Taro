package com.prateek.taro.backup

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.prateek.taro.data.DataSnapshot

data class BackupPref(val key: String, val type: String, val value: String)

data class FullBackupFile(
    val format: String = FullBackup.FORMAT,
    val version: Int = FullBackup.VERSION,
    val app: String = "",
    val appVersion: String = "",
    val exportedAt: Long = 0L,
    val data: DataSnapshot = DataSnapshot(),
    val preferences: List<BackupPref> = emptyList(),
)

object FullBackup {
    const val FORMAT = "full-backup"
    const val VERSION = 1

    private val deviceOnlyKeys = setOf(
        "sensor_baseline",
        "sensor_boot_count",
        "sensor_boot_time",
        "backup_location_uri",
        "app_version_code",
        "alertdialog_last",
    )

    private val gson = GsonBuilder().serializeSpecialFloatingPointValues().create()

    fun isFullBackup(text: String): Boolean = text.trimStart().startsWith("{")

    fun encode(file: FullBackupFile): String = gson.toJson(file)

    fun decode(text: String): FullBackupFile? = try {
        gson.fromJson(text, FullBackupFile::class.java)?.takeIf { it.format == FORMAT && it.version <= VERSION }
    } catch (_: JsonParseException) {
        null
    }

    fun exportPreferences(prefs: Preferences): List<BackupPref> = prefs.asMap().mapNotNull { (key, value) ->
        if (key.name in deviceOnlyKeys) return@mapNotNull null
        when (value) {
            is Boolean -> BackupPref(key.name, "boolean", value.toString())
            is Int -> BackupPref(key.name, "int", value.toString())
            is Long -> BackupPref(key.name, "long", value.toString())
            is Float -> BackupPref(key.name, "float", value.toString())
            is Double -> BackupPref(key.name, "double", value.toString())
            is String -> BackupPref(key.name, "string", value)
            is Set<*> -> BackupPref(key.name, "stringset", value.joinToString("\n"))
            else -> null
        }
    }.sortedBy { it.key }

    fun importPreferences(target: MutablePreferences, preferences: List<BackupPref>) {
        target.asMap().keys.filter { it.name !in deviceOnlyKeys }.forEach { target.remove(it) }
        preferences.filter { it.key !in deviceOnlyKeys }.forEach { pref ->
            runCatching {
                when (pref.type) {
                    "boolean" -> target[booleanPreferencesKey(pref.key)] = pref.value.toBooleanStrict()
                    "int" -> target[intPreferencesKey(pref.key)] = pref.value.toInt()
                    "long" -> target[longPreferencesKey(pref.key)] = pref.value.toLong()
                    "float" -> target[floatPreferencesKey(pref.key)] = pref.value.toFloat()
                    "double" -> target[doublePreferencesKey(pref.key)] = pref.value.toDouble()
                    "string" -> target[stringPreferencesKey(pref.key)] = pref.value
                    "stringset" -> target[stringSetPreferencesKey(pref.key)] = pref.value.split("\n").filter { it.isNotEmpty() }.toSet()
                }
            }
        }
    }
}
