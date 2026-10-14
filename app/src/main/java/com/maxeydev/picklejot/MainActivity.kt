package com.maxeydev.picklejot

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.maxeydev.picklejot.domain.settings.AppSettings
import com.maxeydev.picklejot.ui.navigation.PicklejotNavHost
import com.maxeydev.picklejot.ui.notification.StreakNotification
import com.maxeydev.picklejot.ui.theme.PicklejotTheme

private val LIGHT_NAVIGATION_SCRIM = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
private val DARK_NAVIGATION_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

class MainActivity : ComponentActivity() {
    private var openLogging by mutableStateOf(false)

    private var contentReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { !contentReady }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        openLogging = StreakNotification.wantsLogging(intent)
        val container = (application as PicklejotApplication).container
        setContent {
            val dependencies by produceState<AppContainer?>(initialValue = null, container) {
                value = container.await()
            }
            dependencies?.let { ready ->
                val settings by produceState<AppSettings?>(initialValue = null, ready) {
                    ready.appSettingsStore.observe().collect { value = it }
                }
                settings?.let { loaded ->
                    val darkTheme = loaded.darkTheme ?: isSystemInDarkTheme()
                    DisposableEffect(darkTheme) {
                        enableEdgeToEdge(
                            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                            navigationBarStyle =
                                SystemBarStyle.auto(LIGHT_NAVIGATION_SCRIM, DARK_NAVIGATION_SCRIM) { darkTheme },
                        )
                        onDispose { }
                    }
                    LaunchedEffect(ready, loaded.soundEffectsEnabled) {
                        ready.soundEffects.setEnabled(loaded.soundEffectsEnabled)
                    }
                    PicklejotTheme(darkTheme = darkTheme) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            PicklejotNavHost(
                                dependencies = ready,
                                openLogging = openLogging,
                                onReady = { contentReady = true },
                                onOpenLoggingHandled = {
                                    openLogging = false
                                    intent.removeExtra(StreakNotification.EXTRA_OPEN_LOGGING)
                                },
                            )
                            LaunchedEffect(ready) {
                                withFrameNanos { }
                                Looper.myQueue().addIdleHandler {
                                    ready.warmCardRenderer()
                                    ready.soundEffects.preload()
                                    false
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openLogging = StreakNotification.wantsLogging(intent)
    }
}
