package com.maxeydev.picklejot.ui.common.duck

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

internal val InkStroke6 = Stroke(width = 6f, join = StrokeJoin.Round)
internal val InkStroke6Round = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
internal val InkStroke5 = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
internal val InkStroke35 = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
internal val InkStroke25 = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
internal val CrackStroke = Stroke(width = 3.5f, join = StrokeJoin.Round)
internal val GripStroke = Stroke(width = 2f, cap = StrokeCap.Round)
internal val HighlightStroke = Stroke(width = 5f, cap = StrokeCap.Round)
internal val PaddleHighlightStroke = Stroke(width = 4.5f, cap = StrokeCap.Round)
internal val AccentStroke = Stroke(width = 2.5f, cap = StrokeCap.Round)

internal inline fun DrawScope.drawInViewBox(
    viewWidth: Float,
    viewHeight: Float,
    block: DrawScope.() -> Unit,
) {
    val fit = minOf(size.width / viewWidth, size.height / viewHeight)
    val dx = (size.width - viewWidth * fit) / 2f
    val dy = (size.height - viewHeight * fit) / 2f
    withTransform({
        translate(dx, dy)
        scale(fit, fit, Offset.Zero)
    }) { block() }
}

internal inline fun DrawScope.part(
    originX: Float,
    originY: Float,
    tx: Float = 0f,
    ty: Float = 0f,
    degrees: Float = 0f,
    sx: Float = 1f,
    sy: Float = 1f,
    block: DrawScope.() -> Unit,
) {
    val origin = Offset(originX, originY)
    withTransform({
        translate(tx, ty)
        rotate(degrees, origin)
        scale(sx, sy, origin)
    }) { block() }
}

internal fun DrawScope.fillPath(
    path: Path,
    color: Color,
    alpha: Float = 1f,
) {
    drawPath(path = path, color = color, alpha = alpha)
}

internal fun DrawScope.strokePath(
    path: Path,
    color: Color,
    style: Stroke,
    alpha: Float = 1f,
) {
    drawPath(path = path, color = color, alpha = alpha, style = style)
}

internal fun DrawScope.solid(
    path: Path,
    fill: Color,
    style: Stroke = InkStroke5,
    alpha: Float = 1f,
) {
    fillPath(path, fill, alpha)
    strokePath(path, DuckColors.ink, style, alpha)
}

internal fun DrawScope.oval(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    color: Color,
    alpha: Float = 1f,
) {
    drawOval(color = color, topLeft = Offset(cx - rx, cy - ry), size = Size(rx * 2f, ry * 2f), alpha = alpha)
}

internal fun DrawScope.ovalStroke(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    style: Stroke,
) {
    drawOval(
        color = DuckColors.ink,
        topLeft = Offset(cx - rx, cy - ry),
        size = Size(rx * 2f, ry * 2f),
        style = style,
    )
}

internal fun DrawScope.ovalInk(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    fill: Color,
    style: Stroke = InkStroke5,
    alpha: Float = 1f,
) {
    oval(cx, cy, rx, ry, fill, alpha)
    drawOval(
        color = DuckColors.ink,
        topLeft = Offset(cx - rx, cy - ry),
        size = Size(rx * 2f, ry * 2f),
        alpha = alpha,
        style = style,
    )
}

internal fun DrawScope.circle(
    cx: Float,
    cy: Float,
    radius: Float,
    color: Color,
    alpha: Float = 1f,
) {
    drawCircle(color = color, radius = radius, center = Offset(cx, cy), alpha = alpha)
}

internal fun DrawScope.circleInk(
    cx: Float,
    cy: Float,
    radius: Float,
    fill: Color,
    style: Stroke = InkStroke5,
    alpha: Float = 1f,
) {
    circle(cx, cy, radius, fill, alpha)
    drawCircle(color = DuckColors.ink, radius = radius, center = Offset(cx, cy), alpha = alpha, style = style)
}

internal fun DrawScope.rrect(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    radius: Float,
    color: Color,
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(width, height),
        cornerRadius = CornerRadius(radius, radius),
    )
}

internal fun DrawScope.rrectInk(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    radius: Float,
    fill: Color,
    style: Stroke = InkStroke5,
) {
    rrect(x, y, width, height, radius, fill)
    drawRoundRect(
        color = DuckColors.ink,
        topLeft = Offset(x, y),
        size = Size(width, height),
        cornerRadius = CornerRadius(radius, radius),
        style = style,
    )
}

internal fun DrawScope.segment(
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    color: Color,
    width: Float,
    cap: StrokeCap = StrokeCap.Butt,
) {
    drawLine(color = color, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = width, cap = cap)
}

internal fun DrawScope.drawSparkle(
    x: Float,
    y: Float,
    baseScale: Float,
    fill: Color,
    alpha: Float,
    scale: Float,
    degrees: Float,
) {
    withTransform({
        translate(x, y)
        scale(baseScale, baseScale, Offset.Zero)
        rotate(degrees, Offset.Zero)
        scale(scale, scale, Offset.Zero)
    }) {
        solid(DuckPaths.sparkle, fill, InkStroke25, alpha)
    }
}
