package com.maxeydev.picklejot.ui.common.duck

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.util.lerp

internal val EaseInOutCurve = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
internal val EaseOutCurve = CubicBezierEasing(0f, 0f, 0.58f, 1f)
internal val EaseInCurve = CubicBezierEasing(0.42f, 0f, 1f, 1f)
internal val BoopCurve = CubicBezierEasing(0.3f, 0.7f, 0.4f, 1f)

internal class Frames(
    private val easing: Easing,
    private val rows: Array<FloatArray>,
) {
    private val channels = rows[0].size - 1

    fun sample(
        progress: Float,
        out: FloatArray,
    ) {
        val last = rows.size - 1
        if (progress <= rows[0][0]) {
            copy(rows[0], out)
            return
        }
        if (progress >= rows[last][0]) {
            copy(rows[last], out)
            return
        }
        var index = 0
        while (rows[index + 1][0] < progress) {
            index++
        }
        val from = rows[index]
        val to = rows[index + 1]
        val eased = easing.transform((progress - from[0]) / (to[0] - from[0]))
        for (channel in 1..channels) {
            out[channel - 1] = lerp(from[channel], to[channel], eased)
        }
    }

    private fun copy(
        row: FloatArray,
        out: FloatArray,
    ) {
        for (channel in 1..channels) {
            out[channel - 1] = row[channel]
        }
    }
}

internal fun frames(
    easing: Easing,
    vararg rows: FloatArray,
): Frames = Frames(easing, arrayOf(*rows))

internal fun kf(
    percent: Float,
    vararg values: Float,
): FloatArray = floatArrayOf(percent / 100f, *values)

internal fun loopPhase(
    millis: Long,
    periodMillis: Int,
): Float = (millis % periodMillis).toFloat() / periodMillis

internal const val NANOS_PER_MILLI = 1_000_000L
