package com.nvllz.stepsy.util

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AppDataStoreTest {

    private val context = RuntimeEnvironment.getApplication()
    private val steps = intPreferencesKey("STEPS")
    private val height = stringPreferencesKey("height")

    @Test
    fun readsSettingsWrittenByTheOldSingleProcessStore() = runBlocking {
        val name = "legacy_prefs"
        context.preferencesDataStoreFile(name).delete()
        val legacyScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val legacy = PreferenceDataStoreFactory.create(scope = legacyScope) { context.preferencesDataStoreFile(name) }
        legacy.edit {
            it[steps] = 4_321
            it[height] = "172"
        }
        legacyScope.cancel()

        val prefs = AppDataStore.create(context, name, multiProcess = false).data.first()

        assertEquals(4_321, prefs[steps])
        assertEquals("172", prefs[height])
    }

    @Test
    fun writesAreReadBack() = runBlocking {
        val name = "roundtrip_prefs"
        context.preferencesDataStoreFile(name).delete()
        val store = AppDataStore.create(context, name, multiProcess = false)

        store.edit { it[steps] = 10 }
        store.edit { it[steps] = (it[steps] ?: 0) + 5 }

        assertEquals(15, store.data.first()[steps])
    }
}
