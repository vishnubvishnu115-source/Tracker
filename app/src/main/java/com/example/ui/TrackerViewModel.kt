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
import com.example.data.model.SyncLogEntry
import com.example.data.model.SyncType
import com.example.data.model.UserSession
import com.example.data.model.ZohoAuthResult
import com.example.data.model.ZohoPostResult
import com.example.data.network.ZohoTrackerApi
import com.example.data.session.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.ceil
import kotlin.math.max

data class TrackerUiState(
    val isLoadingSession: Boolean = true,
    val session: UserSession? = null,
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val accentTheme: AppAccentTheme = AppAccentTheme.INDIGO,
    val profileImagePath: String? = null,
    val profileImageVersion: Long = 0L,
    val isUpdatingAvatar: Boolean = false,
    val emailInput: String = "",
    val isSigningIn: Boolean = false,
    val authError: String? = null,
    val isTracking: Boolean = false,
    val isAcquiringInitialFix: Boolean = false,
    val isWakeLockActive: Boolean = false,
    val latestLocation: GpsLocation? = null,
    val liveSyncCount: Int = 0,
    val lastLiveSyncTime: String? = null,
    val isSendingCurrent: Boolean = false,
    val historyLogCount: Int = 0,
    val lastHistorySyncTime: String? = null,
    val historyCountdownSec: Int = 60,
    val trackingError: String? = null,
    val isPermissionError: Boolean = false,
    val recentLogs: List<SyncLogEntry> = emptyList(),
)

class TrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val sessionStore = SessionStore(appContext)
    private val zohoApi = ZohoTrackerApi()
    private val locationClient = LocationTrackerClient(appContext)

    private val _uiState = MutableStateFlow(TrackerUiState())
    val uiState: StateFlow<TrackerUiState> = _uiState.asStateFlow()

    @Volatile
    private var lastCurrentSentAt: Long = 0L

    @Volatile
    private var lastHistorySentAt: Long = 0L

    private val isSendingCurrentLock = AtomicBoolean(false)
    private val isSendingHistoryLock = AtomicBoolean(false)

    @Volatile
    private var latestCoordsRef: GpsLocation? = null

    private var locationStreamJob: Job? = null
    private var schedulerJob: Job? = null
    private var initialFixJob: Job? = null

    private val logIdSequence = AtomicLong(0L)
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        viewModelScope.launch {
            sessionStore.sessionFlow.collect { savedSession ->
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
                    state.copy(
                        themeMode = prefs.themeMode,
                        accentTheme = prefs.accentTheme,
                        profileImagePath = validPath,
                        profileImageVersion = prefs.profileImageVersion,
                    )
                }
            }
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

    /**
     * Copies the selected image from Photo Picker (Gallery) or SAF (Google Drive / Files)
     * into app-private storage so it persists reliably across restarts with zero storage permissions.
     */
    fun saveProfileImageFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingAvatar = true) }
            val savedPath = withContext(Dispatchers.IO) {
                runCatching {
                    val targetFile = File(appContext.filesDir, "operator_profile_avatar.img")
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: return@runCatching null
                    if (targetFile.exists() && targetFile.length() > 0L) {
                        targetFile.absolutePath
                    } else {
                        null
                    }
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
            } else {
                _uiState.update {
                    it.copy(
                        isUpdatingAvatar = false,
                        trackingError = "Could not load the selected image. Please try another photo.",
                        isPermissionError = false,
                    )
                }
            }
        }
    }

    fun removeProfileImage() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    File(appContext.filesDir, "operator_profile_avatar.img").delete()
                }
            }
            sessionStore.saveProfileImage(null, 0L)
            _uiState.update {
                it.copy(profileImagePath = null, profileImageVersion = 0L)
            }
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
        if (_uiState.value.isTracking || _uiState.value.isAcquiringInitialFix) return

        if (!locationClient.hasLocationPermission()) {
            onPermissionDenied()
            return
        }

        if (!locationClient.isLocationEnabled()) {
            _uiState.update {
                it.copy(
                    trackingError = "Device GPS is turned off. Enable Location Services in Android Settings and try again.",
                    isPermissionError = false,
                )
            }
            return
        }

        initialFixJob?.cancel()
        initialFixJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAcquiringInitialFix = true,
                    trackingError = null,
                    isPermissionError = false,
                )
            }

            val initialFix = locationClient.fetchInitialLocation(timeoutMs = 10_000L)
            if (initialFix != null) {
                recordLocationFix(initialFix)
            }

            lastCurrentSentAt = 0L
            lastHistorySentAt = 0L
            isSendingCurrentLock.set(false)
            isSendingHistoryLock.set(false)

            _uiState.update {
                it.copy(
                    isAcquiringInitialFix = false,
                    isTracking = true,
                    isWakeLockActive = true,
                    historyCountdownSec = 60,
                    trackingError = null,
                )
            }

            startLocationStream()
            startMasterSchedulerLoop(currentSession.userId)
        }
    }

    private fun startLocationStream() {
        locationStreamJob?.cancel()
        locationStreamJob = viewModelScope.launch {
            locationClient.locationUpdatesFlow()
                .catch { throwable ->
                    val message = if (throwable is SecurityException) {
                        "Location permission was revoked. Tracking has been stopped."
                    } else {
                        "GPS stream interrupted: ${throwable.localizedMessage ?: "Unknown location error"}"
                    }
                    stopTracking(
                        errorMessage = message,
                        isPermissionIssue = throwable is SecurityException,
                    )
                }
                .collect { fix ->
                    recordLocationFix(fix)
                }
        }
    }

    private fun startMasterSchedulerLoop(userId: String) {
        schedulerJob?.cancel()
        schedulerJob = viewModelScope.launch {
            while (isActive && _uiState.value.isTracking) {
                val now = System.currentTimeMillis()
                val coords = latestCoordsRef

                // a) Current Location (Every 2 seconds) with in-flight lock
                if (
                    coords != null &&
                    now - lastCurrentSentAt >= 2_000L &&
                    isSendingCurrentLock.compareAndSet(false, true)
                ) {
                    lastCurrentSentAt = now
                    _uiState.update { it.copy(isSendingCurrent = true) }
                    launch {
                        try {
                            val result = zohoApi.postLocation(
                                type = "current",
                                userId = userId,
                                latitude = coords.latitude,
                                longitude = coords.longitude,
                            )
                            handleSyncResult(SyncType.CURRENT, result)
                        } finally {
                            isSendingCurrentLock.set(false)
                            _uiState.update { it.copy(isSendingCurrent = false) }
                        }
                    }
                }

                // b) History Location (Once every 1 minute / 60 seconds)
                if (
                    coords != null &&
                    now - lastHistorySentAt >= 60_000L &&
                    isSendingHistoryLock.compareAndSet(false, true)
                ) {
                    lastHistorySentAt = now
                    launch {
                        try {
                            val result = zohoApi.postLocation(
                                type = "history",
                                userId = userId,
                                latitude = coords.latitude,
                                longitude = coords.longitude,
                            )
                            handleSyncResult(SyncType.HISTORY, result)
                        } finally {
                            isSendingHistoryLock.set(false)
                        }
                    }
                }

                // c) Live countdown UI
                val remainingSeconds = if (lastHistorySentAt == 0L) {
                    if (coords == null) 60 else 0
                } else {
                    max(0, ceil((60_000L - (now - lastHistorySentAt)) / 1000.0).toInt())
                }
                _uiState.update { it.copy(historyCountdownSec = remainingSeconds) }

                delay(1_000L)
            }
        }
    }

    private fun handleSyncResult(type: SyncType, result: ZohoPostResult) {
        val nowText = formatCurrentTime()
        when (result) {
            is ZohoPostResult.Success -> {
                val entry = SyncLogEntry(
                    id = logIdSequence.incrementAndGet(),
                    type = type,
                    success = true,
                    message = result.message,
                    timestampFormatted = nowText,
                )
                _uiState.update { state ->
                    when (type) {
                        SyncType.CURRENT -> state.copy(
                            liveSyncCount = state.liveSyncCount + 1,
                            lastLiveSyncTime = nowText,
                            recentLogs = (listOf(entry) + state.recentLogs).take(15),
                        )
                        SyncType.HISTORY -> state.copy(
                            historyLogCount = state.historyLogCount + 1,
                            lastHistorySyncTime = nowText,
                            recentLogs = (listOf(entry) + state.recentLogs).take(15),
                        )
                    }
                }
            }
            is ZohoPostResult.ZohoApiError -> {
                val entry = SyncLogEntry(
                    id = logIdSequence.incrementAndGet(),
                    type = type,
                    success = false,
                    message = result.message,
                    timestampFormatted = nowText,
                )
                _uiState.update { state ->
                    state.copy(recentLogs = (listOf(entry) + state.recentLogs).take(15))
                }
                stopTracking(
                    errorMessage = "Zoho Sync Stopped: ${result.message}",
                    isPermissionIssue = false,
                )
            }
            is ZohoPostResult.NetworkError -> {
                val entry = SyncLogEntry(
                    id = logIdSequence.incrementAndGet(),
                    type = type,
                    success = false,
                    message = result.message,
                    timestampFormatted = nowText,
                )
                _uiState.update { state ->
                    state.copy(
                        trackingError = "Temporary network issue: ${result.message}",
                        isPermissionError = false,
                        recentLogs = (listOf(entry) + state.recentLogs).take(15),
                    )
                }
            }
        }
    }

    private fun recordLocationFix(fix: GpsLocation) {
        latestCoordsRef = fix
        _uiState.update { state ->
            state.copy(latestLocation = fix)
        }
    }

    fun stopTracking(errorMessage: String? = null, isPermissionIssue: Boolean = false) {
        initialFixJob?.cancel()
        initialFixJob = null
        locationStreamJob?.cancel()
        locationStreamJob = null
        schedulerJob?.cancel()
        schedulerJob = null
        isSendingCurrentLock.set(false)
        isSendingHistoryLock.set(false)

        _uiState.update { state ->
            state.copy(
                isTracking = false,
                isAcquiringInitialFix = false,
                isWakeLockActive = false,
                isSendingCurrent = false,
                historyCountdownSec = 60,
                trackingError = errorMessage,
                isPermissionError = isPermissionIssue,
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(trackingError = null, isPermissionError = false) }
    }

    fun logout() {
        stopTracking()
        latestCoordsRef = null
        lastCurrentSentAt = 0L
        lastHistorySentAt = 0L

        viewModelScope.launch {
            sessionStore.clearSession()
            _uiState.update { current ->
                TrackerUiState(
                    isLoadingSession = false,
                    session = null,
                    themeMode = current.themeMode,
                    accentTheme = current.accentTheme,
                    profileImagePath = current.profileImagePath,
                    profileImageVersion = current.profileImageVersion,
                )
            }
        }
    }

    private fun formatCurrentTime(): String = synchronized(timeFormatter) {
        timeFormatter.format(Date())
    }

    override fun onCleared() {
        super.onCleared()
        initialFixJob?.cancel()
        locationStreamJob?.cancel()
        schedulerJob?.cancel()
    }
}
