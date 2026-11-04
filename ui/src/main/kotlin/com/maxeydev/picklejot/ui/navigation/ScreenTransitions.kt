package com.maxeydev.picklejot.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset

private const val SCREEN_MOVE_MILLIS = 340
private const val UNDERLAY_SHIFT_DIVISOR = 4
private const val POP_MOVE_MILLIS = 240
private const val POP_FADE_OUT_MILLIS = 90
private const val POP_FADE_IN_MILLIS = 150
private const val POP_SHIFT_FRACTION = 0.3f

private val ScreenEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val PopEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

private val ScreenSpec: FiniteAnimationSpec<IntOffset> =
    tween(durationMillis = SCREEN_MOVE_MILLIS, easing = ScreenEasing)

private val PopSpec: FiniteAnimationSpec<IntOffset> =
    tween(durationMillis = POP_MOVE_MILLIS, easing = PopEasing)

internal val ScreenEnter: EnterTransition =
    slideInHorizontally(animationSpec = ScreenSpec) { fullWidth -> fullWidth }

internal val ScreenExit: ExitTransition =
    slideOutHorizontally(animationSpec = ScreenSpec) { fullWidth -> -fullWidth / UNDERLAY_SHIFT_DIVISOR }

internal val ScreenPopEnter: EnterTransition =
    slideInHorizontally(animationSpec = PopSpec) { fullWidth -> -(fullWidth * POP_SHIFT_FRACTION).toInt() } +
        fadeIn(animationSpec = tween(durationMillis = POP_FADE_IN_MILLIS, delayMillis = POP_FADE_OUT_MILLIS))

internal val ScreenPopExit: ExitTransition =
    slideOutHorizontally(animationSpec = PopSpec) { fullWidth -> (fullWidth * POP_SHIFT_FRACTION).toInt() } +
        fadeOut(animationSpec = tween(durationMillis = POP_FADE_OUT_MILLIS))

private val PredictiveSpec: FiniteAnimationSpec<IntOffset> =
    tween(durationMillis = SCREEN_MOVE_MILLIS, easing = LinearEasing)

internal val ScreenPredictivePopEnter: EnterTransition =
    slideInHorizontally(animationSpec = PredictiveSpec) { fullWidth -> -fullWidth / UNDERLAY_SHIFT_DIVISOR }

internal val ScreenPredictivePopExit: ExitTransition =
    slideOutHorizontally(animationSpec = PredictiveSpec) { fullWidth -> fullWidth }
