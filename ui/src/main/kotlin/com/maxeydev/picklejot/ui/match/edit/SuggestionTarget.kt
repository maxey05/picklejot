package com.maxeydev.picklejot.ui.match.edit

import com.maxeydev.picklejot.domain.match.FreeTextField

enum class SuggestionTarget(
    val personSlot: PersonSlot?,
    val freeTextField: FreeTextField?,
) {
    OPPONENT_1(PersonSlot.OPPONENT_1, null),
    OPPONENT_2(PersonSlot.OPPONENT_2, null),
    PARTNER(PersonSlot.PARTNER, null),
    LOCATION(null, FreeTextField.LOCATION),
    PADDLE(null, FreeTextField.PADDLE),
    ;

    companion object {
        fun forSlot(slot: PersonSlot): SuggestionTarget = entries.first { it.personSlot == slot }
    }
}
