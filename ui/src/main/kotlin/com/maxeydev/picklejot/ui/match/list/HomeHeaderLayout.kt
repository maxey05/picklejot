package com.maxeydev.picklejot.ui.match.list

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

private const val COLLAPSE_BODY_OVERSHOOT = 0.75f
private const val NOT_FROZEN = -1

private class BodyHeightFreeze {
    private var frozenHeight = NOT_FROZEN

    fun resolve(
        isRunning: Boolean,
        isCollapsing: Boolean,
        exactHeight: Int,
        headerHeight: Int,
    ): Int {
        if (!isRunning) {
            frozenHeight = NOT_FROZEN
            return exactHeight
        }
        if (frozenHeight == NOT_FROZEN) {
            frozenHeight =
                if (isCollapsing) {
                    exactHeight + (headerHeight * COLLAPSE_BODY_OVERSHOOT).roundToInt()
                } else {
                    exactHeight
                }
        }
        return frozenHeight
    }
}

@Composable
internal fun HomeHeaderLayout(
    progress: CollapseProgress,
    isCollapsing: Boolean,
    header: @Composable () -> Unit,
    body: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val freeze = remember { BodyHeightFreeze() }
    Layout(
        content = {
            Box { header() }
            Box { body() }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val fullHeight = constraints.maxHeight
        val headerPlaceable = measurables[0].measure(Constraints(maxWidth = width, maxHeight = fullHeight))
        val exactBodyHeight = (fullHeight - headerPlaceable.height).coerceAtLeast(0)
        val bodyHeight =
            freeze.resolve(
                isRunning = progress.isRunning,
                isCollapsing = isCollapsing,
                exactHeight = exactBodyHeight,
                headerHeight = headerPlaceable.height,
            )
        val bodyPlaceable = measurables[1].measure(Constraints.fixed(width, bodyHeight))
        layout(width, fullHeight) {
            headerPlaceable.place(0, 0)
            bodyPlaceable.place(0, headerPlaceable.height)
        }
    }
}
