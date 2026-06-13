package com.rapsodo.golf.ui.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.delay
import com.rapsodo.golf.domain.logger.AppLogger
import com.rapsodo.golf.R
import io.github.aakira.napier.Napier

@Composable
fun SplashScreen(onReady: () -> Unit) {
    LaunchedEffect(Unit) {
        Napier.d(tag = AppLogger.TAG) { "Splash started" }
        delay(2000)
        Napier.d(tag = AppLogger.TAG) { "Splash complete — navigating to list" }
        onReady()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "golf_ball_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier.fillMaxSize()
        // no background — window drawable shows through
    ) {
        Image(
            painter = painterResource(id = R.drawable.golf_ball),
            contentDescription = "Golf ball",
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .graphicsLayer { rotationZ = rotation }
        )
    }
}