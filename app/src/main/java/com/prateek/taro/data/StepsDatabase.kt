package com.prateek.taro.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.prateek.taro.util.Database as LegacyDatabase

@Database(entities = [DailySteps::class, MinuteSteps::class, CalibrationPoint::class, LoggedActivity::class, WeightLog::class, SleepSession::class, ScreenEvent::class], version = 7, exportSchema = true)
abstract class StepsDatabase : RoomDatabase() {

    abstract fun steps(): StepsDao

    companion object {
        private const val TAG = "StepsDatabase"
        private const val NAME = "taro"

        @Volatile
        private var instance: StepsDatabase? = null

        fun get(context: Context): StepsDatabase = instance ?: synchronized(this) {
            instance ?: build(context, NAME).also { instance = it }
        }

        internal fun build(context: Context, name: String): StepsDatabase =
            Room.databaseBuilder(context.applicationContext, StepsDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .allowMainThreadQueries()
                .build()

        private fun SupportSQLiteDatabase.hasTable(name: String) =
            query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(name)).use { it.moveToFirst() }

        private fun SupportSQLiteDatabase.columns(table: String): Set<String> =
            query("PRAGMA table_info($table)").use { cursor ->
                buildSet { while (cursor.moveToNext()) add(cursor.getString(1)) }
            }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS History (date TEXT PRIMARY KEY, stepsy INT NOT NULL)")
                if ("timestamp" !in db.columns("History")) return

                val totals = sortedMapOf<String, Int>()
                var skipped = 0
                db.query("SELECT timestamp, stepsy FROM History").use { cursor ->
                    while (cursor.moveToNext()) {
                        val timestamp = cursor.getLong(0)
                        val steps = cursor.getInt(1)
                        if (timestamp <= 0 || steps < 0) {
                            skipped++
                            continue
                        }
                        val date = LegacyDatabase.snapTimestampToDate(timestamp)
                        totals[date] = (totals[date] ?: 0) + steps
                    }
                }

                db.execSQL("CREATE TABLE History_v2 (date TEXT PRIMARY KEY, stepsy INT NOT NULL)")
                totals.forEach { (date, steps) ->
                    db.insert(
                        "History_v2",
                        SQLiteDatabase.CONFLICT_REPLACE,
                        ContentValues().apply {
                            put("date", date)
                            put("stepsy", steps)
                        },
                    )
                }
                db.execSQL("DROP TABLE History")
                db.execSQL("ALTER TABLE History_v2 RENAME TO History")
                Log.i(TAG, "Migrated ${totals.size} days from timestamps, skipped $skipped rows")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!db.hasTable("History")) {
                    db.execSQL("CREATE TABLE History (date TEXT PRIMARY KEY, stepsy INT NOT NULL)")
                }
                db.execSQL("CREATE TABLE History_v3 (date TEXT NOT NULL, stepsy INTEGER NOT NULL, PRIMARY KEY(date))")
                db.execSQL("INSERT OR REPLACE INTO History_v3 (date, stepsy) SELECT date, stepsy FROM History WHERE date IS NOT NULL")
                db.execSQL("DROP TABLE History")
                db.execSQL("ALTER TABLE History_v3 RENAME TO History")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS minute_steps (" +
                        "minute_start INTEGER NOT NULL, date TEXT NOT NULL, steps INTEGER NOT NULL, " +
                        "ascent_m REAL NOT NULL DEFAULT 0, PRIMARY KEY(minute_start))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_minute_steps_date ON minute_steps (date)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS calibration_points (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, recorded_at INTEGER NOT NULL, " +
                        "cadence REAL NOT NULL, step_length_m REAL NOT NULL, distance_m REAL NOT NULL, " +
                        "steps INTEGER NOT NULL, source TEXT NOT NULL)"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS logged_activities (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date TEXT NOT NULL, name TEXT NOT NULL, " +
                        "kcal REAL NOT NULL, duration_minutes INTEGER, logged_at INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_logged_activities_date ON logged_activities (date)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS weight_entries (" +
                        "date TEXT NOT NULL, kg REAL NOT NULL, logged_at INTEGER NOT NULL, PRIMARY KEY(date))"
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS sleep_sessions (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, start_at INTEGER NOT NULL, end_at INTEGER NOT NULL, " +
                        "wake_date TEXT NOT NULL, source TEXT NOT NULL, confirmed INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sleep_sessions_wake_date ON sleep_sessions (wake_date)")
                db.execSQL("CREATE TABLE IF NOT EXISTS screen_events (at INTEGER NOT NULL, screen_on INTEGER NOT NULL, PRIMARY KEY(at))")
            }
        }
    }
}
