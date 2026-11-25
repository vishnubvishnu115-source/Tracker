package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timeline
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppAccentTheme
import com.example.data.model.AppThemeMode
import com.example.data.model.GpsLocation
import com.example.data.model.SyncLogEntry
import com.example.data.model.SyncType
import com.example.data.model.UserSession
import com.example.ui.TrackerUiState
import com.example.ui.components.AccountTabContent
import com.example.ui.components.ProfileAvatar
import com.example.ui.theme.JetBrainsMonoFontFamily
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
    onPickProfileImage: (Uri) -> Unit,
    onRemoveProfileImage: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val session = uiState.session ?: return

    var selectedTab by rememberSaveable { mutableStateOf(TrackerNavTab.TRACKER) }
    var selectedFilter by rememberSaveable { mutableStateOf(LogFilter.ALL) }

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
        if (fineGranted || coarseGranted) {
            onStartTracking()
        } else {
            onPermissionDenied()
        }
    }

    val handleStartRequest: () -> Unit = {
        if (hasLocationPermission()) {
            onStartTracking()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CleanTopAppBar(
                session = session,
                profileImagePath = uiState.profileImagePath,
                profileImageVersion = uiState.profileImageVersion,
                isTracking = uiState.isTracking,
                isWakeLockActive = uiState.isWakeLockActive,
                onAvatarClick = { selectedTab = TrackerNavTab.ACCOUNT },
                onLogout = onLogout,
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ThumbZoneActionBar(
                    isTracking = uiState.isTracking,
                    isAcquiringInitialFix = uiState.isAcquiringInitialFix,
                    onStartClick = handleStartRequest,
                    onStopClick = onStopTracking,
                )

                MinimalBottomNavigationBar(
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                )
            }
        },
    ) { innerPadding ->
        // Smooth directional horizontal slide + fade transition between tabs
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
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
                                    isTracking = uiState.isTracking,
                                    isAcquiringInitialFix = uiState.isAcquiringInitialFix,
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
                                    isTracking = uiState.isTracking,
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
                                    hasPermission = hasLocationPermission(),
                                    isWakeLockActive = uiState.isWakeLockActive,
                                    onSelectThemeMode = onSelectThemeMode,
                                    onSelectAccentTheme = onSelectAccentTheme,
                                    onPickProfileImage = onPickProfileImage,
                                    onRemoveProfileImage = onRemoveProfileImage,
                                    onOpenSettings = { context.openAppSettings() },
                                    onLogout = onLogout,
                                    modifier = Modifier.widthIn(max = 560.dp),
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
                                fontFamily = JetBrainsMonoFontFamily,
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
                                        Icons.Filled.WbSunny
                                    } else {
                                        Icons.Filled.ScreenLockPortrait
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

                StatusPillBadge(
                    label = if (isTracking) {
                        stringResource(R.string.status_tracking_live)
                    } else {
                        stringResource(R.string.status_standby)
                    },
                    dotColor = if (isTracking) telemetryColors.liveGreen else telemetryColors.mutedText,
                    bgColor = if (isTracking) {
                        telemetryColors.liveGreenContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    textColor = if (isTracking) {
                        telemetryColors.liveGreenText
                    } else {
                        telemetryColors.mutedText
                    },
                    isPulsing = isTracking,
                )

                IconButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("logout_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
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

    val targetFraction = if (uiState.isTracking) {
        ((60 - uiState.historyCountdownSec.coerceIn(0, 60)) / 60f).coerceIn(0.04f, 1f)
    } else {
        0f
    }
    val animatedSweep by animateFloatAsState(
        targetValue = targetFraction * 360f,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "progressRingSweep",
    )

    val ringPulse = rememberInfiniteTransition(label = "ringPulseGlow")
    val glowAlpha by ringPulse.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ringGlowAlpha",
    )

    val ringColor = if (uiState.isTracking) {
        MaterialTheme.colorScheme.primary
    } else {
        telemetryColors.cardBorder
    }
    val trackColor = telemetryColors.accentContainer

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
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(94.dp),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 9.dp.toPx()
                    val inset = strokeWidth / 2f + 2.dp.toPx()
                    val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                    val topLeft = Offset(inset, inset)

                    if (uiState.isTracking) {
                        drawCircle(
                            color = ringColor.copy(alpha = glowAlpha),
                            radius = size.minDimension / 2f,
                        )
                    }

                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )

                    if (animatedSweep > 0f) {
                        drawArc(
                            color = ringColor,
                            startAngle = -90f,
                            sweepAngle = animatedSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedRollingNumber(
                        value = if (uiState.isTracking) "${uiState.historyCountdownSec}s" else "60s",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("progress_ring_countdown"),
                    )
                    Text(
                        text = "1m cycle",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = telemetryColors.mutedText,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.header_cycle_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (uiState.isTracking) {
                        stringResource(R.string.header_cycle_subtitle_active)
                    } else {
                        stringResource(R.string.header_cycle_subtitle_idle)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = telemetryColors.accentContainer,
                    ) {
                        Text(
                            text = "${stringResource(R.string.header_total_packets)}: $totalSyncs",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }

                    if (uiState.latestLocation != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (uiState.latestLocation.accuracyMeters <= 25f) {
                                telemetryColors.liveGreenContainer
                            } else {
                                telemetryColors.countdownAmberContainer
                            },
                        ) {
                            Text(
                                text = "±${uiState.latestLocation.accuracyMeters.roundToInt()}m",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = if (uiState.latestLocation.accuracyMeters <= 25f) {
                                    telemetryColors.liveGreenText
                                } else {
                                    telemetryColors.countdownAmberText
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
    isTracking: Boolean,
    isAcquiringInitialFix: Boolean,
    modifier: Modifier = Modifier,
) {
    val telemetryColors = LocalTelemetryColors.current

    val latFormatted = location?.let { String.format(Locale.US, "%.6f°", it.latitude) } ?: "--.------°"
    val lngFormatted = location?.let { String.format(Locale.US, "%.6f°", it.longitude) } ?: "--.------°"
    val accFormatted = location?.let { "±${it.accuracyMeters.roundToInt()}m" } ?: "±--m"

    val (badgeText, badgeDot, badgeBg, badgeFg) = when {
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

                StatusPillBadge(
                    label = badgeText,
                    dotColor = badgeDot,
                    bgColor = badgeBg,
                    textColor = badgeFg,
                    isPulsing = isTracking || isAcquiringInitialFix,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CoordinateTile(
                    label = stringResource(R.string.label_latitude),
                    value = latFormatted,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("latitude_value"),
                )
                CoordinateTile(
                    label = stringResource(R.string.label_longitude),
                    value = lngFormatted,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("longitude_value"),
                )
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
                    Text(
                        text = stringResource(R.string.label_accuracy),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = accFormatted,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
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

@Composable
private fun CoordinateTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val telemetryColors = LocalTelemetryColors.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, telemetryColors.cardBorder),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = telemetryColors.mutedText,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
    isTracking: Boolean,
    modifier: Modifier = Modifier,
) {
    val telemetryColors = LocalTelemetryColors.current
    val countdownProgress by animateFloatAsState(
        targetValue = if (isTracking) (historyCountdownSec.coerceIn(0, 60) / 60f) else 1f,
        animationSpec = tween(durationMillis = 450),
        label = "historyCardBarProgress",
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, telemetryColors.cardBorder),
            shadowElevation = 3.dp,
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
                        color = telemetryColors.liveGreenContainer,
                        modifier = Modifier.size(34.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSendingCurrent) {
                                    Icons.Filled.CloudUpload
                                } else {
                                    Icons.Filled.CloudDone
                                },
                                contentDescription = null,
                                tint = telemetryColors.liveGreenText,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    if (isSendingCurrent) {
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
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("live_sync_count"),
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isSendingCurrent) {
                            stringResource(R.string.label_uploading)
                        } else {
                            stringResource(R.string.label_last_sync)
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = telemetryColors.mutedText,
                    )
                    Text(
                        text = lastLiveSyncTime ?: stringResource(R.string.label_never_synced),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = telemetryColors.liveGreenText,
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
                                imageVector = Icons.Filled.History,
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
                            text = if (isTracking) "${historyCountdownSec}s" else "60s",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = telemetryColors.countdownAmberText,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("history_countdown_badge"),
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.card_history_log_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                AnimatedRollingNumber(
                    value = historyLogCount.toString(),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
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
                            text = stringResource(R.string.label_next_upload),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = telemetryColors.mutedText,
                        )
                        Text(
                            text = lastHistorySyncTime ?: stringResource(R.string.label_never_synced),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
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
            subtitle = if (uiState.liveSyncCount > 0) {
                "${uiState.liveSyncCount} sent · Last ${uiState.lastLiveSyncTime ?: "--:--:--"}"
            } else if (uiState.isTracking) {
                "Scheduler active (every 2s)"
            } else {
                "Paused"
            },
            isCompleted = uiState.liveSyncCount > 0,
            isActive = uiState.isTracking,
            accentColor = MaterialTheme.colorScheme.primary,
            badgeText = if (uiState.isTracking) "Active" else "Standby",
            badgeBg = if (uiState.isTracking) {
                telemetryColors.accentContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
            badgeFg = if (uiState.isTracking) {
                MaterialTheme.colorScheme.primary
            } else {
                telemetryColors.mutedText
            },
        ),
        TimelineStage(
            title = stringResource(R.string.timeline_step_history_title),
            subtitle = if (uiState.isTracking) {
                "${uiState.historyLogCount} logged · Next in ${uiState.historyCountdownSec}s"
            } else if (uiState.historyLogCount > 0) {
                "${uiState.historyLogCount} logged · Last ${uiState.lastHistorySyncTime ?: "--:--:--"}"
            } else {
                "Uploads every 60s while tracking"
            },
            isCompleted = uiState.historyLogCount > 0,
            isActive = uiState.isTracking,
            accentColor = telemetryColors.countdownAmber,
            badgeText = if (uiState.isTracking) "${uiState.historyCountdownSec}s" else "1m",
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
    modifier: Modifier = Modifier,
) {
    val telemetryColors = LocalTelemetryColors.current

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
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
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
                            imageVector = Icons.Filled.Schedule,
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = if (log.success) {
                                Icons.Filled.CheckCircle
                            } else {
                                Icons.Filled.ErrorOutline
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
                                    fontFamily = JetBrainsMonoFontFamily,
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
                                fontFamily = JetBrainsMonoFontFamily,
                            ),
                            color = telemetryColors.mutedText,
                        )
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
                        imageVector = Icons.Filled.ErrorOutline,
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
    val buttonColor by animateColorAsState(
        targetValue = if (isTracking) telemetryColors.stopRed else MaterialTheme.colorScheme.primary,
        animationSpec = tween(260),
        label = "ctaColorMorph",
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
                Button(
                    onClick = if (isTracking) onStopClick else onStartClick,
                    enabled = isTracking || !isAcquiringInitialFix,
                    interactionSource = interactionSource,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                        .height(52.dp)
                        .scale(pressScale)
                        .testTag(if (isTracking) "stop_tracking_button" else "start_tracking_button"),
                ) {
                    if (isTracking) {
                        Icon(
                            imageVector = Icons.Filled.StopCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_stop_tracking),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            ),
                            maxLines = 1,
                        )
                    } else if (isAcquiringInitialFix) {
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
                            imageVector = Icons.Filled.RadioButtonChecked,
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
                            Icons.Filled.GpsFixed
                        } else {
                            Icons.Outlined.GpsFixed
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
                        imageVector = if (selectedTab == TrackerNavTab.ACTIVITY) {
                            Icons.Filled.Timeline
                        } else {
                            Icons.Outlined.Timeline
                        },
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

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { startActivity(intent) }
}
