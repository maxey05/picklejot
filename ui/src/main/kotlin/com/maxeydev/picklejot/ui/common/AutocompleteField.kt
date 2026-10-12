package com.maxeydev.picklejot.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.ui.R

private val SUGGESTION_MIN_HEIGHT = 48.dp

@Composable
fun AutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<SuggestionUiState>,
    onSuggestionSelected: (SuggestionUiState) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    label: String,
    fieldTestTag: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    placeholder: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder =
                if (placeholder != null) {
                    { Text(placeholder) }
                } else {
                    null
                },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            isError = isError,
            supportingText = supportingText,
            keyboardOptions = keyboardOptions,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState -> onFocusChanged(focusState.isFocused) }
                    .testTag(fieldTestTag),
        )
        if (suggestions.isNotEmpty()) {
            SuggestionList(
                suggestions = suggestions,
                onSuggestionSelected = onSuggestionSelected,
                fieldTestTag = fieldTestTag,
            )
        }
    }
}

@Composable
private fun SuggestionList(
    suggestions: List<SuggestionUiState>,
    onSuggestionSelected: (SuggestionUiState) -> Unit,
    fieldTestTag: String,
) {
    val useLabel = stringResource(R.string.suggestion_use)
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.small,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .testTag(AutocompleteTestTags.suggestionList(fieldTestTag)),
    ) {
        Column {
            suggestions.forEachIndexed { index, suggestion ->
                if (index > 0) {
                    HorizontalDivider()
                }
                Text(
                    text = suggestion.label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = SUGGESTION_MIN_HEIGHT)
                            .clickable(onClickLabel = useLabel) { onSuggestionSelected(suggestion) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag(AutocompleteTestTags.suggestion(fieldTestTag, index)),
                )
            }
        }
    }
}
