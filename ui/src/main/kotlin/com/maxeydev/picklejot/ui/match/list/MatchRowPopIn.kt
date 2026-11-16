package com.maxeydev.picklejot.ui.match.list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import com.maxeydev.picklejot.ui.dashboard.motionScale
import kotlinx.coroutines.launch

object MatchRowPopInMotion {
    const val START_SCALE = 0.6f
    const val START_ALPHA = 0f
    const val DAMPING_RATIO = 0.6f
    const val STIFFNESS = Spring.StiffnessMediumLow
    const val VISIBILITY_THRESHOLD = 0.001f
    const val ALPHA_MILLIS = 130
    val AlphaEasing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
}

internal fun matchRowPopInTarget(
    savedMatchId: String?,
    poppedInMatchId: String?,
): String? = savedMatchId?.takeIf { it != poppedInMatchId }

@Composable
internal fun Modifier.matchRowPopIn(
    matchId: String,
    enabled: Boolean,
    onStarted: () -> Unit,
): Modifier {
    if (!enabled) {
        return this
    }
    val scale = remember(matchId) { Animatable(MatchRowPopInMotion.START_SCALE) }
    val fade = remember(matchId) { Animatable(MatchRowPopInMotion.START_ALPHA) }
    val latestOnStarted by rememberUpdatedState(onStarted)
    LaunchedEffect(matchId) {
        latestOnStarted()
        if (motionScale() == 0f) {
            scale.snapTo(1f)
            fade.snapTo(1f)
            return@LaunchedEffect
        }
        launch {
            fade.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(
                        durationMillis = MatchRowPopInMotion.ALPHA_MILLIS,
                        easing = MatchRowPopInMotion.AlphaEasing,
                    ),
            )
        }
        scale.animateTo(
            targetValue = 1f,
            animationSpec =
                spring(
                    dampingRatio = MatchRowPopInMotion.DAMPING_RATIO,
                    stiffness = MatchRowPopInMotion.STIFFNESS,
                    visibilityThreshold = MatchRowPopInMotion.VISIBILITY_THRESHOLD,
                ),
        )
    }
    return graphicsLayer {
        transformOrigin = TransformOrigin.Center
        scaleX = scale.value
        scaleY = scale.value
        this.alpha = fade.value
    }
}
