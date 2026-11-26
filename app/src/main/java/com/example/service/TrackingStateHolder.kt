package com.example.service

import com.example.data.model.GpsLocation
import com.example.data.model.SpeedHistoryTier
import com.example.data.model.SyncLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Process-wide live telemetry state shared between [LocationTrackingService] and the UI.
 * Keeps live metrics, speed tiers, idle/movement state, coordinates, countdowns, and sync logs
 * intact even when the Activity is closed and reopened while background tracking is active.
 */
data class TrackingRuntimeState(
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
)

object TrackingStateHolder {

    private val _state = MutableStateFlow(TrackingRuntimeState())
    val state: StateFlow<TrackingRuntimeState> = _state.asStateFlow()

    fun update(transform: (TrackingRuntimeState) -> TrackingRuntimeState) {
        _state.update(transform)
    }

    fun dismissError() {
        _state.update { it.copy(trackingError = null, isPermissionError = false) }
    }

    fun reportError(message: String, isPermissionIssue: Boolean = false) {
        _state.update {
            it.copy(
                trackingError = message,
                isPermissionError = isPermissionIssue,
            )
        }
    }

    fun markStopped(errorMessage: String? = null, isPermissionIssue: Boolean = false) {
        _state.update {
            it.copy(
                isTracking = false,
                isAcquiringInitialFix = false,
                isStationaryIdle = false,
                isWakeLockActive = false,
                isSendingCurrent = false,
                currentSpeedKmh = 0f,
                activeSpeedTier = SpeedHistoryTier.STATIONARY,
                historyCountdownSec = it.activeHistoryIntervalSec,
                trackingError = errorMessage,
                isPermissionError = isPermissionIssue,
            )
        }
    }

    fun resetOnLogout() {
        _state.value = TrackingRuntimeState()
    }
}
