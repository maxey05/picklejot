package com.maxeydev.picklejot.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val FIELD = "field"

@RunWith(AndroidJUnit4::class)
class AutocompleteFieldTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(
        suggestions: List<SuggestionUiState>,
        onSelected: (SuggestionUiState) -> Unit = {},
    ) {
        compose.setContent {
            MaterialTheme {
                AutocompleteField(
                    value = "Da",
                    onValueChange = {},
                    suggestions = suggestions,
                    onSuggestionSelected = onSelected,
                    onFocusChanged = {},
                    label = "Opponent",
                    fieldTestTag = FIELD,
                )
            }
        }
    }

    @Test
    fun `with_no_suggestions_the_field_renders_no_empty_dropdown`() {
        show(emptyList())

        compose.onNodeWithTag(FIELD).assertExists()
        compose.onAllNodesWithTag(AutocompleteTestTags.suggestionList(FIELD)).assertCountEquals(0)
    }

    @Test
    fun `tapping_a_suggestion_hands_back_that_suggestion`() {
        val dave = SuggestionUiState(key = "id-1", label = "Dave R.")
        val picked = mutableListOf<SuggestionUiState>()
        show(listOf(dave, SuggestionUiState(key = "id-2", label = "Dana")), onSelected = { picked += it })

        compose.onNodeWithTag(AutocompleteTestTags.suggestionList(FIELD)).assertExists()
        compose.onNodeWithTag(AutocompleteTestTags.suggestion(FIELD, 0)).assertHeightIsAtLeast(48.dp).performClick()

        assertEquals(listOf(dave), picked)
    }
}
