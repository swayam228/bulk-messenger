package com.example.bulkmessenger.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

private const val NAV_ANIM_DURATION_MS = 200

/**
 * One shared, short (200ms) transition set for every screen change, so navigation reads as a
 * smooth slide instead of the default instant cut — applied once at the NavHost level.
 */
object NavAnimations {
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth / 4 },
            animationSpec = tween(NAV_ANIM_DURATION_MS)
        ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION_MS))
    }

    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = tween(NAV_ANIM_DURATION_MS))
    }

    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = tween(NAV_ANIM_DURATION_MS))
    }

    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth / 4 },
            animationSpec = tween(NAV_ANIM_DURATION_MS)
        ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION_MS))
    }
}
