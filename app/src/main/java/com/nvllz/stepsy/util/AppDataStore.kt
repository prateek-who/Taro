package com.nvllz.stepsy.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.createMultiProcessCoordinator
import androidx.datastore.core.createSingleProcessCoordinator
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import okio.Path.Companion.toPath

object AppDataStore {
    const val NAME = "app_preferences"

    fun create(context: Context, name: String = NAME, multiProcess: Boolean = true): DataStore<Preferences> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = context.applicationContext.preferencesDataStoreFile(name)
        return PreferenceDataStoreFactory.create(
            storage = OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                serializer = PreferencesSerializer,
                coordinatorProducer = { _, _ ->
                    if (multiProcess) createMultiProcessCoordinator(scope.coroutineContext, file)
                    else createSingleProcessCoordinator(file)
                },
                producePath = { file.absolutePath.toPath() },
            ),
            scope = scope,
        )
    }
}
