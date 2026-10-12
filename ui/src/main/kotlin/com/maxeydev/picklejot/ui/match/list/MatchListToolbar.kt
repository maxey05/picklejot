package com.maxeydev.picklejot.ui.match.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.domain.match.MatchSort
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.theme.PicklejotSpacing
import com.maxeydev.picklejot.ui.theme.PicklejotTheme

private val MIN_TOUCH_TARGET = 48.dp
private val FILTER_ICON_SIZE = 24.dp
private val COUNT_BADGE_MIN_SIZE = 20.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MatchListToolbar(
    state: MatchListUiState,
    onSortSelected: (MatchSort) -> Unit,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = PicklejotSpacing.gutter, vertical = PicklejotSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.Center,
    ) {
        MatchCountText(
            state = state,
            modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET).wrapContentHeight(Alignment.CenterVertically),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterButton(activeCount = state.filter.activeKinds.size, onClick = onOpenFilters)
            SortMenu(activeSort = state.sort, onSortSelected = onSortSelected)
        }
    }
}

@Composable
private fun MatchCountText(
    state: MatchListUiState,
    modifier: Modifier = Modifier,
) {
    val total = state.totalCount
    val figure = if (state.isNarrowed) state.resultCount else total
    val suffix =
        if (state.isNarrowed) {
            pluralStringResource(R.plurals.list_count_of_total, total, total)
        } else {
            pluralStringResource(R.plurals.list_match_word, total)
        }
    val description =
        if (state.isNarrowed) {
            pluralStringResource(R.plurals.list_count_a11y_filtered, total, state.resultCount, total)
        } else {
            pluralStringResource(R.plurals.dashboard_match_count, total, total)
        }
    val text =
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                append(figure.toString())
            }
            append(" ")
            append(suffix)
        }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.testTag(MatchListTestTags.COUNT).semantics { contentDescription = description },
    )
}

@Composable
private fun FilterButton(
    activeCount: Int,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val isActive = activeCount > 0
    val description =
        if (isActive) {
            stringResource(R.string.filter_button_active, activeCount)
        } else {
            stringResource(R.string.filter_button)
        }
    Box(modifier = Modifier.size(MIN_TOUCH_TARGET)) {
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            color = if (isActive) colors.primary else colors.surfaceContainerLowest,
            contentColor = if (isActive) colors.onPrimary else colors.onSurface,
            border = if (isActive) null else BorderStroke(1.dp, PicklejotTheme.colors.cardBorder),
            modifier =
                Modifier
                    .size(MIN_TOUCH_TARGET)
                    .testTag(MatchListTestTags.FILTER_BUTTON)
                    .semantics {
                        contentDescription = description
                        role = Role.Button
                    },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_filter),
                    contentDescription = null,
                    modifier = Modifier.size(FILTER_ICON_SIZE),
                )
            }
        }
        if (isActive) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .defaultMinSize(minWidth = COUNT_BADGE_MIN_SIZE, minHeight = COUNT_BADGE_MIN_SIZE)
                        .background(PicklejotTheme.colors.countBadge, CircleShape)
                        .padding(horizontal = PicklejotSpacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = activeCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = PicklejotTheme.colors.onCountBadge,
                )
            }
        }
    }
}
