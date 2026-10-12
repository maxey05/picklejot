package com.maxeydev.picklejot.ui.match.edit

data class PersonSlotUiState(
    val slot: PersonSlot,
    val name: String,
    val duplicateOf: PersonSlot? = null,
)
