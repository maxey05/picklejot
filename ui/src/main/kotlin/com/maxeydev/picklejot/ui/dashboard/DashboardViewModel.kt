@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxeydev.picklejot.domain.match.FilterState
import com.maxeydev.picklejot.domain.match.MatchRepository
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.match.MatchSort
import com.maxeydev.picklejot.domain.person.Person
import com.maxeydev.picklejot.domain.person.PersonRepository
import com.maxeydev.picklejot.domain.profile.ProfileRepository
import com.maxeydev.picklejot.domain.profile.UserProfile
import com.maxeydev.picklejot.domain.stats.AdvancedStats
import com.maxeydev.picklejot.domain.stats.BasicStats
import com.maxeydev.picklejot.domain.stats.MatchStatLine
import com.maxeydev.picklejot.domain.streak.InsuredStreakEngine
import com.maxeydev.picklejot.domain.streak.MissedSkipOpportunity
import com.maxeydev.picklejot.domain.streak.SkipNotices
import com.maxeydev.picklejot.domain.streak.StreakNoticeState
import com.maxeydev.picklejot.domain.streak.StreakNoticeStore
import com.maxeydev.picklejot.domain.streak.streakInsuranceStart
import com.maxeydev.picklejot.ui.PicklejotDependencies
import com.maxeydev.picklejot.ui.match.list.observeHomeFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi

private const val STOP_TIMEOUT_MILLIS = 5_000L
private const val RECENT_RESULT_COUNT = 5

private data class FilteredStats(
    val filter: FilterState,
    val stats: BasicStats,
)

private fun StreakNoticeState.hides(opportunity: MissedSkipOpportunity): Boolean =
    SkipNotices.isMissedOpportunityAcknowledged(opportunity, acknowledgedMissedWeek)

class DashboardViewModel(
    homeEntryState: SavedStateHandle,
    matchRepository: MatchRepository,
    personRepository: PersonRepository,
    profileRepository: ProfileRepository,
    private val streakEngine: InsuredStreakEngine,
    private val streakNoticeStore: StreakNoticeStore,
    defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val homeFilter: Flow<FilterState> = observeHomeFilter(homeEntryState).distinctUntilChanged()

    private val filteredStats: Flow<FilteredStats> =
        homeFilter.flatMapLatest { filter ->
            matchRepository.observeStatLines(filter).map { lines ->
                FilteredStats(filter = filter, stats = BasicStats.from(lines))
            }
        }

    private val wholeHistory: Flow<List<MatchStatLine>> = matchRepository.observeStatLines(FilterState.NONE)

    private val recentResults: Flow<List<MatchResult>> =
        matchRepository
            .observeListPage(MatchSort.DATE_NEWEST, RECENT_RESULT_COUNT)
            .map { items -> items.map { it.result }.reversed() }

    private val advancedStats: Flow<AdvancedStats> =
        homeFilter.flatMapLatest { filter ->
            matchRepository.observeAdvancedLines(filter).map { lines ->
                AdvancedStats.from(lines, streakEngine.today())
            }
        }

    private val baseState: Flow<DashboardUiState> =
        combine(
            filteredStats,
            wholeHistory,
            profileRepository.observeProfile(),
            personRepository.observeAll(),
            streakNoticeStore.observe(),
        ) {
                filtered: FilteredStats,
                history: List<MatchStatLine>,
                profile: UserProfile,
                people: List<Person>,
                notices: StreakNoticeState,
            ->
            val dates = history.map { it.date }
            val insured = streakEngine.compute(dates, profile.entitlement.streakInsuranceStart())
            val today = streakEngine.today()
            val isPro = profile.entitlement.isPro
            val missed =
                if (isPro) null else SkipNotices.findMissedOpportunity(dates, today)?.takeUnless(notices::hides)
            DashboardUiState(
                isLoading = false,
                displayName = profile.displayName.trim(),
                stats = filtered.stats,
                overallStats = BasicStats.from(history),
                streak = insured.streak,
                hasAnyMatches = history.isNotEmpty(),
                filter = filtered.filter,
                filteredOpponentName =
                    filtered.filter.opponentId?.let { id -> people.firstOrNull { it.id == id }?.displayName },
                isPro = isPro,
                peopleNames = people.associate { it.id to it.displayName },
                skipsHeld = if (isPro) insured.skipsHeld else 0,
                usedSkipWeek =
                    if (isPro) {
                        SkipNotices.unacknowledgedSkip(insured, today, notices.acknowledgedSkipWeek)
                    } else {
                        null
                    },
                missedOpportunity = missed,
            )
        }

    val uiState: StateFlow<DashboardUiState> =
        combine(baseState, advancedStats, recentResults) { state, advanced, recent ->
            state.copy(advanced = advanced, recentResults = recent)
        }.flowOn(defaultDispatcher)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DashboardUiState())

    fun dismissUsedSkip() {
        val week = uiState.value.usedSkipWeek ?: return
        viewModelScope.launch { streakNoticeStore.acknowledgeSkip(week.ordinal) }
    }

    fun dismissMissedOpportunity() {
        val opportunity = uiState.value.missedOpportunity ?: return
        viewModelScope.launch { streakNoticeStore.acknowledgeMissed(opportunity.missedWeek.ordinal) }
    }

    companion object {
        fun factory(
            dependencies: PicklejotDependencies,
            homeEntryState: SavedStateHandle,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    DashboardViewModel(
                        homeEntryState = homeEntryState,
                        matchRepository = dependencies.matchRepository,
                        personRepository = dependencies.personRepository,
                        profileRepository = dependencies.profileRepository,
                        streakEngine = InsuredStreakEngine(dependencies.clock, dependencies::currentTimeZone),
                        streakNoticeStore = dependencies.streakNoticeStore,
                        defaultDispatcher = dependencies.defaultDispatcher,
                    )
                }
            }
    }
}
