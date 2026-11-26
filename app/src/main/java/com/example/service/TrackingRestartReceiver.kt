package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.location.LocationTrackerClient
import com.example.data.session.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Watchdog receiver that automatically revives [LocationTrackingService] if the Android OS or
 * an aggressive OEM battery manager kills the process after the user swipes TrackPulse from Recents
 * or after a device reboot — ONLY while the user has not explicitly stopped tracking.
 */
class TrackingRestartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val supported = action == ACTION_RESTART_TRACKING ||
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        if (!supported) return

        // If the tracking service is already alive in this process, no restart needed.
        val runtime = TrackingStateHolder.state.value
        if (runtime.isTracking || runtime.isAcquiringInitialFix) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sessionStore = SessionStore(appContext)
                val shouldKeepTracking = sessionStore.isTrackingPersistedFlow.first()
                if (!shouldKeepTracking) return@launch

                val userId = sessionStore.activeTrackingUserIdFlow.first()
                    ?: sessionStore.sessionFlow.first()?.userId
                if (userId.isNullOrBlank()) return@launch

                val locationClient = LocationTrackerClient(appContext)
                if (!locationClient.hasLocationPermission() || !locationClient.isLocationEnabled()) {
                    return@launch
                }

                runCatching {
                    LocationTrackingService.start(appContext, userId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_RESTART_TRACKING = "com.example.action.RESTART_LOCATION_TRACKING"
    }
}
