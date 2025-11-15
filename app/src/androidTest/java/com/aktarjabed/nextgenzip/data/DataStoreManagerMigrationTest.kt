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

/**
 * Instrumentation tests for DataStoreManager password migration / encrypted storage.
 *
 * These tests run on device/emulator because EncryptedSharedPreferences requires a real Android environment.
 */
@RunWith(AndroidJUnit4::class)
class DataStoreManagerMigrationTest {

    // Matches the name used in DataStoreManager.kt
    private val Context.testDataStore by preferencesDataStore(name = "nextgenzip_prefs")

    private lateinit var context: Context

    private val LEGACY_KEY = stringPreferencesKey("password_plaintext")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Ensure a clean state before each test
        runBlocking {
            // Clear legacy key if present
            context.testDataStore.edit { prefs ->
                prefs.remove(LEGACY_KEY)
            }
            // Ensure encrypted prefs cleared via API
            DataStoreManager.clearPassword(context)
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            // Cleanup after tests
            context.testDataStore.edit { prefs ->
                prefs.remove(LEGACY_KEY)
            }
            DataStoreManager.clearPassword(context)
        }
    }

    @Test
    fun migration_moves_plaintext_to_encrypted_and_removes_legacy() = runBlocking {
        // Arrange: write a plaintext legacy password into DataStore (simulate old app)
        val legacyPassword = "legacySecret123"
        context.testDataStore.edit { prefs ->
            prefs[LEGACY_KEY] = legacyPassword
        }

        // Act: run migration
        DataStoreManager.migratePlaintextPasswordIfAny(context)

        // Assert: legacy key removed
        val snapshot = context.testDataStore.data.first()
        assertFalse("Legacy plaintext key should be removed after migration", snapshot.contains(LEGACY_KEY))

        // Assert: encrypted store contains password via API
        val migrated = DataStoreManager.getPassword(context)
        assertNotNull("Migrated password should be present in encrypted prefs", migrated)
        assertEquals("Migrated password must match original", legacyPassword, migrated)
    }

    @Test
    fun migration_is_idempotent_and_safe_to_run_twice() = runBlocking {
        // Arrange: write plaintext and run migration twice
        val legacyPassword = "repeatable123"
        context.testDataStore.edit { prefs ->
            prefs[LEGACY_KEY] = legacyPassword
        }

        // First run
        DataStoreManager.migratePlaintextPasswordIfAny(context)

        // Second run — should not throw and should be a no-op
        DataStoreManager.migratePlaintextPasswordIfAny(context)

        // Validate
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
