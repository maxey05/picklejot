package com.maxeydev.picklejot.ui.match.detail

import com.maxeydev.picklejot.domain.match.Match
import kotlin.time.Duration

data class MatchDetailUiState(
    val isLoading: Boolean = true,
    val match: Match? = null,
    val matchNumber: Int = 0,
    val duration: Duration? = null,
    val endsNextDay: Boolean = false,
    val isConfirmingDelete: Boolean = false,
    val isGone: Boolean = false,
    val photoPaths: List<String> = emptyList(),
)
