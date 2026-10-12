package com.maxeydev.picklejot.ui.match.list

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.match.GameScore
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult

data class MatchRowUiState(
    val id: String,
    val date: AppDate,
    val format: MatchFormat,
    val result: MatchResult,
    val opponentNames: List<String>,
    val games: List<GameScore>,
    val thumbnailPath: String?,
    val partnerName: String? = null,
    val location: String? = null,
)
