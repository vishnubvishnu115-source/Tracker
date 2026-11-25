package com.example.data.model

data class UserSession(
    val userId: String,
    val name: String,
    val email: String,
)

enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

enum class AppAccentTheme(val displayName: String) {
    INDIGO("Indigo"),
    EMERALD("Emerald"),
    OCEAN("Ocean"),
    SUNSET("Sunset"),
    VIOLET("Violet"),
}

data class UserPreferences(
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val accentTheme: AppAccentTheme = AppAccentTheme.INDIGO,
    val profileImagePath: String? = null,
    val profileImageVersion: Long = 0L,
)

data class GpsLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestampMs: Long,
)

enum class SyncType {
    CURRENT,
    HISTORY,
}

data class SyncLogEntry(
    val id: Long,
    val type: SyncType,
    val success: Boolean,
    val message: String,
    val timestampFormatted: String,
)

sealed interface ZohoAuthResult {
    data class Success(val userId: String, val name: String) : ZohoAuthResult
    data class Error(val message: String) : ZohoAuthResult
}

sealed interface ZohoPostResult {
    data class Success(val message: String) : ZohoPostResult
    data class ZohoApiError(val message: String) : ZohoPostResult
    data class NetworkError(val message: String) : ZohoPostResult
}
