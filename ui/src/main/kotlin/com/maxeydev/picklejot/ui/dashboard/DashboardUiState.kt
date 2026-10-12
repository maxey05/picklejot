@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui.dashboard

import com.maxeydev.picklejot.domain.match.FilterState
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.stats.AdvancedStats
import com.maxeydev.picklejot.domain.stats.BasicStats
import com.maxeydev.picklejot.domain.streak.MissedSkipOpportunity
import com.maxeydev.picklejot.domain.streak.StreakResult
import com.maxeydev.picklejot.domain.streak.WeekKey
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class DashboardUiState(
    val isLoading: Boolean = true,
    val displayName: String = "",
    val stats: BasicStats = BasicStats.EMPTY,
    val overallStats: BasicStats = BasicStats.EMPTY,
    val recentResults: List<MatchResult> = emptyList(),
    val streak: StreakResult = StreakResult.NONE,
    val hasAnyMatches: Boolean = false,
    val filter: FilterState = FilterState.NONE,
    val filteredOpponentName: String? = null,
    val isPro: Boolean = false,
    val advanced: AdvancedStats = AdvancedStats.EMPTY,
    val peopleNames: Map<Uuid, String> = emptyMap(),
    val skipsHeld: Int = 0,
    val usedSkipWeek: WeekKey? = null,
    val missedOpportunity: MissedSkipOpportunity? = null,
) {
    val isFiltered: Boolean
        get() = filter.isActive

    val hasDisplayName: Boolean
        get() = displayName.isNotBlank()

    val hasNoFilteredMatches: Boolean
        get() = hasAnyMatches && stats.totalMatches == 0

    fun nameOf(personId: Uuid): String? = peopleNames[personId]
}
