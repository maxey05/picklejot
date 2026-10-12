package com.maxeydev.picklejot.ui.match.edit

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.ui.common.SuggestionUiState

class MatchEditActions(
    val onFormatSelected: (MatchFormat) -> Unit,
    val onResultSelected: (MatchResult) -> Unit,
    val onDateSelected: (AppDate) -> Unit,
    val onStartTimeChanged: (AppTime?) -> Unit,
    val onEndTimeChanged: (AppTime?) -> Unit,
    val onPersonNameChanged: (PersonSlot, String) -> Unit,
    val onGameAdded: () -> Unit,
    val onGameRemoved: (Int) -> Unit,
    val onGameScoresChanged: (Int, String, String) -> Unit,
    val onLocationChanged: (String) -> Unit,
    val onPaddleChanged: (String) -> Unit,
    val onNotesChanged: (String) -> Unit,
    val onSuggestionFocusChanged: (SuggestionTarget, Boolean) -> Unit,
    val onSuggestionSelected: (SuggestionTarget, SuggestionUiState) -> Unit,
    val photoActions: PhotoPickerActions,
    val onSave: () -> Unit,
    val onClose: () -> Unit,
    val onUpgradePromptDismissed: () -> Unit = {},
    val onSeePro: (() -> Unit)? = null,
)
