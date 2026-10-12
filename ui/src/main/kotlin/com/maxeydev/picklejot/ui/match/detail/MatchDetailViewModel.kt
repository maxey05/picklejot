@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui.match.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxeydev.picklejot.domain.match.MatchRepository
import com.maxeydev.picklejot.domain.match.crossesMidnight
import com.maxeydev.picklejot.domain.match.deriveDuration
import com.maxeydev.picklejot.domain.photo.inDisplayOrder
import com.maxeydev.picklejot.ui.PicklejotDependencies
import com.maxeydev.picklejot.ui.navigation.MATCH_ID_ARGUMENT
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val CONFIRMING_DELETE_KEY = "match_detail_confirming_delete"
private const val STOP_TIMEOUT_MILLIS = 5_000L

class MatchDetailViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val matchRepository: MatchRepository,
    private val photoFile: (String) -> File = { File(it) },
) : ViewModel() {
    private val matchId: Uuid =
        Uuid.parse(
            requireNotNull(savedStateHandle.get<String>(MATCH_ID_ARGUMENT)) {
                "The match detail screen was opened without a match id."
            },
        )

    val uiState: StateFlow<MatchDetailUiState> =
        combine(
            matchRepository.observeById(matchId),
            matchRepository.observeMatchNumber(matchId),
            savedStateHandle.getStateFlow(CONFIRMING_DELETE_KEY, false),
        ) { match, matchNumber, isConfirmingDelete ->
            if (match == null) {
                MatchDetailUiState(isLoading = false, isGone = true)
            } else {
                MatchDetailUiState(
                    isLoading = false,
                    match = match,
                    matchNumber = matchNumber,
                    duration = deriveDuration(match.startTime, match.endTime),
                    endsNextDay = crossesMidnight(match.startTime, match.endTime),
                    isConfirmingDelete = isConfirmingDelete,
                    photoPaths = match.photos.inDisplayOrder().map { photoFile(it.relativePath).path },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MatchDetailUiState())

    fun requestDelete() {
        savedStateHandle[CONFIRMING_DELETE_KEY] = true
    }

    fun dismissDelete() {
        savedStateHandle[CONFIRMING_DELETE_KEY] = false
    }

    fun confirmDelete() {
        if (savedStateHandle.get<Boolean>(CONFIRMING_DELETE_KEY) != true) {
            return
        }
        savedStateHandle[CONFIRMING_DELETE_KEY] = false
        viewModelScope.launch { matchRepository.deleteMatch(matchId) }
    }

    companion object {
        fun factory(dependencies: PicklejotDependencies): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    MatchDetailViewModel(
                        savedStateHandle = createSavedStateHandle(),
                        matchRepository = dependencies.matchRepository,
                        photoFile = dependencies::photoFile,
                    )
                }
            }
    }
}
