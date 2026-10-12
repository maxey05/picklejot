@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.domain.match

import kotlin.uuid.ExperimentalUuidApi

private const val MAX_OPPONENTS_SINGLES = 1
private const val MAX_OPPONENTS_DOUBLES = 2

fun maxOpponentsFor(format: MatchFormat): Int =
    when (format) {
        MatchFormat.SINGLES -> MAX_OPPONENTS_SINGLES
        MatchFormat.DOUBLES -> MAX_OPPONENTS_DOUBLES
    }

fun Match.requireValidRoster() {
    val maxOpponents = maxOpponentsFor(format)
    require(opponents.size <= maxOpponents) {
        "A $format match allows at most $maxOpponents opponent(s), but ${opponents.size} were supplied."
    }
    require(format != MatchFormat.SINGLES || partner == null) {
        "A SINGLES match cannot carry a partner; partner was ${partner?.displayName}."
    }
    val assignedIds = opponents.map { it.id } + listOfNotNull(partner?.id)
    require(assignedIds.size == assignedIds.distinct().size) {
        "The same person cannot occupy more than one slot on a match."
    }
}
