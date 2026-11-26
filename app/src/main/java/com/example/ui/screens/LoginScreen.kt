package com.example.ui.screens

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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppAccentTheme
import com.example.data.model.AppThemeMode
import com.example.ui.components.TelemetryAnimatedBackground
import com.example.ui.components.TelemetryBackdropStyle
import com.example.ui.theme.AppIcons
import com.example.ui.theme.LocalTelemetryColors
import com.example.ui.theme.accentPaletteFor
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    email: String,
    isSigningIn: Boolean,
    errorMessage: String?,
    themeMode: AppThemeMode,
    accentTheme: AppAccentTheme,
    onEmailChanged: (String) -> Unit,
    onSignInClick: () -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectAccentTheme: (AppAccentTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    val telemetryColors = LocalTelemetryColors.current
    val scrollState = rememberScrollState()
    var showAppearanceSheet by remember { mutableStateOf(false) }

    val logoPulseTransition = rememberInfiniteTransition(label = "loginLogoPulse")
    val haloScale by logoPulseTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "loginHaloScale",
    )
    val haloAlpha by logoPulseTransition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "loginHaloAlpha",
    )

    if (showAppearanceSheet) {
        LoginAppearanceBottomSheet(
            themeMode = themeMode,
            accentTheme = accentTheme,
            onSelectThemeMode = onSelectThemeMode,
            onSelectAccentTheme = onSelectAccentTheme,
            onDismiss = { showAppearanceSheet = false },
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TelemetryAnimatedBackground(
            style = TelemetryBackdropStyle.LOGIN,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Branded TrackPulse emblem with ambient breathing halo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(104.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .scale(haloScale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = haloAlpha),
                                    Color.Transparent,
                                ),
                            ),
                            shape = CircleShape,
                        ),
                )
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(80.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_trackpulse_logo),
                        contentDescription = stringResource(R.string.cd_brand_logo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.brand_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Elevated rounded card with subtle translucency so themed background glows around it
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(
                    alpha = if (telemetryColors.isDark) 0.92f else 0.96f,
                ),
                border = BorderStroke(1.dp, telemetryColors.cardBorder),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.login_card_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.login_card_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChanged,
                        label = { Text(stringResource(R.string.email_label)) },
                        placeholder = { Text(stringResource(R.string.email_placeholder)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        singleLine = true,
                        enabled = !isSigningIn,
                        isError = errorMessage != null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { onSignInClick() },
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input"),
                    )

                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = telemetryColors.stopRedContainer,
                            border = BorderStroke(1.dp, telemetryColors.stopRed.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_error_banner"),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = AppIcons.ErrorOutline,
                                    contentDescription = null,
                                    tint = telemetryColors.stopRed,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = errorMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = telemetryColors.stopRedText,
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onSignInClick,
                        enabled = !isSigningIn && email.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("sign_in_button"),
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.signing_in_label),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.sign_in_button),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusFeatureBadge(
                    icon = AppIcons.Sync,
                    label = "2s Live Sync",
                    bgColor = telemetryColors.liveGreenContainer,
                    fgColor = telemetryColors.liveGreenText,
                )
                StatusFeatureBadge(
                    icon = AppIcons.History,
                    label = "1m History Log",
                    bgColor = telemetryColors.countdownAmberContainer,
                    fgColor = telemetryColors.countdownAmberText,
                )
            }
        }

        // Top-right Appearance & Theme quick switcher (Light/Dark/System + Accent Colors)
        Surface(
            onClick = { showAppearanceSheet = true },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(
                alpha = if (telemetryColors.isDark) 0.88f else 0.92f,
            ),
            border = BorderStroke(1.dp, telemetryColors.cardBorder),
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 16.dp)
                .height(48.dp)
                .testTag("login_theme_button"),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = when (themeMode) {
                        AppThemeMode.LIGHT -> AppIcons.LightMode
                        AppThemeMode.DARK -> AppIcons.DarkMode
                        AppThemeMode.SYSTEM -> AppIcons.BrightnessAuto
                    },
                    contentDescription = stringResource(R.string.cd_appearance_button),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accentPaletteFor(accentTheme).swatchColor),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginAppearanceBottomSheet(
    themeMode: AppThemeMode,
    accentTheme: AppAccentTheme,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectAccentTheme: (AppAccentTheme) -> Unit,
    onDismiss: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = telemetryColors.accentContainer,
                    modifier = Modifier.size(34.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = AppIcons.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.account_section_appearance),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Light / Dark / System selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.account_theme_mode_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, telemetryColors.cardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        LoginThemeModeSegment(
                            icon = AppIcons.LightMode,
                            label = stringResource(R.string.theme_mode_light),
                            selected = themeMode == AppThemeMode.LIGHT,
                            onClick = { onSelectThemeMode(AppThemeMode.LIGHT) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("login_theme_mode_light"),
                        )
                        LoginThemeModeSegment(
                            icon = AppIcons.DarkMode,
                            label = stringResource(R.string.theme_mode_dark),
                            selected = themeMode == AppThemeMode.DARK,
                            onClick = { onSelectThemeMode(AppThemeMode.DARK) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("login_theme_mode_dark"),
                        )
                        LoginThemeModeSegment(
                            icon = AppIcons.BrightnessAuto,
                            label = stringResource(R.string.theme_mode_system),
                            selected = themeMode == AppThemeMode.SYSTEM,
                            onClick = { onSelectThemeMode(AppThemeMode.SYSTEM) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("login_theme_mode_system"),
                        )
                    }
                }
            }

            // Accent color swatches
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.account_theme_accent_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = accentTheme.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppAccentTheme.entries.forEach { accent ->
                        val selected = accent == accentTheme
                        val swatch = accentPaletteFor(accent).swatchColor
                        val scale by animateFloatAsState(
                            targetValue = if (selected) 1.12f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                            label = "loginSwatchScale_${accent.name}",
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .clickable { onSelectAccentTheme(accent) }
                                .testTag("login_accent_swatch_${accent.name.lowercase(Locale.US)}"),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .border(
                                        width = if (selected) 3.dp else 0.dp,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                        } else {
                                            Color.Transparent
                                        },
                                        shape = CircleShape,
                                    ),
                            ) {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = selected,
                                    enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.5f),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = accent.displayName,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun LoginThemeModeSegment(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(220),
        label = "loginSegmentBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(220),
        label = "loginSegmentFg",
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        modifier = modifier.height(48.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                ),
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StatusFeatureBadge(
    icon: ImageVector,
    label: String,
    bgColor: Color,
    fgColor: Color,
) {
    Surface(
        shape = CircleShape,
        color = bgColor,
        border = BorderStroke(1.dp, fgColor.copy(alpha = 0.22f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = fgColor,
            )
        }
    }
}
