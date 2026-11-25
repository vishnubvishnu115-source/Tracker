package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.AppAccentTheme
import com.example.data.model.AppThemeMode

@Immutable
data class TelemetrySemanticColors(
    val liveGreen: Color,
    val liveGreenText: Color,
    val liveGreenContainer: Color,
    val countdownAmber: Color,
    val countdownAmberText: Color,
    val countdownAmberContainer: Color,
    val stopRed: Color,
    val stopRedText: Color,
    val stopRedContainer: Color,
    val accentPrimary: Color,
    val accentContainer: Color,
    val cardBorder: Color,
    val mutedText: Color,
    val isDark: Boolean,
)

private val DefaultLightTelemetryColors = TelemetrySemanticColors(
    liveGreen = SemanticGreen,
    liveGreenText = SemanticGreenDark,
    liveGreenContainer = SemanticGreenContainerLight,
    countdownAmber = SemanticAmber,
    countdownAmberText = SemanticAmberDark,
    countdownAmberContainer = SemanticAmberContainerLight,
    stopRed = SemanticRed,
    stopRedText = SemanticRedDark,
    stopRedContainer = SemanticRedContainerLight,
    accentPrimary = Color(0xFF4F46E5),
    accentContainer = Color(0xFFEEF2FF),
    cardBorder = SubtleBorderLight,
    mutedText = SlateMuted,
    isDark = false,
)

val LocalTelemetryColors = staticCompositionLocalOf { DefaultLightTelemetryColors }

private val TrackPulseShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun resolveIsDarkTheme(themeMode: AppThemeMode): Boolean = when (themeMode) {
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
    AppThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun TrackPulseTheme(
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    accentTheme: AppAccentTheme = AppAccentTheme.INDIGO,
    content: @Composable () -> Unit,
) {
    val isDark = resolveIsDarkTheme(themeMode)
    val accent = accentPaletteFor(accentTheme)
    val animSpec = tween<Color>(durationMillis = 320)

    val primary by animateColorAsState(
        targetValue = if (isDark) accent.darkPrimary else accent.lightPrimary,
        animationSpec = animSpec,
        label = "themePrimary",
    )
    val primaryContainer by animateColorAsState(
        targetValue = if (isDark) accent.darkContainer else accent.lightContainer,
        animationSpec = animSpec,
        label = "themePrimaryContainer",
    )
    val onPrimaryContainer by animateColorAsState(
        targetValue = if (isDark) accent.darkOnContainer else accent.lightOnContainer,
        animationSpec = animSpec,
        label = "themeOnPrimaryContainer",
    )
    val background by animateColorAsState(
        targetValue = if (isDark) DarkCanvasBg else LightCanvasBg,
        animationSpec = animSpec,
        label = "themeBackground",
    )
    val surface by animateColorAsState(
        targetValue = if (isDark) DarkCardSurface else PureWhiteSurface,
        animationSpec = animSpec,
        label = "themeSurface",
    )
    val onSurface by animateColorAsState(
        targetValue = if (isDark) TextPrimaryDark else DeepSlatePrimary,
        animationSpec = animSpec,
        label = "themeOnSurface",
    )
    val onSurfaceVariant by animateColorAsState(
        targetValue = if (isDark) TextSecondaryDark else SlateSecondary,
        animationSpec = animSpec,
        label = "themeOnSurfaceVariant",
    )
    val surfaceContainer by animateColorAsState(
        targetValue = if (isDark) DarkElevatedSurface else SoftSlateSurface,
        animationSpec = animSpec,
        label = "themeSurfaceContainer",
    )
    val surfaceContainerHigh by animateColorAsState(
        targetValue = if (isDark) DarkElevatedSurfaceHigh else SoftSlateSurfaceHigh,
        animationSpec = animSpec,
        label = "themeSurfaceContainerHigh",
    )
    val cardBorder by animateColorAsState(
        targetValue = if (isDark) SubtleBorderDark else SubtleBorderLight,
        animationSpec = animSpec,
        label = "themeCardBorder",
    )

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF0A0F1E),
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = SemanticGreen,
            onSecondary = Color(0xFF042016),
            secondaryContainer = SemanticGreenContainerDark,
            onSecondaryContainer = Color(0xFF6EE7B7),
            tertiary = SemanticAmber,
            onTertiary = Color(0xFF2B1B04),
            tertiaryContainer = SemanticAmberContainerDark,
            onTertiaryContainer = Color(0xFFFCD34D),
            error = SemanticRed,
            onError = Color.White,
            errorContainer = SemanticRedContainerDark,
            onErrorContainer = Color(0xFFFCA5A5),
            background = background,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceContainer,
            onSurfaceVariant = onSurfaceVariant,
            surfaceContainerLowest = background,
            surfaceContainerLow = surface,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            outline = CrispOutlineDark,
            outlineVariant = cardBorder,
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = PureWhiteSurface,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = SemanticGreen,
            onSecondary = PureWhiteSurface,
            secondaryContainer = SemanticGreenContainerLight,
            onSecondaryContainer = SemanticGreenDark,
            tertiary = SemanticAmber,
            onTertiary = PureWhiteSurface,
            tertiaryContainer = SemanticAmberContainerLight,
            onTertiaryContainer = SemanticAmberDark,
            error = SemanticRed,
            onError = PureWhiteSurface,
            errorContainer = SemanticRedContainerLight,
            onErrorContainer = SemanticRedDark,
            background = background,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceContainer,
            onSurfaceVariant = onSurfaceVariant,
            surfaceContainerLowest = PureWhiteSurface,
            surfaceContainerLow = background,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            outline = CrispOutlineLight,
            outlineVariant = cardBorder,
        )
    }

    val telemetryColors = TelemetrySemanticColors(
        liveGreen = SemanticGreen,
        liveGreenText = if (isDark) Color(0xFF6EE7B7) else SemanticGreenDark,
        liveGreenContainer = if (isDark) SemanticGreenContainerDark else SemanticGreenContainerLight,
        countdownAmber = SemanticAmber,
        countdownAmberText = if (isDark) Color(0xFFFCD34D) else SemanticAmberDark,
        countdownAmberContainer = if (isDark) SemanticAmberContainerDark else SemanticAmberContainerLight,
        stopRed = SemanticRed,
        stopRedText = if (isDark) Color(0xFFFCA5A5) else SemanticRedDark,
        stopRedContainer = if (isDark) SemanticRedContainerDark else SemanticRedContainerLight,
        accentPrimary = primary,
        accentContainer = primaryContainer,
        cardBorder = cardBorder,
        mutedText = if (isDark) TextMutedDark else SlateMuted,
        isDark = isDark,
    )

    CompositionLocalProvider(LocalTelemetryColors provides telemetryColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TrackPulseTypography,
            shapes = TrackPulseShapes,
            content = content,
        )
    }
}
