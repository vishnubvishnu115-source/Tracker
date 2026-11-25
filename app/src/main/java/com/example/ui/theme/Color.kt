package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.model.AppAccentTheme

// Clean Light-Mode Surfaces (#F8FAFC background, pure white cards, deep slate type)
val LightCanvasBg = Color(0xFFF8FAFC)
val PureWhiteSurface = Color(0xFFFFFFFF)
val SoftSlateSurface = Color(0xFFF1F5F9)
val SoftSlateSurfaceHigh = Color(0xFFE2E8F0)
val SubtleBorderLight = Color(0xFFE2E8F0)
val CrispOutlineLight = Color(0xFFCBD5E1)

val DeepSlatePrimary = Color(0xFF0F172A)
val SlateSecondary = Color(0xFF475569)
val SlateMuted = Color(0xFF64748B)

// Sleek Dark-Mode Surfaces
val DarkCanvasBg = Color(0xFF0B1120)
val DarkCardSurface = Color(0xFF131C2E)
val DarkElevatedSurface = Color(0xFF1B263B)
val DarkElevatedSurfaceHigh = Color(0xFF25334D)
val SubtleBorderDark = Color(0xFF24324A)
val CrispOutlineDark = Color(0xFF334461)

val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextMutedDark = Color(0xFF64748B)

// Semantic Status Colors (Light)
val SemanticGreen = Color(0xFF10B981)
val SemanticGreenDark = Color(0xFF047857)
val SemanticGreenContainerLight = Color(0xFFECFDF5)
val SemanticGreenContainerDark = Color(0xFF0B2E24)

val SemanticAmber = Color(0xFFF59E0B)
val SemanticAmberDark = Color(0xFFB45309)
val SemanticAmberContainerLight = Color(0xFFFFFBEB)
val SemanticAmberContainerDark = Color(0xFF332308)

val SemanticRed = Color(0xFFEF4444)
val SemanticRedDark = Color(0xFFB91C1C)
val SemanticRedContainerLight = Color(0xFFFEF2F2)
val SemanticRedContainerDark = Color(0xFF3B1219)

data class AccentPalette(
    val lightPrimary: Color,
    val lightContainer: Color,
    val lightOnContainer: Color,
    val darkPrimary: Color,
    val darkContainer: Color,
    val darkOnContainer: Color,
    val swatchColor: Color,
)

fun accentPaletteFor(accent: AppAccentTheme): AccentPalette = when (accent) {
    AppAccentTheme.INDIGO -> AccentPalette(
        lightPrimary = Color(0xFF4F46E5),
        lightContainer = Color(0xFFEEF2FF),
        lightOnContainer = Color(0xFF3730A3),
        darkPrimary = Color(0xFF818CF8),
        darkContainer = Color(0xFF1E224A),
        darkOnContainer = Color(0xFFC7D2FE),
        swatchColor = Color(0xFF4F46E5),
    )
    AppAccentTheme.EMERALD -> AccentPalette(
        lightPrimary = Color(0xFF059669),
        lightContainer = Color(0xFFECFDF5),
        lightOnContainer = Color(0xFF065F46),
        darkPrimary = Color(0xFF34D399),
        darkContainer = Color(0xFF0A2E23),
        darkOnContainer = Color(0xFFA7F3D0),
        swatchColor = Color(0xFF059669),
    )
    AppAccentTheme.OCEAN -> AccentPalette(
        lightPrimary = Color(0xFF0284C7),
        lightContainer = Color(0xFFE0F2FE),
        lightOnContainer = Color(0xFF075985),
        darkPrimary = Color(0xFF38BDF8),
        darkContainer = Color(0xFF0C2A3E),
        darkOnContainer = Color(0xFFBAE6FD),
        swatchColor = Color(0xFF0284C7),
    )
    AppAccentTheme.SUNSET -> AccentPalette(
        lightPrimary = Color(0xFFEA580C),
        lightContainer = Color(0xFFFFF7ED),
        lightOnContainer = Color(0xFF9A3412),
        darkPrimary = Color(0xFFFB923C),
        darkContainer = Color(0xFF3B1E0D),
        darkOnContainer = Color(0xFFFED7AA),
        swatchColor = Color(0xFFEA580C),
    )
    AppAccentTheme.VIOLET -> AccentPalette(
        lightPrimary = Color(0xFF7C3AED),
        lightContainer = Color(0xFFF5F3FF),
        lightOnContainer = Color(0xFF5B21B6),
        darkPrimary = Color(0xFFA78BFA),
        darkContainer = Color(0xFF281B49),
        darkOnContainer = Color(0xFFDDD6FE),
        swatchColor = Color(0xFF7C3AED),
    )
}
