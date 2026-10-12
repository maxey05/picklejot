@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class, FlowPreview::class)

package com.maxeydev.picklejot.ui.match.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxeydev.picklejot.domain.entitlement.CapWarning
import com.maxeydev.picklejot.domain.match.FilterKind
import com.maxeydev.picklejot.domain.match.FilterState
import com.maxeydev.picklejot.domain.match.FreeTextField
import com.maxeydev.picklejot.domain.match.MatchListItem
import com.maxeydev.picklejot.domain.match.MatchRepository
import com.maxeydev.picklejot.domain.match.MatchSort
import com.maxeydev.picklejot.domain.match.MatchSortStore
import com.maxeydev.picklejot.domain.match.SearchTerm
import com.maxeydev.picklejot.domain.person.PersonRepository
import com.maxeydev.picklejot.domain.profile.EntitlementRepository
import com.maxeydev.picklejot.ui.PicklejotDependencies
import com.maxeydev.picklejot.ui.navigation.JUST_SAVED_MATCH_ID_KEY
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import kotlin.uuid.ExperimentalUuidApi

const val MATCH_LIST_PAGE_SIZE = 50
const val MATCH_LIST_PREFETCH_DISTANCE = 15
const val SEARCH_DEBOUNCE_MILLIS = 300L
const val FILTER_STATE_KEY = "match_list_filter"
const val SEARCH_TEXT_KEY = "match_list_search"
const val CAP_WARNING_DISMISSED_KEY = "cap_warning_dismissed"
private const val STOP_TIMEOUT_MILLIS = 5_000L

private data class ListQuery(
    val sort: MatchSort,
    val filter: FilterState,
    val search: SearchTerm?,
)

private data class FilterChoices(
    val opponents: List<OpponentChoice>,
    val locations: List<String>,
)

private data class MatchCounts(
    val total: Int,
    val result: Int,
)

private data class CapBanner(
    val warning: CapWarning,
    val remainingFreeMatches: Int,
)

class MatchListViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val matchRepository: MatchRepository,
    private val personRepository: PersonRepository,
    private val matchSortStore: MatchSortStore,
    entitlementRepository: EntitlementRepository,
    private val photoFile: (String) -> File,
    defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val pageLimit = MutableStateFlow(MATCH_LIST_PAGE_SIZE)

    private val filter: Flow<FilterState> = observeHomeFilter(savedStateHandle)

    private val appliedSearch: Flow<SearchTerm?> =
        savedStateHandle
            .getStateFlow(SEARCH_TEXT_KEY, "")
            .debounce { text -> if (text.isBlank()) 0L else SEARCH_DEBOUNCE_MILLIS }
            .map { text -> SearchTerm.of(text) }
            .distinctUntilChanged()

    private val listContent: Flow<MatchListUiState> =
        combine(matchSortStore.observeSort(), filter, appliedSearch, ::ListQuery)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                pageLimit.value = MATCH_LIST_PAGE_SIZE
                pageLimit.flatMapLatest { limit ->
                    matchRepository.observeListPage(query.sort, limit, query.filter, query.search).map { items ->
                        MatchListUiState(
                            isLoading = false,
                            sort = query.sort,
                            matches = items.map { it.toRowUiState() },
                            pageLimit = limit,
                            filter = query.filter,
                            appliedSearch = query.search?.text,
                        )
                    }
                }
            }.flowOn(defaultDispatcher)

    private val filterChoices: Flow<FilterChoices> =
        combine(
            personRepository.observeAll(),
            matchRepository.observePriorValues(FreeTextField.LOCATION),
        ) { people, locations ->
            FilterChoices(
                opponents = people.map { OpponentChoice(id = it.id, name = it.displayName) },
                locations = locations.map { it.value }.sortedWith(String.CASE_INSENSITIVE_ORDER),
            )
        }.flowOn(defaultDispatcher)

    private val matchCounts: Flow<MatchCounts> =
        combine(filter, appliedSearch) { currentFilter, currentSearch -> currentFilter to currentSearch }
            .distinctUntilChanged()
            .flatMapLatest { (currentFilter, currentSearch) ->
                combine(
                    matchRepository.observeMatchCount(),
                    matchRepository.observeFilteredMatchCount(currentFilter, currentSearch),
                    ::MatchCounts,
                )
            }.distinctUntilChanged()
            .flowOn(defaultDispatcher)

    private val capBanner: Flow<CapBanner> =
        combine(
            matchRepository.observeMatchCount(),
            entitlementRepository.observeEntitlement(),
            savedStateHandle.getStateFlow<String?>(CAP_WARNING_DISMISSED_KEY, null),
        ) { savedMatches, entitlement, dismissedName ->
            val warning = CapWarning.forCount(savedMatches, entitlement)
            val dismissed = CapWarning.entries.firstOrNull { it.name == dismissedName }
            val visible = if (dismissed != null && warning.ordinal <= dismissed.ordinal) CapWarning.NONE else warning
            CapBanner(visible, CapWarning.remainingFreeMatches(savedMatches))
        }.distinctUntilChanged()

    val uiState: StateFlow<MatchListUiState> =
        combine(
            listContent,
            filterChoices,
            savedStateHandle.getStateFlow<String?>(JUST_SAVED_MATCH_ID_KEY, null),
            capBanner,
            matchCounts,
        ) { state, choices, savedMatchId, banner, counts ->
            state.copy(
                savedMatchId = savedMatchId,
                opponentChoices = choices.opponents,
                locationChoices = choices.locations,
                capWarning = banner.warning,
                remainingFreeMatches = banner.remainingFreeMatches,
                totalCount = counts.total,
                resultCount = counts.result,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MatchListUiState())

    fun loadMoreIfNeeded(lastVisibleIndex: Int) {
        val state = uiState.value
        if (!state.canLoadMore) {
            return
        }
        if (lastVisibleIndex < state.matches.size - MATCH_LIST_PREFETCH_DISTANCE) {
            return
        }
        pageLimit.compareAndSet(state.pageLimit, state.pageLimit + MATCH_LIST_PAGE_SIZE)
    }

    fun dismissCapWarning() {
        val showing = uiState.value.capWarning
        if (showing != CapWarning.NONE) {
            savedStateHandle[CAP_WARNING_DISMISSED_KEY] = showing.name
        }
    }

    fun dismissSavedConfirmation() {
        savedStateHandle[JUST_SAVED_MATCH_ID_KEY] = null
    }

    fun selectSort(sort: MatchSort) {
        viewModelScope.launch { matchSortStore.saveSort(sort) }
    }

    fun changeFilter(filter: FilterState) {
        savedStateHandle[FILTER_STATE_KEY] = encodeFilterState(filter)
    }

    fun clearFilter(kind: FilterKind) {
        changeFilter(currentFilter().without(kind))
    }

    fun clearAllFilters() {
        changeFilter(FilterState.NONE)
    }

    fun changeSearch(text: String) {
        savedStateHandle[SEARCH_TEXT_KEY] = text
    }

    fun clearFiltersAndSearch() {
        clearAllFilters()
        changeSearch("")
    }

    private fun currentFilter(): FilterState = decodeFilterState(savedStateHandle[FILTER_STATE_KEY])

    private fun MatchListItem.toRowUiState(): MatchRowUiState =
        MatchRowUiState(
            id = id.toString(),
            date = date,
            format = format,
            result = result,
            opponentNames = opponentNames,
            games = games,
            thumbnailPath = primaryPhotoPath?.let { photoFile(it).path },
            partnerName = partnerName,
            location = location,
        )

    companion object {
        fun factory(
            dependencies: PicklejotDependencies,
            homeEntryState: SavedStateHandle,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    MatchListViewModel(
                        savedStateHandle = homeEntryState,
                        matchRepository = dependencies.matchRepository,
                        personRepository = dependencies.personRepository,
                        matchSortStore = dependencies.matchSortStore,
                        entitlementRepository = dependencies.entitlementRepository,
                        photoFile = dependencies::photoFile,
                        defaultDispatcher = dependencies.defaultDispatcher,
                    )
                }
            }
    }
}
