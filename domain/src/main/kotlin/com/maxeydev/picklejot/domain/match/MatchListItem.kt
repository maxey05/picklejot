@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.domain.match

import com.maxeydev.picklejot.domain.datetime.AppDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class MatchListItem(
    val id: Uuid,
    val date: AppDate,
    val format: MatchFormat,
    val result: MatchResult,
    val opponentNames: List<String>,
    val games: List<GameScore>,
    val primaryPhotoPath: String?,
    val partnerName: String? = null,
    val location: String? = null,
)
