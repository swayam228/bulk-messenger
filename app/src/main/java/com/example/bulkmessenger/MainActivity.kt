package com.example.bulkmessenger

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bulkmessenger.ui.navigation.AppNavHost
import com.example.bulkmessenger.ui.onboarding.OnboardingScreen
import com.example.bulkmessenger.ui.splash.WelcomeScreen
import com.example.bulkmessenger.ui.theme.BulkMessengerTheme
import com.example.bulkmessenger.viewmodel.SessionViewModel
import com.example.bulkmessenger.viewmodel.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var keepNativeSplash = true
        splashScreen.setKeepOnScreenCondition { keepNativeSplash }

        setContent {
            val sessionViewModel: SessionViewModel = viewModel()
            val isReady by sessionViewModel.isReady.collectAsState()
            val users by sessionViewModel.users.collectAsState()
            val activeUser by sessionViewModel.activeUser.collectAsState()
            val themeMode by sessionViewModel.themeMode.collectAsState()

            LaunchedEffect(isReady) { if (isReady) keepNativeSplash = false }

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            BulkMessengerTheme(darkTheme = darkTheme) {
                var showWelcome by remember { mutableStateOf(true) }
                when {
                    showWelcome -> {
                        val greeting = activeUser?.let { "Hello, ${it.name}" } ?: "Welcome"
                        WelcomeScreen(
                            greeting = greeting,
                            isReady = isReady,
                            onTimeout = { showWelcome = false }
                        )
                    }
                    users.isEmpty() -> {
                        OnboardingScreen(onCreateUser = { name, colorHex ->
                            sessionViewModel.createUser(name, colorHex)
                        })
                    }
                    else -> {
                        RootScreen(
                            sessionViewModel = sessionViewModel,
                            themeMode = themeMode,
                            onToggleTheme = { sessionViewModel.cycleTheme() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RootScreen(
    sessionViewModel: SessionViewModel,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val requiredPermissions = buildList {
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Check the real, current permission state instead of always starting false — otherwise this
    // screen flashes on every launch even after permission was already granted. SEND_SMS alone
    // gates whether the app is usable; the other permissions (contacts, phone state, notifications)
    // degrade gracefully when missing, so they shouldn't block Home.
    var permissionsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results[Manifest.permission.SEND_SMS] == true
    }

    // Re-requests only whichever of requiredPermissions the OS doesn't already have on file — the
    // system skips a dialog for any that are already granted, so this is what actually prompts for
    // a permission added in a later update (e.g. READ_PHONE_STATE) without re-flashing on the ones
    // already settled.
    LaunchedEffect(Unit) {
        val anyMissing = requiredPermissions.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (anyMissing) launcher.launch(requiredPermissions.toTypedArray())
    }

    if (permissionsGranted) {
        AppNavHost(sessionViewModel = sessionViewModel, themeMode = themeMode, onToggleTheme = onToggleTheme)
    } else {
        Scaffold { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("This app needs SMS permission to send messages. Please grant it to continue.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = { launcher.launch(requiredPermissions.toTypedArray()) }) {
                    Text("Grant Permission")
                }
            }
        }
    }
}
