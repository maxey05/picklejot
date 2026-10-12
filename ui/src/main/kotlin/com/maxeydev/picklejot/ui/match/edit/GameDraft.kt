package com.maxeydev.picklejot.ui.match.edit

import kotlinx.serialization.Serializable

private const val MAX_SCORE_DIGITS = 3

@Serializable
data class GameDraft(
    val myScore: String = "",
    val opponentScore: String = "",
) {
    val isBlank: Boolean
        get() = myScore.isEmpty() && opponentScore.isEmpty()

    val isComplete: Boolean
        get() = myScore.isNotEmpty() && opponentScore.isNotEmpty()

    val isIncomplete: Boolean
        get() = !isBlank && !isComplete

    companion object {
        fun of(
            myScore: String,
            opponentScore: String,
        ): GameDraft = GameDraft(myScore = digitsOnly(myScore), opponentScore = digitsOnly(opponentScore))

        private fun digitsOnly(value: String): String = value.filter { it in '0'..'9' }.take(MAX_SCORE_DIGITS)
    }
}
