package com.nvllz.stepsy.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.nvllz.stepsy.energy.Sex
import com.nvllz.stepsy.util.Util.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import androidx.preference.PreferenceManager
import com.nvllz.stepsy.BuildConfig
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StepCalibration(
    val walkingStepCm: Float,
    val walkingSlope: Float,
    val referenceCadence: Float,
    val runningStepCm: Float?,
    val samples: Int,
)

data class AccuracyResults(
    val stepError: Float?,
    val stepDate: String?,
    val distanceError: Float?,
    val distanceDate: String?,
)

object AppPreferences {
    object PreferenceKeys {
        val STEPS                                = intPreferencesKey("STEPS")
        val DATE                                 = stringPreferencesKey("DATE")
        val THEME                                = stringPreferencesKey("theme")
        val HEIGHT                               = stringPreferencesKey("height")
        val WEIGHT                               = stringPreferencesKey("weight")
        val STEP_LENGTH                          = floatPreferencesKey("step_length")
        val UNIT_SYSTEM                          = stringPreferencesKey("unit_system")
        val DATE_FORMAT                          = stringPreferencesKey("date_format")
        val FIRST_DAY_OF_WEEK                    = stringPreferencesKey("first_day_of_week")
        val APP_VERSION_CODE                     = intPreferencesKey("app_version_code")
        val ALERTDIALOG_LAST_VERSION_CODE        = intPreferencesKey("alertdialog_last")
        val VEHICLE_FILTER_ENABLED               = booleanPreferencesKey("vehicle_filter_enabled")
        val BACKUP_LOCATION_URI                  = stringPreferencesKey("backup_location_uri")
        val BACKUP_FREQUENCY                     = stringPreferencesKey("backup_frequency")
        val BACKUP_RETENTION_COUNT               = intPreferencesKey("backup_retention")
        val DAILY_GOAL_NOTIFICATION              = booleanPreferencesKey("daily_goal_notification")
        val DAILY_GOAL_TARGET                    = intPreferencesKey("daily_goal_target")
        val DAILY_GOAL_NOTIFICATION_PROGRESSBAR  = booleanPreferencesKey("daily_goal_notification_progressbar")
        val ENCOURAGING_NOTIFICATIONS            = booleanPreferencesKey("encouraging_notifications")
        val DAILY_GOAL_CHART_LINE                = booleanPreferencesKey("daily_goal_chart_line")
        val LEG_LENGTH                           = stringPreferencesKey("leg_length")
        val BIRTH_YEAR                           = intPreferencesKey("birth_year")
        val SEX                                  = stringPreferencesKey("sex")
        val ONBOARDING_DONE                      = booleanPreferencesKey("onboarding_done")
        val CALIBRATED_WALK_STEP_CM              = floatPreferencesKey("calibrated_walk_step_cm")
        val CALIBRATED_WALK_SLOPE                = floatPreferencesKey("calibrated_walk_slope")
        val CALIBRATED_REFERENCE_CADENCE         = floatPreferencesKey("calibrated_reference_cadence")
        val CALIBRATED_RUN_STEP_CM               = floatPreferencesKey("calibrated_run_step_cm")
        val CALIBRATION_SAMPLES                  = intPreferencesKey("calibration_samples")
        val ACCURACY_STEP_ERROR                  = floatPreferencesKey("accuracy_step_error")
        val ACCURACY_STEP_DATE                   = stringPreferencesKey("accuracy_step_date")
        val ACCURACY_DISTANCE_ERROR              = floatPreferencesKey("accuracy_distance_error")
        val ACCURACY_DISTANCE_DATE               = stringPreferencesKey("accuracy_distance_date")
        val LAST_CELEBRATION_DATE                = stringPreferencesKey("last_celebration_date")
        val SENSOR_BASELINE                      = intPreferencesKey("sensor_baseline")
        val SENSOR_BOOT_COUNT                    = intPreferencesKey("sensor_boot_count")
        val SENSOR_BOOT_TIME                     = longPreferencesKey("sensor_boot_time")
    }

    lateinit var dataStore: DataStore<Preferences>
        private set

    fun init(context: Context) {
        if (!::dataStore.isInitialized) {
            dataStore = AppDataStore.create(context)
        }
        runBlocking {
            updateAppVersion()
            stepDataStoreMigration(context)
        }
    }

    private fun updateAppVersion() {
        val currentVersionCode = BuildConfig.VERSION_CODE
        runBlocking {
            dataStore.edit { it[PreferenceKeys.APP_VERSION_CODE] = currentVersionCode }
        }
    }

    // Steps

    fun saveStepState(context: Context, steps: Int, date: String, sensorBaseline: Int) = runBlocking {
        dataStore.edit {
            it[PreferenceKeys.STEPS] = steps
            it[PreferenceKeys.DATE] = date
            it[PreferenceKeys.SENSOR_BASELINE] = sensorBaseline
            it[PreferenceKeys.SENSOR_BOOT_COUNT] = BootSession.bootCount(context)
            it[PreferenceKeys.SENSOR_BOOT_TIME] = BootSession.bootTime()
        }
    }

    fun restoreSensorBaseline(context: Context): Int? = runBlocking {
        val prefs = dataStore.data.first()
        val baseline = prefs[PreferenceKeys.SENSOR_BASELINE]?.takeIf { it >= 0 } ?: return@runBlocking null
        val sameBoot = BootSession.isSameBoot(
            context,
            prefs[PreferenceKeys.SENSOR_BOOT_COUNT] ?: -1,
            prefs[PreferenceKeys.SENSOR_BOOT_TIME] ?: 0L,
        )
        baseline.takeIf { sameBoot }
    }

    fun stepsFlow(): Flow<Int> = dataStore.data.map { it[PreferenceKeys.STEPS] ?: 0 }

    var steps: Int
        get() = runBlocking { stepsFlow().first() }
        set(value) = runBlocking { dataStore.edit { it[PreferenceKeys.STEPS] = value } }

    // Date

    fun dateFlow(): Flow<String> = dataStore.data.map { prefs ->
        val raw = prefs.asMap()
            .entries
            .firstOrNull { it.key.name == PreferenceKeys.DATE.name }
            ?.value
        when (raw) {
            is String -> raw
            is Long   -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(raw))
            else      -> ""
        }
    }

    var date: String
        get() = runBlocking { dateFlow().first() }
        set(value) = runBlocking { dataStore.edit { it[PreferenceKeys.DATE] = value } }

    // Theme

    fun themeFlow(): Flow<String> = dataStore.data.map { it[PreferenceKeys.THEME] ?: "system" }

    var theme: String
        get() = runBlocking { themeFlow().first() }
        set(value) = runBlocking { dataStore.edit { it[PreferenceKeys.THEME] = value } }

    // Height

    fun heightFlow(): Flow<Double> = dataStore.data.map { it[PreferenceKeys.HEIGHT]?.toDoubleOrNull() ?: 180.0 }

    var height: Double
        get() = runBlocking { heightFlow().first() }
        set(value) = runBlocking { dataStore.edit { it[PreferenceKeys.HEIGHT] = value.toString() } }

    // Weight

    fun weightFlow(): Flow<Double> = dataStore.data.map { it[PreferenceKeys.WEIGHT]?.toDoubleOrNull() ?: 70.0 }

    var weight: Double
        get() = runBlocking { weightFlow().first() }
        set(value) = runBlocking { dataStore.edit { it[PreferenceKeys.WEIGHT] = value.toString() } }

    // Leg length

    fun legLengthFlow(): Flow<Int?> = dataStore.data.map { it[PreferenceKeys.LEG_LENGTH]?.toIntOrNull() }

    var legLength: Int?
        get() = runBlocking { legLengthFlow().first() }
        set(value) = runBlocking {
            dataStore.edit {
                if (value == null) it.remove(PreferenceKeys.LEG_LENGTH) else it[PreferenceKeys.LEG_LENGTH] = value.toString()
            }
        }

    // Accuracy checks

    fun accuracyFlow(): Flow<AccuracyResults> = dataStore.data.map {
        AccuracyResults(
            stepError = it[PreferenceKeys.ACCURACY_STEP_ERROR],
            stepDate = it[PreferenceKeys.ACCURACY_STEP_DATE],
            distanceError = it[PreferenceKeys.ACCURACY_DISTANCE_ERROR],
            distanceDate = it[PreferenceKeys.ACCURACY_DISTANCE_DATE],
        )
    }

    fun saveStepAccuracy(error: Float, date: String) = runBlocking {
        dataStore.edit {
            it[PreferenceKeys.ACCURACY_STEP_ERROR] = error
            it[PreferenceKeys.ACCURACY_STEP_DATE] = date
        }
    }

    fun saveDistanceAccuracy(error: Float, date: String) = runBlocking {
        dataStore.edit {
            it[PreferenceKeys.ACCURACY_DISTANCE_ERROR] = error
            it[PreferenceKeys.ACCURACY_DISTANCE_DATE] = date
        }
    }

    // Celebration

    var lastCelebrationDate: String?
        get() = runBlocking { dataStore.data.first()[PreferenceKeys.LAST_CELEBRATION_DATE] }
        set(value) = runBlocking {
            dataStore.edit { if (value == null) it.remove(PreferenceKeys.LAST_CELEBRATION_DATE) else it[PreferenceKeys.LAST_CELEBRATION_DATE] = value }
        }

    // Age and sex

    fun ageFlow(): Flow<Int?> = dataStore.data.map { prefs ->
        prefs[PreferenceKeys.BIRTH_YEAR]?.let { Calendar.getInstance().get(Calendar.YEAR) - it }
    }

    var age: Int?
        get() = runBlocking { ageFlow().first() }
        set(value) = runBlocking {
            dataStore.edit {
                if (value == null) it.remove(PreferenceKeys.BIRTH_YEAR)
                else it[PreferenceKeys.BIRTH_YEAR] = Calendar.getInstance().get(Calendar.YEAR) - value
            }
        }

    fun sexFlow(): Flow<Sex?> = dataStore.data.map { Sex.fromKey(it[PreferenceKeys.SEX]) }

    var sex: Sex?
        get() = runBlocking { sexFlow().first() }
        set(value) = runBlocking {
            dataStore.edit { if (value == null) it.remove(PreferenceKeys.SEX) else it[PreferenceKeys.SEX] = value.key }
        }

    // Onboarding

    fun needsOnboarding(): Boolean = runBlocking {
        val prefs = dataStore.data.first()
        prefs[PreferenceKeys.ONBOARDING_DONE] != true &&
            prefs[PreferenceKeys.HEIGHT] == null &&
            prefs[PreferenceKeys.WEIGHT] == null
    }

    fun completeOnboarding() = runBlocking {
        dataStore.edit { it[PreferenceKeys.ONBOARDING_DONE] = true }
    }

    // Step length

    fun resetStepLength() {
        runBlocking { dataStore.edit { it.remove(PreferenceKeys.STEP_LENGTH) } }
    }

    fun stepLengthFlow(): Flow<Float> = dataStore.data.map {
        it[PreferenceKeys.STEP_LENGTH] ?: it[PreferenceKeys.CALIBRATED_WALK_STEP_CM] ?: Util.estimateStepLength(
            it[PreferenceKeys.HEIGHT]?.toDoubleOrNull() ?: 180.0,
            it[PreferenceKeys.LEG_LENGTH]?.toIntOrNull(),
        )
    }

    val manualStepLength: Float?
        get() = runBlocking { dataStore.data.first()[PreferenceKeys.STEP_LENGTH] }

    fun stepCalibrationFlow(): Flow<StepCalibration?> = dataStore.data.map { prefs ->
        prefs[PreferenceKeys.CALIBRATED_WALK_STEP_CM]?.let { walk ->
            StepCalibration(
                walkingStepCm = walk,
                walkingSlope = prefs[PreferenceKeys.CALIBRATED_WALK_SLOPE] ?: 0f,
                referenceCadence = prefs[PreferenceKeys.CALIBRATED_REFERENCE_CADENCE] ?: 100f,
                runningStepCm = prefs[PreferenceKeys.CALIBRATED_RUN_STEP_CM],
                samples = prefs[PreferenceKeys.CALIBRATION_SAMPLES] ?: 0,
            )
        }
    }

    var stepCalibration: StepCalibration?
        get() = runBlocking { stepCalibrationFlow().first() }
        set(value) = runBlocking {
            dataStore.edit { prefs ->
                val keys = listOf(
                    PreferenceKeys.CALIBRATED_WALK_STEP_CM,
                    PreferenceKeys.CALIBRATED_WALK_SLOPE,
                    PreferenceKeys.CALIBRATED_REFERENCE_CADENCE,
                    PreferenceKeys.CALIBRATED_RUN_STEP_CM,
                )
                keys.forEach { prefs.remove(it) }
                prefs.remove(PreferenceKeys.CALIBRATION_SAMPLES)
                if (value != null) {
                    prefs[PreferenceKeys.CALIBRATED_WALK_STEP_CM] = value.walkingStepCm
                    prefs[PreferenceKeys.CALIBRATED_WALK_SLOPE] = value.walkingSlope
                    prefs[PreferenceKeys.CALIBRATED_REFERENCE_CADENCE] = value.referenceCadence
                    value.runningStepCm?.let { prefs[PreferenceKeys.CALIBRATED_RUN_STEP_CM] = it }
                    prefs[PreferenceKeys.CALIBRATION_SAMPLES] = value.samples
                    prefs.remove(PreferenceKeys.STEP_LENGTH)
                }
            }
        }

    val estimatedStepLength: Float
        get() = Util.estimateStepLength(height, legLength)

    var stepLength: Float
        get() = runBlocking { stepLengthFlow().first() }
        set(value) = runBlocking { dataStore.edit { it[PreferenceKeys.STEP_LENGTH] = value } }

    // Unit system

    fun unitSystemFlow(): Flow<UnitSystem> = dataStore.data.map {
        when (it[PreferenceKeys.UNIT_SYSTEM]) {
            "imperial" -> UnitSystem.IMPERIAL
            else       -> UnitSystem.METRIC
        }
    }

    var unitSystem: UnitSystem
        get() = runBlocking { unitSystemFlow().first() }
        set(value) = runBlocking {
            dataStore.edit {
                it[PreferenceKeys.UNIT_SYSTEM] = if (value == UnitSystem.IMPERIAL) "imperial" else "metric"
            }
        }

    // Date format

    fun dateFormatStringFlow(): Flow<String> =
        dataStore.data.map { it[PreferenceKeys.DATE_FORMAT] ?: "yyyy-MM-dd" }

    var dateFormatString: String
        get() = runBlocking { dateFormatStringFlow().first() }
        set(value) = runBlocking { dataStore.edit { it[PreferenceKeys.DATE_FORMAT] = value } }

    // First day of week

    fun firstDayOfWeekFlow(): Flow<Int> = dataStore.data.map {
        it[PreferenceKeys.FIRST_DAY_OF_WEEK]?.toIntOrNull() ?: Calendar.MONDAY
    }

    var firstDayOfWeek: Int
        get() = runBlocking { firstDayOfWeekFlow().first() }
        set(value) = runBlocking { dataStore.edit { it[PreferenceKeys.FIRST_DAY_OF_WEEK] = value.toString() } }

    // Backup

    fun backupLocationUriFlow(): Flow<String?> = dataStore.data.map { it[PreferenceKeys.BACKUP_LOCATION_URI] }

    var backupLocationUri: String?
        get() = runBlocking { backupLocationUriFlow().first() }
        set(value) = runBlocking {
            dataStore.edit {
                if (value != null) it[PreferenceKeys.BACKUP_LOCATION_URI] = value
                else it.remove(PreferenceKeys.BACKUP_LOCATION_URI)
            }
        }

    fun backupFrequencyFlow(): Flow<Int> = dataStore.data.map {
        it[PreferenceKeys.BACKUP_FREQUENCY]?.toIntOrNull() ?: 0
    }

    var backupFrequency: Int
        get() = runBlocking { backupFrequencyFlow().first() }
        set(value) = runBlocking { dataStore.edit { it[PreferenceKeys.BACKUP_FREQUENCY] = value.toString() } }

    fun backupRetentionFlow(): Flow<Int> = dataStore.data.map {
        it[PreferenceKeys.BACKUP_RETENTION_COUNT]?.toInt() ?: 5
    }

    var backupRetention: Int
        get() = runBlocking { backupRetentionFlow().first() }
        set(value) = runBlocking { dataStore.edit { it[PreferenceKeys.BACKUP_RETENTION_COUNT] = value } }

    // Notifications

    fun dailyGoalNotificationFlow(): Flow<Boolean> = dataStore.data.map {
        it[PreferenceKeys.DAILY_GOAL_NOTIFICATION] == true
    }

    var dailyGoalNotification: Boolean
        get() = runBlocking { dailyGoalNotificationFlow().first() }
        set(value) = runBlocking { dataStore.edit { it[PreferenceKeys.DAILY_GOAL_NOTIFICATION] = value } }

    fun dailyGoalTargetFlow(): Flow<Int> = dataStore.data.map {
        it[PreferenceKeys.DAILY_GOAL_TARGET] ?: 10000
    }

    var dailyGoalTarget: Int
        get() = runBlocking { dailyGoalTargetFlow().first() }
        set(value) = runBlocking { dataStore.edit { it[PreferenceKeys.DAILY_GOAL_TARGET] = value } }

    fun dailyGoalNotificationProgressbarFlow(): Flow<Boolean> = dataStore.data.map {
        it[PreferenceKeys.DAILY_GOAL_NOTIFICATION_PROGRESSBAR] ?: true
    }

    var dailyGoalNotificationProgressbar: Boolean
        get() = runBlocking { dailyGoalNotificationProgressbarFlow().first() }
        set(value) = runBlocking { dataStore.edit { it[PreferenceKeys.DAILY_GOAL_NOTIFICATION_PROGRESSBAR] = value } }

    fun encouragingNotificationsFlow(): Flow<Boolean> = dataStore.data.map {
        it[PreferenceKeys.ENCOURAGING_NOTIFICATIONS] ?: true
    }

    var encouragingNotifications: Boolean
        get() = runBlocking { encouragingNotificationsFlow().first() }
        set(value) = runBlocking { dataStore.edit { it[PreferenceKeys.ENCOURAGING_NOTIFICATIONS] = value } }

    fun dailyGoalChartLineFlow(): Flow<Boolean> = dataStore.data.map {
        it[PreferenceKeys.DAILY_GOAL_CHART_LINE] ?: true
    }

    var dailyGoalChartLine: Boolean
        get() = runBlocking { dailyGoalChartLineFlow().first() }
        set(value) = runBlocking { dataStore.edit { it[PreferenceKeys.DAILY_GOAL_CHART_LINE] = value } }

    // Vehicle filter

    fun vehicleFilterEnabledFlow(): Flow<Boolean> = dataStore.data.map {
        it[PreferenceKeys.VEHICLE_FILTER_ENABLED] ?: true
    }

    var vehicleFilterEnabled: Boolean
        get() = runBlocking { vehicleFilterEnabledFlow().first() }
        set(value) = runBlocking { dataStore.edit { it[PreferenceKeys.VEHICLE_FILTER_ENABLED] = value } }

    @OptIn(DelicateCoroutinesApi::class)
    private fun stepDataStoreMigration(context: Context) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (!sharedPrefs.contains("STEPS")) return

        GlobalScope.launch(Dispatchers.IO) {
            val sharedPrefs2 = PreferenceManager.getDefaultSharedPreferences(context)
            val sharedPrefsSteps = sharedPrefs2.getInt("STEPS", 0)
            val sharedPrefsDate  = sharedPrefs2.getString("DATE", "")

            val currentDataStoreSteps = dataStore.data.map { it[PreferenceKeys.STEPS] ?: 0 }.first()

            if (sharedPrefsSteps > currentDataStoreSteps) {
                dataStore.edit { it[PreferenceKeys.STEPS] = sharedPrefsSteps }
                dataStore.edit { it[PreferenceKeys.DATE]  = sharedPrefsDate as String }
                sharedPrefs2.edit().clear().apply()
            }
        }
    }
}