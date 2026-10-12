package com.maxeydev.picklejot.ui.match.list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.maxeydev.picklejot.domain.match.MatchSort
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.theme.PicklejotTheme

private val MIN_TOUCH_TARGET = 48.dp
private val ICON_SIZE = 18.dp
private val MENU_GAP = 4.dp
private val MENU_MIN_WIDTH = 200.dp
private val MENU_MAX_HEIGHT = 400.dp
private val MENU_ELEVATION = 3.dp
private val MENU_CORNER = 12.dp
private val MENU_VERTICAL_PADDING = 4.dp
private val ITEM_START_PADDING = 16.dp
private val ITEM_END_PADDING = 12.dp
private val ITEM_GAP = 12.dp
private const val OPEN_MILLIS = 280
private const val CLOSE_MILLIS = 160
private const val ITEM_STAGGER_FRACTION = 0.06f
private const val ITEM_FADE_FRACTION = 0.5f
private const val CHEVRON_MILLIS = 180
private const val CHEVRON_EXPANDED_DEGREES = 180f
private val OpenEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val CloseEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
private val ChevronEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

@Composable
fun SortMenu(
    activeSort: MatchSort,
    onSortSelected: (MatchSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val activeLabel = sortLabel(activeSort)
    val description = stringResource(R.string.sort_button, activeLabel)
    val borderColor = if (isExpanded) colors.primary else PicklejotTheme.colors.cardBorder
    val borderWidth = if (isExpanded) 2.dp else 1.dp
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) CHEVRON_EXPANDED_DEGREES else 0f,
        animationSpec = tween(durationMillis = CHEVRON_MILLIS, easing = ChevronEasing),
        label = "sortChevronRotation",
    )
    Box(modifier = modifier) {
        Surface(
            onClick = { isExpanded = true },
            shape = MaterialTheme.shapes.medium,
            color = colors.surfaceContainerLowest,
            contentColor = colors.onSurface,
            border = BorderStroke(borderWidth, borderColor),
            modifier =
                Modifier
                    .heightIn(min = MIN_TOUCH_TARGET)
                    .testTag(MatchListTestTags.SORT_BUTTON)
                    .semantics {
                        contentDescription = description
                        role = Role.Button
                    },
        ) {
            Row(
                modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_sort),
                    contentDescription = null,
                    modifier = Modifier.size(ICON_SIZE),
                )
                Text(text = activeLabel, style = MaterialTheme.typography.labelLarge)
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(ICON_SIZE)
                            .graphicsLayer { rotationZ = chevronRotation },
                )
            }
        }
        SortMenuPopup(
            isExpanded = isExpanded,
            activeSort = activeSort,
            onDismiss = { isExpanded = false },
            onSortSelected = { sort ->
                isExpanded = false
                onSortSelected(sort)
            },
        )
    }
}

@Composable
private fun SortMenuPopup(
    isExpanded: Boolean,
    activeSort: MatchSort,
    onDismiss: () -> Unit,
    onSortSelected: (MatchSort) -> Unit,
) {
    val reveal = remember { Animatable(0f) }
    var isShowing by remember { mutableStateOf(false) }
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            isShowing = true
            reveal.animateTo(1f, tween(durationMillis = OPEN_MILLIS, easing = OpenEasing))
        } else if (isShowing) {
            reveal.animateTo(0f, tween(durationMillis = CLOSE_MILLIS, easing = CloseEasing))
            isShowing = false
        }
    }
    val gap = with(LocalDensity.current) { MENU_GAP.roundToPx() }
    val positionProvider = remember(gap) { BelowAnchorPositionProvider(gap) }
    if (isExpanded || isShowing) {
        Popup(
            popupPositionProvider = positionProvider,
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = true),
        ) {
            SortMenuSurface(
                activeSort = activeSort,
                progress = { reveal.value },
                onSortSelected = onSortSelected,
            )
        }
    }
}

private class TopRevealShape(
    private val fraction: Float,
    private val corner: Dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val radius = with(density) { corner.toPx() }
        return Outline.Rounded(RoundRect(0f, 0f, size.width, size.height * fraction, CornerRadius(radius)))
    }
}

private fun itemAlpha(
    progress: Float,
    index: Int,
): Float = ((progress - index * ITEM_STAGGER_FRACTION) / ITEM_FADE_FRACTION).coerceIn(0f, 1f)

@Composable
private fun SortMenuSurface(
    activeSort: MatchSort,
    progress: () -> Float,
    onSortSelected: (MatchSort) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(MENU_CORNER),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = MENU_ELEVATION,
        border = BorderStroke(1.dp, PicklejotTheme.colors.cardBorder),
        modifier =
            Modifier.graphicsLayer {
                val fraction = progress()
                shape = TopRevealShape(fraction, MENU_CORNER)
                clip = fraction < 1f
            },
    ) {
        Column(
            modifier =
                Modifier
                    .widthIn(min = MENU_MIN_WIDTH)
                    .width(IntrinsicSize.Max)
                    .heightIn(max = MENU_MAX_HEIGHT)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = MENU_VERTICAL_PADDING)
                    .selectableGroup(),
        ) {
            visibleSorts(activeSort).forEachIndexed { index, sort ->
                SortMenuItem(
                    sort = sort,
                    isActive = sort == activeSort,
                    itemAlpha = { itemAlpha(progress(), index) },
                    onClick = { onSortSelected(sort) },
                )
            }
        }
    }
}

@Composable
private fun SortMenuItem(
    sort: MatchSort,
    isActive: Boolean,
    itemAlpha: () -> Float,
    onClick: () -> Unit,
) {
    val rowColor =
        if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = MIN_TOUCH_TARGET)
                .graphicsLayer { alpha = itemAlpha() }
                .background(rowColor)
                .testTag(MatchListTestTags.sortOption(sort))
                .semantics { selected = isActive }
                .clickable(role = Role.Button, onClick = onClick)
                .padding(start = ITEM_START_PADDING, end = ITEM_END_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ITEM_GAP),
    ) {
        Text(
            text = sortLabel(sort),
            style = MaterialTheme.typography.bodyLarge,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (isActive) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private class BelowAnchorPositionProvider(
    private val gap: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val furthestLeft = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        return IntOffset(anchorBounds.left.coerceIn(0, furthestLeft), anchorBounds.bottom + gap)
    }
}

private fun visibleSorts(activeSort: MatchSort): List<MatchSort> =
    MatchSort.entries.filter { sort -> sort != MatchSort.DURATION_SHORTEST || sort == activeSort }

@Composable
fun sortLabel(sort: MatchSort): String =
    when (sort) {
        MatchSort.DATE_NEWEST -> stringResource(R.string.sort_date_newest)
        MatchSort.DATE_OLDEST -> stringResource(R.string.sort_date_oldest)
        MatchSort.RESULT_WINS_FIRST -> stringResource(R.string.sort_result_wins_first)
        MatchSort.RESULT_LOSSES_FIRST -> stringResource(R.string.sort_result_losses_first)
        MatchSort.OPPONENT_A_TO_Z -> stringResource(R.string.sort_opponent_a_to_z)
        MatchSort.LOCATION_A_TO_Z -> stringResource(R.string.sort_location_a_to_z)
        MatchSort.DURATION_SHORTEST -> stringResource(R.string.sort_duration_shortest)
        MatchSort.DURATION_LONGEST -> stringResource(R.string.sort_duration_longest)
    }
