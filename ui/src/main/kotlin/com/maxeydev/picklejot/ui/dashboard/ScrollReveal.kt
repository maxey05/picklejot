package com.maxeydev.picklejot.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first

private const val ENTRANCE_MILLIS = 520
private val ENTRANCE_OFFSET = 22.dp
private val REVEAL_MARGIN = 72.dp
private val SLIDE_DISTANCE = 32.dp

@Stable
internal class RevealState(
    val scroll: ScrollState,
) {
    var viewportTop by mutableFloatStateOf(0f)
    var viewportHeight by mutableIntStateOf(0)

    fun contentTopOf(positionInRoot: Float): Float = positionInRoot + scroll.value - viewportTop
}

internal val LocalRevealState = staticCompositionLocalOf<RevealState?> { null }

internal fun Modifier.trackViewport(state: RevealState): Modifier =
    onGloballyPositioned { coordinates ->
        state.viewportTop = coordinates.positionInRoot().y
        state.viewportHeight = coordinates.size.height
    }

@Stable
internal class SectionReveal(
    val revealed: Boolean,
    val track: Modifier,
    val entrance: Modifier,
) {
    val modifier: Modifier
        get() = track.then(entrance)
}

@Composable
internal fun rememberSectionReveal(key: String): SectionReveal {
    val state = LocalRevealState.current
    var revealed by rememberSaveable(key) { mutableStateOf(state == null) }
    var top by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    val density = LocalDensity.current
    val marginPx = with(density) { REVEAL_MARGIN.roundToPx() }
    val offsetPx = with(density) { ENTRANCE_OFFSET.toPx() }
    val entrance = remember { Animatable(if (revealed) 1f else 0f) }

    if (state != null && !revealed) {
        LaunchedEffect(state) {
            snapshotFlow { (state.scroll.value + state.viewportHeight - marginPx).toFloat() >= top }.first { it }
            revealed = true
        }
    }
    LaunchedEffect(revealed) {
        if (revealed) {
            entrance.animateTo(1f, tween(ENTRANCE_MILLIS, easing = FastOutSlowInEasing))
        }
    }

    val track =
        Modifier.onGloballyPositioned { coordinates ->
            if (state != null) {
                top = state.contentTopOf(coordinates.positionInRoot().y)
            }
        }
    val entranceModifier =
        Modifier.graphicsLayer {
            alpha = entrance.value
            translationY = (1f - entrance.value) * offsetPx
        }
    return SectionReveal(revealed = revealed, track = track, entrance = entranceModifier)
}

@Composable
internal fun rememberRevealProgress(
    revealed: Boolean,
    durationMillis: Int,
    delayMillis: Int = 0,
    easing: Easing = FastOutSlowInEasing,
): Animatable<Float, AnimationVector1D> {
    val progress = remember { Animatable(if (revealed) 1f else 0f) }
    LaunchedEffect(revealed) {
        if (revealed) {
            progress.animateTo(1f, tween(durationMillis, delayMillis, easing))
        }
    }
    return progress
}

internal fun Modifier.slideInFromEnd(progress: Animatable<Float, AnimationVector1D>): Modifier =
    graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        translationX = (1f - progress.value) * SLIDE_DISTANCE.toPx()
    }

internal fun Modifier.growFromBottom(progress: Animatable<Float, AnimationVector1D>): Modifier =
    graphicsLayer {
        scaleY = progress.value
        transformOrigin = TransformOrigin(0.5f, 1f)
    }

internal fun Modifier.growFromTop(progress: Animatable<Float, AnimationVector1D>): Modifier =
    graphicsLayer {
        scaleY = progress.value
        transformOrigin = TransformOrigin(0.5f, 0f)
    }
