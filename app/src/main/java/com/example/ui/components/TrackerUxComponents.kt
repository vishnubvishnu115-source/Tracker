package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LocalTelemetryColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Swipe-to-stop safety slider that prevents accidental pocket taps while streaming live GPS.
 * Includes spring rebound, animated chevron trail, haptic feedback, and TalkBack semantics.
 */
@Composable
fun SlideToStopTrack(
    onStopConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val telemetryColors = LocalTelemetryColors.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val thumbSize = 46.dp
    val trackPadding = 5.dp
    val dragOffsetPx = remember { Animatable(0f) }
    var triggered by remember { mutableStateOf(false) }

    val chevronTransition = rememberInfiniteTransition(label = "slideChevronShimmer")
    val chevronAlpha by chevronTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "chevronAlpha",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(telemetryColors.stopRed)
            .semantics {
                onClick(label = "Stop Tracking") {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStopConfirmed()
                    true
                }
            }
            .testTag("stop_tracking_button"),
    ) {
        val maxDragPx = with(density) {
            (maxWidth - thumbSize - trackPadding * 2).toPx().coerceAtLeast(1f)
        }
        val progress = (dragOffsetPx.value / maxDragPx).coerceIn(0f, 1f)
        val isReadyToRelease = progress >= 0.80f

        // Subtle brighter fill trailing behind the dragged thumb
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = (progress + 0.16f).coerceIn(0f, 1f))
                .background(Color.White.copy(alpha = 0.14f)),
        )

        // Centered prompt label + animated chevrons
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 36.dp, end = 14.dp)
                .alpha((1f - progress * 1.15f).coerceIn(0.2f, 1f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = if (isReadyToRelease) {
                    stringResource(R.string.action_release_to_stop)
                } else {
                    stringResource(R.string.action_slide_to_stop)
                },
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!isReadyToRelease) {
                Row(modifier = Modifier.alpha(chevronAlpha)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier
                            .offset(x = (-8).dp)
                            .size(18.dp),
                    )
                }
            }
        }

        // Draggable white stop thumb
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = trackPadding)
                .offset { IntOffset(dragOffsetPx.value.roundToInt(), 0) }
                .size(thumbSize)
                .pointerInput(maxDragPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (!triggered && dragOffsetPx.value >= maxDragPx * 0.80f) {
                                triggered = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    dragOffsetPx.animateTo(maxDragPx, tween(120))
                                    onStopConfirmed()
                                }
                            } else {
                                scope.launch {
                                    dragOffsetPx.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium,
                                        ),
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                dragOffsetPx.animateTo(0f, spring())
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            if (!triggered) {
                                val next = (dragOffsetPx.value + dragAmount).coerceIn(0f, maxDragPx)
                                scope.launch { dragOffsetPx.snapTo(next) }
                            }
                        },
                    )
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Stop,
                    contentDescription = stringResource(R.string.action_stop_tracking),
                    tint = telemetryColors.stopRed,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/**
 * 4-bar GPS Signal Quality indicator derived from live accuracy in meters.
 */
@Composable
fun GpsSignalBars(
    accuracyMeters: Float?,
    modifier: Modifier = Modifier,
) {
    val telemetryColors = LocalTelemetryColors.current

    val (activeBars, barColor, qualityLabel) = when {
        accuracyMeters == null -> Triple(0, telemetryColors.mutedText, stringResource(R.string.signal_no_fix))
        accuracyMeters <= 10f -> Triple(4, telemetryColors.liveGreen, stringResource(R.string.signal_excellent))
        accuracyMeters <= 25f -> Triple(3, telemetryColors.liveGreen, stringResource(R.string.signal_good))
        accuracyMeters <= 50f -> Triple(2, telemetryColors.countdownAmber, stringResource(R.string.signal_fair))
        else -> Triple(1, telemetryColors.stopRed, stringResource(R.string.signal_weak))
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
            modifier = Modifier.height(14.dp),
        ) {
            val heights = listOf(5.dp, 8.dp, 11.dp, 14.dp)
            heights.forEachIndexed { idx, height ->
                val isLit = idx < activeBars
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(height)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isLit) barColor else telemetryColors.cardBorder,
                        ),
                )
            }
        }
        Text(
            text = qualityLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.5.sp,
            ),
            color = if (activeBars > 0) barColor else telemetryColors.mutedText,
            maxLines = 1,
        )
    }
}

/**
 * Animated horizontal shimmer bar used while acquiring initial GPS coordinates.
 */
@Composable
fun ShimmerBar(
    widthFraction: Float = 0.75f,
    height: Dp = 18.dp,
    modifier: Modifier = Modifier,
) {
    val baseColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlightColor = MaterialTheme.colorScheme.surface

    val transition = rememberInfiniteTransition(label = "shimmerSweep")
    val translate by transition.animateFloat(
        initialValue = -300f,
        targetValue = 700f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )

    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(baseColor, highlightColor, baseColor),
                    start = Offset(translate, 0f),
                    end = Offset(translate + 240f, 0f),
                ),
            ),
    )
}

/**
 * Floating pill toast for clipboard copy and tracking state feedback.
 */
@Composable
fun FloatingToastPill(
    message: String?,
    modifier: Modifier = Modifier,
) {
    val telemetryColors = LocalTelemetryColors.current

    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
            initialOffsetY = { -it * 2 },
        ) + fadeIn(tween(180)),
        exit = slideOutVertically(
            animationSpec = tween(200),
            targetOffsetY = { -it * 2 },
        ) + fadeOut(tween(180)),
        modifier = modifier,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 8.dp,
            modifier = Modifier.testTag("floating_toast_pill"),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = telemetryColors.liveGreen,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = message.orEmpty(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.surface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Material 3 confirmation bottom sheet preventing accidental sign-out during active GPS tracking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogoutConfirmationSheet(
    isTracking: Boolean,
    onDismiss: () -> Unit,
    onConfirmLogout: () -> Unit,
) {
    val telemetryColors = LocalTelemetryColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isTracking) {
                        telemetryColors.countdownAmberContainer
                    } else {
                        telemetryColors.stopRedContainer
                    },
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isTracking) {
                                Icons.Filled.WarningAmber
                            } else {
                                Icons.AutoMirrored.Filled.Logout
                            },
                            contentDescription = null,
                            tint = if (isTracking) {
                                telemetryColors.countdownAmberText
                            } else {
                                telemetryColors.stopRedText
                            },
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isTracking) {
                            stringResource(R.string.logout_sheet_title_active)
                        } else {
                            stringResource(R.string.logout_sheet_title_idle)
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Text(
                text = if (isTracking) {
                    stringResource(R.string.logout_sheet_desc_active)
                } else {
                    stringResource(R.string.logout_sheet_desc_idle)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("logout_cancel_button"),
                ) {
                    Text(
                        text = stringResource(R.string.logout_sheet_cancel),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Button(
                    onClick = onConfirmLogout,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = telemetryColors.stopRed,
                        contentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("logout_confirm_button"),
                ) {
                    Text(
                        text = if (isTracking) {
                            stringResource(R.string.logout_sheet_confirm_active)
                        } else {
                            stringResource(R.string.logout_button)
                        },
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Branded animated launch splash displayed while DataStore loads the saved operator session.
 */
@Composable
fun BrandedLaunchSplash(
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val pulseTransition = rememberInfiniteTransition(label = "splashBeaconPulse")
    val ring1 by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "splashRing1",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(132.dp),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxR = size.minDimension / 2f
                    drawCircle(
                        color = primary.copy(alpha = (1f - ring1) * 0.28f),
                        radius = (30.dp.toPx()) + (maxR - 30.dp.toPx()) * ring1,
                    )
                    val secondPhase = (ring1 + 0.5f) % 1f
                    drawCircle(
                        color = primary.copy(alpha = (1f - secondPhase) * 0.22f),
                        radius = (30.dp.toPx()) + (maxR - 30.dp.toPx()) * secondPhase,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = primary,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(68.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.GpsFixed,
                            contentDescription = stringResource(R.string.cd_brand_logo),
                            tint = Color.White,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.brand_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
