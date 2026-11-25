package com.example.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppAccentTheme
import com.example.data.model.AppThemeMode
import com.example.data.model.HistorySyncMode
import com.example.data.model.UserPreferences
import com.example.data.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "trackpulse_user_session",
)

class SessionStore(private val context: Context) {

    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val NAME = stringPreferencesKey("name")
        val EMAIL = stringPreferencesKey("email")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_THEME = stringPreferencesKey("accent_theme")
        val PROFILE_IMAGE_PATH = stringPreferencesKey("profile_image_path")
        val PROFILE_IMAGE_VERSION = longPreferencesKey("profile_image_version")
        val HISTORY_SYNC_MODE = stringPreferencesKey("history_sync_mode")
        val IS_TRACKING_ACTIVE = booleanPreferencesKey("is_tracking_active")
        val ACTIVE_TRACKING_USER_ID = stringPreferencesKey("active_tracking_user_id")
    }

    val sessionFlow: Flow<UserSession?> = context.sessionDataStore.data.map { prefs ->
        val userId = prefs[Keys.USER_ID]
        val name = prefs[Keys.NAME]
        val email = prefs[Keys.EMAIL].orEmpty()
        if (!userId.isNullOrBlank() && !name.isNullOrBlank()) {
            UserSession(userId = userId, name = name, email = email)
        } else {
            null
        }
    }

    val preferencesFlow: Flow<UserPreferences> = context.sessionDataStore.data.map { prefs ->
        val themeMode = prefs[Keys.THEME_MODE]
            ?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
            ?: AppThemeMode.LIGHT
        val accentTheme = prefs[Keys.ACCENT_THEME]
            ?.let { runCatching { AppAccentTheme.valueOf(it) }.getOrNull() }
            ?: AppAccentTheme.INDIGO
        val historySyncMode = prefs[Keys.HISTORY_SYNC_MODE]
            ?.let { runCatching { HistorySyncMode.valueOf(it) }.getOrNull() }
            ?: HistorySyncMode.SPEED_ADAPTIVE
        UserPreferences(
            themeMode = themeMode,
            accentTheme = accentTheme,
            profileImagePath = prefs[Keys.PROFILE_IMAGE_PATH],
            profileImageVersion = prefs[Keys.PROFILE_IMAGE_VERSION] ?: 0L,
            historySyncMode = historySyncMode,
        )
    }

    val historySyncModeFlow: Flow<HistorySyncMode> = context.sessionDataStore.data.map { prefs ->
        prefs[Keys.HISTORY_SYNC_MODE]
            ?.let { runCatching { HistorySyncMode.valueOf(it) }.getOrNull() }
            ?: HistorySyncMode.SPEED_ADAPTIVE
    }

    val isTrackingPersistedFlow: Flow<Boolean> = context.sessionDataStore.data.map { prefs ->
        prefs[Keys.IS_TRACKING_ACTIVE] ?: false
    }

    val activeTrackingUserIdFlow: Flow<String?> = context.sessionDataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_TRACKING_USER_ID]
    }

    suspend fun saveSession(session: UserSession) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.USER_ID] = session.userId
            prefs[Keys.NAME] = session.name
            prefs[Keys.EMAIL] = session.email
        }
    }

    suspend fun saveThemeMode(mode: AppThemeMode) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun saveAccentTheme(accent: AppAccentTheme) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.ACCENT_THEME] = accent.name
        }
    }

    suspend fun saveHistorySyncMode(mode: HistorySyncMode) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.HISTORY_SYNC_MODE] = mode.name
        }
    }

    suspend fun saveProfileImage(path: String?, version: Long) {
        context.sessionDataStore.edit { prefs ->
            if (path.isNullOrBlank()) {
                prefs.remove(Keys.PROFILE_IMAGE_PATH)
                prefs.remove(Keys.PROFILE_IMAGE_VERSION)
            } else {
                prefs[Keys.PROFILE_IMAGE_PATH] = path
                prefs[Keys.PROFILE_IMAGE_VERSION] = version
            }
        }
    }

    suspend fun saveTrackingActive(active: Boolean, userId: String?) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.IS_TRACKING_ACTIVE] = active
            if (active && !userId.isNullOrBlank()) {
                prefs[Keys.ACTIVE_TRACKING_USER_ID] = userId
            } else {
                prefs.remove(Keys.ACTIVE_TRACKING_USER_ID)
            }
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.NAME)
            prefs.remove(Keys.EMAIL)
            prefs.remove(Keys.IS_TRACKING_ACTIVE)
            prefs.remove(Keys.ACTIVE_TRACKING_USER_ID)
        }
    }
}
