package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.HistorySyncMode
import com.example.data.model.SpeedHistoryTier
import com.example.ui.theme.AppIcons
import com.example.ui.theme.LocalTelemetryColors
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Interactive card allowing the operator to switch between Speed-Based Adaptive
 * History Sync (15s / 30s / 60s / 120s) and Fixed 60s sync while highlighting
 * the active speed tier in real time.
 */
@Composable
fun SpeedAdaptiveSyncCard(
    historySyncMode: HistorySyncMode,
    currentSpeedKmh: Float,
    activeSpeedTier: SpeedHistoryTier,
    isTracking: Boolean,
    onSelectMode: (HistorySyncMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val telemetryColors = LocalTelemetryColors.current
    val isSpeedAuto = historySyncMode == HistorySyncMode.SPEED_ADAPTIVE
    val speedRounded = currentSpeedKmh.roundToInt()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, telemetryColors.cardBorder),
        shadowElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("speed_adaptive_sync_card"),
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
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = telemetryColors.accentContainer,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = AppIcons.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = stringResource(R.string.section_speed_history),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (isSpeedAuto) {
                                stringResource(R.string.speed_mode_desc_auto)
                            } else {
                                stringResource(R.string.speed_mode_desc_fixed)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = if (isTracking) {
                        telemetryColors.liveGreenContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                ) {
                    Text(
                        text = "$speedRounded km/h",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = if (isTracking) {
                            telemetryColors.liveGreenText
                        } else {
                            telemetryColors.mutedText
                        },
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("live_speed_badge"),
                    )
                }
            }

            // Segmented mode toggle: Speed Auto vs Fixed 60s
            Surface(
                shape = RoundedCornerShape(12.dp),
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
                    SpeedModeSegment(
                        icon = AppIcons.Speed,
                        label = stringResource(R.string.speed_mode_auto),
                        selected = isSpeedAuto,
                        onClick = { onSelectMode(HistorySyncMode.SPEED_ADAPTIVE) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("history_mode_speed_auto"),
                    )
                    SpeedModeSegment(
                        icon = AppIcons.Timer,
                        label = stringResource(R.string.speed_mode_fixed),
                        selected = !isSpeedAuto,
                        onClick = { onSelectMode(HistorySyncMode.FIXED_60S) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("history_mode_fixed_60s"),
                    )
                }
            }

            // 4 live speed-tier chips showing dynamic sync cadence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SpeedHistoryTier.entries.forEach { tier ->
                    val highlighted = isSpeedAuto && tier == activeSpeedTier
                    val chipBg by animateColorAsState(
                        targetValue = if (highlighted) {
                            telemetryColors.accentContainer
                        } else {
                            MaterialTheme.colorScheme.background
                        },
                        animationSpec = tween(220),
                        label = "tierBg_${tier.name}",
                    )
                    val chipBorder by animateColorAsState(
                        targetValue = if (highlighted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            telemetryColors.cardBorder
                        },
                        animationSpec = tween(220),
                        label = "tierBorder_${tier.name}",
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = chipBg,
                        border = BorderStroke(if (highlighted) 1.5.dp else 1.dp, chipBorder),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("speed_tier_${tier.name.lowercase(Locale.US)}"),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = tier.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (highlighted) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 10.5.sp,
                                ),
                                color = if (highlighted) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "${tier.intervalSeconds}s",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum",
                                ),
                                color = if (highlighted) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    telemetryColors.countdownAmberText
                                },
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = tier.rangeLabel.replace(" km/h", ""),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                ),
                                color = telemetryColors.mutedText,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedModeSegment(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(220),
        label = "speedModeBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "speedModeFg",
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        color = bgColor,
        modifier = modifier.height(40.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
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
