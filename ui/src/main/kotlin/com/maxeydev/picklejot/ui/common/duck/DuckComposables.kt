package com.maxeydev.picklejot.ui.common.duck

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import com.maxeydev.picklejot.ui.dashboard.motionScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

private const val HELLO_DELAY_MILLIS = 250L
private const val SMASH_DELAY_MILLIS = 450L
private const val HATCH_DELAY_MILLIS = 650L
private const val BOOP_MILLIS = 560
private const val SWAP_HALF_MILLIS = 100

internal class DuckClock {
    var millis by mutableLongStateOf(0L)
    var frozen by mutableStateOf(false)
}

// Compose tests install an InfiniteAnimationPolicy that cancels endless frame loops; a bare
// withFrameNanos loop would keep waitForIdle from ever returning.
private suspend fun withLoopFrameNanos(onFrame: (Long) -> Unit) {
    val policy = coroutineContext[InfiniteAnimationPolicy]
    if (policy == null) withFrameNanos(onFrame) else policy.onInfiniteOperation { withFrameNanos(onFrame) }
}

@Composable
internal fun rememberDuckClock(): DuckClock {
    val clock = remember { DuckClock() }
    LaunchedEffect(Unit) {
        if (motionScale() > 0f) {
            val origin = withFrameNanos { it }
            while (true) {
                withLoopFrameNanos { clock.millis = (it - origin) / NANOS_PER_MILLI }
            }
        } else {
            clock.frozen = true
        }
    }
    return clock
}

@Composable
private fun rememberOneShot(
    end: Float,
    cycleMillis: Int,
    delayMillis: Long,
): Animatable<Float, AnimationVector1D> {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (motionScale() > 0f) {
            delay(delayMillis)
            progress.animateTo(end, tween(durationMillis = (end * cycleMillis).toInt(), easing = LinearEasing))
        } else {
            progress.snapTo(end)
        }
    }
    return progress
}

@Composable
internal fun ReadyDuck(modifier: Modifier = Modifier) {
    val clock = rememberDuckClock()
    val hello = rememberOneShot(ReadyTimeline.HELLO_END, ReadyTimeline.HELLO_CYCLE_MILLIS, HELLO_DELAY_MILLIS)
    val boop = remember { Animatable(1f) }
    val pose = remember { DuckPose() }
    val scratch = remember { FloatArray(FRAME_CHANNELS) }
    val scope = rememberCoroutineScope()
    Canvas(
        modifier =
            modifier
                .aspectRatio(READY_VIEW_WIDTH / READY_VIEW_HEIGHT)
                .graphicsLayer { alpha = ReadyTimeline.helloAlpha(hello.value) }
                .pointerInput(Unit) {
                    detectTapGestures {
                        scope.launch {
                            if (motionScale() > 0f) {
                                boop.snapTo(0f)
                                boop.animateTo(1f, tween(durationMillis = BOOP_MILLIS, easing = LinearEasing))
                            }
                        }
                    }
                },
    ) {
        ReadyTimeline.apply(pose, clock.millis, hello.value, boop.value, scratch)
        drawReadyDuck(pose)
    }
}

@Composable
internal fun SmashDuck(modifier: Modifier = Modifier) {
    val progress = rememberOneShot(SmashTimeline.ACTIVE_END, SmashTimeline.CYCLE_MILLIS, SMASH_DELAY_MILLIS)
    val pose = remember { DuckPose() }
    val scratch = remember { FloatArray(FRAME_CHANNELS) }
    Canvas(modifier = modifier.aspectRatio(SMASH_VIEW_WIDTH / SMASH_VIEW_HEIGHT)) {
        SmashTimeline.apply(pose, progress.value, scratch)
        drawSmashDuck(pose)
    }
}

@Composable
internal fun BallBuddyDuck(modifier: Modifier = Modifier) {
    val clock = rememberDuckClock()
    val pose = remember { DuckPose() }
    val scratch = remember { FloatArray(FRAME_CHANNELS) }
    Canvas(modifier = modifier.aspectRatio(BUDDY_VIEW_WIDTH / BUDDY_VIEW_HEIGHT)) {
        BuddyTimeline.apply(pose, clock.millis, scratch)
        drawBuddyDuck(pose)
    }
}

@Composable
internal fun HatchlingDuck(modifier: Modifier = Modifier) {
    val progress = rememberOneShot(HatchTimeline.REST, HatchTimeline.CYCLE_MILLIS, HATCH_DELAY_MILLIS)
    val pose = remember { HatchPose() }
    val scratch = remember { FloatArray(FRAME_CHANNELS) }
    Canvas(modifier = modifier.aspectRatio(HATCH_VIEW_WIDTH / HATCH_VIEW_HEIGHT)) {
        HatchTimeline.apply(pose, progress.value, scratch)
        drawHatchling(pose)
    }
}

@Composable
internal fun DuckHead(
    expression: DuckExpression,
    modifier: Modifier = Modifier,
) {
    val clock = rememberDuckClock()
    val swap = remember { Animatable(1f) }
    var shown by remember { mutableStateOf(expression) }
    val pose = remember { HeadPose() }
    val scratch = remember { FloatArray(FRAME_CHANNELS) }
    LaunchedEffect(expression) {
        if (expression == shown) {
            return@LaunchedEffect
        }
        if (motionScale() > 0f) {
            swap.snapTo(0f)
            swap.animateTo(0.5f, tween(durationMillis = SWAP_HALF_MILLIS, easing = LinearEasing))
            shown = expression
            swap.animateTo(1f, tween(durationMillis = SWAP_HALF_MILLIS, easing = LinearEasing))
        } else {
            shown = expression
            swap.snapTo(1f)
        }
    }
    Canvas(modifier = modifier.aspectRatio(HEAD_VIEW_WIDTH / HEAD_VIEW_HEIGHT)) {
        HeadTimeline.apply(pose, shown, clock.millis, clock.frozen, swap.value, scratch)
        drawHead(pose, HEAD_VIEW_HEIGHT)
    }
}

@Composable
internal fun WorkingDuck(modifier: Modifier = Modifier) {
    val clock = rememberDuckClock()
    val pose = remember { HeadPose() }
    val scratch = remember { FloatArray(FRAME_CHANNELS) }
    Canvas(modifier = modifier.aspectRatio(HEAD_VIEW_WIDTH / WORKING_VIEW_HEIGHT)) {
        HeadTimeline.applyWorking(pose, clock.millis, scratch)
        drawHead(pose, WORKING_VIEW_HEIGHT)
    }
}
