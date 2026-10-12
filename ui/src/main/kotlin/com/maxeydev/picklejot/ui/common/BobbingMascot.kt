package com.maxeydev.picklejot.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.ui.dashboard.motionScale

private val BOB_DISTANCE = 3.dp
private const val BOB_MILLIS = 450

@Composable
fun BobbingMascot(
    mascot: Mascot,
    modifier: Modifier = Modifier,
) {
    val lift = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (motionScale() > 0f) {
            lift.animateTo(
                targetValue = -1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = BOB_MILLIS, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
            )
        }
    }
    MascotImage(
        mascot = mascot,
        modifier = modifier.graphicsLayer { translationY = lift.value * BOB_DISTANCE.toPx() },
    )
}
