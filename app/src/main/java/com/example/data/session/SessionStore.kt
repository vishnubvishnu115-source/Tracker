package com.example.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppAccentTheme
import com.example.data.model.AppThemeMode
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
        UserPreferences(
            themeMode = themeMode,
            accentTheme = accentTheme,
            profileImagePath = prefs[Keys.PROFILE_IMAGE_PATH],
            profileImageVersion = prefs[Keys.PROFILE_IMAGE_VERSION] ?: 0L,
        )
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

    suspend fun clearSession() {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.NAME)
            prefs.remove(Keys.EMAIL)
        }
    }
}
