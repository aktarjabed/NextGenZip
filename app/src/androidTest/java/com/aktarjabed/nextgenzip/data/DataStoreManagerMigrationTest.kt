package com.aktarjabed.nextgenzip.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataStoreManagerMigrationTest {

    private val Context.testDataStore by preferencesDataStore(name = "nextgenzip_prefs")
    private lateinit var context: Context
    private val LEGACY_KEY = stringPreferencesKey("password_plaintext")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        runBlocking {
            context.testDataStore.edit { prefs ->
                prefs.remove(LEGACY_KEY)
            }
            DataStoreManager.clearPassword(context)
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            context.testDataStore.edit { prefs ->
                prefs.remove(LEGACY_KEY)
            }
            DataStoreManager.clearPassword(context)
        }
    }

    @Test
    fun migration_moves_plaintext_to_encrypted_and_removes_legacy() = runBlocking {
        val legacyPassword = "legacySecret123"
        context.testDataStore.edit { prefs ->
            prefs[LEGACY_KEY] = legacyPassword
        }

        DataStoreManager.migratePlaintextPasswordIfAny(context)

        val snapshot = context.testDataStore.data.first()
        assertFalse("Legacy plaintext key should be removed after migration", snapshot.contains(LEGACY_KEY))

        val migrated = DataStoreManager.getPassword(context)
        assertNotNull("Migrated password should be present in encrypted prefs", migrated)
        assertEquals("Migrated password must match original", legacyPassword, migrated)
    }

    @Test
    fun migration_is_idempotent_and_safe_to_run_twice() = runBlocking {
        val legacyPassword = "repeatable123"
        context.testDataStore.edit { prefs ->
            prefs[LEGACY_KEY] = legacyPassword
        }

        DataStoreManager.migratePlaintextPasswordIfAny(context)
        DataStoreManager.migratePlaintextPasswordIfAny(context)

        val migrated = DataStoreManager.getPassword(context)
        assertEquals(legacyPassword, migrated)
    }

    @Test
    fun encrypted_set_and_get_roundtrip() = runBlocking {
        val password = "roundtrip-456"
        DataStoreManager.setPassword(context, password)
        val readBack = DataStoreManager.getPassword(context)
        assertEquals("Set/get must return the same password", password, readBack)
    }

    @Test
    fun clearPassword_removes_value() = runBlocking {
        val password = "toBeCleared"
        DataStoreManager.setPassword(context, password)
        var present = DataStoreManager.getPassword(context)
        assertNotNull(present)

        DataStoreManager.clearPassword(context)
        present = DataStoreManager.getPassword(context)
        assertNull("Password should be null after clear", present)
    }
}
