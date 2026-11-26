package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTelemetryColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class TelemetryBackdropStyle {
    LOGIN,
    SPLASH,
}

/**
 * Full-bleed animated GPS telemetry backdrop with drifting ambient aurora orbs,
 * subtle cartographic dot grid, animated cubic-bezier GPS routes with traveling signal packets,
 * and a rotating radar sweep + sonar ripples.
 *
 * Fully theme-reactive: every atmospheric layer derives from MaterialTheme.colorScheme and
 * LocalTelemetryColors so Light, Dark, System, and all Accent themes transform the canvas smoothly.
 */
@Composable
fun TelemetryAnimatedBackground(
    style: TelemetryBackdropStyle,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val telemetryColors = LocalTelemetryColors.current
    val isDark = telemetryColors.isDark

    val primary = colorScheme.primary
    val onPrimaryContainer = colorScheme.onPrimaryContainer
    val background = colorScheme.background
    val accentContainer = telemetryColors.accentContainer
    val harmonicAccent = lerp(primary, onPrimaryContainer, if (isDark) 0.42f else 0.28f)
    val softComplement = lerp(primary, telemetryColors.liveGreen, 0.38f)

    val transition = rememberInfiniteTransition(label = "telemetryBackdropTransition")

    val orbAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbOrbitAngle",
    )

    val radarAngleDeg by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7_500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radarSweepDeg",
    )

    val pulsePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sonarPulsePhase",
    )

    val routeProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8_500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "gpsRouteProgress",
    )

    val upperRoutePath = remember { Path() }
    val lowerRoutePath = remember { Path() }
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 12f), 0f) }
    val fineRingDash = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 10f), 0f) }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val minDim = size.minDimension

        // 1. Base vertical atmosphere wash tinted by active Accent + Light/Dark surface
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    accentContainer.copy(alpha = if (isDark) 0.72f else 0.80f),
                    background,
                    lerp(background, accentContainer, if (isDark) 0.24f else 0.30f),
                ),
            ),
        )

        // 2. Drifting ambient aurora orbs harmonized to the active Accent Theme
        val orbAlphaPrimary = if (isDark) 0.28f else 0.18f
        val orbAlphaHarmonic = if (isDark) 0.22f else 0.14f
        val orbAlphaComplement = if (isDark) 0.14f else 0.10f

        val orb1Center = Offset(
            x = w * (0.22f + 0.14f * cos(orbAngle)),
            y = h * (0.16f + 0.07f * sin(orbAngle * 1.5f)),
        )
        drawGlowOrb(
            center = orb1Center,
            radius = minDim * 0.74f,
            color = primary.copy(alpha = orbAlphaPrimary),
        )

        val orb2Center = Offset(
            x = w * (0.82f + 0.12f * sin(orbAngle + 1.1f)),
            y = h * (if (style == TelemetryBackdropStyle.SPLASH) 0.74f else 0.84f) +
                h * 0.06f * cos(orbAngle),
        )
        drawGlowOrb(
            center = orb2Center,
            radius = minDim * 0.68f,
            color = harmonicAccent.copy(alpha = orbAlphaHarmonic),
        )

        val orb3Center = Offset(
            x = w * (0.14f + 0.10f * sin(orbAngle * 0.8f + 2.4f)),
            y = h * (if (style == TelemetryBackdropStyle.SPLASH) 0.78f else 0.90f) +
                h * 0.05f * sin(orbAngle),
        )
        drawGlowOrb(
            center = orb3Center,
            radius = minDim * 0.54f,
            color = softComplement.copy(alpha = orbAlphaComplement),
        )

        // 3. Focal coordinates for radar beacon & cartographic vignette
        val focalCenter = if (style == TelemetryBackdropStyle.SPLASH) {
            Offset(w * 0.5f, h * 0.46f)
        } else {
            Offset(w * 0.5f, h * 0.18f)
        }

        // Subtle cartographic dot grid with radial fade
        drawCartographicDotGrid(
            focalCenter = focalCenter,
            dotColor = primary,
            isDark = isDark,
            maxFadeRadius = minDim * 0.92f,
        )

        // 4. Concentric radar rings, rotating sweep wedge, and expanding sonar ripples
        val maxRadarRadius = if (style == TelemetryBackdropStyle.SPLASH) {
            minDim * 0.48f
        } else {
            minDim * 0.44f
        }

        val ringStrokeWidth = 1.dp.toPx()
        val ringBaseAlpha = if (isDark) 0.18f else 0.13f

        drawCircle(
            color = primary.copy(alpha = ringBaseAlpha),
            radius = maxRadarRadius * 0.45f,
            center = focalCenter,
            style = Stroke(width = ringStrokeWidth),
        )
        drawCircle(
            color = harmonicAccent.copy(alpha = ringBaseAlpha * 0.9f),
            radius = maxRadarRadius * 0.74f,
            center = focalCenter,
            style = Stroke(width = ringStrokeWidth, pathEffect = fineRingDash),
        )
        drawCircle(
            color = primary.copy(alpha = ringBaseAlpha * 0.55f),
            radius = maxRadarRadius,
            center = focalCenter,
            style = Stroke(width = ringStrokeWidth),
        )

        // Rotating radar scanner wedge
        val sweepAlpha = if (style == TelemetryBackdropStyle.SPLASH) {
            if (isDark) 0.26f else 0.16f
        } else {
            if (isDark) 0.18f else 0.11f
        }
        withTransform({
            rotate(degrees = radarAngleDeg, pivot = focalCenter)
        }) {
            val sweepRadius = maxRadarRadius * 0.98f
            drawArc(
                brush = Brush.sweepGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.72f to Color.Transparent,
                        0.92f to primary.copy(alpha = sweepAlpha * 0.5f),
                        1.0f to primary.copy(alpha = sweepAlpha),
                    ),
                    center = focalCenter,
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = true,
                topLeft = Offset(focalCenter.x - sweepRadius, focalCenter.y - sweepRadius),
                size = Size(sweepRadius * 2f, sweepRadius * 2f),
            )
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        primary.copy(alpha = 0f),
                        primary.copy(alpha = sweepAlpha * 1.45f),
                    ),
                    start = focalCenter,
                    end = Offset(focalCenter.x + sweepRadius, focalCenter.y),
                ),
                start = focalCenter,
                end = Offset(focalCenter.x + sweepRadius, focalCenter.y),
                strokeWidth = 1.4.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // 3 staggered expanding GPS sonar ripples
        val rippleMinR = 36.dp.toPx()
        for (i in 0 until 3) {
            val phase = (pulsePhase + i / 3f) % 1f
            val eased = 1f - (1f - phase) * (1f - phase)
            val radius = rippleMinR + (maxRadarRadius * 1.15f - rippleMinR) * eased
            val alpha = (1f - phase) * (1f - phase) * if (isDark) 0.36f else 0.25f
            drawCircle(
                color = if (i == 1) harmonicAccent.copy(alpha = alpha) else primary.copy(alpha = alpha),
                radius = radius,
                center = focalCenter,
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }

        // 2 subtle orbiting satellite blips
        val sat1Rad = Math.toRadians((radarAngleDeg * 0.8f + 35f).toDouble())
        val sat1Pos = Offset(
            x = focalCenter.x + (maxRadarRadius * 0.74f) * cos(sat1Rad).toFloat(),
            y = focalCenter.y + (maxRadarRadius * 0.74f) * sin(sat1Rad).toFloat(),
        )
        drawTelemetryBlip(
            center = sat1Pos,
            color = harmonicAccent,
            coreRadius = 3.dp.toPx(),
            glowRadius = 11.dp.toPx(),
            alpha = if (isDark) 0.88f else 0.78f,
        )

        val sat2Rad = Math.toRadians((-radarAngleDeg * 0.55f + 210f).toDouble())
        val sat2Pos = Offset(
            x = focalCenter.x + (maxRadarRadius * 0.45f) * cos(sat2Rad).toFloat(),
            y = focalCenter.y + (maxRadarRadius * 0.45f) * sin(sat2Rad).toFloat(),
        )
        drawTelemetryBlip(
            center = sat2Pos,
            color = primary,
            coreRadius = 2.5.dp.toPx(),
            glowRadius = 9.dp.toPx(),
            alpha = if (isDark) 0.78f else 0.68f,
        )

        // 5. Animated curved GPS route paths with traveling signal packets
        val routeStrokeAlpha = if (isDark) 0.20f else 0.14f

        val uP0 = Offset(-w * 0.08f, h * 0.26f)
        val uP1 = Offset(w * 0.28f, h * 0.06f)
        val uP2 = Offset(w * 0.72f, h * 0.30f)
        val uP3 = Offset(w * 1.08f, h * 0.12f)
        upperRoutePath.reset()
        upperRoutePath.moveTo(uP0.x, uP0.y)
        upperRoutePath.cubicTo(uP1.x, uP1.y, uP2.x, uP2.y, uP3.x, uP3.y)

        drawPath(
            path = upperRoutePath,
            color = primary.copy(alpha = routeStrokeAlpha),
            style = Stroke(
                width = 1.4.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = dashEffect,
            ),
        )
        drawTravelingPacket(
            p0 = uP0,
            p1 = uP1,
            p2 = uP2,
            p3 = uP3,
            progress = routeProgress,
            color = primary,
            isDark = isDark,
        )

        val lP0 = Offset(w * 1.08f, h * 0.78f)
        val lP1 = Offset(w * 0.72f, h * 0.94f)
        val lP2 = Offset(w * 0.28f, h * 0.76f)
        val lP3 = Offset(-w * 0.08f, h * 0.92f)
        lowerRoutePath.reset()
        lowerRoutePath.moveTo(lP0.x, lP0.y)
        lowerRoutePath.cubicTo(lP1.x, lP1.y, lP2.x, lP2.y, lP3.x, lP3.y)

        drawPath(
            path = lowerRoutePath,
            color = harmonicAccent.copy(alpha = routeStrokeAlpha),
            style = Stroke(
                width = 1.4.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = dashEffect,
            ),
        )
        drawTravelingPacket(
            p0 = lP0,
            p1 = lP1,
            p2 = lP2,
            p3 = lP3,
            progress = (routeProgress + 0.48f) % 1f,
            color = harmonicAccent,
            isDark = isDark,
        )
    }
}

private fun DrawScope.drawGlowOrb(
    center: Offset,
    radius: Float,
    color: Color,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, color.copy(alpha = color.alpha * 0.35f), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.drawCartographicDotGrid(
    focalCenter: Offset,
    dotColor: Color,
    isDark: Boolean,
    maxFadeRadius: Float,
) {
    val spacing = 34.dp.toPx()
    val dotRadius = 1.25.dp.toPx()
    val maxAlpha = if (isDark) 0.18f else 0.12f

    val cols = (size.width / spacing).toInt() + 1
    val rows = (size.height / spacing).toInt() + 1
    val offsetX = (size.width - (cols - 1) * spacing) / 2f
    val offsetY = (size.height - (rows - 1) * spacing) / 2f

    for (r in 0 until rows) {
        val y = offsetY + r * spacing
        for (c in 0 until cols) {
            val x = offsetX + c * spacing
            val dist = hypot(x - focalCenter.x, y - focalCenter.y)
            val norm = (1f - (dist / maxFadeRadius)).coerceIn(0f, 1f)
            if (norm > 0.05f) {
                drawCircle(
                    color = dotColor.copy(alpha = maxAlpha * norm * norm),
                    radius = dotRadius,
                    center = Offset(x, y),
                )
            }
        }
    }
}

private fun DrawScope.drawTravelingPacket(
    p0: Offset,
    p1: Offset,
    p2: Offset,
    p3: Offset,
    progress: Float,
    color: Color,
    isDark: Boolean,
) {
    val waypoints = floatArrayOf(0.24f, 0.74f)
    for (wp in waypoints) {
        val wpPos = cubicBezierPoint(p0, p1, p2, p3, wp)
        val proximity = (1f - (abs(progress - wp) / 0.12f)).coerceIn(0f, 1f)
        val baseAlpha = if (isDark) 0.30f else 0.22f
        drawCircle(
            color = color.copy(alpha = baseAlpha + proximity * 0.45f),
            radius = 3.dp.toPx() + proximity * 2.dp.toPx(),
            center = wpPos,
            style = Stroke(width = 1.2.dp.toPx()),
        )
        if (proximity > 0.05f) {
            drawCircle(
                color = color.copy(alpha = proximity * 0.26f),
                radius = 7.dp.toPx() + proximity * 5.dp.toPx(),
                center = wpPos,
            )
        }
    }

    val trailSegments = 10
    val trailSpan = 0.09f
    for (s in 1..trailSegments) {
        val frac = s / trailSegments.toFloat()
        val t = (progress - trailSpan * (1f - frac)).coerceIn(0f, 1f)
        val pos = cubicBezierPoint(p0, p1, p2, p3, t)
        drawCircle(
            color = color.copy(alpha = frac * (if (isDark) 0.34f else 0.24f)),
            radius = (1.5f + frac * 1.8f).dp.toPx(),
            center = pos,
        )
    }

    val headPos = cubicBezierPoint(p0, p1, p2, p3, progress)
    drawTelemetryBlip(
        center = headPos,
        color = color,
        coreRadius = 3.2.dp.toPx(),
        glowRadius = 13.dp.toPx(),
        alpha = if (isDark) 0.94f else 0.84f,
    )
}

private fun DrawScope.drawTelemetryBlip(
    center: Offset,
    color: Color,
    coreRadius: Float,
    glowRadius: Float,
    alpha: Float,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha * 0.55f), Color.Transparent),
            center = center,
            radius = glowRadius,
        ),
        radius = glowRadius,
        center = center,
    )
    drawCircle(
        color = color.copy(alpha = alpha),
        radius = coreRadius,
        center = center,
    )
    drawCircle(
        color = Color.White.copy(alpha = alpha * 0.85f),
        radius = coreRadius * 0.45f,
        center = center,
    )
}

private fun cubicBezierPoint(
    p0: Offset,
    p1: Offset,
    p2: Offset,
    p3: Offset,
    t: Float,
): Offset {
    val u = 1f - t
    val uu = u * u
    val uuu = uu * u
    val tt = t * t
    val ttt = tt * t
    return Offset(
        x = uuu * p0.x + 3f * uu * t * p1.x + 3f * u * tt * p2.x + ttt * p3.x,
        y = uuu * p0.y + 3f * uu * t * p1.y + 3f * u * tt * p2.y + ttt * p3.y,
    )
}
