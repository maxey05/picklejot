package com.maxeydev.picklejot.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LegalDocumentScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var backs = 0
    private val opened = mutableListOf<LegalDocument>()

    private fun show(document: LegalDocument) {
        compose.setContent {
            LegalDocumentScreen(
                document = document,
                onBack = { backs++ },
                onOpenDocument = { opened += it },
            )
        }
    }

    @Test
    fun the_privacy_policy_shows_its_title_and_the_online_link() {
        show(LegalDocument.PRIVACY)

        compose.onNodeWithTag(LegalDocument.PRIVACY.screenTag).assertIsDisplayed()
        compose.onNodeWithText("Privacy policy").assertIsDisplayed()
        compose.onNodeWithTag(LegalTestTags.ONLINE_LINK).assertIsDisplayed()
    }

    @Test
    fun the_privacy_policy_text_is_bundled_with_the_app() {
        show(LegalDocument.PRIVACY)

        compose.onNodeWithTag(LegalTestTags.CONTENT).performScrollToNode(hasText("At a glance"))
        compose.onNodeWithText("At a glance").assertIsDisplayed()
        compose.onNodeWithTag(LegalTestTags.LOAD_FAILED).assertDoesNotExist()
    }

    @Test
    fun the_terms_of_service_text_is_bundled_with_the_app() {
        show(LegalDocument.TERMS)

        compose.onNodeWithTag(LegalDocument.TERMS.screenTag).assertIsDisplayed()
        compose.onNodeWithTag(LegalTestTags.CONTENT).performScrollToNode(hasText("1. Defined terms"))
        compose.onNodeWithText("1. Defined terms").assertIsDisplayed()
    }

    @Test
    fun the_back_button_leaves_the_screen() {
        show(LegalDocument.PRIVACY)

        compose.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs)
    }
}
