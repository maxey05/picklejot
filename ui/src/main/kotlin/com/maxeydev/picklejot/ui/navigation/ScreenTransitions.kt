package com.maxeydev.picklejot.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute

private const val PUSH_MOVE_MILLIS = 300
private const val POP_MOVE_MILLIS = 250
private const val UNDERLAY_SHIFT_DIVISOR = 4

private val ScreenEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private val PushSpec: FiniteAnimationSpec<IntOffset> =
    tween(durationMillis = PUSH_MOVE_MILLIS, easing = ScreenEasing)

private val PopSpec: FiniteAnimationSpec<IntOffset> =
    tween(durationMillis = POP_MOVE_MILLIS, easing = ScreenEasing)

private fun underlayOffset(fullWidth: Int): Int = -fullWidth / UNDERLAY_SHIFT_DIVISOR

internal val ScreenEnter: EnterTransition =
    slideInHorizontally(animationSpec = PushSpec) { fullWidth -> fullWidth }

internal val ScreenExit: ExitTransition =
    slideOutHorizontally(animationSpec = PushSpec, targetOffsetX = ::underlayOffset)

private val ScreenPopEnter: EnterTransition =
    slideInHorizontally(animationSpec = PopSpec, initialOffsetX = ::underlayOffset)

private val ScreenPopExit: ExitTransition =
    slideOutHorizontally(animationSpec = PopSpec) { fullWidth -> fullWidth }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isLeavingSheet(): Boolean =
    initialState.destination.hasRoute<MatchEditRoute>()

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.screenPopEnter(): EnterTransition =
    if (isLeavingSheet()) SheetPopEnter else ScreenPopEnter

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.screenPopExit(): ExitTransition =
    if (isLeavingSheet()) SheetExit else ScreenPopExit
