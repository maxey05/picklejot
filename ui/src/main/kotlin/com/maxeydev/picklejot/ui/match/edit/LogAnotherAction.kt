@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui.match.edit

import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.match.Match
import kotlin.uuid.ExperimentalUuidApi

fun logAnotherDraft(
    source: Match,
    matchId: String,
    startTime: AppTime,
): MatchDraft =
    MatchDraft(
        matchId = matchId,
        isNew = true,
        format = source.format,
        date = source.date.toString(),
        startTime = startTime.toString(),
        opponentNames =
            listOf(
                source.opponents.getOrNull(0)?.displayName.orEmpty(),
                source.opponents.getOrNull(1)?.displayName.orEmpty(),
            ),
        opponentIds =
            listOf(
                source.opponents.getOrNull(0)?.id?.toString(),
                source.opponents.getOrNull(1)?.id?.toString(),
            ),
        partnerName = source.partner?.displayName.orEmpty(),
        partnerId = source.partner?.id?.toString(),
        location = source.location.orEmpty(),
    )
