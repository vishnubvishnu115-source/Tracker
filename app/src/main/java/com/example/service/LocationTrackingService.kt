package com.example.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.location.LocationTrackerClient
import com.example.data.model.GpsLocation
import com.example.data.model.HistorySyncMode
import com.example.data.model.SpeedHistoryTier
import com.example.data.model.SyncLogEntry
import com.example.data.model.SyncType
import com.example.data.model.ZohoPostResult
import com.example.data.network.ZohoTrackerApi
import com.example.data.session.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Foreground Location Service that keeps high-accuracy GPS streaming and
 * Zoho Creator 2s Live + Speed-Adaptive (or Fixed 60s) History telemetry running
 * continuously even when the user closes or swipes the app away from Recents —
 * until the user explicitly stops it.
 */
class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var sessionStore: SessionStore
    private lateinit var locationClient: LocationTrackerClient
    private val zohoApi = ZohoTrackerApi()

    private var cpuWakeLock: PowerManager.WakeLock? = null

    @Volatile
    private var lastCurrentSentAt: Long = 0L

    @Volatile
    private var lastHistorySentAt: Long = 0L

    @Volatile
    private var currentHistorySyncMode: HistorySyncMode = HistorySyncMode.SPEED_ADAPTIVE

    private val isSendingCurrentLock = AtomicBoolean(false)
    private val isSendingHistoryLock = AtomicBoolean(false)

    @Volatile
    private var latestCoordsRef: GpsLocation? = null

    @Volatile
    private var lastSentCurrentCoords: GpsLocation? = null

    @Volatile
    private var lastSentHistoryCoords: GpsLocation? = null

    @Volatile
    private var activeUserId: String? = null

    private var initialFixJob: Job? = null
    private var locationStreamJob: Job? = null
    private var schedulerJob: Job? = null

    private val logIdSequence = AtomicLong(0L)
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    @Volatile
    private var lastNotificationUpdateAt: Long = 0L

    @Volatile
    private var explicitStopRequested: Boolean = false

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStore(applicationContext)
        locationClient = LocationTrackerClient(applicationContext)
        ensureNotificationChannel()

        // React immediately if the operator toggles between Speed-Adaptive and Fixed 60s mode
        serviceScope.launch {
            sessionStore.historySyncModeFlow.collect { mode ->
                currentHistorySyncMode = mode
                val speedKmh = latestCoordsRef?.speedKmh ?: 0f
                val tier = SpeedHistoryTier.fromSpeedKmh(speedKmh)
                val intervalSec = resolveHistoryIntervalSec(mode, tier)
                TrackingStateHolder.update { state ->
                    state.copy(
                        activeSpeedTier = tier,
                        activeHistoryIntervalSec = intervalSec,
                        historyCountdownSec = if (!state.isTracking) intervalSec else state.historyCountdownSec.coerceAtMost(intervalSec),
                    )
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                explicitStopRequested = true
                cancelRestartWatchdog()
                stopTrackingAndSelf(clearPersistence = true)
                return START_NOT_STICKY
            }

            ACTION_START -> {
                val requestedUserId = intent.getStringExtra(EXTRA_USER_ID)?.trim().orEmpty()
                if (requestedUserId.isEmpty()) {
                    explicitStopRequested = true
                    cancelRestartWatchdog()
                    stopTrackingAndSelf(clearPersistence = true)
                    return START_NOT_STICKY
                }
                explicitStopRequested = false
                cancelRestartWatchdog()
                if (!promoteToForeground(isAcquiring = true)) {
                    TrackingStateHolder.markStopped(
                        errorMessage = getString(R.string.error_permission_denied),
                        isPermissionIssue = true,
                    )
                    stopSelf()
                    return START_NOT_STICKY
                }
                startTrackingSession(requestedUserId)
                return START_STICKY
            }

            null -> {
                explicitStopRequested = false
                if (!promoteToForeground(isAcquiring = true)) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                serviceScope.launch {
                    val shouldResume = sessionStore.isTrackingPersistedFlow.first()
                    val persistedUserId = sessionStore.activeTrackingUserIdFlow.first()
                        ?: sessionStore.sessionFlow.first()?.userId
                    if (shouldResume && !persistedUserId.isNullOrBlank() && locationClient.hasLocationPermission()) {
                        startTrackingSession(persistedUserId)
                    } else {
                        explicitStopRequested = true
                        cancelRestartWatchdog()
                        stopTrackingAndSelf(clearPersistence = false)
                    }
                }
                return START_STICKY
            }

            else -> return START_STICKY
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val state = TrackingStateHolder.state.value
        if (!explicitStopRequested && (state.isTracking || state.isAcquiringInitialFix || activeUserId != null)) {
            updateNotification(force = true)
            // Schedule a lightweight self-recovery watchdog in case the device OEM kills
            // the process shortly after the user swipes TrackPulse from Recents.
            scheduleRestartWatchdog(delayMs = 2_000L)
        }
    }

    private fun startTrackingSession(userId: String) {
        val currentState = TrackingStateHolder.state.value
        if (activeUserId == userId && (currentState.isTracking || currentState.isAcquiringInitialFix)) {
            updateNotification(force = true)
            return
        }

        if (!locationClient.hasLocationPermission()) {
            stopTrackingAndSelf(
                clearPersistence = true,
                errorMessage = getString(R.string.error_permission_denied),
                isPermissionIssue = true,
            )
            return
        }

        if (!locationClient.isLocationEnabled()) {
            stopTrackingAndSelf(
                clearPersistence = true,
                errorMessage = getString(R.string.error_location_disabled),
                isPermissionIssue = false,
            )
            return
        }

        activeUserId = userId
        acquireCpuWakeLock()

        serviceScope.launch {
            sessionStore.saveTrackingActive(active = true, userId = userId)
        }

        initialFixJob?.cancel()
        locationStreamJob?.cancel()
        schedulerJob?.cancel()

        initialFixJob = serviceScope.launch {
            currentHistorySyncMode = sessionStore.historySyncModeFlow.first()

            TrackingStateHolder.update {
                it.copy(
                    isAcquiringInitialFix = true,
                    trackingError = null,
                    isPermissionError = false,
                )
            }
            updateNotification(force = true)

            val initialFix = locationClient.fetchInitialLocation(timeoutMs = 10_000L)
            if (initialFix != null) {
                recordLocationFix(initialFix)
            }

            lastCurrentSentAt = 0L
            lastHistorySentAt = 0L
            lastSentCurrentCoords = null
            lastSentHistoryCoords = null
            isSendingCurrentLock.set(false)
            isSendingHistoryLock.set(false)

            val initialSpeed = latestCoordsRef?.speedKmh ?: 0f
            val initialTier = SpeedHistoryTier.fromSpeedKmh(initialSpeed)
            val initialIntervalSec = resolveHistoryIntervalSec(currentHistorySyncMode, initialTier)

            TrackingStateHolder.update {
                it.copy(
                    isAcquiringInitialFix = false,
                    isTracking = true,
                    isStationaryIdle = false,
                    isWakeLockActive = true,
                    currentSpeedKmh = initialSpeed,
                    activeSpeedTier = initialTier,
                    activeHistoryIntervalSec = initialIntervalSec,
                    historyCountdownSec = initialIntervalSec,
                    trackingError = null,
                )
            }
            updateNotification(force = true)

            startLocationStream()
            startMasterSchedulerLoop(userId)
        }
    }

    private fun startLocationStream() {
        locationStreamJob?.cancel()
        locationStreamJob = serviceScope.launch {
            locationClient.locationUpdatesFlow()
                .catch { throwable ->
                    val message = if (throwable is SecurityException) {
                        "Location permission was revoked. Tracking has been stopped."
                    } else {
                        "GPS stream interrupted: ${throwable.localizedMessage ?: "Unknown location error"}"
                    }
                    stopTrackingAndSelf(
                        clearPersistence = true,
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
        schedulerJob = serviceScope.launch {
            while (isActive && TrackingStateHolder.state.value.isTracking) {
                val now = System.currentTimeMillis()
                val coords = latestCoordsRef
                val speedKmh = coords?.speedKmh ?: 0f
                val speedTier = SpeedHistoryTier.fromSpeedKmh(speedKmh)
                val historyIntervalSec = resolveHistoryIntervalSec(currentHistorySyncMode, speedTier)
                val historyIntervalMs = historyIntervalSec * 1_000L

                val hasMovedForLive = coords != null && (
                    lastSentCurrentCoords == null ||
                        hasMovedBeyondTolerance(
                            previous = lastSentCurrentCoords!!,
                            current = coords,
                            minMeters = MIN_LIVE_MOVE_METERS,
                            maxMeters = MAX_LIVE_MOVE_METERS,
                        )
                    )
                val hasMovedForHistory = coords != null && (
                    lastSentHistoryCoords == null ||
                        hasMovedBeyondTolerance(
                            previous = lastSentHistoryCoords!!,
                            current = coords,
                            minMeters = MIN_HISTORY_TOLERANCE_METERS,
                            maxMeters = MAX_HISTORY_TOLERANCE_METERS,
                        )
                    )

                // Stationary idle whenever initial sync has completed and device is not moving
                val isCurrentlyIdle = coords != null &&
                    lastSentCurrentCoords != null &&
                    !hasMovedForLive &&
                    !hasMovedForHistory

                // a) Current Location (Every 2 seconds — ONLY when moved or initial fix)
                if (
                    coords != null &&
                    hasMovedForLive &&
                    now - lastCurrentSentAt >= 2_000L &&
                    isSendingCurrentLock.compareAndSet(false, true)
                ) {
                    lastCurrentSentAt = now
                    TrackingStateHolder.update {
                        it.copy(isSendingCurrent = true, isStationaryIdle = false)
                    }
                    launch {
                        try {
                            val result = zohoApi.postLocation(
                                type = "current",
                                userId = userId,
                                latitude = coords.latitude,
                                longitude = coords.longitude,
                            )
                            if (result is ZohoPostResult.Success) {
                                lastSentCurrentCoords = coords
                            }
                            handleSyncResult(SyncType.CURRENT, result, coords)
                        } finally {
                            isSendingCurrentLock.set(false)
                            TrackingStateHolder.update { it.copy(isSendingCurrent = false) }
                        }
                    }
                }

                // b) History Location (Speed-Adaptive or Fixed 60s — ONLY when moved or initial fix)
                if (
                    coords != null &&
                    hasMovedForHistory &&
                    now - lastHistorySentAt >= historyIntervalMs &&
                    isSendingHistoryLock.compareAndSet(false, true)
                ) {
                    lastHistorySentAt = now
                    TrackingStateHolder.update { it.copy(isStationaryIdle = false) }
                    launch {
                        try {
                            val result = zohoApi.postLocation(
                                type = "history",
                                userId = userId,
                                latitude = coords.latitude,
                                longitude = coords.longitude,
                            )
                            if (result is ZohoPostResult.Success) {
                                lastSentHistoryCoords = coords
                            }
                            handleSyncResult(SyncType.HISTORY, result, coords)
                        } finally {
                            isSendingHistoryLock.set(false)
                        }
                    }
                }

                // c) Live dynamic countdown & idle state UI
                val remainingSeconds = when {
                    isCurrentlyIdle -> historyIntervalSec
                    lastHistorySentAt == 0L -> if (coords == null) historyIntervalSec else 0
                    else -> max(0, ceil((historyIntervalMs - (now - lastHistorySentAt)) / 1000.0).toInt())
                }
                TrackingStateHolder.update {
                    it.copy(
                        isStationaryIdle = isCurrentlyIdle,
                        currentSpeedKmh = speedKmh,
                        activeSpeedTier = speedTier,
                        activeHistoryIntervalSec = historyIntervalSec,
                        historyCountdownSec = remainingSeconds,
                    )
                }
                updateNotification(force = false)
                if (cpuWakeLock?.isHeld != true) {
                    acquireCpuWakeLock()
                }

                delay(1_000L)
            }
        }
    }

    private fun handleSyncResult(type: SyncType, result: ZohoPostResult, coords: GpsLocation) {
        val nowText = formatCurrentTime()
        when (result) {
            is ZohoPostResult.Success -> {
                val entry = SyncLogEntry(
                    id = logIdSequence.incrementAndGet(),
                    type = type,
                    success = true,
                    message = result.message,
                    timestampFormatted = nowText,
                    latitude = coords.latitude,
                    longitude = coords.longitude,
                    accuracyMeters = coords.accuracyMeters,
                    speedKmh = coords.speedKmh,
                )
                TrackingStateHolder.update { state ->
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
                    latitude = coords.latitude,
                    longitude = coords.longitude,
                    accuracyMeters = coords.accuracyMeters,
                    speedKmh = coords.speedKmh,
                )
                TrackingStateHolder.update { state ->
                    state.copy(
                        trackingError = "Zoho sync issue: ${result.message}",
                        isPermissionError = false,
                        recentLogs = (listOf(entry) + state.recentLogs).take(15),
                    )
                }
            }

            is ZohoPostResult.NetworkError -> {
                val entry = SyncLogEntry(
                    id = logIdSequence.incrementAndGet(),
                    type = type,
                    success = false,
                    message = result.message,
                    timestampFormatted = nowText,
                    latitude = coords.latitude,
                    longitude = coords.longitude,
                    accuracyMeters = coords.accuracyMeters,
                    speedKmh = coords.speedKmh,
                )
                TrackingStateHolder.update { state ->
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
        val tier = SpeedHistoryTier.fromSpeedKmh(fix.speedKmh)
        val intervalSec = resolveHistoryIntervalSec(currentHistorySyncMode, tier)
        val movedNow = lastSentCurrentCoords?.let { prev ->
            hasMovedBeyondTolerance(
                previous = prev,
                current = fix,
                minMeters = MIN_LIVE_MOVE_METERS,
                maxMeters = MAX_LIVE_MOVE_METERS,
            )
        } ?: true

        TrackingStateHolder.update { state ->
            state.copy(
                latestLocation = fix,
                currentSpeedKmh = fix.speedKmh,
                activeSpeedTier = tier,
                activeHistoryIntervalSec = intervalSec,
                isStationaryIdle = if (movedNow) false else state.isStationaryIdle,
            )
        }
    }

    private fun resolveHistoryIntervalSec(
        mode: HistorySyncMode,
        tier: SpeedHistoryTier,
    ): Int = when (mode) {
        HistorySyncMode.SPEED_ADAPTIVE -> tier.intervalSeconds
        HistorySyncMode.FIXED_60S -> 60
    }

    /**
     * Checks whether [current] has moved beyond an approximate +/- average radius around [previous]
     * so stationary GPS drift does not trigger redundant Live or History API uploads.
     */
    private fun hasMovedBeyondTolerance(
        previous: GpsLocation,
        current: GpsLocation,
        minMeters: Float,
        maxMeters: Float,
    ): Boolean {
        val distanceResult = FloatArray(1)
        Location.distanceBetween(
            previous.latitude,
            previous.longitude,
            current.latitude,
            current.longitude,
            distanceResult,
        )
        val distanceMeters = distanceResult[0]
        val averageAccuracy = (previous.accuracyMeters + current.accuracyMeters) / 2f
        val toleranceMeters = averageAccuracy.coerceIn(minMeters, maxMeters)
        return distanceMeters > toleranceMeters
    }

    private fun stopTrackingAndSelf(
        clearPersistence: Boolean,
        errorMessage: String? = null,
        isPermissionIssue: Boolean = false,
    ) {
        initialFixJob?.cancel()
        initialFixJob = null
        locationStreamJob?.cancel()
        locationStreamJob = null
        schedulerJob?.cancel()
        schedulerJob = null
        isSendingCurrentLock.set(false)
        isSendingHistoryLock.set(false)
        lastSentCurrentCoords = null
        lastSentHistoryCoords = null
        activeUserId = null

        releaseCpuWakeLock()
        TrackingStateHolder.markStopped(
            errorMessage = errorMessage,
            isPermissionIssue = isPermissionIssue,
        )

        if (clearPersistence) {
            explicitStopRequested = true
            cancelRestartWatchdog()
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { sessionStore.saveTrackingActive(active = false, userId = null) }
            }
        }

        runCatching {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
    }

    private fun promoteToForeground(isAcquiring: Boolean): Boolean {
        return try {
            val notification = buildTrackingNotification(isAcquiringOverride = isAcquiring)
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            }
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun updateNotification(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastNotificationUpdateAt < 2_000L) return
        lastNotificationUpdateAt = now

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        runCatching {
            manager.notify(NOTIFICATION_ID, buildTrackingNotification())
        }
    }

    private fun buildTrackingNotification(isAcquiringOverride: Boolean? = null): Notification {
        val state = TrackingStateHolder.state.value
        val acquiring = isAcquiringOverride ?: state.isAcquiringInitialFix

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            REQ_OPEN_APP,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            REQ_STOP_TRACKING,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = when {
            acquiring -> getString(R.string.notif_tracking_acquiring_title)
            state.isStationaryIdle -> getString(R.string.notif_tracking_idle_title)
            else -> getString(R.string.notif_tracking_active_title)
        }

        val speedRounded = state.currentSpeedKmh.roundToInt()
        val summaryLine = when {
            acquiring -> getString(R.string.notif_tracking_acquiring_body)
            state.isStationaryIdle -> getString(
                R.string.notif_tracking_summary_idle,
                state.liveSyncCount,
                state.historyLogCount,
            )
            else -> getString(
                R.string.notif_tracking_summary_speed,
                speedRounded,
                state.liveSyncCount,
                state.historyLogCount,
                state.historyCountdownSec,
            )
        }

        val coordsLine = state.latestLocation?.let { loc ->
            String.format(
                Locale.US,
                "GPS: %.5f, %.5f (±%dm) · Cycle: %ds",
                loc.latitude,
                loc.longitude,
                loc.accuracyMeters.roundToInt(),
                state.activeHistoryIntervalSec,
            )
        }

        val expandedText = buildString {
            append(summaryLine)
            if (coordsLine != null) {
                append('\n')
                append(coordsLine)
            }
            append('\n')
            append(getString(R.string.notif_tracking_background_hint))
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_trackpulse)
            .setContentTitle(title)
            .setContentText(summaryLine)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                R.drawable.ic_stat_trackpulse,
                getString(R.string.action_stop_tracking),
                stopPendingIntent,
            )
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = getString(R.string.notif_channel_description)
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun acquireCpuWakeLock() {
        if (cpuWakeLock?.isHeld == true) return
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        runCatching {
            val lock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "TrackPulse::LiveGpsTelemetryLock",
            )
            lock.setReferenceCounted(false)
            lock.acquire(12L * 60L * 60L * 1000L)
            cpuWakeLock = lock
        }
    }

    private fun releaseCpuWakeLock() {
        runCatching {
            if (cpuWakeLock?.isHeld == true) {
                cpuWakeLock?.release()
            }
        }
        cpuWakeLock = null
    }

    private fun formatCurrentTime(): String = synchronized(timeFormatter) {
        timeFormatter.format(Date())
    }

    private fun scheduleRestartWatchdog(delayMs: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val restartIntent = Intent(this, TrackingRestartReceiver::class.java).apply {
            action = TrackingRestartReceiver.ACTION_RESTART_TRACKING
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            REQ_RESTART_WATCHDOG,
            restartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val triggerAt = SystemClock.elapsedRealtime() + delayMs
        runCatching {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAt,
                pendingIntent,
            )
        }
    }

    private fun cancelRestartWatchdog() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val restartIntent = Intent(this, TrackingRestartReceiver::class.java).apply {
            action = TrackingRestartReceiver.ACTION_RESTART_TRACKING
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            REQ_RESTART_WATCHDOG,
            restartIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pendingIntent != null) {
            runCatching { alarmManager.cancel(pendingIntent) }
            pendingIntent.cancel()
        }
    }

    override fun onDestroy() {
        val shouldRevive = !explicitStopRequested && activeUserId != null
        initialFixJob?.cancel()
        locationStreamJob?.cancel()
        schedulerJob?.cancel()
        releaseCpuWakeLock()
        serviceScope.cancel()
        if (shouldRevive) {
            scheduleRestartWatchdog(delayMs = 1_500L)
        }
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "trackpulse_live_gps_channel"
        private const val NOTIFICATION_ID = 4011
        private const val REQ_OPEN_APP = 100
        private const val REQ_STOP_TRACKING = 101
        private const val REQ_RESTART_WATCHDOG = 102
        private const val MIN_LIVE_MOVE_METERS = 10f
        private const val MAX_LIVE_MOVE_METERS = 20f
        private const val MIN_HISTORY_TOLERANCE_METERS = 20f
        private const val MAX_HISTORY_TOLERANCE_METERS = 35f

        const val ACTION_START = "com.example.action.START_LOCATION_TRACKING"
        const val ACTION_STOP = "com.example.action.STOP_LOCATION_TRACKING"
        const val EXTRA_USER_ID = "extra_user_id"

        fun start(context: Context, userId: String) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_USER_ID, userId)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching {
                context.startService(intent)
            }.onFailure {
                runCatching { context.stopService(intent) }
            }
        }
    }
}
