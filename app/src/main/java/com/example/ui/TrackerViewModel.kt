package com.example.ui

import android.app.Application
import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.location.LocationTrackerClient
import com.example.data.model.AppAccentTheme
import com.example.data.model.AppThemeMode
import com.example.data.model.GpsLocation
import com.example.data.model.HistorySyncMode
import com.example.data.model.SpeedHistoryTier
import com.example.data.model.SyncLogEntry
import com.example.data.model.UserSession
import com.example.data.model.ZohoAuthResult
import com.example.data.network.ZohoTrackerApi
import com.example.data.session.SessionStore
import com.example.data.storage.AppStorageManager
import com.example.data.storage.ProfileImageStorage
import com.example.data.storage.StorageUsageSummary
import com.example.service.LocationTrackingService
import com.example.service.TrackingStateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class TrackerUiState(
    val isLoadingSession: Boolean = true,
    val session: UserSession? = null,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val accentTheme: AppAccentTheme = AppAccentTheme.INDIGO,
    val historySyncMode: HistorySyncMode = HistorySyncMode.SPEED_ADAPTIVE,
    val profileImagePath: String? = null,
    val profileImageVersion: Long = 0L,
    val isUpdatingAvatar: Boolean = false,
    val emailInput: String = "",
    val isSigningIn: Boolean = false,
    val authError: String? = null,
    val isTracking: Boolean = false,
    val isAcquiringInitialFix: Boolean = false,
    val isStationaryIdle: Boolean = false,
    val isWakeLockActive: Boolean = false,
    val latestLocation: GpsLocation? = null,
    val currentSpeedKmh: Float = 0f,
    val activeSpeedTier: SpeedHistoryTier = SpeedHistoryTier.STATIONARY,
    val activeHistoryIntervalSec: Int = 120,
    val liveSyncCount: Int = 0,
    val lastLiveSyncTime: String? = null,
    val isSendingCurrent: Boolean = false,
    val historyLogCount: Int = 0,
    val lastHistorySyncTime: String? = null,
    val historyCountdownSec: Int = 120,
    val trackingError: String? = null,
    val isPermissionError: Boolean = false,
    val recentLogs: List<SyncLogEntry> = emptyList(),
    val storageUsage: StorageUsageSummary = StorageUsageSummary(),
    val hasBackgroundLocation: Boolean = false,
    val isBatteryUnrestricted: Boolean = false,
)

class TrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val sessionStore = SessionStore(appContext)
    private val zohoApi = ZohoTrackerApi()
    private val locationClient = LocationTrackerClient(appContext)

    private val _uiState = MutableStateFlow(TrackerUiState())
    val uiState: StateFlow<TrackerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            var initialSplashShown = false
            sessionStore.sessionFlow.collect { savedSession ->
                if (!initialSplashShown) {
                    initialSplashShown = true
                    // Ensure saved theme & accent preferences are hydrated before ending launch splash
                    val initialPrefs = sessionStore.preferencesFlow.first()
                    val validPath = initialPrefs.profileImagePath?.takeIf { File(it).exists() }
                    _uiState.update { state ->
                        state.copy(
                            themeMode = initialPrefs.themeMode,
                            accentTheme = initialPrefs.accentTheme,
                            profileImagePath = validPath,
                            profileImageVersion = initialPrefs.profileImageVersion,
                            historySyncMode = initialPrefs.historySyncMode,
                        )
                    }
                    delay(850L)
                }
                _uiState.update { state ->
                    state.copy(
                        isLoadingSession = false,
                        session = savedSession,
                    )
                }
            }
        }

        viewModelScope.launch {
            sessionStore.preferencesFlow.collect { prefs ->
                val validPath = prefs.profileImagePath?.takeIf { File(it).exists() }
                _uiState.update { state ->
                    val resolvedInterval = when (prefs.historySyncMode) {
                        HistorySyncMode.SPEED_ADAPTIVE -> state.activeSpeedTier.intervalSeconds
                        HistorySyncMode.FIXED_60S -> 60
                    }
                    state.copy(
                        themeMode = prefs.themeMode,
                        accentTheme = prefs.accentTheme,
                        historySyncMode = prefs.historySyncMode,
                        profileImagePath = validPath,
                        profileImageVersion = prefs.profileImageVersion,
                        activeHistoryIntervalSec = if (state.isTracking) state.activeHistoryIntervalSec else resolvedInterval,
                        historyCountdownSec = if (state.isTracking) state.historyCountdownSec else resolvedInterval,
                    )
                }
            }
        }

        // Observe live foreground-service state so closing and reopening the app
        // seamlessly restores active tracking telemetry, speed tier, counts, countdown, and logs.
        viewModelScope.launch {
            TrackingStateHolder.state.collect { runtime ->
                _uiState.update { state ->
                    val resolvedInterval = if (runtime.isTracking) {
                        runtime.activeHistoryIntervalSec
                    } else {
                        when (state.historySyncMode) {
                            HistorySyncMode.SPEED_ADAPTIVE -> runtime.activeSpeedTier.intervalSeconds
                            HistorySyncMode.FIXED_60S -> 60
                        }
                    }
                    state.copy(
                        isTracking = runtime.isTracking,
                        isAcquiringInitialFix = runtime.isAcquiringInitialFix,
                        isStationaryIdle = runtime.isStationaryIdle,
                        isWakeLockActive = runtime.isWakeLockActive,
                        latestLocation = runtime.latestLocation,
                        currentSpeedKmh = runtime.currentSpeedKmh,
                        activeSpeedTier = runtime.activeSpeedTier,
                        activeHistoryIntervalSec = resolvedInterval,
                        liveSyncCount = runtime.liveSyncCount,
                        lastLiveSyncTime = runtime.lastLiveSyncTime,
                        isSendingCurrent = runtime.isSendingCurrent,
                        historyLogCount = runtime.historyLogCount,
                        lastHistorySyncTime = runtime.lastHistorySyncTime,
                        historyCountdownSec = if (runtime.isTracking) runtime.historyCountdownSec else resolvedInterval,
                        trackingError = runtime.trackingError,
                        isPermissionError = runtime.isPermissionError,
                        recentLogs = runtime.recentLogs,
                    )
                }
            }
        }

        // If tracking was previously active and the OS reclaimed the process,
        // automatically resume the foreground service as soon as the app opens.
        viewModelScope.launch {
            val wasTracking = sessionStore.isTrackingPersistedFlow.first()
            val runtime = TrackingStateHolder.state.value
            if (wasTracking && !runtime.isTracking && !runtime.isAcquiringInitialFix) {
                val userId = sessionStore.activeTrackingUserIdFlow.first()
                    ?: sessionStore.sessionFlow.first()?.userId
                if (
                    !userId.isNullOrBlank() &&
                    locationClient.hasLocationPermission() &&
                    locationClient.isLocationEnabled()
                ) {
                    LocationTrackingService.start(appContext, userId)
                }
            }
        }

        refreshStorageUsage()
        refreshBackgroundProtection()
    }

    fun refreshBackgroundProtection() {
        _uiState.update { state ->
            state.copy(
                hasBackgroundLocation = locationClient.hasBackgroundLocationPermission(),
                isBatteryUnrestricted = locationClient.isBatteryOptimizationIgnored(),
            )
        }
    }

    fun refreshStorageUsage() {
        viewModelScope.launch {
            val summary = withContext(Dispatchers.IO) {
                AppStorageManager.inspectStorage(appContext)
            }
            _uiState.update { state ->
                state.copy(
                    storageUsage = summary.copy(isClearingCache = state.storageUsage.isClearingCache),
                )
            }
        }
    }

    fun clearAppCache(onComplete: (Long) -> Unit) {
        if (_uiState.value.storageUsage.isClearingCache) return
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(storageUsage = state.storageUsage.copy(isClearingCache = true))
            }
            val (freedBytes, updatedSummary) = withContext(Dispatchers.IO) {
                val freed = AppStorageManager.clearCache(appContext)
                val summary = AppStorageManager.inspectStorage(appContext)
                freed to summary
            }
            _uiState.update { state ->
                state.copy(storageUsage = updatedSummary.copy(isClearingCache = false))
            }
            onComplete(freedBytes)
        }
    }

    fun onEmailChanged(newValue: String) {
        _uiState.update { it.copy(emailInput = newValue, authError = null) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        viewModelScope.launch {
            sessionStore.saveThemeMode(mode)
        }
    }

    fun setAccentTheme(accent: AppAccentTheme) {
        _uiState.update { it.copy(accentTheme = accent) }
        viewModelScope.launch {
            sessionStore.saveAccentTheme(accent)
        }
    }

    fun setHistorySyncMode(mode: HistorySyncMode) {
        _uiState.update { state ->
            val interval = when (mode) {
                HistorySyncMode.SPEED_ADAPTIVE -> state.activeSpeedTier.intervalSeconds
                HistorySyncMode.FIXED_60S -> 60
            }
            state.copy(
                historySyncMode = mode,
                activeHistoryIntervalSec = interval,
                historyCountdownSec = if (state.isTracking) state.historyCountdownSec.coerceAtMost(interval) else interval,
            )
        }
        viewModelScope.launch {
            sessionStore.saveHistorySyncMode(mode)
        }
    }

    fun saveProfileImageFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingAvatar = true) }
            val savedPath = withContext(Dispatchers.IO) {
                runCatching {
                    ProfileImageStorage.saveCompressedAvatar(appContext, uri)
                }.getOrNull()
            }

            if (savedPath != null) {
                val version = System.currentTimeMillis()
                sessionStore.saveProfileImage(savedPath, version)
                _uiState.update {
                    it.copy(
                        profileImagePath = savedPath,
                        profileImageVersion = version,
                        isUpdatingAvatar = false,
                    )
                }
                refreshStorageUsage()
            } else {
                _uiState.update { it.copy(isUpdatingAvatar = false) }
                TrackingStateHolder.reportError(
                    message = "Could not load the selected image. Please try another photo.",
                    isPermissionIssue = false,
                )
            }
        }
    }

    fun removeProfileImage() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                ProfileImageStorage.deleteAvatar(appContext)
            }
            sessionStore.saveProfileImage(null, 0L)
            _uiState.update {
                it.copy(profileImagePath = null, profileImageVersion = 0L)
            }
            refreshStorageUsage()
        }
    }

    fun signIn() {
        val trimmedEmail = _uiState.value.emailInput.trim()
        if (trimmedEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _uiState.update { it.copy(authError = "Please enter a valid email address.") }
            return
        }

        if (_uiState.value.isSigningIn) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSigningIn = true, authError = null) }
            when (val result = zohoApi.authenticateByEmail(trimmedEmail)) {
                is ZohoAuthResult.Success -> {
                    val session = UserSession(
                        userId = result.userId,
                        name = result.name,
                        email = trimmedEmail,
                    )
                    sessionStore.saveSession(session)
                    _uiState.update {
                        it.copy(
                            isSigningIn = false,
                            authError = null,
                            emailInput = "",
                        )
                    }
                }

                is ZohoAuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSigningIn = false,
                            authError = result.message,
                        )
                    }
                }
            }
        }
    }

    fun hasLocationPermission(): Boolean = locationClient.hasLocationPermission()

    fun onPermissionDenied() {
        stopTracking(
            errorMessage = "Location permission was denied. Enable precise location access to stream live GPS coordinates.",
            isPermissionIssue = true,
        )
    }

    fun startTrackingWithPermission() {
        val currentSession = _uiState.value.session ?: return
        val runtime = TrackingStateHolder.state.value
        if (runtime.isTracking || runtime.isAcquiringInitialFix) return

        if (!locationClient.hasLocationPermission()) {
            onPermissionDenied()
            return
        }

        if (!locationClient.isLocationEnabled()) {
            TrackingStateHolder.reportError(
                message = "Device GPS is turned off. Enable Location Services in Android Settings and try again.",
                isPermissionIssue = false,
            )
            return
        }

        TrackingStateHolder.dismissError()
        LocationTrackingService.start(appContext, currentSession.userId)
        refreshBackgroundProtection()
    }

    fun stopTracking(errorMessage: String? = null, isPermissionIssue: Boolean = false) {
        LocationTrackingService.stop(appContext)
        TrackingStateHolder.markStopped(
            errorMessage = errorMessage,
            isPermissionIssue = isPermissionIssue,
        )
        viewModelScope.launch {
            sessionStore.saveTrackingActive(active = false, userId = null)
        }
    }

    fun dismissError() {
        TrackingStateHolder.dismissError()
    }

    fun logout() {
        LocationTrackingService.stop(appContext)
        TrackingStateHolder.resetOnLogout()

        viewModelScope.launch {
            sessionStore.clearSession()
            _uiState.update { current ->
                TrackerUiState(
                    isLoadingSession = false,
                    session = null,
                    themeMode = current.themeMode,
                    accentTheme = current.accentTheme,
                    historySyncMode = current.historySyncMode,
                    profileImagePath = current.profileImagePath,
                    profileImageVersion = current.profileImageVersion,
                    storageUsage = current.storageUsage,
                )
            }
            refreshStorageUsage()
        }
    }
}
