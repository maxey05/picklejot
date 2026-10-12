package com.maxeydev.picklejot.ui.common.duck

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope

internal const val HEAD_VIEW_WIDTH = 200f
internal const val HEAD_VIEW_HEIGHT = 184f
internal const val WORKING_VIEW_HEIGHT = 190f

internal enum class HeadMouth {
    SMILE,
    BIG,
    OPEN,
}

internal class HeadPose {
    var rigTy = 0f
    var rigDegrees = 0f
    var rigSx = 1f
    var rigSy = 1f
    var eyeSy = 1f
    var tuftDegrees = 0f
    var joyLeft = 0f
    var joyRight = 0f
    var mouth = HeadMouth.SMILE
    var dropAlpha = 0f
    var dropTy = 0f
    var medallionDegrees = 0f
    var shadowSx = 1f
    var showShadow = false

    fun reset() {
        rigTy = 0f
        rigDegrees = 0f
        rigSx = 1f
        rigSy = 1f
        eyeSy = 1f
        tuftDegrees = 0f
        joyLeft = 0f
        joyRight = 0f
        mouth = HeadMouth.SMILE
        dropAlpha = 0f
        dropTy = 0f
        medallionDegrees = 0f
        shadowSx = 1f
        showShadow = false
    }
}

private fun DrawScope.drawHeadEye(
    cx: Float,
    glintX: Float,
    joy: Float,
    arcPath: Path,
) {
    val dotAlpha = 1f - joy
    if (dotAlpha > 0f) {
        oval(cx, 108f, 6.5f, 8.5f, DuckColors.ink, dotAlpha)
        circle(glintX, 105f, 2.4f, DuckColors.glint, dotAlpha)
    }
    if (joy > 0f) {
        strokePath(arcPath, DuckColors.ink, InkStroke6Round, joy)
    }
}

private fun DrawScope.drawHeadMouth(mouth: HeadMouth) {
    when (mouth) {
        HeadMouth.SMILE -> {
            solid(DuckPaths.mouthSmile, DuckColors.mouth, InkStroke35)
            oval(100f, 140f, 5.5f, 2.8f, DuckColors.tongue)
        }
        HeadMouth.BIG -> {
            solid(DuckPaths.mouthBig, DuckColors.mouth, InkStroke35)
            oval(100f, 146f, 7f, 3.5f, DuckColors.tongue)
        }
        HeadMouth.OPEN -> ovalInk(100f, 141f, 5.5f, 4.5f, DuckColors.mouth, InkStroke35)
    }
}

internal fun DrawScope.drawHead(
    pose: HeadPose,
    viewHeight: Float,
) {
    drawInViewBox(HEAD_VIEW_WIDTH, viewHeight) {
        if (pose.showShadow) {
            part(originX = 100f, originY = 180f, sx = pose.shadowSx) {
                oval(100f, 180f, 60f, 5f, DuckColors.ink, 0.1f)
            }
        }
        part(
            originX = 100f,
            originY = 170f,
            ty = pose.rigTy,
            degrees = pose.rigDegrees,
            sx = pose.rigSx,
            sy = pose.rigSy,
        ) {
            part(originX = 106f, originY = 42f, degrees = pose.tuftDegrees) {
                solid(DuckPaths.headTuft, DuckColors.cream)
            }
            drawOval(brush = DuckPaths.headBrush, topLeft = Offset(22f, 38f), size = Size(156f, 132f))
            fillPath(DuckPaths.headBand, DuckColors.band)
            segment(29.5f, 77f, 170.5f, 77f, DuckColors.stripe, 2.6f)
            segment(27f, 83f, 173f, 83f, DuckColors.stripeSoft, 2.6f)
            strokePath(DuckPaths.headBandEdges, DuckColors.ink, InkStroke35)
            ovalStroke(100f, 104f, 78f, 66f, InkStroke6)
            part(originX = 100f, originY = 80f, degrees = pose.medallionDegrees) {
                circleInk(100f, 80f, 10f, DuckColors.ball, InkStroke35)
                circle(96f, 77.5f, 1.9f, DuckColors.ballDot)
                circle(104f, 78f, 1.9f, DuckColors.ballDot)
                circle(100f, 84f, 1.9f, DuckColors.ballDot)
            }
            oval(52f, 126f, 13f, 8f, DuckColors.cheek, 0.85f)
            oval(148f, 126f, 13f, 8f, DuckColors.cheek, 0.85f)
            part(originX = 100f, originY = 108f, sy = pose.eyeSy) {
                drawHeadEye(78f, 76f, pose.joyLeft, DuckPaths.joyLeft)
                drawHeadEye(122f, 120f, pose.joyRight, DuckPaths.joyRight)
            }
            drawHeadMouth(pose.mouth)
            rrectInk(74f, 118f, 52f, 17f, 8.5f, DuckColors.orange)
            segment(82f, 123f, 95f, 123f, DuckColors.billHighlight, 2.5f, StrokeCap.Round)
            if (pose.dropAlpha > 0f) {
                part(originX = 158f, originY = 70f, ty = pose.dropTy) {
                    solid(DuckPaths.drop, DuckColors.mint, InkStroke35, pose.dropAlpha)
                }
            }
        }
    }
}
