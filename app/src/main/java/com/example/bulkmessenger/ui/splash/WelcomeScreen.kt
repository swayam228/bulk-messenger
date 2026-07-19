package com.example.bulkmessenger.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val BUBBLE_FADE_IN_MS = 250
private const val CHAR_TYPE_DELAY_MS = 40L
private const val POST_TYPE_HOLD_MS = 500L

/**
 * Shown briefly after the OS-level splash (which is icon-only) so we can display a
 * personalized/generic greeting before landing on Home or Onboarding. The greeting types itself
 * out letter by letter inside a chat-bubble, echoing what the app actually does.
 */
@Composable
fun WelcomeScreen(greeting: String, isReady: Boolean, onTimeout: () -> Unit) {
    val bubbleAlpha = remember { Animatable(0f) }
    var visibleChars by remember(greeting) { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        bubbleAlpha.animateTo(1f, tween(BUBBLE_FADE_IN_MS, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(greeting) {
        visibleChars = 0
        for (i in greeting.indices) {
            delay(CHAR_TYPE_DELAY_MS)
            visibleChars = i + 1
        }
    }
    // Waits for both Room's first response (isReady) and the typing animation to finish before
    // starting the auto-advance countdown, so we never route to Home/Onboarding on a guess or
    // cut the greeting off mid-type.
    LaunchedEffect(isReady, visibleChars) {
        if (isReady && visibleChars >= greeting.length) {
            delay(POST_TYPE_HOLD_MS)
            onTimeout()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 22.dp,
                    topEnd = 22.dp,
                    bottomEnd = 22.dp,
                    bottomStart = 4.dp
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .padding(horizontal = 32.dp)
                    .graphicsLayer { alpha = bubbleAlpha.value }
            ) {
                Text(
                    greeting.take(visibleChars),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
