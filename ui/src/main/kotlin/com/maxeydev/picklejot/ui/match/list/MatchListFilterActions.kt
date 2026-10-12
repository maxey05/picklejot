package com.maxeydev.picklejot.ui.match.list

import com.maxeydev.picklejot.domain.match.FilterKind
import com.maxeydev.picklejot.domain.match.FilterState

data class MatchListFilterActions(
    val onSearchChanged: (String) -> Unit,
    val onFilterChanged: (FilterState) -> Unit,
    val onFilterCleared: (FilterKind) -> Unit,
    val onAllFiltersCleared: () -> Unit,
    val onFiltersAndSearchCleared: () -> Unit,
)
