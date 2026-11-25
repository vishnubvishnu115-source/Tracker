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

enum class HistorySyncMode {
    SPEED_ADAPTIVE,
    FIXED_60S,
}

/**
 * Speed-based dynamic intervals for historical GPS sync checkpoints.
 * Slower movement saves battery & avoids duplicate stationary records;
 * faster travel captures tighter route breadcrumbs.
 */
enum class SpeedHistoryTier(
    val label: String,
    val rangeLabel: String,
    val intervalSeconds: Int,
) {
    STATIONARY(label = "Idle", rangeLabel = "< 3 km/h", intervalSeconds = 120),
    WALKING(label = "Walking", rangeLabel = "3–15 km/h", intervalSeconds = 60),
    CITY_DRIVE(label = "City", rangeLabel = "15–45 km/h", intervalSeconds = 30),
    HIGHWAY(label = "Fast", rangeLabel = "> 45 km/h", intervalSeconds = 15);

    companion object {
        fun fromSpeedKmh(speedKmh: Float): SpeedHistoryTier = when {
            speedKmh < 3f -> STATIONARY
            speedKmh < 15f -> WALKING
            speedKmh < 45f -> CITY_DRIVE
            else -> HIGHWAY
        }
    }
}

data class UserPreferences(
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val accentTheme: AppAccentTheme = AppAccentTheme.INDIGO,
    val profileImagePath: String? = null,
    val profileImageVersion: Long = 0L,
    val historySyncMode: HistorySyncMode = HistorySyncMode.SPEED_ADAPTIVE,
)

data class GpsLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestampMs: Long,
    val speedKmh: Float = 0f,
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
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val speedKmh: Float? = null,
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
