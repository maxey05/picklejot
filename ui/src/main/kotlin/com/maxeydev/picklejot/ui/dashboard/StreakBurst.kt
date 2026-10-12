package com.maxeydev.picklejot.ui.dashboard

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal const val BURST_MILLIS = 900
internal const val BURST_DELAY_MILLIS = 300L

private const val PULSE_PEAK = 0.25f
private const val PULSE_END = 0.6f
private const val PILL_SCALE_GAIN = 0.12f
private const val FLAME_WIGGLE_DEGREES = 12f
private const val FLAME_WIGGLE_CYCLES = 2f
private const val RING_COUNT = 2
private const val RING_STAGGER = 0.15f
private const val RING_SPAN = 0.7f
private const val RING_ALPHA = 0.6f
private const val SPARK_COUNT = 7
private const val SPARK_FIRST_DEGREES = -160f
private const val SPARK_LAST_DEGREES = -20f
private const val SPARK_DELAY = 0.1f
private const val SPARK_SPAN = 0.7f
private const val SPARK_SHRINK = 0.5f
private val RingGrowth = 28.dp
private val RingStroke = 2.dp
private val SparkStart = 6.dp
private val SparkTravel = 26.dp
private val SparkRadius = 2.dp

internal fun burstPulse(progress: Float): Float =
    when {
        progress <= 0f || progress >= PULSE_END -> 0f
        progress < PULSE_PEAK -> progress / PULSE_PEAK
        else -> 1f - (progress - PULSE_PEAK) / (PULSE_END - PULSE_PEAK)
    }

internal fun flameWiggle(progress: Float): Float =
    (sin(progress * FLAME_WIGGLE_CYCLES * 2.0 * PI) * FLAME_WIGGLE_DEGREES * (1f - progress)).toFloat()

internal fun Modifier.streakBurst(
    progress: () -> Float,
    color: Color,
    originX: Dp,
): Modifier =
    this
        .drawBehind { drawBurst(progress(), color, originX.toPx()) }
        .graphicsLayer {
            val scale = 1f + PILL_SCALE_GAIN * burstPulse(progress())
            scaleX = scale
            scaleY = scale
        }

private fun DrawScope.drawBurst(
    progress: Float,
    color: Color,
    originX: Float,
) {
    if (progress <= 0f || progress >= 1f) {
        return
    }
    repeat(RING_COUNT) { ring ->
        val local = ((progress - ring * RING_STAGGER) / RING_SPAN).coerceIn(0f, 1f)
        if (local > 0f && local < 1f) {
            val grow = RingGrowth.toPx() * local
            drawRoundRect(
                color = color.copy(alpha = (1f - local) * RING_ALPHA),
                topLeft = Offset(-grow, -grow),
                size = Size(size.width + 2f * grow, size.height + 2f * grow),
                cornerRadius = CornerRadius(size.height / 2f + grow),
                style = Stroke(width = RingStroke.toPx()),
            )
        }
    }
    val sparkProgress = ((progress - SPARK_DELAY) / SPARK_SPAN).coerceIn(0f, 1f)
    if (sparkProgress <= 0f || sparkProgress >= 1f) {
        return
    }
    val origin = Offset(originX, size.height / 2f)
    val eased = 1f - (1f - sparkProgress) * (1f - sparkProgress)
    val distance = SparkStart.toPx() + SparkTravel.toPx() * eased
    repeat(SPARK_COUNT) { index ->
        val degrees = SPARK_FIRST_DEGREES + index * (SPARK_LAST_DEGREES - SPARK_FIRST_DEGREES) / (SPARK_COUNT - 1)
        val radians = degrees * (PI / 180.0)
        drawCircle(
            color = color.copy(alpha = 1f - sparkProgress),
            radius = SparkRadius.toPx() * (1f - SPARK_SHRINK * sparkProgress),
            center = origin + Offset((cos(radians) * distance).toFloat(), (sin(radians) * distance).toFloat()),
        )
    }
}
