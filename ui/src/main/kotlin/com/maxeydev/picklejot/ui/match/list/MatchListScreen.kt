@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui.match.list

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.domain.match.MatchSort
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.dashboard.DashboardUiState
import com.maxeydev.picklejot.ui.theme.PicklejotSpacing
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.uuid.ExperimentalUuidApi

private val LIST_BOTTOM_SPACE = 88.dp
private const val MATCH_ROW_CONTENT_TYPE = "match_row"
private const val ROW_GLIDE_MILLIS = 360

private val RowGlideSpec: FiniteAnimationSpec<IntOffset> =
    tween(durationMillis = ROW_GLIDE_MILLIS, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MatchListScreen(
    state: MatchListUiState,
    onNewMatch: () -> Unit,
    onOpenMatch: (String) -> Unit,
    onSortSelected: (MatchSort) -> Unit,
    onLastVisibleIndexChanged: (Int) -> Unit,
    onLogAnother: (String) -> Unit,
    onSavedConfirmationDismissed: () -> Unit,
    filterActions: MatchListFilterActions,
    modifier: Modifier = Modifier,
    dashboard: DashboardUiState = DashboardUiState(),
    notices: @Composable () -> Unit = {},
    onOpenStats: () -> Unit = {},
    onOpenSettings: (() -> Unit)? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var searchText by rememberSaveable { mutableStateOf("") }
    var isFilterSheetOpen by rememberSaveable { mutableStateOf(false) }
    var isScrolledCollapse by rememberSaveable { mutableStateOf(false) }
    val queryKey = listOf(state.sort, state.filter, state.appliedSearch).toString()
    var queryOnScreen by rememberSaveable { mutableStateOf(queryKey) }
    val isEmptyState = state.isEmpty && searchText.isBlank()
    val showsList = !state.isLoading && !isEmptyState
    val isCollapsed = showsList && (searchText.isNotBlank() || (isScrolledCollapse && !state.hasNoResults))
    val collapseProgress = rememberCollapseProgress(isCollapsed)

    SavedMatchSnackbarEffect(
        savedMatchId = state.savedMatchId,
        snackbarHostState = snackbarHostState,
        onLogAnother = onLogAnother,
        onDismissed = onSavedConfirmationDismissed,
    )
    val collapseConnection =
        remember(listState) {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (consumed.y < 0f) {
                        isScrolledCollapse = true
                    } else if (available.y > 0f || (consumed.y > 0f && !listState.canScrollBackward)) {
                        isScrolledCollapse = false
                    }
                    return Offset.Zero
                }
            }
        }
    LaunchedEffect(queryKey) {
        if (queryKey != queryOnScreen) {
            queryOnScreen = queryKey
            isScrolledCollapse = false
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        modifier = modifier.semantics { testTagsAsResourceId = true },
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (showsList) {
                FloatingActionButton(
                    onClick = onNewMatch,
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag(MatchListTestTags.NEW_MATCH),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.new_match),
                    )
                }
            }
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val bottomInset = innerPadding.calculateBottomPadding()
        HomeHeaderLayout(
            progress = collapseProgress,
            isCollapsing = isCollapsed,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        top = innerPadding.calculateTopPadding(),
                        end = innerPadding.calculateEndPadding(layoutDirection),
                    ),
            header = {
                HomeHeader(
                    dashboard = dashboard,
                    progress = collapseProgress,
                    isCollapsed = isCollapsed,
                    showDetails = showsList,
                    searchText = searchText,
                    onSearchChanged = { text ->
                        searchText = text
                        filterActions.onSearchChanged(text)
                    },
                    onOpenStats = onOpenStats,
                    onOpenSettings = onOpenSettings,
                )
            },
            body = {
                Column(modifier = Modifier.fillMaxSize().nestedScroll(collapseConnection)) {
                    notices()
                    when {
                        state.isLoading ->
                            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(bottom = bottomInset))
                        isEmptyState ->
                            MatchListEmptyState(
                                onNewMatch = onNewMatch,
                                modifier = Modifier.weight(1f).fillMaxWidth().padding(bottom = bottomInset),
                            )
                        else ->
                            MatchListContent(
                                state = state,
                                listState = listState,
                                onOpenMatch = onOpenMatch,
                                onSortSelected = onSortSelected,
                                onOpenFilters = { isFilterSheetOpen = true },
                                onLastVisibleIndexChanged = onLastVisibleIndexChanged,
                                onFiltersAndSearchCleared = {
                                    searchText = ""
                                    filterActions.onFiltersAndSearchCleared()
                                },
                                filterActions = filterActions,
                                bottomInset = bottomInset,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                            )
                    }
                }
            },
        )
    }
    if (isFilterSheetOpen) {
        FilterSheet(
            filter = state.filter,
            opponentChoices = state.opponentChoices,
            locationChoices = state.locationChoices,
            resultCount = state.resultCount,
            onFilterChanged = filterActions.onFilterChanged,
            onAllFiltersCleared = filterActions.onAllFiltersCleared,
            onDismiss = { isFilterSheetOpen = false },
        )
    }
}

@Composable
private fun SavedMatchSnackbarEffect(
    savedMatchId: String?,
    snackbarHostState: SnackbarHostState,
    onLogAnother: (String) -> Unit,
    onDismissed: () -> Unit,
) {
    val message = stringResource(R.string.match_saved)
    val actionLabel = stringResource(R.string.log_another)
    val latestOnLogAnother by rememberUpdatedState(onLogAnother)
    val latestOnDismissed by rememberUpdatedState(onDismissed)
    LaunchedEffect(savedMatchId) {
        if (savedMatchId == null) {
            return@LaunchedEffect
        }
        val result =
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
        latestOnDismissed()
        if (result == SnackbarResult.ActionPerformed) {
            latestOnLogAnother(savedMatchId)
        }
    }
}

@Composable
private fun MatchListContent(
    state: MatchListUiState,
    listState: LazyListState,
    onOpenMatch: (String) -> Unit,
    onSortSelected: (MatchSort) -> Unit,
    onOpenFilters: () -> Unit,
    onLastVisibleIndexChanged: (Int) -> Unit,
    onFiltersAndSearchCleared: () -> Unit,
    filterActions: MatchListFilterActions,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    val latestOnLastVisibleIndexChanged by rememberUpdatedState(onLastVisibleIndexChanged)
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) to layoutInfo.totalItemsCount
        }.distinctUntilChanged()
            .collect { (lastVisibleIndex, _) -> latestOnLastVisibleIndexChanged(lastVisibleIndex) }
    }
    Column(modifier = modifier) {
        MatchListToolbar(state = state, onSortSelected = onSortSelected, onOpenFilters = onOpenFilters)
        if (state.filter.isActive) {
            FilterChips(
                filter = state.filter,
                opponentName = state.filteredOpponentName,
                onFilterCleared = filterActions.onFilterCleared,
                onAllFiltersCleared = filterActions.onAllFiltersCleared,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        if (state.hasNoResults) {
            NoResultsState(
                onClear = onFiltersAndSearchCleared,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(bottom = bottomInset),
            )
        } else {
            LazyColumn(
                state = listState,
                contentPadding =
                    PaddingValues(
                        start = PicklejotSpacing.gutter,
                        end = PicklejotSpacing.gutter,
                        top = PicklejotSpacing.xs,
                        bottom = LIST_BOTTOM_SPACE + bottomInset,
                    ),
                verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.sm),
                modifier = Modifier.weight(1f).fillMaxWidth().testTag(MatchListTestTags.LIST),
            ) {
                items(
                    items = state.matches,
                    key = { row -> row.id },
                    contentType = { MATCH_ROW_CONTENT_TYPE },
                ) { row ->
                    MatchRow(
                        state = row,
                        onClick = { onOpenMatch(row.id) },
                        modifier = Modifier.animateItem(placementSpec = RowGlideSpec),
                    )
                }
            }
        }
    }
}
