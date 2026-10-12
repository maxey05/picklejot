package com.maxeydev.picklejot.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset

private const val SCREEN_MOVE_MILLIS = 340
private const val UNDERLAY_SHIFT_DIVISOR = 4

private val ScreenEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private val ScreenSpec: FiniteAnimationSpec<IntOffset> =
    tween(durationMillis = SCREEN_MOVE_MILLIS, easing = ScreenEasing)

internal val ScreenEnter: EnterTransition =
    slideInHorizontally(animationSpec = ScreenSpec) { fullWidth -> fullWidth }

internal val ScreenExit: ExitTransition =
    slideOutHorizontally(animationSpec = ScreenSpec) { fullWidth -> -fullWidth / UNDERLAY_SHIFT_DIVISOR }

internal val ScreenPopEnter: EnterTransition =
    slideInHorizontally(animationSpec = ScreenSpec) { fullWidth -> -fullWidth / UNDERLAY_SHIFT_DIVISOR }

internal val ScreenPopExit: ExitTransition =
    slideOutHorizontally(animationSpec = ScreenSpec) { fullWidth -> fullWidth }

private val PredictiveSpec: FiniteAnimationSpec<IntOffset> =
    tween(durationMillis = SCREEN_MOVE_MILLIS, easing = LinearEasing)

internal val ScreenPredictivePopEnter: EnterTransition =
    slideInHorizontally(animationSpec = PredictiveSpec) { fullWidth -> -fullWidth / UNDERLAY_SHIFT_DIVISOR }

internal val ScreenPredictivePopExit: ExitTransition =
    slideOutHorizontally(animationSpec = PredictiveSpec) { fullWidth -> fullWidth }
