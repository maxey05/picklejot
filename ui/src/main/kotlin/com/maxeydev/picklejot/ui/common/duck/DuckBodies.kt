package com.maxeydev.picklejot.ui.common.duck

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

internal const val READY_VIEW_WIDTH = 240f
internal const val READY_VIEW_HEIGHT = 230f
internal const val SMASH_VIEW_WIDTH = 260f
internal const val SMASH_VIEW_HEIGHT = 236f
internal const val BUDDY_VIEW_WIDTH = 220f
internal const val BUDDY_VIEW_HEIGHT = 230f
internal const val SPARKLE_COUNT = 3
internal const val SPARKLE_CHANNELS = 3
private const val SPEED_DASH = 150f

internal class DuckPose {
    var rigTx = 0f
    var rigTy = 0f
    var rigDegrees = 0f
    var rigSx = 1f
    var rigSy = 1f
    var shadowSx = 1f
    var padDegrees = 0f
    var wingDegrees = 0f
    var tuftDegrees = 0f
    var tailDegrees = 0f
    var eyeSy = 1f
    var joy = 0f
    var ballTx = 0f
    var ballTy = 0f
    var ballAlpha = 0f
    var ballSx = 1f
    var ballSy = 1f
    var speedAlpha = 1f
    var dashPhase = SPEED_DASH
    val sparkles = FloatArray(SPARKLE_COUNT * SPARKLE_CHANNELS)

    fun reset() {
        rigTx = 0f
        rigTy = 0f
        rigDegrees = 0f
        rigSx = 1f
        rigSy = 1f
        shadowSx = 1f
        padDegrees = 0f
        wingDegrees = 0f
        tuftDegrees = 0f
        tailDegrees = 0f
        eyeSy = 1f
        joy = 0f
        ballTx = 0f
        ballTy = 0f
        ballAlpha = 0f
        ballSx = 1f
        ballSy = 1f
        speedAlpha = 1f
        dashPhase = SPEED_DASH
    }
}

private class DuckFace(
    val cheekY: Float,
    val mouth: Path,
    val tongueY: Float,
    val tongueRx: Float,
    val tongueRy: Float,
    val highlight: Boolean,
)

private val ReadyFace = DuckFace(126f, DuckPaths.mouthReady, 135.5f, 4.5f, 2.5f, true)
private val SmashFace = DuckFace(126f, DuckPaths.mouthSmash, 138f, 5.5f, 3f, true)
private val BuddyFace = DuckFace(124f, DuckPaths.mouthBuddy, 134f, 4f, 2.2f, false)

private fun DrawScope.drawEyes(pose: DuckPose) {
    val dotAlpha = 1f - pose.joy
    if (dotAlpha > 0f) {
        part(originX = 110f, originY = 111f, sy = pose.eyeSy) {
            oval(94f, 111f, 5.5f, 7f, DuckColors.ink, dotAlpha)
            oval(126f, 111f, 5.5f, 7f, DuckColors.ink, dotAlpha)
            circle(92.4f, 108.4f, 2f, DuckColors.glint, dotAlpha)
            circle(124.4f, 108.4f, 2f, DuckColors.glint, dotAlpha)
        }
    }
    if (pose.joy > 0f) {
        strokePath(DuckPaths.joyBody, DuckColors.ink, InkStroke5, pose.joy)
    }
}

private fun DrawScope.drawBodyAndFace(
    face: DuckFace,
    pose: DuckPose,
) {
    drawPath(path = DuckPaths.body, brush = DuckPaths.bodyBrush)
    fillPath(DuckPaths.band, DuckColors.band)
    segment(59f, 87.5f, 161.5f, 87.5f, DuckColors.stripe, 2.6f)
    segment(57.5f, 94f, 162.5f, 94f, DuckColors.stripeSoft, 2.6f)
    strokePath(DuckPaths.bandEdges, DuckColors.ink, InkStroke35)
    strokePath(DuckPaths.body, DuckColors.ink, InkStroke5)
    circleInk(110f, 91f, 8.5f, DuckColors.ball, InkStroke35)
    circle(106.5f, 89f, 1.6f, DuckColors.ballDot)
    circle(113f, 89.5f, 1.6f, DuckColors.ballDot)
    circle(110f, 94.5f, 1.6f, DuckColors.ballDot)
    if (face.highlight) {
        strokePath(DuckPaths.highlight, DuckColors.stripe, HighlightStroke, 0.6f)
    }
    oval(76f, face.cheekY, 10f, 6f, DuckColors.cheek, 0.85f)
    oval(144f, face.cheekY, 10f, 6f, DuckColors.cheek, 0.85f)
    drawEyes(pose)
    solid(face.mouth, DuckColors.mouth, InkStroke35)
    oval(110f, face.tongueY, face.tongueRx, face.tongueRy, DuckColors.tongue)
    rrectInk(88f, 117f, 44f, 14f, 7f, DuckColors.orange)
    segment(95f, 121.5f, 106f, 121.5f, DuckColors.billHighlight, 2.5f, StrokeCap.Round)
}

private fun DrawScope.drawFeet() {
    ovalInk(92f, 204f, 13f, 8f, DuckColors.orange)
    ovalInk(128f, 204f, 13f, 8f, DuckColors.orange)
}

private fun DrawScope.drawReadyPaddle() {
    part(originX = 176f, originY = 150f, degrees = 14f) {
        rrectInk(170f, 112f, 13f, 44f, 4f, DuckColors.grip)
        strokePath(DuckPaths.readyGrip, DuckColors.mint, GripStroke)
        rrectInk(152f, 44f, 50f, 70f, 15f, DuckColors.edge)
        rrect(158f, 50f, 38f, 58f, 11f, DuckColors.band)
        strokePath(DuckPaths.readyPaddleHighlight, DuckColors.stripe, PaddleHighlightStroke, 0.3f)
        solid(DuckPaths.paddleWing, DuckColors.cream)
        strokePath(DuckPaths.paddleMark, DuckColors.mark, AccentStroke)
    }
}

internal fun DrawScope.drawReadyDuck(pose: DuckPose) {
    drawInViewBox(READY_VIEW_WIDTH, READY_VIEW_HEIGHT) {
        part(originX = 110f, originY = 215f, sx = pose.shadowSx) {
            oval(110f, 215f, 62f, 7f, DuckColors.ink, 0.1f)
        }
        part(
            originX = 110f,
            originY = 212f,
            tx = pose.rigTx,
            ty = pose.rigTy,
            degrees = pose.rigDegrees,
            sx = pose.rigSx,
            sy = pose.rigSy,
        ) {
            drawFeet()
            part(originX = 58f, originY = 142f, degrees = pose.wingDegrees) {
                solid(DuckPaths.wingLeft, DuckColors.cream)
            }
            part(originX = 112f, originY = 52f, degrees = pose.tuftDegrees) {
                solid(DuckPaths.tuft, DuckColors.cream)
            }
            drawBodyAndFace(ReadyFace, pose)
            part(originX = 174f, originY = 156f, degrees = pose.padDegrees) {
                drawReadyPaddle()
            }
        }
    }
}

private fun DrawScope.drawSmashPaddle() {
    part(originX = 56f, originY = 130f, degrees = -38f) {
        rrectInk(50f, 86f, 12f, 46f, 4f, DuckColors.grip)
        strokePath(DuckPaths.smashGrip, DuckColors.mint, GripStroke)
        rrectInk(31f, 20f, 50f, 70f, 15f, DuckColors.edge)
        rrect(37f, 26f, 38f, 58f, 11f, DuckColors.band)
        strokePath(DuckPaths.smashPaddleHighlight, DuckColors.stripe, PaddleHighlightStroke, 0.3f)
    }
}

private fun DrawScope.drawSpeedLines(pose: DuckPose) {
    val style =
        Stroke(
            width = 5f,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(SPEED_DASH, SPEED_DASH), pose.dashPhase),
        )
    strokePath(DuckPaths.smashSpeedOuter, DuckColors.mint, style, pose.speedAlpha)
    strokePath(DuckPaths.smashSpeedInner, DuckColors.mint, style, pose.speedAlpha)
}

private fun DrawScope.drawSmashBall(pose: DuckPose) {
    if (pose.ballAlpha <= 0f) {
        return
    }
    val alpha = pose.ballAlpha
    part(originX = 0f, originY = 0f, tx = pose.ballTx, ty = pose.ballTy) {
        strokePath(DuckPaths.smashBallLines, DuckColors.ink, InkStroke35, alpha)
        circleInk(20f, 142f, 12f, DuckColors.ball, InkStroke35, alpha)
        circle(16f, 138f, 2f, DuckColors.ballDot, alpha)
        circle(24f, 139f, 2f, DuckColors.ballDot, alpha)
        circle(20f, 147f, 2f, DuckColors.ballDot, alpha)
    }
}

internal fun DrawScope.drawSmashDuck(pose: DuckPose) {
    drawInViewBox(SMASH_VIEW_WIDTH, SMASH_VIEW_HEIGHT) {
        part(originX = 156f, originY = 226f, sx = pose.shadowSx) {
            oval(156f, 226f, 56f, 6f, DuckColors.ink, 0.1f)
        }
        if (pose.speedAlpha > 0f) {
            drawSpeedLines(pose)
        }
        withTransform({ translate(46f, 8f) }) {
            part(
                originX = 110f,
                originY = 206f,
                tx = pose.rigTx,
                ty = pose.rigTy,
                degrees = pose.rigDegrees,
                sx = pose.rigSx,
                sy = pose.rigSy,
            ) {
                part(originX = 96f, originY = 204f, degrees = -20f) {
                    ovalInk(96f, 204f, 13f, 8f, DuckColors.orange)
                }
                part(originX = 130f, originY = 202f, degrees = 25f) {
                    ovalInk(130f, 202f, 13f, 8f, DuckColors.orange)
                }
                part(originX = 166f, originY = 146f, degrees = pose.wingDegrees) {
                    solid(DuckPaths.smashWrist, DuckColors.cream)
                }
                part(originX = 162f, originY = 92f, degrees = pose.tailDegrees) {
                    solid(DuckPaths.smashTailUpper, DuckColors.band, InkStroke35)
                    solid(DuckPaths.smashTailLower, DuckColors.band, InkStroke35)
                }
                solid(DuckPaths.tuft, DuckColors.cream)
                drawBodyAndFace(SmashFace, pose)
                part(originX = 56f, originY = 130f, degrees = pose.padDegrees) {
                    drawSmashPaddle()
                }
                solid(DuckPaths.smashHugWing, DuckColors.cream)
            }
        }
        drawSmashBall(pose)
    }
}

internal fun DrawScope.drawBuddyDuck(pose: DuckPose) {
    drawInViewBox(BUDDY_VIEW_WIDTH, BUDDY_VIEW_HEIGHT) {
        oval(110f, 215f, 60f, 7f, DuckColors.ink, 0.1f)
        val spark = pose.sparkles
        drawSparkle(34f, 78f, 1f, DuckColors.pink, spark[0], spark[1], spark[2])
        drawSparkle(190f, 52f, 1f, DuckColors.ball, spark[3], spark[4], spark[5])
        drawSparkle(196f, 122f, 0.7f, DuckColors.mint, spark[6], spark[7], spark[8])
        part(
            originX = 110f,
            originY = 212f,
            tx = pose.rigTx,
            ty = pose.rigTy,
            degrees = pose.rigDegrees,
            sx = pose.rigSx,
            sy = pose.rigSy,
        ) {
            drawFeet()
            solid(DuckPaths.tuft, DuckColors.cream)
            drawBodyAndFace(BuddyFace, pose)
            part(originX = 110f, originY = 168f, sx = pose.ballSx, sy = pose.ballSy) {
                circleInk(110f, 168f, 33f, DuckColors.ball)
                circle(96f, 152f, 3.2f, DuckColors.ballDot)
                circle(124f, 154f, 3.2f, DuckColors.ballDot)
                circle(132f, 170f, 3.2f, DuckColors.ballDot)
                circle(108f, 168f, 3.2f, DuckColors.ballDot)
                circle(92f, 184f, 3.2f, DuckColors.ballDot)
                circle(122f, 188f, 3.2f, DuckColors.ballDot)
            }
            solid(DuckPaths.buddyWingLeft, DuckColors.cream)
            solid(DuckPaths.buddyWingRight, DuckColors.cream)
            strokePath(DuckPaths.buddyMarkLeft, DuckColors.mark, AccentStroke)
            strokePath(DuckPaths.buddyMarkRight, DuckColors.mark, AccentStroke)
        }
    }
}
