package com.rapsodo.golf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import com.rapsodo.golf.ui.players.PlayerListScreen
import com.rapsodo.golf.ui.splash.SplashScreen
import com.rapsodo.golf.ui.theme.GolfTrackerTheme
import com.rapsodo.golf.domain.logger.AppLogger

import dagger.hilt.android.AndroidEntryPoint
import io.github.aakira.napier.Napier

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GolfTrackerTheme {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    Napier.i(tag = AppLogger.TAG) { "Showing splash view" }
                    SplashScreen(onReady = { showSplash = false })
                } else {
                    Napier.i(tag = AppLogger.TAG) { "Showing players list view" }
                    PlayerListScreen()
                }
            }
        }
    }
}