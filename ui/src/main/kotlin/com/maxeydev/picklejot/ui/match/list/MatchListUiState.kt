@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui.match.list

import com.maxeydev.picklejot.domain.entitlement.CapWarning
import com.maxeydev.picklejot.domain.match.FilterState
import com.maxeydev.picklejot.domain.match.MatchSort
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class OpponentChoice(
    val id: Uuid,
    val name: String,
)

data class MatchListUiState(
    val isLoading: Boolean = true,
    val sort: MatchSort = MatchSort.DEFAULT,
    val matches: List<MatchRowUiState> = emptyList(),
    val pageLimit: Int = 0,
    val savedMatchId: String? = null,
    val filter: FilterState = FilterState.NONE,
    val appliedSearch: String? = null,
    val opponentChoices: List<OpponentChoice> = emptyList(),
    val locationChoices: List<String> = emptyList(),
    val capWarning: CapWarning = CapWarning.NONE,
    val remainingFreeMatches: Int = 0,
    val totalCount: Int = 0,
    val resultCount: Int = 0,
) {
    val canLoadMore: Boolean
        get() = pageLimit > 0 && matches.size >= pageLimit

    val isNarrowed: Boolean
        get() = filter.isActive || appliedSearch != null

    val isEmpty: Boolean
        get() = !isLoading && matches.isEmpty() && !isNarrowed

    val hasNoResults: Boolean
        get() = !isLoading && matches.isEmpty() && isNarrowed

    val filteredOpponentName: String?
        get() = filter.opponentId?.let { id -> opponentChoices.firstOrNull { it.id == id }?.name }
}
