package com.maxeydev.picklejot.ui.match.list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt

internal const val COLLAPSE_MILLIS = 320
internal val CollapseEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

@Stable
internal class CollapseProgress(
    initial: Float,
) {
    private val animatable = Animatable(initial)

    val value: Float get() = animatable.value

    val isRunning: Boolean get() = animatable.isRunning

    suspend fun animateTo(collapsed: Boolean) {
        animatable.animateTo(
            targetValue = if (collapsed) 1f else 0f,
            animationSpec = tween(durationMillis = COLLAPSE_MILLIS, easing = CollapseEasing),
        )
    }
}

@Composable
internal fun rememberCollapseProgress(isCollapsed: Boolean): CollapseProgress {
    val progress = remember { CollapseProgress(if (isCollapsed) 1f else 0f) }
    LaunchedEffect(isCollapsed) { progress.animateTo(isCollapsed) }
    return progress
}

internal fun Modifier.collapseVertically(fraction: () -> Float): Modifier =
    graphicsLayer { alpha = 1f - fraction() }
        .clipToBounds()
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val height = (placeable.height * (1f - fraction())).roundToInt()
            layout(placeable.width, height) {
                placeable.placeRelative(0, 0)
            }
        }
