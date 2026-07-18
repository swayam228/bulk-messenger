package com.example.bulkmessenger.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bulkmessenger.R
import kotlinx.coroutines.delay

private const val AUTO_ADVANCE_DELAY_MS = 900L
private const val LOGO_ANIM_DURATION_MS = 250

/**
 * Shown briefly after the OS-level splash (which is icon-only) so we can display a
 * personalized/generic greeting before landing on Home or Onboarding.
 */
@Composable
fun WelcomeScreen(greeting: String, isReady: Boolean, onTimeout: () -> Unit) {
    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(LOGO_ANIM_DURATION_MS, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(LOGO_ANIM_DURATION_MS, easing = FastOutSlowInEasing))
    }
    // Waits for Room's first response (isReady) before starting the auto-advance countdown, so we
    // never route to Home/Onboarding on a guess.
    LaunchedEffect(isReady) {
        if (isReady) {
            delay(AUTO_ADVANCE_DELAY_MS)
            onTimeout()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        this.alpha = alpha.value
                    }
            )
            Spacer(Modifier.height(20.dp))
            Text(
                greeting,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.graphicsLayer { this.alpha = alpha.value }
            )
        }
    }
}
