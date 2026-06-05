package com.krata.orbit

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krata.orbit.notifications.EXTRA_TAB_ROUTE
import com.krata.orbit.ui.navigation.MainNavigation
import com.krata.orbit.ui.splash.SplashScreen
import com.krata.orbit.ui.theme.AppTheme
import com.krata.orbit.ui.theme.OrbitTheme
import com.krata.orbit.ui.welcome.WelcomeScreen
import com.krata.orbit.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not — no blocking UI needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Which tab to open (from notification deep link)
        val deepLinkTab = intent?.getStringExtra(EXTRA_TAB_ROUTE)

        setContent {
            val appState by appViewModel.state.collectAsStateWithLifecycle()

            OrbitTheme(appTheme = appState.appTheme) {
                if (!appState.isReady) {
                    // Show nothing while prefs are loading (avoids flash)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                    return@OrbitTheme
                }

                AppFlow(
                    isFirstLaunch = appState.isFirstLaunch,
                    username      = appState.username,
                    appTheme      = appState.appTheme,
                    startTab      = deepLinkTab ?: "home",
                    onOnboardingComplete = { name -> appViewModel.completeOnboarding(name) },
                    appViewModel  = appViewModel
                )
            }
        }
    }
}

// ── App navigation flow: splash → (welcome?) → main ──────────────────────────
private enum class Screen { SPLASH, WELCOME, MAIN }

@Composable
private fun AppFlow(
    isFirstLaunch: Boolean,
    username: String,
    appTheme: AppTheme,
    startTab: String,
    onOnboardingComplete: (String) -> Unit,
    appViewModel: AppViewModel
) {
    var screen by remember { mutableStateOf(Screen.SPLASH) }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            fadeIn(animationSpec = androidx.compose.animation.core.tween(400)) togetherWith
            fadeOut(animationSpec = androidx.compose.animation.core.tween(400))
        },
        label = "app_flow"
    ) { current ->
        when (current) {
            Screen.SPLASH -> {
                SplashScreen(
                    onFinished = {
                        screen = if (isFirstLaunch) Screen.WELCOME else Screen.MAIN
                    }
                )
            }
            Screen.WELCOME -> {
                WelcomeScreen(
                    onGetStarted = { name ->
                        onOnboardingComplete(name)
                        screen = Screen.MAIN
                    }
                )
            }
            Screen.MAIN -> {
                // Pass username down to HomeScreen via wrapper
                MainNavigationWithUsername(
                    username     = username,
                    startTab     = startTab,
                    appTheme     = appTheme,
                    appViewModel = appViewModel
                )
            }
        }
    }
}

// ── Wrapper that provides username context to HomeScreen ──────────────────────
@Composable
private fun MainNavigationWithUsername(
    username: String,
    startTab: String,
    appTheme: AppTheme,
    appViewModel: AppViewModel
) {
    // We pass username into MainNavigation which routes it into HomeScreen
    MainNavigation(
        startTab     = startTab,
        appTheme     = appTheme,
        appViewModel = appViewModel,
        username     = username
    )
}
