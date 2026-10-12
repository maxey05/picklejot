package com.maxeydev.picklejot.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleOut
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute

private const val SHEET_OPEN_MILLIS = 450
private const val SHEET_CLOSE_MILLIS = 300
private const val HOLD_MARGIN_MILLIS = 150
private const val HOLD_SCALE = 0.9995f

internal const val SHEET_READY_TIMEOUT_MILLIS = 250L
internal const val SHEET_SCRIM_ALPHA = 0.32f
internal val SHEET_CORNER = 28.dp

private val SheetOpenEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val SheetCloseEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

internal val SheetOpenSpec: FiniteAnimationSpec<Float> =
    tween(durationMillis = SHEET_OPEN_MILLIS, easing = SheetOpenEasing)

internal val SheetCloseSpec: FiniteAnimationSpec<Float> =
    tween(durationMillis = SHEET_CLOSE_MILLIS, easing = SheetCloseEasing)

private fun holdExit(durationMillis: Int): ExitTransition =
    scaleOut(animationSpec = tween(durationMillis = durationMillis), targetScale = HOLD_SCALE)

internal val SheetEnter: EnterTransition = EnterTransition.None

internal val SheetExit: ExitTransition = holdExit(SHEET_CLOSE_MILLIS + HOLD_MARGIN_MILLIS)

private val UnderSheetHold: ExitTransition =
    holdExit(SHEET_OPEN_MILLIS + SHEET_READY_TIMEOUT_MILLIS.toInt() + HOLD_MARGIN_MILLIS)

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.stayUnderSheet(): ExitTransition? =
    if (targetState.destination.hasRoute<MatchEditRoute>()) {
        UnderSheetHold
    } else {
        null
    }

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.returnFromSheet(): EnterTransition? =
    if (initialState.destination.hasRoute<MatchEditRoute>()) {
        EnterTransition.None
    } else {
        null
    }
