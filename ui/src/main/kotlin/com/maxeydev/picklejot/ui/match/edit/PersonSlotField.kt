package com.maxeydev.picklejot.ui.match.edit

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.common.AutocompleteField
import com.maxeydev.picklejot.ui.common.SuggestionUiState

@Composable
fun PersonSlotField(
    slot: PersonSlotUiState,
    format: MatchFormat,
    suggestions: List<SuggestionUiState>,
    onNameChanged: (String) -> Unit,
    onSuggestionSelected: (SuggestionUiState) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val duplicateOf = slot.duplicateOf
    AutocompleteField(
        value = slot.name,
        onValueChange = onNameChanged,
        suggestions = suggestions,
        onSuggestionSelected = onSuggestionSelected,
        onFocusChanged = onFocusChanged,
        label = personSlotLabel(slot.slot, format),
        fieldTestTag = MatchEditTestTags.personSlot(slot.slot),
        isError = duplicateOf != null,
        supportingText =
            if (duplicateOf != null) {
                { Text(stringResource(R.string.duplicate_person, personSlotLabel(duplicateOf, format))) }
            } else {
                null
            },
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
        modifier = modifier,
    )
}

@Composable
fun personSlotLabel(
    slot: PersonSlot,
    format: MatchFormat,
): String =
    when (slot) {
        PersonSlot.OPPONENT_1 ->
            if (format == MatchFormat.SINGLES) {
                stringResource(R.string.slot_opponent)
            } else {
                stringResource(R.string.slot_opponent_1)
            }
        PersonSlot.OPPONENT_2 -> stringResource(R.string.slot_opponent_2)
        PersonSlot.PARTNER -> stringResource(R.string.slot_partner)
    }
