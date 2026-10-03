package com.prateek.taro.ui

import com.prateek.taro.backup.CsvExport
import com.prateek.taro.backup.BackupIO
import com.prateek.taro.backup.FullBackup
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.text.format.DateFormat
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prateek.taro.R
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.service.MotionService
import com.prateek.taro.service.MotionService.Companion.KEY_DATE
import com.prateek.taro.service.MotionService.Companion.KEY_STEPS
import com.prateek.taro.ui.components.MessageDialog
import com.prateek.taro.ui.components.NumberInputDialog
import com.prateek.taro.ui.components.PreferenceDivider
import com.prateek.taro.ui.components.PreferenceRow
import com.prateek.taro.ui.components.ScrollingColumn
import com.prateek.taro.ui.components.SectionHeader
import com.prateek.taro.ui.components.SettingsCard
import com.prateek.taro.ui.components.SingleChoiceDialog
import com.prateek.taro.ui.components.TaroScaffold
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.AppPreferences.PreferenceKeys
import com.prateek.taro.util.BackupScheduler
import com.prateek.taro.util.Database
import com.prateek.taro.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "BackupActivity"

object BackupScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        BackupContent(onBack = { navigator.pop() })
    }
}

private sealed interface BackupDialog {
    data object Frequency : BackupDialog
    data object Retention : BackupDialog
    data class ImportWarning(val uri: Uri) : BackupDialog
}

@Composable
private fun BackupContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toast = LocalToast.current

    val frequency by AppPreferences.backupFrequencyFlow().collectAsStateWithLifecycle(AppPreferences.backupFrequency)
    val retention by AppPreferences.backupRetentionFlow().collectAsStateWithLifecycle(AppPreferences.backupRetention)
    val locationUri by AppPreferences.backupLocationUriFlow().collectAsStateWithLifecycle(AppPreferences.backupLocationUri)

    var dialog by remember { mutableStateOf<BackupDialog?>(null) }
    var nextBackup by remember { mutableStateOf<String?>(null) }

    val frequencyEntries = stringArrayResource(R.array.backup_frequency_entries)
    val frequencyValues = stringArrayResource(R.array.backup_frequency_values)
    val locationSet = locationUri != null

    fun showMessage(textRes: Int, kind: ToastKind = ToastKind.ERROR) {
        toast.show(context.getString(textRes), kind)
    }

    LifecycleResumeEffect(frequency, locationUri) {
        nextBackup = if (frequency > 0 && locationSet) {
            val time = BackupScheduler.getNextBackupTime(context)
            val date = SimpleDateFormat(AppPreferences.dateFormatString, Locale.getDefault()).format(Date(time))
            context.getString(R.string.next_backup_scheduled, date, DateFormat.getTimeFormat(context).format(time))
        } else {
            null
        }
        onPauseOrDispose {}
    }

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            scope.launch {
                saveBackupLocation(context, uri)
                BackupScheduler.ensureBackupScheduled(context)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            scope.launch {
                val valid = withContext(Dispatchers.IO) { isImportFileValid(context, uri) }
                if (valid) dialog = BackupDialog.ImportWarning(uri) else showMessage(R.string.import_invalid_file)
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            scope.launch {
                val success = withContext(Dispatchers.IO) { exportToUri(context, uri) }
                if (success) showMessage(R.string.manual_backup_successful, ToastKind.SUCCESS) else showMessage(R.string.cannot_open_file)
            }
        }
    }

    val foodCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            val success = withContext(Dispatchers.IO) { writeText(context, uri) { CsvExport.food(it) } }
            if (success) showMessage(R.string.csv_saved, ToastKind.SUCCESS) else showMessage(R.string.cannot_open_file)
        }
    }
    val weightCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            val success = withContext(Dispatchers.IO) { writeText(context, uri) { CsvExport.weight(it) } }
            if (success) showMessage(R.string.csv_saved, ToastKind.SUCCESS) else showMessage(R.string.cannot_open_file)
        }
    }

    TaroScaffold(
        title = stringResource(R.string.header_data_backup),
        onBack = onBack,
    ) { padding ->
        ScrollingColumn(padding) {
            SectionHeader(stringResource(R.string.import_data))
            SettingsCard {
                PreferenceRow(
                    icon = R.drawable.ic_import,
                    title = stringResource(R.string.import_data),
                    summary = stringResource(R.string.import_data_summary),
                    onClick = {
                        importLauncher.launch(
                            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "*/*"
                                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/*", "application/octet-stream"))
                            }
                        )
                    },
                )
            }

            SectionHeader(stringResource(R.string.export_data), topPadding = 8.dp)
            SettingsCard {
                PreferenceRow(
                    icon = R.drawable.ic_frequency,
                    title = stringResource(R.string.backup_frequency),
                    summary = frequencyEntries.getOrNull(frequencyValues.indexOf(frequency.toString())) ?: frequency.toString(),
                    enabled = locationSet,
                    onClick = { dialog = BackupDialog.Frequency },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = R.drawable.ic_retention,
                    title = stringResource(R.string.backup_retention_count),
                    summary = if (retention == 0) stringResource(R.string.backup_retention_unlimited) else retention.toString(),
                    enabled = frequency > 0,
                    onClick = { dialog = BackupDialog.Retention },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = R.drawable.ic_location,
                    title = stringResource(R.string.backup_location),
                    summary = locationUri?.let { displayPath(it.toUri()) } ?: stringResource(R.string.backup_location_not_set),
                    onClick = {
                        locationLauncher.launch(
                            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                            }
                        )
                    },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = R.drawable.ic_export,
                    title = stringResource(R.string.manual_backup),
                    onClick = { exportLauncher.launch(manualExportIntent()) },
                )
            }

            SectionHeader(stringResource(R.string.csv_export), topPadding = 8.dp)
            SettingsCard {
                PreferenceRow(
                    icon = R.drawable.ic_calorie_goal,
                    title = stringResource(R.string.csv_food),
                    summary = stringResource(R.string.csv_food_summary),
                    onClick = { foodCsvLauncher.launch("taro_food_${Util.todayDateString()}.csv") },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = R.drawable.ic_weight,
                    title = stringResource(R.string.csv_weight),
                    summary = stringResource(R.string.csv_weight_summary),
                    onClick = { weightCsvLauncher.launch("taro_weight_${Util.todayDateString()}.csv") },
                )
            }

            nextBackup?.let {
                Text(
                    text = it,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .alpha(0.5f),
                )
            }
        }
    }

    when (val current = dialog) {
        BackupDialog.Frequency -> SingleChoiceDialog(
            title = stringResource(R.string.backup_frequency),
            entries = frequencyEntries.toList(),
            selectedIndex = frequencyValues.indexOf(frequency.toString()).coerceAtLeast(0),
            onSelect = { index ->
                dialog = null
                val newFrequency = frequencyValues[index].toInt()
                scope.launch {
                    AppPreferences.dataStore.edit { it[PreferenceKeys.BACKUP_FREQUENCY] = newFrequency.toString() }
                    BackupScheduler.cancelBackup(context)
                    BackupScheduler.scheduleBackup(context)
                    Log.d(TAG, "Backup rescheduled with new frequency: $newFrequency days")
                }
            },
            onDismiss = { dialog = null },
        )
        BackupDialog.Retention -> NumberInputDialog(
            title = stringResource(R.string.backup_retention_count),
            initial = retention.toString(),
            hint = stringResource(R.string.backup_retention_hint),
            onConfirm = { input ->
                dialog = null
                val newRetention = input.toIntOrNull()
                if (newRetention == null || newRetention < 0) {
                    showMessage(R.string.enter_valid_value)
                } else {
                    scope.launch {
                        AppPreferences.dataStore.edit { it[PreferenceKeys.BACKUP_RETENTION_COUNT] = newRetention }
                        if (AppPreferences.backupFrequency > 0) BackupScheduler.scheduleImmediateCleanup(context)
                    }
                }
            },
            onDismiss = { dialog = null },
        )
        is BackupDialog.ImportWarning -> MessageDialog(
            title = stringResource(R.string.import_warning_title),
            message = stringResource(R.string.import_warning_message),
            onConfirm = {
                dialog = null
                scope.launch { importDataWithClear(context, current.uri) }
            },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

private fun displayPath(uri: Uri): String = try {
    DocumentsContract.getTreeDocumentId(uri).substringAfter(':', "")
} catch (_: Exception) {
    ""
}

private suspend fun saveBackupLocation(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        AppPreferences.dataStore.edit { it[PreferenceKeys.BACKUP_LOCATION_URI] = uri.toString() }
        Log.d(TAG, "Backup location set to: ${displayPath(uri)} (URI: $uri)")
    } catch (e: Exception) {
        Log.e(TAG, "Error setting backup location URI permissions", e)
    }
}

private fun manualExportIntent(): Intent {
    return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = BackupIO.MIME_TYPE
        putExtra(Intent.EXTRA_TITLE, BackupIO.fileName())
        AppPreferences.backupLocationUri?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, it.toUri()) }
    }
}

private fun writeText(context: Context, uri: Uri, content: (Context) -> String): Boolean = try {
    val text = content(context)
    context.contentResolver.openOutputStream(uri)?.use { stream -> stream.bufferedWriter().use { it.write(text) } } != null
} catch (e: Exception) {
    Log.e(TAG, "CSV export failed", e)
    false
}

private suspend fun exportToUri(context: Context, uri: Uri): Boolean {
    return try {
        val content = BackupIO.export(context)
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.bufferedWriter().use { it.write(content) }
        } ?: return false
        true
    } catch (e: Exception) {
        Log.e(TAG, "Manual export failed", e)
        false
    }
}

private fun parseImportDate(raw: String): String =
    if (raw.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) raw else Database.snapTimestampToDate(raw.toLong())

private fun readImportLines(context: Context, uri: Uri): List<String> =
    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
        FileInputStream(pfd.fileDescriptor).bufferedReader().use { it.readLines() }
    }.orEmpty()

private fun readImportText(context: Context, uri: Uri): String =
    context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }.orEmpty()

private fun isImportFileValid(context: Context, uri: Uri): Boolean = try {
    val text = readImportText(context, uri)
    if (FullBackup.isFullBackup(text)) FullBackup.decode(text) != null else text.lines().any { line ->
        val split = line.split(",")
        split.size >= 2 && runCatching {
            split[1].trim().toInt()
            parseImportDate(split[0].trim())
        }.isSuccess
    }
} catch (_: Exception) {
    false
}

private suspend fun restoreFullBackup(context: Context, uri: Uri): Boolean {
    val file = withContext(Dispatchers.IO) { FullBackup.decode(readImportText(context, uri)) } ?: return false
    val todaySteps = withContext(Dispatchers.IO) { BackupIO.restore(context, file) }
    context.startService(
        Intent(context, MotionService::class.java).apply {
            putExtra("FORCE_UPDATE", true)
            putExtra(KEY_STEPS, todaySteps)
            putExtra(KEY_DATE, Util.todayDateString())
        }
    )
    Toast.makeText(context, R.string.import_full_done, Toast.LENGTH_LONG).show()
    context.startActivity(
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    )
    (context as? Activity)?.finishAffinity()
    return true
}

private suspend fun importDataWithClear(context: Context, uri: Uri) {
    val isFull = withContext(Dispatchers.IO) { FullBackup.isFullBackup(readImportText(context, uri)) }
    if (isFull) {
        if (!restoreFullBackup(context, uri)) Toast.makeText(context, R.string.cannot_open_file, Toast.LENGTH_SHORT).show()
        return
    }
    val today = Util.todayDateString()
    val entries = mutableListOf<Pair<String, Int>>()
    var failed = 0
    var importedTodaySteps = 0

    val imported = withContext(Dispatchers.IO) {
        try {
            for (line in readImportLines(context, uri)) {
                if (line.isBlank()) continue
                try {
                    val split = line.split(",")
                    if (split.size < 2) {
                        failed++
                        continue
                    }
                    val steps = split[1].trim().toInt()
                    val dateStr = parseImportDate(split[0].trim())
                    entries.add(dateStr to steps)
                    if (dateStr == today) importedTodaySteps += steps
                } catch (ex: Exception) {
                    Log.e(TAG, "Cannot parse line", ex)
                    failed++
                }
            }
            Database.getInstance(context).clearAllAndImport(entries)
            true
        } catch (ex: Exception) {
            Log.e(TAG, "Import failed", ex)
            false
        }
    }

    if (!imported) {
        Toast.makeText(context, R.string.cannot_open_file, Toast.LENGTH_SHORT).show()
        return
    }

    AppPreferences.dataStore.edit { prefs ->
        prefs[PreferenceKeys.STEPS] = importedTodaySteps
        prefs[PreferenceKeys.DATE] = today
    }
    context.startService(
        Intent(context, MotionService::class.java).apply {
            putExtra("FORCE_UPDATE", true)
            putExtra(KEY_STEPS, importedTodaySteps)
            putExtra(KEY_DATE, today)
        }
    )

    val todayNote = if (importedTodaySteps > 0) context.getString(R.string.today_steps_set, importedTodaySteps) else ""
    Toast.makeText(context, context.getString(R.string.import_result, entries.size, failed, todayNote), Toast.LENGTH_LONG).show()

    context.startActivity(
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    )
    (context as? Activity)?.finishAffinity()
}
