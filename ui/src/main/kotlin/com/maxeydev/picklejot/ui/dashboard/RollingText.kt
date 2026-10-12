package com.maxeydev.picklejot.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal val FinalRollEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

internal object RollPlan {
    const val FINAL_MILLIS = 300
    private const val MAX_STEPS = 10
    private const val TARGET_TOTAL_MILLIS = 320f
    private const val MIN_BASE_MILLIS = 70f
    private const val MAX_BASE_MILLIS = 150f

    fun steps(
        from: Int,
        to: Int,
    ): List<Int> {
        if (from == to) {
            return emptyList()
        }
        val every = if (to > from) ((from + 1)..to).toList() else (from - 1 downTo to).toList()
        if (every.size <= MAX_STEPS) {
            return every
        }
        return List(MAX_STEPS) { index ->
            every[(index.toFloat() * (every.size - 1) / (MAX_STEPS - 1)).roundToInt()]
        }
    }

    fun gapMillis(
        index: Int,
        count: Int,
    ): Long {
        val base = (TARGET_TOTAL_MILLIS / count).coerceIn(MIN_BASE_MILLIS, MAX_BASE_MILLIS)
        val position = if (count == 1) 1f else index.toFloat() / (count - 1)
        return (base * (0.75f + 0.5f * position)).roundToLong()
    }

    fun durationMillis(
        index: Int,
        count: Int,
    ): Int = if (index == count - 1) FINAL_MILLIS else gapMillis(index, count).toInt()
}

internal class RollStep {
    var isRising: Boolean = true
    var durationMillis: Int = RollPlan.FINAL_MILLIS
    var easing: Easing = FinalRollEasing
}

internal suspend fun motionScale(): Float = currentCoroutineContext()[MotionDurationScale]?.scaleFactor ?: 1f

@Composable
internal fun rememberRolledValue(
    target: Int,
    step: RollStep,
    startDelayMillis: Long = 0L,
): Int {
    var shown by rememberSaveable { mutableIntStateOf(target) }
    LaunchedEffect(target) {
        if (shown == target) {
            return@LaunchedEffect
        }
        val scale = motionScale()
        if (scale == 0f) {
            shown = target
            return@LaunchedEffect
        }
        delay((startDelayMillis * scale).roundToLong())
        val isRising = target > shown
        val plan = RollPlan.steps(shown, target)
        plan.forEachIndexed { index, value ->
            val isLast = index == plan.lastIndex
            step.isRising = isRising
            step.durationMillis = RollPlan.durationMillis(index, plan.size)
            step.easing = if (isLast) FinalRollEasing else LinearEasing
            shown = value
            if (!isLast) {
                delay((RollPlan.gapMillis(index, plan.size) * scale).roundToLong())
            }
        }
    }
    return shown
}

@Composable
internal fun RollingText(
    value: String,
    step: RollStep,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    travelFraction: Float = 1f,
) {
    var previous by remember { mutableStateOf(value) }
    var current by remember { mutableStateOf(value) }
    var direction by remember { mutableIntStateOf(1) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(value) {
        if (value == current) {
            return@LaunchedEffect
        }
        previous = current
        current = value
        direction = if (step.isRising) 1 else -1
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = step.durationMillis, easing = step.easing))
        previous = current
    }
    Row(modifier = modifier.clearAndSetSemantics { text = AnnotatedString(current) }) {
        val slots = max(previous.length, current.length)
        for (fromEnd in slots downTo 1) {
            key(fromEnd) {
                val before = previous.getOrNull(previous.length - fromEnd)
                val after = current.getOrNull(current.length - fromEnd)
                if (before == after) {
                    Text(text = after?.toString().orEmpty(), style = style, color = color)
                } else {
                    Box(modifier = Modifier.clipToBounds()) {
                        if (before != null) {
                            Text(
                                text = before.toString(),
                                style = style,
                                color = color,
                                modifier =
                                    Modifier.graphicsLayer {
                                        translationY = -direction * progress.value * size.height * travelFraction
                                        alpha = 1f - progress.value
                                    },
                            )
                        }
                        if (after != null) {
                            Text(
                                text = after.toString(),
                                style = style,
                                color = color,
                                modifier =
                                    Modifier.graphicsLayer {
                                        translationY = direction * (1f - progress.value) * size.height * travelFraction
                                        alpha = progress.value
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}
