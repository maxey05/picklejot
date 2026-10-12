package com.maxeydev.picklejot.ui.match.edit

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.match.LastUsedFormatStore
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.match.crossesMidnight
import com.maxeydev.picklejot.domain.match.deriveDuration
import com.maxeydev.picklejot.domain.match.resultAdvisory
import com.maxeydev.picklejot.ui.common.SuggestionUiState
import com.maxeydev.picklejot.ui.paywall.UpgradeReason
import kotlin.time.Duration

data class MatchEditUiState(
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val format: MatchFormat = LastUsedFormatStore.FIRST_LAUNCH_FORMAT,
    val date: AppDate? = null,
    val result: MatchResult? = null,
    val advisory: MatchResult? = null,
    val startTime: AppTime? = null,
    val endTime: AppTime? = null,
    val duration: Duration? = null,
    val endsNextDay: Boolean = false,
    val personSlots: List<PersonSlotUiState> = emptyList(),
    val games: List<GameScoreRowUiState> = emptyList(),
    val location: String = "",
    val paddle: String = "",
    val notes: String = "",
    val clearedOnFormatSwitch: Boolean = false,
    val suggestionTarget: SuggestionTarget? = null,
    val suggestions: List<SuggestionUiState> = emptyList(),
    val recentLocations: List<String> = emptyList(),
    val canSave: Boolean = false,
    val isSaveTappable: Boolean = false,
    val showResultError: Boolean = false,
    val resultErrorAttempts: Int = 0,
    val isSaving: Boolean = false,
    val isFinished: Boolean = false,
    val savedNewMatchId: String? = null,
    val didSave: Boolean = false,
    val photos: List<PhotoUiState> = emptyList(),
    val hasPhotoError: Boolean = false,
    val upgradePrompt: UpgradeReason? = null,
    val isPaywallRequested: Boolean = false,
) {
    fun suggestionsFor(target: SuggestionTarget): List<SuggestionUiState> =
        if (target == suggestionTarget) {
            suggestions
        } else {
            emptyList()
        }
}

fun MatchDraft.toUiState(
    isSaving: Boolean,
    isFinished: Boolean,
    suggestionTarget: SuggestionTarget? = null,
    suggestions: List<SuggestionUiState> = emptyList(),
    recentLocations: List<String> = emptyList(),
    savedNewMatchId: String? = null,
    didSave: Boolean = false,
    photoFilePath: (String) -> String = { it },
    hasPhotoError: Boolean = false,
    showResultError: Boolean = false,
    resultErrorAttempts: Int = 0,
    upgradePrompt: UpgradeReason? = null,
    isPaywallRequested: Boolean = false,
): MatchEditUiState {
    val start = startTime?.let(AppTime::parse)
    val end = endTime?.let(AppTime::parse)
    val duplicates = duplicateSlots()
    return MatchEditUiState(
        isLoading = false,
        isEditing = !isNew,
        format = format,
        date = AppDate.parse(date),
        result = result,
        advisory = result?.let { tapped -> resultAdvisory(tapped, completeGames()) },
        startTime = start,
        endTime = end,
        duration = deriveDuration(start, end),
        endsNextDay = crossesMidnight(start, end),
        personSlots = visibleSlots().map { slot -> PersonSlotUiState(slot, nameIn(slot), duplicates[slot]) },
        games =
            games.mapIndexed { index, game ->
                GameScoreRowUiState(
                    gameNumber = index + 1,
                    myScore = game.myScore,
                    opponentScore = game.opponentScore,
                    isIncomplete = game.isIncomplete,
                )
            },
        location = location,
        paddle = paddle,
        notes = notes,
        clearedOnFormatSwitch = clearedOnFormatSwitch,
        suggestionTarget = suggestionTarget,
        suggestions = suggestions,
        recentLocations = recentLocations,
        canSave = canSave() && !isSaving && !isFinished,
        isSaveTappable = canSaveIgnoringResult() && !isSaving && !isFinished,
        showResultError = showResultError && result == null,
        resultErrorAttempts = resultErrorAttempts,
        isSaving = isSaving,
        isFinished = isFinished,
        savedNewMatchId = savedNewMatchId,
        didSave = didSave,
        photos = photos.map { PhotoUiState(key = it.key, filePath = it.relativePath?.let(photoFilePath)) },
        hasPhotoError = hasPhotoError,
        upgradePrompt = upgradePrompt,
        isPaywallRequested = isPaywallRequested,
    )
}
