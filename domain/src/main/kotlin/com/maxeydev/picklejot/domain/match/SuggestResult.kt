package com.maxeydev.picklejot.domain.match

fun suggestedResult(games: List<GameScore>): MatchResult? {
    val wins = games.count { it.myScore > it.opponentScore }
    val losses = games.count { it.opponentScore > it.myScore }
    return when {
        wins > losses -> MatchResult.WIN
        losses > wins -> MatchResult.LOSS
        else -> null
    }
}

fun resultAdvisory(
    tappedResult: MatchResult,
    games: List<GameScore>,
): MatchResult? {
    val suggested = suggestedResult(games) ?: return null
    return if (suggested == tappedResult) {
        null
    } else {
        suggested
    }
}
