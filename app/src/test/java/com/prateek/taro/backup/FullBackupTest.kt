package com.prateek.taro.backup

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.prateek.taro.data.CalibrationPoint
import com.prateek.taro.data.DailySteps
import com.prateek.taro.data.DataSnapshot
import com.prateek.taro.data.LoggedActivity
import com.prateek.taro.data.MinuteSteps
import com.prateek.taro.data.SleepSession
import com.prateek.taro.data.StepsDatabase
import com.prateek.taro.data.WeightLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FullBackupTest {

    private val snapshot = DataSnapshot(
        days = listOf(DailySteps("2026-09-28", 5363), DailySteps("2026-09-29", 1200)),
        minutes = listOf(MinuteSteps(1_790_600_000_000L, "2026-09-29", 42, 1.5f)),
        calibration = listOf(CalibrationPoint(1, 1_790_000_000_000L, 112.0, 0.74, 400.0, 540, "gps")),
        activities = listOf(LoggedActivity(3, "2026-09-28", "Calisthenics", 180.0, 45, 1_790_500_000_000L)),
        weights = listOf(WeightLog("2026-09-28", 71.4, 1_790_500_000_000L)),
        sleeps = listOf(SleepSession(7, 1_790_620_000_000L, 1_790_648_800_000L, "2026-09-29", "manual", true)),
    )

    @Test
    fun fileRoundTripsEveryTable() {
        val file = FullBackupFile(app = "Taro", appVersion = "1.0.2", exportedAt = 1L, data = snapshot)
        val decoded = FullBackup.decode(FullBackup.encode(file))
        assertEquals(file, decoded)
    }

    @Test
    fun rejectsCsvAndForeignJson() {
        assertFalse(FullBackup.isFullBackup("2026-09-28,5363"))
        assertTrue(FullBackup.isFullBackup("  {\"format\":\"x\"}"))
        assertNull(FullBackup.decode("{\"format\":\"something-else\"}"))
        assertNull(FullBackup.decode("{not json"))
    }

    @Test
    fun missingSectionsFallBackToEmpty() {
        val decoded = FullBackup.decode("{\"format\":\"full-backup\",\"version\":1}")
        assertEquals(DataSnapshot(), decoded?.data)
        assertEquals(emptyList<BackupPref>(), decoded?.preferences)
    }

    @Test
    fun preferencesRoundTripWithoutDeviceKeys() {
        val source = mutablePreferencesOf(
            stringPreferencesKey("weight") to "71.4",
            intPreferencesKey("daily_goal_target") to 5000,
            booleanPreferencesKey("vehicle_filter_enabled") to true,
            floatPreferencesKey("calibrated_walk_step_cm") to 74.5f,
            intPreferencesKey("sensor_baseline") to 123456,
            stringPreferencesKey("backup_location_uri") to "content://old",
        )
        val exported = FullBackup.exportPreferences(source)
        assertFalse(exported.any { it.key == "sensor_baseline" || it.key == "backup_location_uri" })

        val target = mutablePreferencesOf(
            intPreferencesKey("sensor_baseline") to 99,
            stringPreferencesKey("theme") to "light",
        )
        FullBackup.importPreferences(target, exported)

        assertEquals("71.4", target[stringPreferencesKey("weight")])
        assertEquals(5000, target[intPreferencesKey("daily_goal_target")])
        assertEquals(true, target[booleanPreferencesKey("vehicle_filter_enabled")])
        assertEquals(74.5f, target[floatPreferencesKey("calibrated_walk_step_cm")])
        assertEquals(99, target[intPreferencesKey("sensor_baseline")])
        assertNull(target[stringPreferencesKey("theme")])
    }

    @Test
    fun restoreReplacesEveryTable() {
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("restore.db")
        val db = StepsDatabase.build(context, "restore.db")
        val dao = db.steps()
        dao.insertDays(listOf(DailySteps("2020-01-01", 1)))
        dao.upsertWeight(WeightLog("2020-01-01", 90.0, 0L))

        dao.restoreAll(snapshot)

        assertEquals(snapshot.days, dao.allDays())
        assertEquals(snapshot.minutes, dao.allMinutes())
        assertEquals(snapshot.calibration, dao.allCalibration())
        assertEquals(snapshot.activities, dao.allActivities())
        assertEquals(snapshot.weights, dao.allWeights())
        assertEquals(snapshot.sleeps, dao.allSleeps())
        db.close()
    }

    @Test
    fun backupFileNamesAreRecognised() {
        assertEquals("20261001-080000", BackupIO.timestampOf("taro_20261001-080000.json"))
        assertEquals("20260929-101500", BackupIO.timestampOf("stepsytoo_20260929-101500.json"))
        assertEquals("20250101", BackupIO.timestampOf("stepsy_20250101.csv"))
        assertNull(BackupIO.timestampOf("photo.jpg"))
    }
}
