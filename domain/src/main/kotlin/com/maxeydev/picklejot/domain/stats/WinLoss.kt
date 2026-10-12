package com.maxeydev.picklejot.domain.stats

import kotlin.math.roundToInt

private const val WHOLE = 100
private const val JUST_BELOW_WHOLE = 99
private const val JUST_ABOVE_NONE = 1

data class WinLoss(
    val wins: Int,
    val losses: Int,
) {
    init {
        require(wins >= 0) { "wins cannot be negative: $wins" }
        require(losses >= 0) { "losses cannot be negative: $losses" }
    }

    val total: Int
        get() = wins + losses

    val hasMatches: Boolean
        get() = total > 0

    val winPercent: Int?
        get() {
            if (total == 0) {
                return null
            }
            val rounded = (wins * WHOLE.toDouble() / total).roundToInt()
            return when {
                losses > 0 && rounded >= WHOLE -> JUST_BELOW_WHOLE
                wins > 0 && rounded <= 0 -> JUST_ABOVE_NONE
                else -> rounded
            }
        }

    companion object {
        val NONE: WinLoss = WinLoss(wins = 0, losses = 0)
    }
}
