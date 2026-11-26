package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppAccentTheme
import com.example.data.model.AppThemeMode
import com.example.data.model.GpsLocation
import com.example.data.model.HistorySyncMode
import com.example.data.model.SpeedHistoryTier
import com.example.data.model.SyncLogEntry
import com.example.data.model.SyncType
import com.example.data.model.UserSession
import com.example.data.storage.AppStorageManager
import com.example.ui.TrackerUiState
import com.example.ui.components.AccountTabContent
import com.example.ui.components.FloatingToastPill
import com.example.ui.components.GpsSignalBars
import com.example.ui.components.LiquidWaveSecondsLoader
import com.example.ui.components.LogoutConfirmationSheet
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.ShimmerBar
import com.example.ui.components.SlideToStopTrack
import com.example.ui.theme.AppIcons
import com.example.ui.theme.LocalTelemetryColors
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

private enum class TrackerNavTab(val index: Int) {
    TRACKER(0),
    ACTIVITY(1),
    ACCOUNT(2),
}

private enum class LogFilter {
    ALL,
    LIVE,
    HISTORY,
    ERRORS,
}

@Composable
fun TrackerDashboardScreen(
    uiState: TrackerUiState,
    hasLocationPermission: () -> Boolean,
    onStartTracking: () -> Unit,
    onPermissionDenied: () -> Unit,
    onStopTracking: () -> Unit,
    onDismissError: () -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectAccentTheme: (AppAccentTheme) -> Unit,
    onSelectHistorySyncMode: (HistorySyncMode) -> Unit,
    onPickProfileImage: (Uri) -> Unit,
    onRemoveProfileImage: () -> Unit,
    onRefreshStorage: () -> Unit,
    onRefreshBackgroundProtection: () -> Unit,
    onClearCache: ((Long) -> Unit) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val session = uiState.session ?: return

    var selectedTab by rememberSaveable { mutableStateOf(TrackerNavTab.TRACKER) }
    var selectedFilter by rememberSaveable { mutableStateOf(LogFilter.ALL) }
    var showLogoutSheet by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var askedNotificationPermission by rememberSaveable { mutableStateOf(false) }

    val coordsCopiedLabel = stringResource(R.string.toast_coords_copied)
    val trackingStartedLabel = stringResource(R.string.toast_tracking_started)
    val trackingStoppedLabel = stringResource(R.string.toast_tracking_stopped)
    val speedAutoLabel = stringResource(R.string.toast_speed_mode_auto)
    val speedFixedLabel = stringResource(R.string.toast_speed_mode_fixed)
    val cacheAlreadyCleanLabel = stringResource(R.string.toast_cache_already_clean)

    LaunchedEffect(selectedTab) {
        if (selectedTab == TrackerNavTab.ACCOUNT) {
            onRefreshStorage()
            onRefreshBackgroundProtection()
        }
    }

    // Automatically re-check "Allow all the time" and "Unrestricted battery" when returning from Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshBackgroundProtection()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        onRefreshBackgroundProtection()
    }

    val requestBackgroundLocation: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasLocationPermission()) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            context.openAppSettings()
        }
    }

    var previousTrackingState by remember { mutableStateOf(uiState.isTracking) }
    LaunchedEffect(uiState.isTracking) {
        if (!previousTrackingState && uiState.isTracking) {
            toastMessage = trackingStartedLabel
        }
        previousTrackingState = uiState.isTracking
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2_200L)
            toastMessage = null
        }
    }

    val copyCoordsToClipboard: (Double, Double) -> Unit = { lat, lng ->
        val formatted = String.format(Locale.US, "%.6f, %.6f", lat, lng)
        clipboardManager.setText(AnnotatedString(formatted))
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        toastMessage = coordsCopiedLabel
    }

    BackHandler(enabled = selectedTab != TrackerNavTab.TRACKER) {
        selectedTab = TrackerNavTab.TRACKER
    }

    // Keep Screen Awake (Android equivalent of navigator.wakeLock.request("screen"))
    DisposableEffect(uiState.isWakeLockActive) {
        val activity = context.findActivity()
        if (uiState.isWakeLockActive) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val fineGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted || hasLocationPermission()) {
            onStartTracking()
        } else {
            onPermissionDenied()
        }
    }

    val handleStartRequest: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val needsNotificationPermission =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !askedNotificationPermission &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED

        if (hasLocationPermission() && !needsNotificationPermission) {
            onStartTracking()
        } else {
            val permissionsToRequest = buildList {
                if (!hasLocationPermission()) {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                if (needsNotificationPermission) {
                    askedNotificationPermission = true
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray()
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    val filteredLogs = remember(uiState.recentLogs, selectedFilter) {
        when (selectedFilter) {
            LogFilter.ALL -> uiState.recentLogs
            LogFilter.LIVE -> uiState.recentLogs.filter { it.type == SyncType.CURRENT && it.success }
            LogFilter.HISTORY -> uiState.recentLogs.filter { it.type == SyncType.HISTORY && it.success }
            LogFilter.ERRORS -> uiState.recentLogs.filter { !it.success }
        }
    }

    if (showLogoutSheet) {
        LogoutConfirmationSheet(
            isTracking = uiState.isTracking,
            onDismiss = { showLogoutSheet = false },
            onConfirmLogout = {
                showLogoutSheet = false
                onLogout()
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CleanTopAppBar(
                session = session,
                profileImagePath = uiState.profileImagePath,
                profileImageVersion = uiState.profileImageVersion,
                isTracking = uiState.isTracking,
                isStationaryIdle = uiState.isStationaryIdle,
                isWakeLockActive = uiState.isWakeLockActive,
                onAvatarClick = { selectedTab = TrackerNavTab.ACCOUNT },
                onLogout = { showLogoutSheet = true },
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ThumbZoneActionBar(
                    isTracking = uiState.isTracking,
                    isAcquiringInitialFix = uiState.isAcquiringInitialFix,
                    onStartClick = handleStartRequest,
                    onStopClick = {
                        onStopTracking()
                        toastMessage = trackingStoppedLabel
                    },
                )

                MinimalBottomNavigationBar(
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val movingForward = targetState.index >= initialState.index
                    val slideOffset = if (movingForward) {
                        { width: Int -> width / 5 }
                    } else {
                        { width: Int -> -width / 5 }
                    }
                    val exitOffset = if (movingForward) {
                        { width: Int -> -width / 6 }
                    } else {
                        { width: Int -> width / 6 }
                    }
                    (slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                        initialOffsetX = slideOffset,
                    ) + fadeIn(animationSpec = tween(220))).togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(180),
                            targetOffsetX = exitOffset,
                        ) + fadeOut(animationSpec = tween(160)),
                    )
                },
                label = "tabSlideTransition",
                modifier = Modifier.fillMaxSize(),
            ) { currentTab ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 14.dp, bottom = 18.dp),
                ) {
                    item(key = "error_alert") {
                        TrackingErrorBanner(
                            errorMessage = uiState.trackingError,
                            isPermissionError = uiState.isPermissionError,
                            onDismiss = onDismissError,
                            onOpenSettings = { context.openAppSettings() },
                            modifier = Modifier.widthIn(max = 560.dp),
                        )
                    }

                    when (currentTab) {
                        TrackerNavTab.TRACKER -> {
                            if (hasLocationPermission() && (!uiState.hasBackgroundLocation || !uiState.isBatteryUnrestricted)) {
                                item(key = "background_protection_guard") {
                                    BackgroundProtectionCard(
                                        hasBackgroundLocation = uiState.hasBackgroundLocation,
                                        isBatteryUnrestricted = uiState.isBatteryUnrestricted,
                                        onRequestBackgroundLocation = requestBackgroundLocation,
                                        onOpenBatterySettings = { context.openBatteryOptimizationSettings() },
                                        modifier = Modifier.widthIn(max = 560.dp),
                                    )
                                }
                            }

                            item(key = "hero_progress_ring") {
                                AnimatedCardEntrance(delayMillis = 0) {
                                    ProgressRingHeaderCard(
                                        uiState = uiState,
                                        modifier = Modifier.widthIn(max = 560.dp),
                                    )
                                }
                            }

                            item(key = "live_coordinates") {
                                AnimatedCardEntrance(delayMillis = 45) {
                                    LiveCoordinatesCard(
                                        location = uiState.latestLocation,
                                        currentSpeedKmh = uiState.currentSpeedKmh,
                                        activeSpeedTier = uiState.activeSpeedTier,
                                        isTracking = uiState.isTracking,
                                        isStationaryIdle = uiState.isStationaryIdle,
                                        isAcquiringInitialFix = uiState.isAcquiringInitialFix,
                                        onCopyCoordinates = copyCoordsToClipboard,
                                        modifier = Modifier.widthIn(max = 560.dp),
                                    )
                                }
                            }

                            item(key = "sync_metrics_row") {
                                AnimatedCardEntrance(delayMillis = 90) {
                                    SyncStatusCardsRow(
                                        liveSyncCount = uiState.liveSyncCount,
                                        lastLiveSyncTime = uiState.lastLiveSyncTime,
                                        isSendingCurrent = uiState.isSendingCurrent,
                                        historyLogCount = uiState.historyLogCount,
                                        lastHistorySyncTime = uiState.lastHistorySyncTime,
                                        historyCountdownSec = uiState.historyCountdownSec,
                                        activeHistoryIntervalSec = uiState.activeHistoryIntervalSec,
                                        isTracking = uiState.isTracking,
                                        isStationaryIdle = uiState.isStationaryIdle,
                                        modifier = Modifier.widthIn(max = 560.dp),
                                    )
                                }
                            }

                            item(key = "status_timeline") {
                                AnimatedCardEntrance(delayMillis = 135) {
                                    StatusTimelineCard(
                                        session = session,
                                        uiState = uiState,
                                        modifier = Modifier.widthIn(max = 560.dp),
                                    )
                                }
                            }

                            item(key = "filter_pills_home") {
                                FilterPillsBar(
                                    selectedFilter = selectedFilter,
                                    onSelectFilter = { selectedFilter = it },
                                    modifier = Modifier.widthIn(max = 560.dp),
                                )
                            }

                            item(key = "recent_entries_home") {
                                RecentEntriesCard(
                                    logs = filteredLogs,
                                    hasAnyLogs = uiState.recentLogs.isNotEmpty(),
                                    maxItems = 5,
                                    onCopyCoordinates = copyCoordsToClipboard,
                                    modifier = Modifier.widthIn(max = 560.dp),
                                )
                            }
                        }

                        TrackerNavTab.ACTIVITY -> {
                            item(key = "activity_timeline") {
                                AnimatedCardEntrance(delayMillis = 0) {
                                    StatusTimelineCard(
                                        session = session,
                                        uiState = uiState,
                                        modifier = Modifier.widthIn(max = 560.dp),
                                    )
                                }
                            }

                            item(key = "activity_filter_pills") {
                                AnimatedCardEntrance(delayMillis = 45) {
                                    FilterPillsBar(
                                        selectedFilter = selectedFilter,
                                        onSelectFilter = { selectedFilter = it },
                                        modifier = Modifier.widthIn(max = 560.dp),
                                    )
                                }
                            }

                            item(key = "activity_entries_full") {
                                AnimatedCardEntrance(delayMillis = 90) {
                                    RecentEntriesCard(
                                        logs = filteredLogs,
                                        hasAnyLogs = uiState.recentLogs.isNotEmpty(),
                                        maxItems = 15,
                                        onCopyCoordinates = copyCoordsToClipboard,
                                        modifier = Modifier.widthIn(max = 560.dp),
                                    )
                                }
                            }
                        }

                        TrackerNavTab.ACCOUNT -> {
                            item(key = "account_content") {
                                AnimatedCardEntrance(delayMillis = 0) {
                                    AccountTabContent(
                                        session = session,
                                        profileImagePath = uiState.profileImagePath,
                                        profileImageVersion = uiState.profileImageVersion,
                                        isUpdatingAvatar = uiState.isUpdatingAvatar,
                                        themeMode = uiState.themeMode,
                                        accentTheme = uiState.accentTheme,
                                        historySyncMode = uiState.historySyncMode,
                                        currentSpeedKmh = uiState.currentSpeedKmh,
                                        activeSpeedTier = uiState.activeSpeedTier,
                                        storageUsage = uiState.storageUsage,
                                        hasPermission = hasLocationPermission(),
                                        hasBackgroundLocation = uiState.hasBackgroundLocation,
                                        isBatteryUnrestricted = uiState.isBatteryUnrestricted,
                                        isTracking = uiState.isTracking,
                                        isWakeLockActive = uiState.isWakeLockActive,
                                        onSelectThemeMode = onSelectThemeMode,
                                        onSelectAccentTheme = onSelectAccentTheme,
                                        onSelectHistorySyncMode = { mode ->
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onSelectHistorySyncMode(mode)
                                            toastMessage = if (mode == HistorySyncMode.SPEED_ADAPTIVE) {
                                                speedAutoLabel
                                            } else {
                                                speedFixedLabel
                                            }
                                        },
                                        onPickProfileImage = onPickProfileImage,
                                        onRemoveProfileImage = onRemoveProfileImage,
                                        onClearCache = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onClearCache { freedBytes ->
                                                toastMessage = if (freedBytes > 0L) {
                                                    context.getString(
                                                        R.string.toast_cache_cleared,
                                                        AppStorageManager.formatByteSize(freedBytes),
                                                    )
                                                } else {
                                                    cacheAlreadyCleanLabel
                                                }
                                            }
                                        },
                                        onRequestBackgroundLocation = requestBackgroundLocation,
                                        onOpenBatterySettings = { context.openBatteryOptimizationSettings() },
                                        onOpenSettings = { context.openAppSettings() },
                                        onLogout = { showLogoutSheet = true },
                                        modifier = Modifier.widthIn(max = 560.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sleek floating confirmation toast pill
            FloatingToastPill(
                message = toastMessage,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp, start = 20.dp, end = 20.dp),
            )
        }
    }
}

@Composable
private fun AnimatedCardEntrance(
    delayMillis: Long,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0L) delay(delayMillis)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(260)) + slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            initialOffsetY = { it / 6 },
        ),
    ) {
        content()
    }
}

@Composable
private fun CleanTopAppBar(
    session: UserSession,
    profileImagePath: String?,
    profileImageVersion: Long,
    isTracking: Boolean,
    isStationaryIdle: Boolean,
    isWakeLockActive: Boolean,
    onAvatarClick: () -> Unit,
    onLogout: () -> Unit,
) {
    val telemetryColors = LocalTelemetryColors.current

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ProfileAvatar(
                    name = session.name,
                    profileImagePath = profileImagePath,
                    profileImageVersion = profileImageVersion,
                    size = 42.dp,
                    onClick = onAvatarClick,
                    modifier = Modifier.testTag("header_profile_avatar"),
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = session.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("logged_in_user_name"),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "${stringResource(R.string.user_id_prefix)}: ${session.userId}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                            color = telemetryColors.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("logged_in_user_id"),
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (isWakeLockActive) {
                                telemetryColors.countdownAmberContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    imageVector = if (isWakeLockActive) {
                                        AppIcons.WbSunny
                                    } else {
                                        AppIcons.ScreenLockPortrait
                                    },
                                    contentDescription = null,
                                    tint = if (isWakeLockActive) {
                                        telemetryColors.countdownAmberText
                                    } else {
                                        telemetryColors.mutedText
                                    },
                                    modifier = Modifier.size(10.dp),
                                )
                                Text(
                                    text = if (isWakeLockActive) {
                                        stringResource(R.string.wake_lock_active)
                                    } else {
                                        stringResource(R.string.wake_lock_idle)
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                    color = if (isWakeLockActive) {
                                        telemetryColors.countdownAmberText
                                    } else {
                                        telemetryColors.mutedText
                                    },
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }
                }

                val isIdleNow = isTracking && isStationaryIdle
                StatusPillBadge(
                    label = when {
                        isIdleNow -> stringResource(R.string.status_tracking_idle)
                        isTracking -> stringResource(R.string.status_tracking_live)
                        else -> stringResource(R.string.status_standby)
                    },
                    dotColor = when {
                        isIdleNow -> telemetryColors.countdownAmber
                        isTracking -> telemetryColors.liveGreen
                        else -> telemetryColors.mutedText
                    },
                    bgColor = when {
                        isIdleNow -> telemetryColors.countdownAmberContainer
                        isTracking -> telemetryColors.liveGreenContainer
                        else -> MaterialTheme.colorScheme.surfaceContainer
                    },
                    textColor = when {
                        isIdleNow -> telemetryColors.countdownAmberText
                        isTracking -> telemetryColors.liveGreenText
                        else -> telemetryColors.mutedText
                    },
                    isPulsing = isTracking && !isIdleNow,
                )

                IconButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("logout_button"),
                ) {
                    Icon(
                        imageVector = AppIcons.Logout,
                        contentDescription = stringResource(R.string.cd_logout_icon),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            HorizontalDivider(color = telemetryColors.cardBorder)
        }
    }
}

@Composable
private fun ProgressRingHeaderCard(
    uiState: TrackerUiState,
    modifier: Modifier = Modifier,
) {
    val telemetryColors = LocalTelemetryColors.current
    val totalSyncs = uiState.liveSyncCount + uiState.historyLogCount
    val cycleSec = uiState.activeHistoryIntervalSec.coerceAtLeast(1)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, telemetryColors.cardBorder),
        shadowElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("progress_ring_header_card"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            LiquidWaveSecondsLoader(
                secondsRemaining = uiState.historyCountdownSec,
                cycleSeconds = cycleSec,
                isTracking = uiState.isTracking,
                isIdle = uiState.isStationaryIdle,
                size = 104.dp,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val isIdleNow = uiState.isTracking && uiState.isStationaryIdle
                Text(
                    text = if (isIdleNow) "Tracker Idle" else stringResource(R.string.header_cycle_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = when {
                        isIdleNow ->
                            stringResource(R.string.idle_header_subtitle)
                        uiState.isTracking && uiState.historySyncMode == HistorySyncMode.SPEED_ADAPTIVE ->
                            "Live every 2s · ${uiState.activeSpeedTier.label} (${cycleSec}s history)"
                        uiState.isTracking ->
                            stringResource(R.string.header_cycle_subtitle_active)
                        else ->
                            stringResource(R.string.header_cycle_subtitle_idle)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = telemetryColors.accentContainer,
                    ) {
                        Text(
                            text = "${stringResource(R.string.header_total_packets)}: $totalSyncs",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum",
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (uiState.isTracking) {
                            telemetryColors.liveGreenContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = AppIcons.Speed,
                                contentDescription = null,
                                tint = if (uiState.isTracking) {
                                    telemetryColors.liveGreenText
                                } else {
                                    telemetryColors.mutedText
                                },
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = "${uiState.currentSpeedKmh.roundToInt()} km/h",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum",
                                ),
                                color = if (uiState.isTracking) {
                                    telemetryColors.liveGreenText
                                } else {
                                    telemetryColors.mutedText
                                },
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.testTag("header_speed_pill"),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedRollingNumber(
    value: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            (slideInVertically(tween(220)) { -it / 2 } + fadeIn(tween(180)))
                .togetherWith(slideOutVertically(tween(180)) { it / 2 } + fadeOut(tween(140)))
        },
        label = "rollingCounter",
        modifier = modifier,
    ) { target ->
        Text(
            text = target,
            style = style,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun LiveCoordinatesCard(
    location: GpsLocation?,
    currentSpeedKmh: Float,
    activeSpeedTier: SpeedHistoryTier,
    isTracking: Boolean,
    isStationaryIdle: Boolean,
    isAcquiringInitialFix: Boolean,
    onCopyCoordinates: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val telemetryColors = LocalTelemetryColors.current
    val showShimmer = (isAcquiringInitialFix || isTracking) && location == null
    val isIdleNow = isTracking && isStationaryIdle

    val latFormatted = location?.let { String.format(Locale.US, "%.6f°", it.latitude) } ?: "--.------°"
    val lngFormatted = location?.let { String.format(Locale.US, "%.6f°", it.longitude) } ?: "--.------°"
    val accFormatted = location?.let { "±${it.accuracyMeters.roundToInt()}m" } ?: "±--m"
    val speedRounded = currentSpeedKmh.roundToInt()

    val (badgeText, badgeDot, badgeBg, badgeFg) = when {
        isIdleNow -> Quadruple(
            stringResource(R.string.status_tracking_idle),
            telemetryColors.countdownAmber,
            telemetryColors.countdownAmberContainer,
            telemetryColors.countdownAmberText,
        )
        isTracking -> Quadruple(
            stringResource(R.string.status_tracking_live),
            telemetryColors.liveGreen,
            telemetryColors.liveGreenContainer,
            telemetryColors.liveGreenText,
        )
        isAcquiringInitialFix -> Quadruple(
            stringResource(R.string.status_acquiring_gps),
            telemetryColors.countdownAmber,
            telemetryColors.countdownAmberContainer,
            telemetryColors.countdownAmberText,
        )
        else -> Quadruple(
            stringResource(R.string.status_standby),
            telemetryColors.mutedText,
            MaterialTheme.colorScheme.surfaceContainer,
            telemetryColors.mutedText,
        )
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, telemetryColors.cardBorder),
        shadowElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_gps_card"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = telemetryColors.accentContainer,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.section_live_gps),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (location != null) {
                        Surface(
                            onClick = { onCopyCoordinates(location.latitude, location.longitude) },
                            shape = CircleShape,
                            color = telemetryColors.accentContainer,
                            modifier = Modifier.testTag("copy_coordinates_button"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = AppIcons.ContentCopy,
                                    contentDescription = stringResource(R.string.cd_copy_coords),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = stringResource(R.string.action_copy_coords),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    StatusPillBadge(
                        label = badgeText,
                        dotColor = badgeDot,
                        bgColor = badgeBg,
                        textColor = badgeFg,
                        isPulsing = (isTracking && !isIdleNow) || isAcquiringInitialFix,
                    )
                }
            }

            AnimatedVisibility(
                visible = isIdleNow,
                enter = expandVertically(tween(220)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(150)),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = telemetryColors.countdownAmberContainer,
                    border = BorderStroke(1.dp, telemetryColors.countdownAmber.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stationary_idle_banner"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = AppIcons.PauseCircleOutline,
                            contentDescription = null,
                            tint = telemetryColors.countdownAmberText,
                            modifier = Modifier.size(17.dp),
                        )
                        Text(
                            text = stringResource(R.string.idle_card_banner),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = telemetryColors.countdownAmberText,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CoordinateTile(
                    label = stringResource(R.string.label_latitude),
                    value = latFormatted,
                    isLoading = showShimmer,
                    onClick = location?.let { loc -> { onCopyCoordinates(loc.latitude, loc.longitude) } },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("latitude_value"),
                )
                CoordinateTile(
                    label = stringResource(R.string.label_longitude),
                    value = lngFormatted,
                    isLoading = showShimmer,
                    onClick = location?.let { loc -> { onCopyCoordinates(loc.latitude, loc.longitude) } },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("longitude_value"),
                )
            }

            // Live Speed telemetry strip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, telemetryColors.cardBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = AppIcons.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.label_current_speed),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (isTracking) {
                                telemetryColors.accentContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                        ) {
                            Text(
                                text = activeSpeedTier.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = if (isTracking) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    telemetryColors.mutedText
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }

                    if (showShimmer) {
                        ShimmerBar(widthFraction = 0.25f, height = 16.dp)
                    } else {
                        AnimatedRollingNumber(
                            value = "$speedRounded km/h",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum",
                            ),
                            color = if (isTracking) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                telemetryColors.mutedText
                            },
                            modifier = Modifier.testTag("main_live_speed_value"),
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, telemetryColors.cardBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.label_accuracy),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        GpsSignalBars(accuracyMeters = location?.accuracyMeters)
                    }

                    if (showShimmer) {
                        ShimmerBar(widthFraction = 0.25f, height = 16.dp)
                    } else {
                        Text(
                            text = accFormatted,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum",
                            ),
                            color = if (location != null && location.accuracyMeters <= 25f) {
                                telemetryColors.liveGreenText
                            } else if (location != null) {
                                telemetryColors.countdownAmberText
                            } else {
                                telemetryColors.mutedText
                            },
                            modifier = Modifier.testTag("gps_accuracy_value"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoordinateTile(
    label: String,
    value: String,
    isLoading: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val telemetryColors = LocalTelemetryColors.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, telemetryColors.cardBorder),
        modifier = if (onClick != null) {
            modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
        } else {
            modifier
        },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = telemetryColors.mutedText,
            )
            if (isLoading) {
                ShimmerBar(widthFraction = 0.85f, height = 18.dp)
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFeatureSettings = "tnum",
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SyncStatusCardsRow(
    liveSyncCount: Int,
    lastLiveSyncTime: String?,
    isSendingCurrent: Boolean,
    historyLogCount: Int,
    lastHistorySyncTime: String?,
    historyCountdownSec: Int,
    activeHistoryIntervalSec: Int,
    isTracking: Boolean,
    isStationaryIdle: Boolean,
    modifier: Modifier = Modifier,
) {
    val telemetryColors = LocalTelemetryColors.current
    val isIdleNow = isTracking && isStationaryIdle
    val cycleSec = activeHistoryIntervalSec.coerceAtLeast(1)
    val countdownProgress by animateFloatAsState(
        targetValue = when {
            isIdleNow -> 0f
            isTracking -> (historyCountdownSec.coerceIn(0, cycleSec) / cycleSec.toFloat())
            else -> 1f
        },
        animationSpec = tween(durationMillis = 450),
        label = "historyCardBarProgress",
    )

    // Sync Heartbeat ripple glow whenever a Live Sync (2s) packet succeeds
    val syncHeartbeat = remember { Animatable(0f) }
    LaunchedEffect(liveSyncCount) {
        if (liveSyncCount > 0) {
            syncHeartbeat.snapTo(1f)
            syncHeartbeat.animateTo(0f, tween(durationMillis = 680, easing = FastOutSlowInEasing))
        }
    }

    val liveCardBorderColor = lerp(
        telemetryColors.cardBorder,
        telemetryColors.liveGreen,
        syncHeartbeat.value,
    )
    val liveCardBorderWidth = (1f + syncHeartbeat.value * 1.2f).dp

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(liveCardBorderWidth, liveCardBorderColor),
            shadowElevation = (3f + syncHeartbeat.value * 3f).dp,
            modifier = Modifier
                .weight(1f)
                .testTag("live_sync_card"),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isIdleNow) {
                            telemetryColors.countdownAmberContainer
                        } else {
                            telemetryColors.liveGreenContainer
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .scale(1f + syncHeartbeat.value * 0.08f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when {
                                    isIdleNow -> AppIcons.PauseCircleOutline
                                    isSendingCurrent -> AppIcons.CloudUpload
                                    else -> AppIcons.CloudDone
                                },
                                contentDescription = null,
                                tint = if (isIdleNow) {
                                    telemetryColors.countdownAmberText
                                } else {
                                    telemetryColors.liveGreenText
                                },
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    if (isIdleNow) {
                        Surface(
                            shape = CircleShape,
                            color = telemetryColors.countdownAmberContainer,
                        ) {
                            Text(
                                text = stringResource(R.string.status_tracking_idle),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = telemetryColors.countdownAmberText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    } else if (isSendingCurrent) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = telemetryColors.liveGreen,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.card_live_sync_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                AnimatedRollingNumber(
                    value = liveSyncCount.toString(),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("live_sync_count"),
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = when {
                            isIdleNow -> stringResource(R.string.idle_waiting_move)
                            isSendingCurrent -> stringResource(R.string.label_uploading)
                            else -> stringResource(R.string.label_last_sync)
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = telemetryColors.mutedText,
                    )
                    Text(
                        text = lastLiveSyncTime ?: stringResource(R.string.label_never_synced),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = if (isIdleNow) telemetryColors.countdownAmberText else telemetryColors.liveGreenText,
                        modifier = Modifier.testTag("live_sync_last_time"),
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, telemetryColors.cardBorder),
            shadowElevation = 3.dp,
            modifier = Modifier
                .weight(1f)
                .testTag("history_log_card"),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = telemetryColors.countdownAmberContainer,
                        modifier = Modifier.size(34.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isIdleNow) AppIcons.PauseCircleOutline else AppIcons.History,
                                contentDescription = null,
                                tint = telemetryColors.countdownAmberText,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = telemetryColors.countdownAmberContainer,
                    ) {
                        Text(
                            text = when {
                                isIdleNow -> stringResource(R.string.status_tracking_idle)
                                isTracking -> "${historyCountdownSec}s"
                                else -> "${cycleSec}s"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum",
                            ),
                            color = telemetryColors.countdownAmberText,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("history_countdown_badge"),
                        )
                    }
                }

                Text(
                    text = "History (${cycleSec}s)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                AnimatedRollingNumber(
                    value = historyLogCount.toString(),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("history_log_count"),
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isIdleNow) {
                                stringResource(R.string.idle_waiting_move)
                            } else {
                                stringResource(R.string.label_next_upload)
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = telemetryColors.mutedText,
                        )
                        Text(
                            text = lastHistorySyncTime ?: stringResource(R.string.label_never_synced),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFeatureSettings = "tnum",
                            ),
                            color = telemetryColors.countdownAmberText,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { countdownProgress },
                        color = telemetryColors.countdownAmber,
                        trackColor = telemetryColors.countdownAmberContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusTimelineCard(
    session: UserSession,
    uiState: TrackerUiState,
    modifier: Modifier = Modifier,
) {
    val telemetryColors = LocalTelemetryColors.current

    val steps = listOf(
        TimelineStage(
            title = stringResource(R.string.timeline_step_auth_title),
            subtitle = "${session.name} (${session.userId})",
            isCompleted = true,
            isActive = false,
            accentColor = telemetryColors.liveGreen,
            badgeText = "Verified",
            badgeBg = telemetryColors.liveGreenContainer,
            badgeFg = telemetryColors.liveGreenText,
        ),
        TimelineStage(
            title = stringResource(R.string.timeline_step_gps_title),
            subtitle = when {
                uiState.latestLocation != null ->
                    "Locked (±${uiState.latestLocation.accuracyMeters.roundToInt()}m)"
                uiState.isAcquiringInitialFix -> "Requesting high-accuracy fix…"
                else -> "Waiting to start tracking"
            },
            isCompleted = uiState.latestLocation != null,
            isActive = uiState.isAcquiringInitialFix || (uiState.isTracking && uiState.latestLocation == null),
            accentColor = if (uiState.latestLocation != null) {
                telemetryColors.liveGreen
            } else {
                telemetryColors.countdownAmber
            },
            badgeText = when {
                uiState.latestLocation != null -> "Locked"
                uiState.isAcquiringInitialFix -> "Locating"
                else -> "Idle"
            },
            badgeBg = when {
                uiState.latestLocation != null -> telemetryColors.liveGreenContainer
                uiState.isAcquiringInitialFix -> telemetryColors.countdownAmberContainer
                else -> MaterialTheme.colorScheme.surfaceContainer
            },
            badgeFg = when {
                uiState.latestLocation != null -> telemetryColors.liveGreenText
                uiState.isAcquiringInitialFix -> telemetryColors.countdownAmberText
                else -> telemetryColors.mutedText
            },
        ),
        TimelineStage(
            title = stringResource(R.string.timeline_step_live_title),
            subtitle = when {
                uiState.isTracking && uiState.isStationaryIdle ->
                    "Idle · Syncs automatically when moving"
                uiState.liveSyncCount > 0 ->
                    "${uiState.liveSyncCount} sent · Last ${uiState.lastLiveSyncTime ?: "--:--:--"}"
                uiState.isTracking ->
                    "Scheduler active (every 2s)"
                else -> "Paused"
            },
            isCompleted = uiState.liveSyncCount > 0,
            isActive = uiState.isTracking,
            accentColor = if (uiState.isTracking && uiState.isStationaryIdle) {
                telemetryColors.countdownAmber
            } else {
                MaterialTheme.colorScheme.primary
            },
            badgeText = when {
                uiState.isTracking && uiState.isStationaryIdle -> stringResource(R.string.status_tracking_idle)
                uiState.isTracking -> "Active"
                else -> "Standby"
            },
            badgeBg = when {
                uiState.isTracking && uiState.isStationaryIdle -> telemetryColors.countdownAmberContainer
                uiState.isTracking -> telemetryColors.accentContainer
                else -> MaterialTheme.colorScheme.surfaceContainer
            },
            badgeFg = when {
                uiState.isTracking && uiState.isStationaryIdle -> telemetryColors.countdownAmberText
                uiState.isTracking -> MaterialTheme.colorScheme.primary
                else -> telemetryColors.mutedText
            },
        ),
        TimelineStage(
            title = stringResource(R.string.timeline_step_history_title),
            subtitle = when {
                uiState.isTracking && uiState.isStationaryIdle ->
                    "Idle · Syncs when moving (${uiState.historyLogCount} logged)"
                uiState.isTracking ->
                    "${uiState.historyLogCount} logged · ${uiState.activeSpeedTier.label} (${uiState.activeHistoryIntervalSec}s)"
                uiState.historyLogCount > 0 ->
                    "${uiState.historyLogCount} logged · Last ${uiState.lastHistorySyncTime ?: "--:--:--"}"
                uiState.historySyncMode == HistorySyncMode.SPEED_ADAPTIVE ->
                    "Speed-adaptive (15s–120s) while moving"
                else ->
                    "Uploads every 60s while moving"
            },
            isCompleted = uiState.historyLogCount > 0,
            isActive = uiState.isTracking,
            accentColor = telemetryColors.countdownAmber,
            badgeText = when {
                uiState.isTracking && uiState.isStationaryIdle -> stringResource(R.string.status_tracking_idle)
                uiState.isTracking -> "${uiState.historyCountdownSec}s"
                else -> "${uiState.activeHistoryIntervalSec}s"
            },
            badgeBg = if (uiState.isTracking) {
                telemetryColors.countdownAmberContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
            badgeFg = if (uiState.isTracking) {
                telemetryColors.countdownAmberText
            } else {
                telemetryColors.mutedText
            },
        ),
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, telemetryColors.cardBorder),
        shadowElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("status_timeline_card"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.section_status_timeline),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Column {
                steps.forEachIndexed { index, step ->
                    val isLast = index == steps.lastIndex
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(22.dp),
                        ) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                shape = CircleShape,
                                color = when {
                                    step.isCompleted -> step.accentColor
                                    step.isActive -> step.accentColor.copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                border = if (step.isActive && !step.isCompleted) {
                                    BorderStroke(2.dp, step.accentColor)
                                } else {
                                    null
                                },
                                modifier = Modifier.size(20.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (step.isCompleted) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp),
                                        )
                                    } else if (step.isActive) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(step.accentColor),
                                        )
                                    }
                                }
                            }
                            if (!isLast) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(34.dp)
                                        .background(
                                            if (step.isCompleted) {
                                                step.accentColor.copy(alpha = 0.35f)
                                            } else {
                                                telemetryColors.cardBorder
                                            },
                                        ),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = if (isLast) 2.dp else 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = step.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = step.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = CircleShape,
                                color = step.badgeBg,
                            ) {
                                Text(
                                    text = step.badgeText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = step.badgeFg,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPillsBar(
    selectedFilter: LogFilter,
    onSelectFilter: (LogFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val telemetryColors = LocalTelemetryColors.current
    val filters = listOf(
        LogFilter.ALL to stringResource(R.string.filter_all),
        LogFilter.LIVE to stringResource(R.string.filter_live),
        LogFilter.HISTORY to stringResource(R.string.filter_history),
        LogFilter.ERRORS to stringResource(R.string.filter_alerts),
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag("filter_pills_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        filters.forEach { (filter, label) ->
            val isSelected = selectedFilter == filter
            val pillBg by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                },
                animationSpec = tween(220),
                label = "pillBg_${filter.name}",
            )
            val pillScale by animateFloatAsState(
                targetValue = if (isSelected) 1.03f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "pillScale_${filter.name}",
            )

            Surface(
                onClick = { onSelectFilter(filter) },
                shape = CircleShape,
                color = pillBg,
                border = if (isSelected) null else BorderStroke(1.dp, telemetryColors.cardBorder),
                shadowElevation = if (isSelected) 2.dp else 1.dp,
                modifier = Modifier
                    .height(38.dp)
                    .scale(pillScale)
                    .testTag("filter_pill_${filter.name.lowercase(Locale.US)}"),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = if (isSelected) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentEntriesCard(
    logs: List<SyncLogEntry>,
    hasAnyLogs: Boolean,
    maxItems: Int,
    onCopyCoordinates: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val telemetryColors = LocalTelemetryColors.current
    var expandedEntryId by remember { mutableStateOf<Long?>(null) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, telemetryColors.cardBorder),
        shadowElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("recent_entries_card"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.section_recent_activity),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${logs.size}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = telemetryColors.mutedText,
                )
            }

            if (logs.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = AppIcons.Schedule,
                            contentDescription = null,
                            tint = telemetryColors.mutedText,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = if (hasAnyLogs) {
                                stringResource(R.string.empty_filtered_log)
                            } else {
                                stringResource(R.string.empty_activity_log)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                logs.take(maxItems).forEachIndexed { index, log ->
                    if (index > 0) {
                        HorizontalDivider(color = telemetryColors.cardBorder)
                    }
                    val isExpanded = expandedEntryId == log.id
                    val chevronRotation by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = tween(220),
                        label = "chevronRot_${log.id}",
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                expandedEntryId = if (isExpanded) null else log.id
                            }
                            .padding(vertical = 6.dp, horizontal = 2.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = if (log.success) {
                                    Icons.Filled.CheckCircle
                                } else {
                                    AppIcons.ErrorOutline
                                },
                                contentDescription = null,
                                tint = when {
                                    !log.success -> telemetryColors.stopRed
                                    log.type == SyncType.HISTORY -> telemetryColors.countdownAmber
                                    else -> telemetryColors.liveGreen
                                },
                                modifier = Modifier.size(18.dp),
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when {
                                    !log.success -> telemetryColors.stopRedContainer
                                    log.type == SyncType.HISTORY -> telemetryColors.countdownAmberContainer
                                    else -> telemetryColors.liveGreenContainer
                                },
                            ) {
                                Text(
                                    text = if (log.type == SyncType.HISTORY) "HIST" else "LIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = when {
                                        !log.success -> telemetryColors.stopRedText
                                        log.type == SyncType.HISTORY -> telemetryColors.countdownAmberText
                                        else -> telemetryColors.liveGreenText
                                    },
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                )
                            }

                            Text(
                                text = log.message,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )

                            Text(
                                text = log.timestampFormatted,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFeatureSettings = "tnum",
                                ),
                                color = telemetryColors.mutedText,
                            )

                            Icon(
                                imageVector = AppIcons.ExpandMore,
                                contentDescription = stringResource(R.string.cd_expand_entry),
                                tint = telemetryColors.mutedText,
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(chevronRotation),
                            )
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically(tween(200)) + fadeIn(tween(180)),
                            exit = shrinkVertically(tween(180)) + fadeOut(tween(140)),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.background,
                                border = BorderStroke(1.dp, telemetryColors.cardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        val coordLine = if (log.latitude != null && log.longitude != null) {
                                            String.format(Locale.US, "%.6f°, %.6f°", log.latitude, log.longitude)
                                        } else {
                                            "No coordinate snapshot"
                                        }
                                        Text(
                                            text = coordLine,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontFeatureSettings = "tnum",
                                            ),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        val detailMeta = buildString {
                                            append("Payload: ")
                                            append(if (log.type == SyncType.HISTORY) "history" else "current")
                                            if (log.speedKmh != null) {
                                                append(" · ${log.speedKmh.roundToInt()} km/h")
                                            }
                                            if (log.accuracyMeters != null) {
                                                append(" · ±${log.accuracyMeters.roundToInt()}m")
                                            }
                                        }
                                        Text(
                                            text = detailMeta,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }

                                    if (log.latitude != null && log.longitude != null) {
                                        Surface(
                                            onClick = { onCopyCoordinates(log.latitude, log.longitude) },
                                            shape = CircleShape,
                                            color = telemetryColors.accentContainer,
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                Icon(
                                                    imageVector = AppIcons.ContentCopy,
                                                    contentDescription = stringResource(R.string.cd_copy_coords),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(12.dp),
                                                )
                                                Text(
                                                    text = stringResource(R.string.action_copy_coords),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackingErrorBanner(
    errorMessage: String?,
    isPermissionError: Boolean,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val telemetryColors = LocalTelemetryColors.current

    AnimatedVisibility(
        visible = errorMessage != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier.fillMaxWidth(),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = telemetryColors.stopRedContainer,
            border = BorderStroke(1.dp, telemetryColors.stopRed.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tracking_error_alert"),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = AppIcons.ErrorOutline,
                        contentDescription = null,
                        tint = telemetryColors.stopRed,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(20.dp),
                    )
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = telemetryColors.stopRedText,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_dismiss_error),
                            tint = telemetryColors.stopRedText,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                if (isPermissionError) {
                    OutlinedButton(
                        onClick = onOpenSettings,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, telemetryColors.stopRed.copy(alpha = 0.45f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                            tint = telemetryColors.stopRedText,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_open_settings),
                            style = MaterialTheme.typography.labelMedium,
                            color = telemetryColors.stopRedText,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThumbZoneActionBar(
    isTracking: Boolean,
    isAcquiringInitialFix: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
) {
    val telemetryColors = LocalTelemetryColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "ctaPressScale",
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            HorizontalDivider(color = telemetryColors.cardBorder)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = isTracking,
                    transitionSpec = {
                        fadeIn(tween(220)).togetherWith(fadeOut(tween(180)))
                    },
                    label = "thumbActionTransition",
                ) { trackingActive ->
                    if (trackingActive) {
                        SlideToStopTrack(
                            onStopConfirmed = onStopClick,
                            modifier = Modifier.widthIn(max = 560.dp),
                        )
                    } else {
                        Button(
                            onClick = onStartClick,
                            enabled = !isAcquiringInitialFix,
                            interactionSource = interactionSource,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 560.dp)
                                .height(56.dp)
                                .scale(pressScale)
                                .testTag("start_tracking_button"),
                        ) {
                            if (isAcquiringInitialFix) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.5.dp,
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.action_starting_tracking),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    maxLines = 1,
                                )
                            } else {
                                Icon(
                                    imageVector = AppIcons.RadioButtonChecked,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.action_start_tracking),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimalBottomNavigationBar(
    selectedTab: TrackerNavTab,
    onSelectTab: (TrackerNavTab) -> Unit,
) {
    val telemetryColors = LocalTelemetryColors.current

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = telemetryColors.cardBorder)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets.navigationBars,
        ) {
            NavigationBarItem(
                selected = selectedTab == TrackerNavTab.TRACKER,
                onClick = { onSelectTab(TrackerNavTab.TRACKER) },
                icon = {
                    Icon(
                        imageVector = if (selectedTab == TrackerNavTab.TRACKER) {
                            AppIcons.GpsFixed
                        } else {
                            AppIcons.GpsFixedOutlined
                        },
                        contentDescription = stringResource(R.string.nav_tracker),
                    )
                },
                label = {
                    Text(
                        text = stringResource(R.string.nav_tracker),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = telemetryColors.accentContainer,
                    unselectedIconColor = telemetryColors.mutedText,
                    unselectedTextColor = telemetryColors.mutedText,
                ),
                modifier = Modifier.testTag("nav_tab_tracker"),
            )

            NavigationBarItem(
                selected = selectedTab == TrackerNavTab.ACTIVITY,
                onClick = { onSelectTab(TrackerNavTab.ACTIVITY) },
                icon = {
                    Icon(
                        imageVector = AppIcons.Timeline,
                        contentDescription = stringResource(R.string.nav_activity),
                    )
                },
                label = {
                    Text(
                        text = stringResource(R.string.nav_activity),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = telemetryColors.accentContainer,
                    unselectedIconColor = telemetryColors.mutedText,
                    unselectedTextColor = telemetryColors.mutedText,
                ),
                modifier = Modifier.testTag("nav_tab_activity"),
            )

            NavigationBarItem(
                selected = selectedTab == TrackerNavTab.ACCOUNT,
                onClick = { onSelectTab(TrackerNavTab.ACCOUNT) },
                icon = {
                    Icon(
                        imageVector = if (selectedTab == TrackerNavTab.ACCOUNT) {
                            Icons.Filled.Person
                        } else {
                            Icons.Outlined.Person
                        },
                        contentDescription = stringResource(R.string.nav_account),
                    )
                },
                label = {
                    Text(
                        text = stringResource(R.string.nav_account),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = telemetryColors.accentContainer,
                    unselectedIconColor = telemetryColors.mutedText,
                    unselectedTextColor = telemetryColors.mutedText,
                ),
                modifier = Modifier.testTag("nav_tab_account"),
            )
        }
    }
}

@Composable
private fun StatusPillBadge(
    label: String,
    dotColor: Color,
    bgColor: Color,
    textColor: Color,
    isPulsing: Boolean,
) {
    val alpha = if (isPulsing) {
        val transition = rememberInfiniteTransition(label = "statusDotPulse")
        val animated by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(850, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "statusDotAlpha",
        )
        animated
    } else {
        1f
    }

    Surface(shape = CircleShape, color = bgColor) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = textColor,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

private data class TimelineStage(
    val title: String,
    val subtitle: String,
    val isCompleted: Boolean,
    val isActive: Boolean,
    val accentColor: Color,
    val badgeText: String,
    val badgeBg: Color,
    val badgeFg: Color,
)

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun BackgroundProtectionCard(
    hasBackgroundLocation: Boolean,
    isBatteryUnrestricted: Boolean,
    onRequestBackgroundLocation: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val telemetryColors = LocalTelemetryColors.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = telemetryColors.countdownAmberContainer,
        border = BorderStroke(1.dp, telemetryColors.countdownAmber.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("background_protection_card"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = AppIcons.WarningAmber,
                    contentDescription = null,
                    tint = telemetryColors.countdownAmberText,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.bg_guard_title),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = telemetryColors.countdownAmberText,
                )
            }

            Text(
                text = stringResource(R.string.bg_guard_desc),
                style = MaterialTheme.typography.bodySmall,
                color = telemetryColors.countdownAmberText.copy(alpha = 0.9f),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!hasBackgroundLocation) {
                    Button(
                        onClick = onRequestBackgroundLocation,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = telemetryColors.countdownAmberText,
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("allow_background_location_button"),
                    ) {
                        Text(
                            text = stringResource(R.string.bg_guard_allow_always),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (!isBatteryUnrestricted) {
                    OutlinedButton(
                        onClick = onOpenBatterySettings,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, telemetryColors.countdownAmberText.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = telemetryColors.countdownAmberText,
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("unrestrict_battery_button"),
                    ) {
                        Text(
                            text = stringResource(R.string.bg_guard_battery_btn),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { startActivity(intent) }
}

private fun Context.openBatteryOptimizationSettings() {
    val batteryIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val opened = runCatching {
        startActivity(batteryIntent)
        true
    }.getOrDefault(false)
    if (!opened) {
        openAppSettings()
    }
}
