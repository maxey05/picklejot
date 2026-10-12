package com.maxeydev.picklejot.ui.match.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.offset
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.dashboard.DashboardHeader
import com.maxeydev.picklejot.ui.dashboard.DashboardUiState
import com.maxeydev.picklejot.ui.theme.PicklejotSpacing
import com.maxeydev.picklejot.ui.theme.PicklejotTextStyles
import com.maxeydev.picklejot.ui.theme.PicklejotTheme
import com.maxeydev.picklejot.ui.theme.StatusBarIcons

private val APP_BAR_MIN_HEIGHT = 48.dp
private val EXPANDED_SEARCH_TOP_PADDING = PicklejotSpacing.md
private val COLLAPSED_SEARCH_TOP_PADDING = 12.dp
private val COLLAPSED_DASHBOARD_TOP_PADDING = 12.dp
private val HEADER_HORIZONTAL_PADDING = PicklejotSpacing.gutter
private const val DIVIDER_ALPHA = 0.22f

@Composable
internal fun HomeHeader(
    dashboard: DashboardUiState,
    progress: CollapseProgress,
    isCollapsed: Boolean,
    showDetails: Boolean,
    searchText: String,
    onSearchChanged: (String) -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = PicklejotTheme.colors
    val showAppBar by remember(progress) { derivedStateOf { progress.value < 1f } }
    StatusBarIcons(useLightIcons = true)
    val gradient =
        remember(colors.headerTop, colors.headerBottom) {
            Brush.verticalGradient(listOf(colors.headerTop, colors.headerBottom))
        }
    Surface(
        color = Color.Transparent,
        contentColor = colors.onHeader,
        modifier = modifier.fillMaxWidth().background(gradient),
    ) {
        Column(
            modifier =
                Modifier
                    .statusBarsPadding()
                    .padding(bottom = PicklejotSpacing.lg),
        ) {
            if (showAppBar) {
                Column(modifier = Modifier.collapseVertically { progress.value }) {
                    HomeAppBar(onOpenSettings = onOpenSettings)
                    if (showDetails) {
                        HeaderDivider(color = colors.onHeader.copy(alpha = DIVIDER_ALPHA))
                    }
                }
            }
            if (showDetails) {
                DashboardHeader(
                    state = dashboard,
                    isCollapsed = isCollapsed,
                    onOpenStats = onOpenStats,
                    modifier =
                        Modifier
                            .padding(horizontal = HEADER_HORIZONTAL_PADDING)
                            .progressTopPadding(progress, 0.dp, COLLAPSED_DASHBOARD_TOP_PADDING),
                    collapseFraction = { progress.value },
                )
                MatchSearchBar(
                    text = searchText,
                    onTextChanged = onSearchChanged,
                    modifier =
                        Modifier
                            .padding(horizontal = HEADER_HORIZONTAL_PADDING)
                            .progressTopPadding(progress, EXPANDED_SEARCH_TOP_PADDING, COLLAPSED_SEARCH_TOP_PADDING),
                )
            }
        }
    }
}

private fun Modifier.progressTopPadding(
    progress: CollapseProgress,
    expanded: Dp,
    collapsed: Dp,
): Modifier =
    layout { measurable, constraints ->
        val top = lerp(expanded, collapsed, progress.value).roundToPx()
        val placeable = measurable.measure(constraints.offset(vertical = -top))
        layout(placeable.width, placeable.height + top) {
            placeable.placeRelative(0, top)
        }
    }

@Composable
private fun HeaderDivider(color: Color) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = HEADER_HORIZONTAL_PADDING)
                .padding(top = PicklejotSpacing.xs, bottom = PicklejotSpacing.md)
                .height(1.dp)
                .background(color),
    )
}

@Composable
private fun HomeAppBar(onOpenSettings: (() -> Unit)?) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = APP_BAR_MIN_HEIGHT)
                .padding(start = HEADER_HORIZONTAL_PADDING, end = PicklejotSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = PicklejotTextStyles.displaySmall,
            modifier = Modifier.weight(1f).padding(top = 4.dp).semantics { heading() },
        )
        if (onOpenSettings != null) {
            IconButton(onClick = onOpenSettings, modifier = Modifier.testTag(MatchListTestTags.SETTINGS)) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = stringResource(R.string.settings_open),
                )
            }
        }
    }
}
