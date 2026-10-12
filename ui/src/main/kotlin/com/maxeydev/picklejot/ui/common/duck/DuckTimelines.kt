package com.maxeydev.picklejot.ui.common.duck

import androidx.compose.animation.core.LinearEasing
import kotlin.math.abs
import kotlin.math.max

internal const val FRAME_CHANNELS = 4

internal enum class DuckExpression {
    SMILE,
    JOY,
    WINK,
    CHEER,
    OOPS,
}

internal object ReadyTimeline {
    const val HELLO_END = 0.7f
    const val HELLO_CYCLE_MILLIS = 4000
    private const val BREATHE_MILLIS = 3200
    private const val BLINK_MILLIS = 4600
    private const val TUFT_MILLIS = 6400

    private val breathe = frames(EaseInOutCurve, kf(0f, 1f, 1f), kf(50f, 0.988f, 1.028f), kf(100f, 1f, 1f))
    private val shadow = frames(EaseInOutCurve, kf(0f, 1f), kf(50f, 0.97f), kf(100f, 1f))
    private val sway = frames(EaseInOutCurve, kf(0f, 0f), kf(50f, -5f), kf(100f, 0f))
    private val flap = frames(EaseInOutCurve, kf(0f, 0f), kf(50f, 10f), kf(100f, 0f))
    private val blink = frames(LinearEasing, kf(0f, 1f), kf(92f, 1f), kf(95f, 0.1f), kf(100f, 1f))
    private val tuft =
        frames(
            EaseInOutCurve,
            kf(0f, 0f),
            kf(80f, 0f),
            kf(84f, -12f),
            kf(88f, 8f),
            kf(92f, -4f),
            kf(96f, 0f),
            kf(100f, 0f),
        )
    private val helloRig =
        frames(
            EaseOutCurve,
            kf(0f, -44f, 1f, 1f),
            kf(8f, -30f, 1f, 1f),
            kf(14f, 0f, 1.08f, 0.9f),
            kf(20f, -6f, 0.97f, 1.04f),
            kf(26f, 0f, 1f, 1f),
            kf(100f, 0f, 1f, 1f),
        )
    private val helloShadow = frames(EaseOutCurve, kf(0f, 0.4f), kf(8f, 0.4f), kf(14f, 1f), kf(100f, 1f))
    private val wave =
        frames(
            EaseInOutCurve,
            kf(0f, 0f),
            kf(28f, 0f),
            kf(34f, 38f),
            kf(40f, 8f),
            kf(46f, 38f),
            kf(52f, 8f),
            kf(58f, 38f),
            kf(66f, 0f),
            kf(100f, 0f),
        )
    private val helloJoy =
        frames(LinearEasing, kf(0f, 0f), kf(28f, 0f), kf(31f, 1f), kf(66f, 1f), kf(70f, 0f), kf(100f, 0f))
    private val boopRig =
        frames(
            BoopCurve,
            kf(0f, 0f, 1f, 1f),
            kf(18f, 0f, 1.1f, 0.86f),
            kf(45f, -26f, 0.94f, 1.08f),
            kf(70f, 0f, 1.05f, 0.94f),
            kf(85f, 0f, 0.98f, 1.02f),
            kf(100f, 0f, 1f, 1f),
        )
    private val boopFlick =
        frames(EaseOutCurve, kf(0f, 0f), kf(30f, 12f), kf(55f, -14f), kf(80f, 4f), kf(100f, 0f))
    private val boopJoy = frames(LinearEasing, kf(0f, 0f), kf(12f, 1f), kf(80f, 1f), kf(100f, 0f))
    private val boopShadow =
        frames(EaseOutCurve, kf(0f, 1f), kf(18f, 1f), kf(45f, 0.7f), kf(70f, 1f), kf(100f, 1f))

    fun helloAlpha(hello: Float): Float = (hello / 0.08f).coerceIn(0f, 1f)

    fun apply(
        pose: DuckPose,
        millis: Long,
        hello: Float,
        boop: Float,
        out: FloatArray,
    ) {
        pose.reset()
        val idle = loopPhase(millis, BREATHE_MILLIS)
        breathe.sample(idle, out)
        var sx = out[0]
        var sy = out[1]
        shadow.sample(idle, out)
        pose.shadowSx = out[0]
        sway.sample(idle, out)
        pose.padDegrees = out[0]
        flap.sample(idle, out)
        pose.wingDegrees = out[0]
        blink.sample(loopPhase(millis, BLINK_MILLIS), out)
        pose.eyeSy = out[0]
        tuft.sample(loopPhase(millis, TUFT_MILLIS), out)
        pose.tuftDegrees = out[0]
        helloRig.sample(hello, out)
        var ty = out[0]
        sx *= out[1]
        sy *= out[2]
        helloShadow.sample(hello, out)
        pose.shadowSx *= out[0]
        wave.sample(hello, out)
        pose.wingDegrees += out[0]
        helloJoy.sample(hello, out)
        var joy = out[0]
        boopRig.sample(boop, out)
        ty += out[0]
        sx *= out[1]
        sy *= out[2]
        boopFlick.sample(boop, out)
        pose.padDegrees += out[0]
        boopJoy.sample(boop, out)
        joy = max(joy, out[0])
        boopShadow.sample(boop, out)
        pose.shadowSx *= out[0]
        pose.rigTy = ty
        pose.rigSx = sx
        pose.rigSy = sy
        pose.joy = joy
    }
}

internal object SmashTimeline {
    const val ACTIVE_END = 0.54f
    const val CYCLE_MILLIS = 2800
    private const val TAIL_MILLIS = 700

    private val rig =
        frames(
            EaseInOutCurve,
            kf(0f, 0f, -8f, 1f, 1f),
            kf(9f, 6f, -4f, 1.06f, 0.9f),
            kf(16f, -26f, -12f, 0.95f, 1.07f),
            kf(30f, -30f, -10f, 1f, 1f),
            kf(40f, 4f, -6f, 1.07f, 0.9f),
            kf(47f, -3f, -8f, 0.98f, 1.03f),
            kf(54f, 0f, -8f, 1f, 1f),
            kf(100f, 0f, -8f, 1f, 1f),
        )
    private val pad =
        frames(
            EaseInOutCurve,
            kf(0f, 0f),
            kf(9f, 48f),
            kf(15f, 56f),
            kf(21f, -30f),
            kf(30f, -14f),
            kf(44f, 0f),
            kf(100f, 0f),
        )
    private val ball =
        frames(
            EaseOutCurve,
            kf(0f, 0f, 36f, -34f),
            kf(20f, 0f, 36f, -34f),
            kf(22f, 1f, 36f, -34f),
            kf(36f, 1f, 0f, 0f),
            kf(52f, 0f, -56f, 8f),
            kf(100f, 0f, -56f, 8f),
        )
    private val speed =
        frames(EaseOutCurve, kf(0f, 1f, 150f), kf(18f, 1f, 150f), kf(30f, 1f, 0f), kf(50f, 0f, 0f), kf(100f, 0f, 0f))
    private val tail =
        frames(EaseInOutCurve, kf(0f, 0f), kf(25f, -12f), kf(50f, 4f), kf(75f, -7f), kf(100f, 0f))
    private val wrist =
        frames(
            EaseInOutCurve,
            kf(0f, 0f),
            kf(14f, 0f),
            kf(20f, -34f),
            kf(27f, 8f),
            kf(34f, -24f),
            kf(44f, 0f),
            kf(100f, 0f),
        )
    private val shadow =
        frames(EaseInOutCurve, kf(0f, 1f), kf(9f, 1f), kf(16f, 0.68f), kf(30f, 0.68f), kf(40f, 1f), kf(100f, 1f))

    fun apply(
        pose: DuckPose,
        progress: Float,
        out: FloatArray,
    ) {
        pose.reset()
        pose.joy = 1f
        rig.sample(progress, out)
        pose.rigTy = out[0]
        pose.rigDegrees = out[1]
        pose.rigSx = out[2]
        pose.rigSy = out[3]
        pad.sample(progress, out)
        pose.padDegrees = out[0]
        ball.sample(progress, out)
        pose.ballAlpha = out[0]
        pose.ballTx = out[1]
        pose.ballTy = out[2]
        speed.sample(progress, out)
        pose.speedAlpha = out[0]
        pose.dashPhase = out[1]
        wrist.sample(progress, out)
        pose.wingDegrees = out[0]
        shadow.sample(progress, out)
        pose.shadowSx = out[0]
        if (progress < ACTIVE_END) {
            tail.sample(loopPhase((progress * CYCLE_MILLIS).toLong(), TAIL_MILLIS), out)
            pose.tailDegrees = out[0]
        }
    }
}

internal object BuddyTimeline {
    private const val RIG_MILLIS = 3000
    private const val SPARKLE_MILLIS = 2400

    private val rig =
        frames(
            EaseInOutCurve,
            kf(0f, 0f, 1f, 1f),
            kf(25f, -3f, 1f, 1f),
            kf(50f, 0f, 1.02f, 0.975f),
            kf(75f, 3f, 1f, 1f),
            kf(100f, 0f, 1f, 1f),
        )
    private val ball = frames(EaseInOutCurve, kf(0f, 1f, 1f), kf(50f, 1.06f, 0.93f), kf(100f, 1f, 1f))
    private val twinkle =
        frames(EaseInOutCurve, kf(0f, 0.25f, 0.45f, 0f), kf(50f, 1f, 1.1f, 45f), kf(100f, 0.25f, 0.45f, 0f))

    fun apply(
        pose: DuckPose,
        millis: Long,
        out: FloatArray,
    ) {
        pose.reset()
        pose.joy = 1f
        val loop = loopPhase(millis, RIG_MILLIS)
        rig.sample(loop, out)
        pose.rigDegrees = out[0]
        pose.rigSx = out[1]
        pose.rigSy = out[2]
        ball.sample(loop, out)
        pose.ballSx = out[0]
        pose.ballSy = out[1]
        val sparkle = loopPhase(millis, SPARKLE_MILLIS)
        for (index in 0 until SPARKLE_COUNT) {
            twinkle.sample((sparkle + index / SPARKLE_COUNT.toFloat()) % 1f, out)
            val base = index * SPARKLE_CHANNELS
            pose.sparkles[base] = out[0]
            pose.sparkles[base + 1] = out[1]
            pose.sparkles[base + 2] = out[2]
        }
    }
}

internal object HatchTimeline {
    const val REST = 0.88f
    const val CYCLE_MILLIS = 5000
    private const val WHOLE_END = 0.3725f

    private val egg =
        frames(
            EaseInOutCurve,
            kf(0f, 0f),
            kf(8f, 0f),
            kf(12f, -7f),
            kf(16f, 6f),
            kf(20f, -3f),
            kf(23f, 0f),
            kf(27f, -6f),
            kf(30f, 5f),
            kf(33f, 0f),
            kf(100f, 0f),
        )
    private val crack =
        frames(EaseInCurve, kf(0f, 1f, 160f), kf(29f, 1f, 160f), kf(36f, 1f, 0f), kf(37.5f, 0f, 0f), kf(100f, 0f, 0f))
    private val cap =
        frames(
            EaseOutCurve,
            kf(0f, 0f, 0f, 0f, 0f),
            kf(37f, 0f, 0f, 0f, 0f),
            kf(37.5f, 1f, 0f, 0f, 0f),
            kf(48f, 1f, 30f, -56f, 38f),
            kf(56f, 0f, 44f, -70f, 56f),
            kf(100f, 0f, 44f, -70f, 56f),
        )
    private val head =
        frames(
            EaseOutCurve,
            kf(0f, 48f, 1f, 1f),
            kf(38f, 48f, 1f, 1f),
            kf(45f, -10f, 0.95f, 1.07f),
            kf(51f, 3f, 1.05f, 0.95f),
            kf(56f, 0f, 1f, 1f),
            kf(100f, 0f, 1f, 1f),
        )
    private val wings = frames(EaseOutCurve, kf(0f, 0f), kf(52f, 0f), kf(57f, 1.15f), kf(61f, 1f), kf(100f, 1f))
    private val spark =
        frames(
            EaseOutCurve,
            kf(0f, 0f, 0f, 0f),
            kf(58f, 0f, 0f, 0f),
            kf(64f, 1f, 1.2f, 45f),
            kf(72f, 1f, 0.85f, 60f),
            kf(80f, 1f, 1.05f, 80f),
            kf(88f, 1f, 1f, 90f),
            kf(100f, 1f, 1f, 90f),
        )
    private val blink = frames(LinearEasing, kf(0f, 1f), kf(74f, 1f), kf(76f, 0.1f), kf(78f, 1f), kf(100f, 1f))

    fun apply(
        pose: HatchPose,
        progress: Float,
        out: FloatArray,
    ) {
        egg.sample(progress, out)
        pose.eggDegrees = out[0]
        pose.wholeAlpha = if (progress < WHOLE_END) 1f else 0f
        crack.sample(progress, out)
        pose.crackAlpha = out[0]
        pose.crackPhase = out[1]
        cap.sample(progress, out)
        pose.capAlpha = out[0]
        pose.capTx = out[1]
        pose.capTy = out[2]
        pose.capDegrees = out[3]
        head.sample(progress, out)
        pose.headTy = out[0]
        pose.headSx = out[1]
        pose.headSy = out[2]
        wings.sample(progress, out)
        pose.wingScale = out[0]
        spark.sample(progress, out)
        pose.sparkAlpha = out[0]
        pose.sparkScale = out[1]
        pose.sparkDegrees = out[2]
        blink.sample(progress, out)
        pose.eyeSy = out[0]
    }
}

internal object HeadTimeline {
    private const val IDLE_MILLIS = 5000
    private const val HOP_MILLIS = 1600
    private const val WINK_MILLIS = 2800
    private const val BLINK_MILLIS = 3000
    private const val CHEER_MILLIS = 1800
    private const val OOPS_MILLIS = 2400
    private const val WORK_MILLIS = 1200
    private const val WORK_BLINK_MILLIS = 3600
    private const val FULL_TURN = 360f
    private const val SWAP_DEPTH = 0.9f
    private const val SWAP_SQUASH = 0.04f
    private const val SWAP_PEAK = 0.4f

    private val idleBlink = frames(LinearEasing, kf(0f, 1f), kf(10f, 1f), kf(13f, 0.1f), kf(16f, 1f), kf(100f, 1f))
    private val idleTuft =
        frames(
            EaseInOutCurve,
            kf(0f, 0f),
            kf(14f, 0f),
            kf(18f, -14f),
            kf(22f, 9f),
            kf(26f, -4f),
            kf(30f, 0f),
            kf(100f, 0f),
        )
    private val hop =
        frames(
            EaseInOutCurve,
            kf(0f, 0f, 1f, 1f),
            kf(10f, 2f, 1.06f, 0.94f),
            kf(22f, -14f, 0.96f, 1.05f),
            kf(34f, 0f, 1.04f, 0.96f),
            kf(45f, 0f, 1f, 1f),
            kf(100f, 0f, 1f, 1f),
        )
    private val winkBlink = frames(LinearEasing, kf(0f, 1f), kf(90f, 1f), kf(94f, 0.1f), kf(100f, 1f))
    private val winkJoy =
        frames(LinearEasing, kf(0f, 0f), kf(22f, 0f), kf(27f, 1f), kf(68f, 1f), kf(73f, 0f), kf(100f, 0f))
    private val cheer =
        frames(
            EaseInOutCurve,
            kf(0f, 0f, 1f, 1f),
            kf(12f, 3f, 1.08f, 0.92f),
            kf(26f, -22f, 0.94f, 1.08f),
            kf(40f, 0f, 1.06f, 0.94f),
            kf(50f, 0f, 1f, 1f),
            kf(100f, 0f, 1f, 1f),
        )
    private val cheerTuft =
        frames(
            EaseInOutCurve,
            kf(0f, 0f),
            kf(20f, 0f),
            kf(28f, -14f),
            kf(36f, 9f),
            kf(44f, -4f),
            kf(60f, 0f),
            kf(100f, 0f),
        )
    private val shake =
        frames(
            EaseInOutCurve,
            kf(0f, 0f),
            kf(50f, 0f),
            kf(56f, -4f),
            kf(62f, 4f),
            kf(68f, -2f),
            kf(74f, 0f),
            kf(100f, 0f),
        )
    private val drip =
        frames(EaseInCurve, kf(0f, 1f, 0f), kf(30f, 1f, 0f), kf(70f, 1f, 8f), kf(86f, 0f, 14f), kf(100f, 0f, 14f))
    private val workBlink = frames(LinearEasing, kf(0f, 1f), kf(92f, 1f), kf(95f, 0.1f), kf(100f, 1f))
    private val work =
        frames(
            EaseInOutCurve,
            kf(0f, 0f, -5f),
            kf(25f, -8f, 0f),
            kf(50f, 0f, 5f),
            kf(75f, -8f, 0f),
            kf(100f, 0f, -5f),
        )
    private val workShadow =
        frames(EaseInOutCurve, kf(0f, 1f), kf(25f, 0.85f), kf(50f, 1f), kf(75f, 0.85f), kf(100f, 1f))

    fun apply(
        pose: HeadPose,
        expression: DuckExpression,
        millis: Long,
        frozen: Boolean,
        swap: Float,
        out: FloatArray,
    ) {
        pose.reset()
        when (expression) {
            DuckExpression.SMILE -> {
                idleBlink.sample(loopPhase(millis, IDLE_MILLIS), out)
                pose.eyeSy = out[0]
                idleTuft.sample(loopPhase(millis, IDLE_MILLIS), out)
                pose.tuftDegrees = out[0]
            }
            DuckExpression.JOY -> {
                pose.joyLeft = 1f
                pose.joyRight = 1f
                hop.sample(loopPhase(millis, HOP_MILLIS), out)
                pose.rigTy = out[0]
                pose.rigSx = out[1]
                pose.rigSy = out[2]
            }
            DuckExpression.WINK -> {
                winkBlink.sample(loopPhase(millis, BLINK_MILLIS), out)
                pose.eyeSy = out[0]
                winkJoy.sample(loopPhase(millis, WINK_MILLIS), out)
                pose.joyRight = if (frozen) 1f else out[0]
            }
            DuckExpression.CHEER -> {
                pose.joyLeft = 1f
                pose.joyRight = 1f
                pose.mouth = HeadMouth.BIG
                cheer.sample(loopPhase(millis, CHEER_MILLIS), out)
                pose.rigTy = out[0]
                pose.rigSx = out[1]
                pose.rigSy = out[2]
                cheerTuft.sample(loopPhase(millis, CHEER_MILLIS), out)
                pose.tuftDegrees = out[0]
            }
            DuckExpression.OOPS -> {
                pose.mouth = HeadMouth.OPEN
                shake.sample(loopPhase(millis, OOPS_MILLIS), out)
                pose.rigDegrees = out[0]
                drip.sample(loopPhase(millis, OOPS_MILLIS), out)
                pose.dropAlpha = out[0]
                pose.dropTy = out[1]
            }
        }
        pose.eyeSy *= 1f - SWAP_DEPTH * (1f - abs(2f * swap - 1f))
        val squash = if (swap < SWAP_PEAK) swap / SWAP_PEAK else (1f - swap) / (1f - SWAP_PEAK)
        pose.rigSx *= 1f + SWAP_SQUASH * squash
        pose.rigSy *= 1f - SWAP_SQUASH * squash
    }

    fun applyWorking(
        pose: HeadPose,
        millis: Long,
        out: FloatArray,
    ) {
        pose.reset()
        pose.mouth = HeadMouth.OPEN
        pose.showShadow = true
        val phase = loopPhase(millis, WORK_MILLIS)
        work.sample(phase, out)
        pose.rigTy = out[0]
        pose.rigDegrees = out[1]
        workShadow.sample(phase, out)
        pose.shadowSx = out[0]
        pose.medallionDegrees = phase * FULL_TURN
        workBlink.sample(loopPhase(millis, WORK_BLINK_MILLIS), out)
        pose.eyeSy = out[0]
    }
}
