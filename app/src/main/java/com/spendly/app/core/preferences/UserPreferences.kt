package com.spendly.app.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.spendly.app.core.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "spendly_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_DEMO_DATA_SEEDED = booleanPreferencesKey("demo_data_seeded")
        private val KEY_GOOGLE_ACCOUNT_EMAIL = stringPreferencesKey("google_account_email")
        private val KEY_GOOGLE_ACCOUNT_NAME = stringPreferencesKey("google_account_name")
        private val KEY_GOOGLE_PHOTO_URL = stringPreferencesKey("google_photo_url")
        private val KEY_CLOUD_SYNC_ENABLED = booleanPreferencesKey("cloud_sync_enabled")
        private val KEY_LAST_CLOUD_SYNC_TIME = androidx.datastore.preferences.core.longPreferencesKey("last_cloud_sync_time")
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_THEME_MODE]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.DARK // Default to dark as shown in the showcase
        }
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val appLockEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_LOCK_ENABLED] ?: false
    }

    val userNameFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_NAME] ?: "Aadesh"
    }

    val userEmailFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_EMAIL] ?: "aadesh@gmail.com"
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_LOCK_ENABLED] = enabled
        }
    }

    suspend fun updateProfile(name: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = name
            prefs[KEY_USER_EMAIL] = email
        }
    }

    suspend fun setDemoDataSeeded(seeded: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEMO_DATA_SEEDED] = seeded
        }
    }

    val isDemoDataSeededFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEMO_DATA_SEEDED] ?: false
    }

    val googleAccountEmailFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_GOOGLE_ACCOUNT_EMAIL]
    }

    val googleAccountNameFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_GOOGLE_ACCOUNT_NAME]
    }

    val googlePhotoUrlFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_GOOGLE_PHOTO_URL]
    }

    val isGoogleSignedInFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[KEY_GOOGLE_ACCOUNT_EMAIL].isNullOrBlank()
    }

    val isCloudSyncEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_CLOUD_SYNC_ENABLED] ?: false
    }

    val lastCloudSyncTimeFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_CLOUD_SYNC_TIME] ?: 0L
    }

    suspend fun setGoogleAccount(email: String?, name: String?, photoUrl: String?) {
        context.dataStore.edit { prefs ->
            if (email != null) {
                prefs[KEY_GOOGLE_ACCOUNT_EMAIL] = email
                prefs[KEY_USER_EMAIL] = email
            } else {
                prefs.remove(KEY_GOOGLE_ACCOUNT_EMAIL)
            }
            if (name != null) {
                prefs[KEY_GOOGLE_ACCOUNT_NAME] = name
                prefs[KEY_USER_NAME] = name
            } else {
                prefs.remove(KEY_GOOGLE_ACCOUNT_NAME)
            }
            if (photoUrl != null) {
                prefs[KEY_GOOGLE_PHOTO_URL] = photoUrl
            } else {
                prefs.remove(KEY_GOOGLE_PHOTO_URL)
            }
        }
    }

    suspend fun signOutGoogle() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_GOOGLE_ACCOUNT_EMAIL)
            prefs.remove(KEY_GOOGLE_ACCOUNT_NAME)
            prefs.remove(KEY_GOOGLE_PHOTO_URL)
            prefs[KEY_CLOUD_SYNC_ENABLED] = false
        }
    }

    suspend fun setCloudSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CLOUD_SYNC_ENABLED] = enabled
        }
    }

    suspend fun setLastCloudSyncTime(timeMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_CLOUD_SYNC_TIME] = timeMs
        }
    }
}
