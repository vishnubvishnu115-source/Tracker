package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LocalTelemetryColors
import kotlin.math.PI
import kotlin.math.sin

/**
 * Circular liquid-wave countdown loader inspired by a glass water-fill timer.
 * Automatically adapts its vessel fill, dual animated waves, outline ring, and
 * text contrast to Light/Dark mode, all 5 TrackPulse accent themes, and the calm
 * stationary Idle state when no movement is detected.
 */
@Composable
fun LiquidWaveSecondsLoader(
    secondsRemaining: Int,
    cycleSeconds: Int,
    isTracking: Boolean,
    isIdle: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 104.dp,
) {
    val telemetryColors = LocalTelemetryColors.current
    val safeCycle = cycleSeconds.coerceAtLeast(1)
    val clampedRemaining = secondsRemaining.coerceIn(0, safeCycle)
    val showIdleVisual = isTracking && isIdle

    // Map countdown progress smoothly into visible liquid fill height (14% -> 94%)
    val targetFill = when {
        showIdleVisual -> 0.36f
        isTracking -> {
            val elapsedFraction = (safeCycle - clampedRemaining) / safeCycle.toFloat()
            0.14f + (elapsedFraction * 0.80f)
        }
        else -> 0.30f
    }

    val animatedFill by animateFloatAsState(
        targetValue = targetFill,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "liquidFillLevel",
    )

    val waveTransition = rememberInfiniteTransition(label = "liquidWaveMotion")
    val frontPhase by waveTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isTracking && !isIdle) 2200 else 3800,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "frontWavePhase",
    )
    val backPhase by waveTransition.animateFloat(
        initialValue = (2.0 * PI).toFloat(),
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isTracking && !isIdle) 3100 else 5000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "backWavePhase",
    )

    val activeAccent by animateColorAsState(
        targetValue = if (showIdleVisual) {
            telemetryColors.countdownAmber
        } else {
            MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 360),
        label = "waveAccentColor",
    )
    val vesselBgColor by animateColorAsState(
        targetValue = if (showIdleVisual) {
            telemetryColors.countdownAmberContainer.copy(alpha = 0.55f)
        } else {
            telemetryColors.accentContainer.copy(alpha = 0.42f)
        },
        animationSpec = tween(durationMillis = 360),
        label = "vesselBgColor",
    )

    val backWaveColor = activeAccent.copy(alpha = if (isTracking && !isIdle) 0.34f else 0.24f)
    val frontWaveTopColor = activeAccent.copy(alpha = if (isTracking && !isIdle) 0.86f else 0.68f)
    val frontWaveBottomColor = activeAccent.copy(alpha = if (isTracking && !isIdle) 1.0f else 0.84f)
    val ringBorderColor = when {
        showIdleVisual -> telemetryColors.countdownAmber.copy(alpha = 0.7f)
        isTracking -> MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
        else -> telemetryColors.cardBorder
    }

    // Automatically ensure high text contrast as the liquid wave rises behind each label
    val countdownTextColor by animateColorAsState(
        targetValue = when {
            showIdleVisual -> telemetryColors.countdownAmberText
            animatedFill >= 0.44f -> Color.White
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(durationMillis = 280),
        label = "countdownContrastColor",
    )
    val captionTextColor by animateColorAsState(
        targetValue = when {
            showIdleVisual -> telemetryColors.countdownAmberText.copy(alpha = 0.82f)
            animatedFill >= 0.72f -> Color.White.copy(alpha = 0.86f)
            else -> telemetryColors.mutedText
        },
        animationSpec = tween(durationMillis = 280),
        label = "captionContrastColor",
    )

    val circlePath = remember { Path() }
    val backWavePath = remember { Path() }
    val frontWavePath = remember { Path() }
    val crestLinePath = remember { Path() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 1.6.dp.toPx()
            val inset = strokePx / 2f + 1.dp.toPx()
            val diameter = this.size.minDimension - inset * 2f
            val circleRect = Rect(
                left = inset,
                top = inset,
                right = inset + diameter,
                bottom = inset + diameter,
            )

            circlePath.reset()
            circlePath.addOval(circleRect)

            val waterY = circleRect.bottom - (diameter * animatedFill.coerceIn(0f, 1f))
            val edgeDamping = sin(animatedFill.coerceIn(0.05f, 0.95f) * PI).toFloat().coerceIn(0.35f, 1f)
            val baseAmplitude = (if (isTracking && !isIdle) 5.5.dp.toPx() else 3.0.dp.toPx()) * edgeDamping
            val backAmplitude = baseAmplitude * 0.78f
            val stepPx = 4f
            val waveLength = diameter * 0.92f

            clipPath(circlePath) {
                drawRect(
                    color = vesselBgColor,
                    topLeft = circleRect.topLeft,
                    size = circleRect.size,
                )

                // Back liquid wave for depth
                backWavePath.reset()
                backWavePath.moveTo(circleRect.left, circleRect.bottom)
                var x = circleRect.left
                while (x <= circleRect.right + stepPx) {
                    val norm = (x - circleRect.left) / waveLength
                    val y = waterY - 1.5.dp.toPx() +
                        sin(norm * 2.0 * PI + backPhase).toFloat() * backAmplitude
                    backWavePath.lineTo(x, y)
                    x += stepPx
                }
                backWavePath.lineTo(circleRect.right, circleRect.bottom)
                backWavePath.close()
                drawPath(path = backWavePath, color = backWaveColor)

                // Front primary liquid wave with vertical gradient
                frontWavePath.reset()
                crestLinePath.reset()
                frontWavePath.moveTo(circleRect.left, circleRect.bottom)
                var fx = circleRect.left
                var firstPoint = true
                while (fx <= circleRect.right + stepPx) {
                    val norm = (fx - circleRect.left) / waveLength
                    val y = waterY + sin(norm * 2.0 * PI + frontPhase).toFloat() * baseAmplitude
                    frontWavePath.lineTo(fx, y)
                    if (firstPoint) {
                        crestLinePath.moveTo(fx, y)
                        firstPoint = false
                    } else {
                        crestLinePath.lineTo(fx, y)
                    }
                    fx += stepPx
                }
                frontWavePath.lineTo(circleRect.right, circleRect.bottom)
                frontWavePath.close()

                drawPath(
                    path = frontWavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(frontWaveTopColor, frontWaveBottomColor),
                        startY = (waterY - baseAmplitude).coerceAtLeast(circleRect.top),
                        endY = circleRect.bottom,
                    ),
                )

                // Subtle luminous highlight along wave crest
                drawPath(
                    path = crestLinePath,
                    color = Color.White.copy(alpha = 0.28f),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }

            drawOval(
                color = ringBorderColor,
                topLeft = circleRect.topLeft,
                size = circleRect.size,
                style = Stroke(width = strokePx),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Text(
                text = if (showIdleVisual) "Paused" else "${safeCycle}s cycle",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = captionTextColor,
                maxLines = 1,
            )

            val centerText = when {
                showIdleVisual -> stringResource(R.string.status_tracking_idle).uppercase()
                isTracking -> "${clampedRemaining}s"
                else -> "${safeCycle}s"
            }

            AnimatedContent(
                targetState = centerText,
                transitionSpec = {
                    (slideInVertically(tween(220)) { -it / 2 } + fadeIn(tween(180)))
                        .togetherWith(slideOutVertically(tween(180)) { it / 2 } + fadeOut(tween(140)))
                },
                label = "liquidCountdownText",
                modifier = Modifier.testTag("progress_ring_countdown"),
            ) { displayText ->
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = if (showIdleVisual) 20.sp else 23.sp,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = countdownTextColor,
                    maxLines = 1,
                )
            }
        }
    }
}
