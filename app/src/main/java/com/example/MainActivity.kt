package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.TrackerViewModel
import com.example.ui.components.BrandedLaunchSplash
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.TrackerDashboardScreen
import com.example.ui.theme.TrackPulseTheme
import com.example.ui.theme.resolveIsDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            TrackPulseApp(activity = this)
        }
    }
}

@Composable
private fun TrackPulseApp(
    activity: ComponentActivity,
    viewModel: TrackerViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = resolveIsDarkTheme(uiState.themeMode)

    DisposableEffect(isDark) {
        val barStyle = if (isDark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        activity.enableEdgeToEdge(
            statusBarStyle = barStyle,
            navigationBarStyle = barStyle,
        )
        onDispose {}
    }

    TrackPulseTheme(
        themeMode = uiState.themeMode,
        accentTheme = uiState.accentTheme,
    ) {
        val screenState = when {
            uiState.isLoadingSession -> 0
            uiState.session == null -> 1
            else -> 2
        }

        AnimatedContent(
            targetState = screenState,
            transitionSpec = {
                (fadeIn(animationSpec = tween(280)) + scaleIn(initialScale = 0.98f, animationSpec = tween(280)))
                    .togetherWith(fadeOut(animationSpec = tween(180)))
            },
            label = "sessionTransition",
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) { state ->
            when (state) {
                0 -> {
                    BrandedLaunchSplash()
                }
                1 -> {
                    LoginScreen(
                        email = uiState.emailInput,
                        isSigningIn = uiState.isSigningIn,
                        errorMessage = uiState.authError,
                        onEmailChanged = viewModel::onEmailChanged,
                        onSignInClick = viewModel::signIn,
                    )
                }
                else -> {
                    TrackerDashboardScreen(
                        uiState = uiState,
                        hasLocationPermission = viewModel::hasLocationPermission,
                        onStartTracking = viewModel::startTrackingWithPermission,
                        onPermissionDenied = viewModel::onPermissionDenied,
                        onStopTracking = { viewModel.stopTracking() },
                        onDismissError = viewModel::dismissError,
                        onSelectThemeMode = viewModel::setThemeMode,
                        onSelectAccentTheme = viewModel::setAccentTheme,
                        onSelectHistorySyncMode = viewModel::setHistorySyncMode,
                        onPickProfileImage = viewModel::saveProfileImageFromUri,
                        onRemoveProfileImage = viewModel::removeProfileImage,
                        onLogout = viewModel::logout,
                    )
                }
            }
        }
    }
}
