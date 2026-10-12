package com.maxeydev.picklejot.domain.stats

import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult

data class BasicStats(
    val overall: WinLoss,
    val singles: WinLoss,
    val doubles: WinLoss,
) {
    init {
        require(singles.wins + doubles.wins == overall.wins) {
            "The singles and doubles wins must add up to the overall wins."
        }
        require(singles.losses + doubles.losses == overall.losses) {
            "The singles and doubles losses must add up to the overall losses."
        }
    }

    val totalMatches: Int
        get() = overall.total

    companion object {
        val EMPTY: BasicStats = BasicStats(WinLoss.NONE, WinLoss.NONE, WinLoss.NONE)

        fun from(lines: Iterable<MatchStatLine>): BasicStats {
            var singlesWins = 0
            var singlesLosses = 0
            var doublesWins = 0
            var doublesLosses = 0
            for (line in lines) {
                val isWin = line.result == MatchResult.WIN
                when (line.format) {
                    MatchFormat.SINGLES -> if (isWin) singlesWins++ else singlesLosses++
                    MatchFormat.DOUBLES -> if (isWin) doublesWins++ else doublesLosses++
                }
            }
            return BasicStats(
                overall = WinLoss(wins = singlesWins + doublesWins, losses = singlesLosses + doublesLosses),
                singles = WinLoss(wins = singlesWins, losses = singlesLosses),
                doubles = WinLoss(wins = doublesWins, losses = doublesLosses),
            )
        }
    }
}
