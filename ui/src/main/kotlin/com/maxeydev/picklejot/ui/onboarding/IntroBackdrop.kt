package com.maxeydev.picklejot.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.floor
import kotlin.math.min

private val MinIllustrationHeight = 140.dp
private val MaxIllustrationHeight = 420.dp

private enum class Edge { LEFT, RIGHT }

@Immutable
private data class EdgeCircle(
    val edge: Edge,
    val fromEdge: Float,
    val centerY: Float,
    val radius: Float,
)

@Immutable
private data class PageCircles(
    val soft: EdgeCircle,
    val blob: EdgeCircle,
)

@Immutable
private data class PlacedCircle(
    val center: Offset,
    val radius: Float,
)

internal fun introIllustrationHeight(
    page: IntroPage,
    availableHeight: Dp,
    topInset: Dp,
): Dp =
    ((availableHeight - topInset) * page.illustrationShare)
        .coerceIn(MinIllustrationHeight, MaxIllustrationHeight) + topInset

private fun circlesFor(page: IntroPage): PageCircles =
    when (page) {
        IntroPage.LOGGING ->
            PageCircles(
                soft = EdgeCircle(Edge.RIGHT, 7f, 321f, 125f),
                blob = EdgeCircle(Edge.LEFT, 85f, 105f, 235f),
            )
        IntroPage.STATS ->
            PageCircles(
                soft = EdgeCircle(Edge.LEFT, -2f, 340f, 110f),
                blob = EdgeCircle(Edge.RIGHT, 85f, 95f, 245f),
            )
        IntroPage.SHARE ->
            PageCircles(
                soft = EdgeCircle(Edge.RIGHT, 35f, 35f, 105f),
                blob = EdgeCircle(Edge.LEFT, 25f, 235f, 215f),
            )
        IntroPage.PRIVACY ->
            PageCircles(
                soft = EdgeCircle(Edge.LEFT, -1f, 331f, 95f),
                blob = EdgeCircle(Edge.RIGHT, 40f, 140f, 230f),
            )
    }

private fun DrawScope.place(
    circle: EdgeCircle,
    page: IntroPage,
    topInset: Dp,
): PlacedCircle {
    val topInsetPx = topInset.toPx()
    val illustrationHeight = introIllustrationHeight(page, size.height.toDp(), topInset).toPx()
    val fit = min(size.width / INTRO_VIEW_WIDTH, (illustrationHeight - topInsetPx) / page.viewportHeight)
    val top = illustrationHeight - page.viewportHeight * fit
    val centerX =
        when (circle.edge) {
            Edge.LEFT -> circle.fromEdge * fit
            Edge.RIGHT -> size.width - circle.fromEdge * fit
        }
    return PlacedCircle(Offset(centerX, top + circle.centerY * fit), circle.radius * fit)
}

private fun DrawScope.drawMorphing(
    color: Color,
    from: PlacedCircle,
    to: PlacedCircle,
    progress: Float,
) {
    drawCircle(
        color = color,
        radius = lerp(from.radius, to.radius, progress),
        center = lerp(from.center, to.center, progress),
    )
}

@Composable
internal fun IntroBackdrop(
    pagerState: PagerState,
    scrollStates: List<ScrollState>,
    topInset: Dp,
    modifier: Modifier = Modifier,
) {
    val softColor = MaterialTheme.colorScheme.surfaceContainer
    val blobColor = MaterialTheme.colorScheme.secondaryContainer
    Canvas(modifier = modifier) {
        val lastPage = IntroPage.entries.lastIndex
        val position = (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, lastPage.toFloat())
        val fromIndex = min(floor(position).toInt(), lastPage - 1)
        val progress = position - fromIndex
        val fromPage = IntroPage.entries[fromIndex]
        val toPage = IntroPage.entries[fromIndex + 1]
        val from = circlesFor(fromPage)
        val to = circlesFor(toPage)
        val fromScroll = scrollStates[fromIndex].value.toFloat()
        val toScroll = scrollStates[fromIndex + 1].value.toFloat()
        val scroll = lerp(fromScroll, toScroll, progress)
        translate(top = -scroll) {
            drawMorphing(softColor, place(from.soft, fromPage, topInset), place(to.soft, toPage, topInset), progress)
            drawMorphing(blobColor, place(from.blob, fromPage, topInset), place(to.blob, toPage, topInset), progress)
        }
    }
}
