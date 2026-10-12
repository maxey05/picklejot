@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.ui.common.MascotSheetTestTags
import com.maxeydev.picklejot.ui.fakes.FakeDependencies
import com.maxeydev.picklejot.ui.match.edit.MatchEditTestTags
import com.maxeydev.picklejot.ui.match.edit.PersonSlot
import com.maxeydev.picklejot.ui.match.list.MatchListTestTags
import com.maxeydev.picklejot.ui.navigation.PicklejotNavHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.uuid.ExperimentalUuidApi

private const val WAIT_MILLIS = 5_000L
private const val LOG_ANOTHER = "Log another"

@RunWith(AndroidJUnit4::class)
class LogAnotherFlowTest {
    @get:Rule
    val compose = createComposeRule()

    private fun waitForTag(tag: String) {
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun editableText(value: String): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(value))

    private fun logFirstMatch(dependencies: FakeDependencies) {
        compose.setContent { PicklejotNavHost(dependencies = dependencies) }
        waitForTag(MatchListTestTags.EMPTY_LOG_MATCH)
        compose.onNodeWithTag(MatchListTestTags.EMPTY_LOG_MATCH).performClick()
        waitForTag(MatchEditTestTags.RESULT_WIN)
        compose.onNodeWithTag(MatchEditTestTags.RESULT_WIN).performClick()
        compose.onNodeWithTag(MatchEditTestTags.personSlot(PersonSlot.OPPONENT_1)).performTextInput("Ana")
        compose.onNodeWithTag(MatchEditTestTags.personSlot(PersonSlot.OPPONENT_2)).performTextInput("Ben")
        compose.onNodeWithTag(MatchEditTestTags.personSlot(PersonSlot.PARTNER)).performTextInput("Cy")
        compose.onNodeWithTag(MatchEditTestTags.ADD_GAME).performScrollTo().performClick()
        compose.onNode(hasText("Me") and hasSetTextAction()).performTextInput("11")
        compose.onNode(hasText("Them") and hasSetTextAction()).performTextInput("7")
        compose.onNodeWithTag(MatchEditTestTags.LOCATION).performTextInput("Ayala Triangle")
        compose.onNodeWithTag(MatchEditTestTags.NOTES).performTextInput("Windy on court 3")
        compose.onNodeWithTag(MatchEditTestTags.SAVE).performClick()
        compose.waitUntil(WAIT_MILLIS) { dependencies.matchRepository.saved.isNotEmpty() }
        waitForTag(MatchListTestTags.FIRST_MATCH_SHEET)
    }

    @Test
    fun `log_another_opens_a_new_match_with_the_session_fields_and_blank_per_game_fields`() {
        val dependencies = FakeDependencies()
        logFirstMatch(dependencies)
        val first = dependencies.matchRepository.saved.single()

        compose.onNodeWithText(LOG_ANOTHER).performClick()
        waitForTag(MatchEditTestTags.RESULT_WIN)

        compose.onNodeWithTag(MatchEditTestTags.RESULT_WIN).assertIsNotSelected()
        compose.onNodeWithTag(MatchEditTestTags.RESULT_LOSS).assertIsNotSelected()
        assertTrue(compose.onAllNodesWithText("Game 1").fetchSemanticsNodes().isEmpty())
        compose.onNodeWithTag(MatchEditTestTags.NOTES).assert(editableText(""))
        compose.onNodeWithTag(MatchEditTestTags.LOCATION).assert(editableText("Ayala Triangle"))
        compose.onNodeWithTag(MatchEditTestTags.personSlot(PersonSlot.OPPONENT_1)).assert(editableText("Ana"))
        compose.onNodeWithTag(MatchEditTestTags.personSlot(PersonSlot.OPPONENT_2)).assert(editableText("Ben"))
        compose.onNodeWithTag(MatchEditTestTags.personSlot(PersonSlot.PARTNER)).assert(editableText("Cy"))

        compose.onNodeWithTag(MatchEditTestTags.RESULT_LOSS).performClick()
        compose.onNodeWithTag(MatchEditTestTags.SAVE).performClick()
        compose.waitUntil(WAIT_MILLIS) { dependencies.matchRepository.saved.size == 2 }

        val second = dependencies.matchRepository.saved.last()
        assertTrue(second.id != first.id)
        assertEquals(MatchResult.LOSS, second.result)
        assertEquals(first.date, second.date)
        assertEquals("Ayala Triangle", second.location)
        assertEquals(first.opponents.map { it.id }, second.opponents.map { it.id })
        assertEquals(first.partner?.id, second.partner?.id)
        assertTrue(second.games.isEmpty())
        assertTrue(second.photos.isEmpty())
        assertNull(second.notes)
        assertNull(second.paddle)
        assertEquals(3, dependencies.personRepository.current.size)
    }

    @Test
    fun `dismissing_the_offer_returns_to_the_list_with_the_saved_match_present`() {
        val dependencies = FakeDependencies()
        logFirstMatch(dependencies)
        val saved = dependencies.matchRepository.saved.single()

        compose.onNodeWithTag(MascotSheetTestTags.DISMISS).performClick()
        compose.waitUntil(WAIT_MILLIS) {
            compose.onAllNodesWithTag(MatchListTestTags.FIRST_MATCH_SHEET).fetchSemanticsNodes().isEmpty()
        }

        waitForTag(MatchListTestTags.row(saved.id.toString()))
        assertEquals(1, dependencies.matchRepository.current.size)
    }
}
