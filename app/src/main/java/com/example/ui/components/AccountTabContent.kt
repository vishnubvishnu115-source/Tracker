package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AppAccentTheme
import com.example.data.model.AppThemeMode
import com.example.data.model.UserSession
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalTelemetryColors
import com.example.ui.theme.accentPaletteFor
import java.io.File
import java.util.Locale

@Composable
fun ProfileAvatar(
    name: String,
    profileImagePath: String?,
    profileImageVersion: Long,
    size: Dp,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val telemetryColors = LocalTelemetryColors.current

    val imageModel = remember(profileImagePath, profileImageVersion) {
        profileImagePath?.let { path ->
            ImageRequest.Builder(context)
                .data(File(path))
                .memoryCacheKey("avatar_${path}_$profileImageVersion")
                .diskCacheKey("avatar_${path}_$profileImageVersion")
                .crossfade(280)
                .build()
        }
    }

    val clickableMod = if (onClick != null) {
        modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    } else {
        modifier
            .size(size)
            .clip(CircleShape)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = clickableMod
            .background(telemetryColors.accentContainer)
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                shape = CircleShape,
            ),
    ) {
        if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = stringResource(R.string.cd_profile_avatar),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = name.trim().take(1).uppercase(Locale.getDefault()).ifEmpty { "U" },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.42f).sp,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (isLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.5.dp,
                    color = Color.White,
                    modifier = Modifier.size(size * 0.38f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountTabContent(
    session: UserSession,
    profileImagePath: String?,
    profileImageVersion: Long,
    isUpdatingAvatar: Boolean,
    themeMode: AppThemeMode,
    accentTheme: AppAccentTheme,
    hasPermission: Boolean,
    isWakeLockActive: Boolean,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectAccentTheme: (AppAccentTheme) -> Unit,
    onPickProfileImage: (Uri) -> Unit,
    onRemoveProfileImage: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val telemetryColors = LocalTelemetryColors.current
    var showPhotoSourceSheet by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker (Gallery)
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            onPickProfileImage(uri)
        }
    }

    // Zero-permission Storage Access Framework (Google Drive, Files, Cloud)
    val drivePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            onPickProfileImage(uri)
        }
    }

    if (showPhotoSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoSourceSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.photo_sheet_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.photo_sheet_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(4.dp))

                PhotoSourceOptionTile(
                    icon = Icons.Filled.PhotoLibrary,
                    title = stringResource(R.string.photo_source_gallery),
                    subtitle = stringResource(R.string.photo_source_gallery_desc),
                    accentColor = MaterialTheme.colorScheme.primary,
                    accentBg = telemetryColors.accentContainer,
                    onClick = {
                        showPhotoSourceSheet = false
                        galleryPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    modifier = Modifier.testTag("pick_photo_gallery_button"),
                )

                PhotoSourceOptionTile(
                    icon = Icons.Filled.CloudQueue,
                    title = stringResource(R.string.photo_source_drive),
                    subtitle = stringResource(R.string.photo_source_drive_desc),
                    accentColor = telemetryColors.liveGreenText,
                    accentBg = telemetryColors.liveGreenContainer,
                    onClick = {
                        showPhotoSourceSheet = false
                        drivePickerLauncher.launch(arrayOf("image/*"))
                    },
                    modifier = Modifier.testTag("pick_photo_drive_button"),
                )

                if (profileImagePath != null) {
                    PhotoSourceOptionTile(
                        icon = Icons.Filled.DeleteOutline,
                        title = stringResource(R.string.photo_source_remove),
                        subtitle = "Revert to initial avatar badge",
                        accentColor = telemetryColors.stopRedText,
                        accentBg = telemetryColors.stopRedContainer,
                        onClick = {
                            showPhotoSourceSheet = false
                            onRemoveProfileImage()
                        },
                        modifier = Modifier.testTag("remove_photo_button"),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. Hero Profile Avatar + Operator Info Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, telemetryColors.cardBorder),
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("account_profile_card"),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        ProfileAvatar(
                            name = session.name,
                            profileImagePath = profileImagePath,
                            profileImageVersion = profileImageVersion,
                            size = 76.dp,
                            isLoading = isUpdatingAvatar,
                            onClick = { showPhotoSourceSheet = true },
                            modifier = Modifier.testTag("account_profile_avatar"),
                        )

                        Surface(
                            onClick = { showPhotoSourceSheet = true },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("edit_profile_photo_badge"),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = stringResource(R.string.cd_edit_avatar),
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = session.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${stringResource(R.string.user_id_prefix)}: ${session.userId}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Surface(
                            onClick = { showPhotoSourceSheet = true },
                            shape = CircleShape,
                            color = telemetryColors.accentContainer,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .testTag("change_photo_pill_button"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = stringResource(R.string.account_change_photo),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = telemetryColors.cardBorder)

                AccountInfoRow(label = "Name", value = session.name)
                HorizontalDivider(color = telemetryColors.cardBorder)
                AccountInfoRow(label = "User ID", value = session.userId, isMono = true)
                if (session.email.isNotBlank()) {
                    HorizontalDivider(color = telemetryColors.cardBorder)
                    AccountInfoRow(label = "Email", value = session.email)
                }
            }
        }

        // 2. Appearance & Theme Switcher Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, telemetryColors.cardBorder),
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("appearance_theme_card"),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
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
                                imageVector = Icons.Filled.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.account_section_appearance),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // Light / Dark / System Mode Selector
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
                            ThemeModeSegment(
                                icon = Icons.Filled.LightMode,
                                label = stringResource(R.string.theme_mode_light),
                                selected = themeMode == AppThemeMode.LIGHT,
                                onClick = { onSelectThemeMode(AppThemeMode.LIGHT) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_mode_light"),
                            )
                            ThemeModeSegment(
                                icon = Icons.Filled.DarkMode,
                                label = stringResource(R.string.theme_mode_dark),
                                selected = themeMode == AppThemeMode.DARK,
                                onClick = { onSelectThemeMode(AppThemeMode.DARK) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_mode_dark"),
                            )
                            ThemeModeSegment(
                                icon = Icons.Filled.BrightnessAuto,
                                label = stringResource(R.string.theme_mode_system),
                                selected = themeMode == AppThemeMode.SYSTEM,
                                onClick = { onSelectThemeMode(AppThemeMode.SYSTEM) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_mode_system"),
                            )
                        }
                    }
                }

                // Accent Color Swatches
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
                                label = "swatchScale_${accent.name}",
                            )

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .scale(scale)
                                    .clip(CircleShape)
                                    .clickable { onSelectAccentTheme(accent) }
                                    .testTag("accent_swatch_${accent.name.lowercase(Locale.US)}"),
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
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
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
            }
        }

        // 3. Device & Sync Settings Card (Fixed clean single-line alignment!)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, telemetryColors.cardBorder),
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.account_section_diagnostics),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                AccountBadgeRow(
                    label = stringResource(R.string.account_gps_permission),
                    badgeText = if (hasPermission) {
                        stringResource(R.string.account_permission_granted)
                    } else {
                        stringResource(R.string.account_permission_needed)
                    },
                    bgColor = if (hasPermission) {
                        telemetryColors.liveGreenContainer
                    } else {
                        telemetryColors.countdownAmberContainer
                    },
                    fgColor = if (hasPermission) {
                        telemetryColors.liveGreenText
                    } else {
                        telemetryColors.countdownAmberText
                    },
                )
                HorizontalDivider(color = telemetryColors.cardBorder)
                AccountBadgeRow(
                    label = stringResource(R.string.account_wake_lock),
                    badgeText = if (isWakeLockActive) {
                        stringResource(R.string.wake_lock_active)
                    } else {
                        stringResource(R.string.wake_lock_idle)
                    },
                    bgColor = if (isWakeLockActive) {
                        telemetryColors.countdownAmberContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    fgColor = if (isWakeLockActive) {
                        telemetryColors.countdownAmberText
                    } else {
                        telemetryColors.mutedText
                    },
                )
                HorizontalDivider(color = telemetryColors.cardBorder)
                AccountInfoRow(
                    label = stringResource(R.string.account_intervals),
                    value = stringResource(R.string.account_intervals_value),
                    isMono = true,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onOpenSettings,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("account_settings_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_open_settings),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }

                    OutlinedButton(
                        onClick = onLogout,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, telemetryColors.stopRed.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = telemetryColors.stopRedText,
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("account_logout_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.logout_button),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeModeSegment(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(220),
        label = "segmentBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "segmentFg",
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        modifier = modifier.height(42.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
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
private fun PhotoSourceOptionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    accentBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val telemetryColors = LocalTelemetryColors.current

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, telemetryColors.cardBorder),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentBg,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AccountInfoRow(
    label: String,
    value: String,
    isMono: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = if (isMono) {
                MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                )
            } else {
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            },
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AccountBadgeRow(
    label: String,
    badgeText: String,
    bgColor: Color,
    fgColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Surface(shape = CircleShape, color = bgColor) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = fgColor,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}
