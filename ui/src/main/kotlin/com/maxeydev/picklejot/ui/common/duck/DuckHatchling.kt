package com.maxeydev.picklejot.ui.common.duck

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal const val HATCH_VIEW_WIDTH = 200f
internal const val HATCH_VIEW_HEIGHT = 220f
private const val CRACK_DASH = 160f
private const val WING_VISIBLE_THRESHOLD = 0.001f

internal class HatchPose {
    var eggDegrees = 0f
    var headTy = 48f
    var headSx = 1f
    var headSy = 1f
    var eyeSy = 1f
    var wholeAlpha = 1f
    var crackAlpha = 1f
    var crackPhase = CRACK_DASH
    var capAlpha = 0f
    var capTx = 0f
    var capTy = 0f
    var capDegrees = 0f
    var wingScale = 0f
    var sparkAlpha = 0f
    var sparkScale = 0f
    var sparkDegrees = 0f
}

private const val DOT_RADIUS = 4f

private fun DrawScope.drawShellDots(alpha: Float) {
    circle(70f, 150f, DOT_RADIUS, DuckColors.ballDot, alpha)
    circle(98f, 146f, DOT_RADIUS, DuckColors.ballDot, alpha)
    circle(128f, 152f, DOT_RADIUS, DuckColors.ballDot, alpha)
    drawLowerDots(alpha)
}

private fun DrawScope.drawLowerDots(alpha: Float) {
    circle(58f, 174f, DOT_RADIUS, DuckColors.ballDot, alpha)
    circle(86f, 174f, DOT_RADIUS, DuckColors.ballDot, alpha)
    circle(116f, 176f, DOT_RADIUS, DuckColors.ballDot, alpha)
    circle(142f, 172f, DOT_RADIUS, DuckColors.ballDot, alpha)
    circle(100f, 198f, DOT_RADIUS, DuckColors.ballDot, alpha)
}

private fun DrawScope.drawWholeDots(alpha: Float) {
    circle(82f, 112f, DOT_RADIUS, DuckColors.ballDot, alpha)
    circle(114f, 106f, DOT_RADIUS, DuckColors.ballDot, alpha)
    circle(70f, 150f, DOT_RADIUS, DuckColors.ballDot, alpha)
    circle(98f, 140f, DOT_RADIUS, DuckColors.ballDot, alpha)
    circle(128f, 146f, DOT_RADIUS, DuckColors.ballDot, alpha)
    drawLowerDots(alpha)
}

private fun DrawScope.drawHatchHead(pose: HatchPose) {
    part(originX = 100f, originY = 136f, ty = pose.headTy, sx = pose.headSx, sy = pose.headSy) {
        solid(DuckPaths.hatchTuft, DuckColors.cream)
        drawOval(brush = DuckPaths.hatchBrush, topLeft = Offset(56f, 56f), size = Size(88f, 80f))
        fillPath(DuckPaths.hatchBand, DuckColors.band)
        segment(60f, 80f, 140f, 80f, DuckColors.stripe, 2.6f)
        segment(58.5f, 84f, 141.5f, 84f, DuckColors.stripeSoft, 2.6f)
        strokePath(DuckPaths.hatchBandEdges, DuckColors.ink, InkStroke35)
        ovalStroke(100f, 96f, 44f, 40f, InkStroke5)
        circleInk(100f, 82f, 6f, DuckColors.ball, InkStroke25)
        oval(70f, 108f, 8f, 5f, DuckColors.cheek, 0.85f)
        oval(130f, 108f, 8f, 5f, DuckColors.cheek, 0.85f)
        part(originX = 100f, originY = 98f, sy = pose.eyeSy) {
            oval(86f, 98f, 4.2f, 5.4f, DuckColors.ink)
            oval(114f, 98f, 4.2f, 5.4f, DuckColors.ink)
            circle(84.8f, 96f, 1.5f, DuckColors.glint)
            circle(112.8f, 96f, 1.5f, DuckColors.glint)
        }
        solid(DuckPaths.hatchMouth, DuckColors.mouth, InkStroke25)
        rrectInk(86f, 102f, 28f, 10f, 5f, DuckColors.orange, InkStroke35)
    }
}

private fun DrawScope.drawShell(pose: HatchPose) {
    fillPath(DuckPaths.shell, DuckColors.ball)
    fillPath(DuckPaths.eggShade, DuckColors.eggShade, 0.55f)
    drawShellDots(1f)
    strokePath(DuckPaths.shell, DuckColors.ink, InkStroke5)
    if (pose.wingScale > WING_VISIBLE_THRESHOLD) {
        part(originX = 66f, originY = 120f, sx = pose.wingScale, sy = pose.wingScale) {
            solid(DuckPaths.hatchWingLeft, DuckColors.cream)
        }
        part(originX = 134f, originY = 120f, sx = pose.wingScale, sy = pose.wingScale) {
            solid(DuckPaths.hatchWingRight, DuckColors.cream)
        }
    }
}

private fun DrawScope.drawWholeEgg(alpha: Float) {
    circleInk(100f, 150f, 58f, DuckColors.ball, InkStroke5, alpha)
    fillPath(DuckPaths.eggShade, DuckColors.eggShade, alpha * 0.55f)
    drawWholeDots(alpha)
}

private fun DrawScope.drawCrack(pose: HatchPose) {
    val style =
        Stroke(
            width = CrackStroke.width,
            join = CrackStroke.join,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(CRACK_DASH, CRACK_DASH), pose.crackPhase),
        )
    strokePath(DuckPaths.shellCrack, DuckColors.ink, style, pose.crackAlpha)
}

private fun DrawScope.drawCap(pose: HatchPose) {
    part(originX = 100f, originY = 112f, tx = pose.capTx, ty = pose.capTy, degrees = pose.capDegrees) {
        solid(DuckPaths.shellCap, DuckColors.ball, InkStroke5, pose.capAlpha)
        circle(82f, 108f, 3.5f, DuckColors.ballDot, pose.capAlpha)
        circle(112f, 104f, 3.5f, DuckColors.ballDot, pose.capAlpha)
    }
}

internal fun DrawScope.drawHatchling(pose: HatchPose) {
    drawInViewBox(HATCH_VIEW_WIDTH, HATCH_VIEW_HEIGHT) {
        oval(100f, 212f, 56f, 6f, DuckColors.ink, 0.1f)
        drawSparkle(34f, 62f, 1f, DuckColors.ball, pose.sparkAlpha, pose.sparkScale, pose.sparkDegrees)
        drawSparkle(170f, 56f, 0.7f, DuckColors.pink, pose.sparkAlpha, pose.sparkScale, pose.sparkDegrees)
        part(originX = 100f, originY = 208f, degrees = pose.eggDegrees) {
            drawHatchHead(pose)
            drawShell(pose)
            if (pose.wholeAlpha > 0f) {
                drawWholeEgg(pose.wholeAlpha)
            }
            if (pose.crackAlpha > 0f) {
                drawCrack(pose)
            }
            if (pose.capAlpha > 0f) {
                drawCap(pose)
            }
        }
    }
}
