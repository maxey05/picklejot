package com.maxeydev.picklejot.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maxeydev.picklejot.ui.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var continues = 0
    private val typed = mutableListOf<String>()

    private fun show(
        state: OnboardingUiState,
        initialPage: Int = 0,
    ) {
        compose.setContent {
            OnboardingScreen(
                state = state,
                onNameChanged = { typed += it },
                onContinue = { continues++ },
                initialPage = initialPage,
            )
        }
    }

    private val lastPage = IntroPage.entries.lastIndex

    @Test
    fun the_intro_opens_on_the_logging_page() {
        show(OnboardingUiState())

        compose.onNodeWithTag(OnboardingTestTags.title(IntroPage.LOGGING)).assertIsDisplayed()
        compose.onNodeWithTag(OnboardingTestTags.NEXT).assertIsDisplayed()
        compose.onNodeWithTag(OnboardingTestTags.SKIP).assertIsDisplayed()
    }

    @Test
    fun next_moves_to_the_following_page() {
        show(OnboardingUiState())

        compose.onNodeWithTag(OnboardingTestTags.NEXT).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(OnboardingTestTags.title(IntroPage.STATS)).assertIsDisplayed()
    }

    @Test
    fun skip_jumps_to_the_last_page() {
        show(OnboardingUiState())

        compose.onNodeWithTag(OnboardingTestTags.SKIP).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(OnboardingTestTags.title(IntroPage.PRIVACY)).assertIsDisplayed()
        compose.onNodeWithTag(OnboardingTestTags.CONTINUE).assertIsDisplayed()
    }

    @Test
    fun the_last_page_says_automatic_backup_may_not_be_running() {
        show(OnboardingUiState(), initialPage = lastPage)

        compose.onNodeWithTag(OnboardingTestTags.BACKUP_NOTE).assertIsDisplayed()
    }

    @Test
    fun get_started_without_a_name_still_reports_the_tap_so_the_name_can_be_flagged() {
        show(OnboardingUiState(name = ""), initialPage = lastPage)

        compose.onNodeWithTag(OnboardingTestTags.CONTINUE).assertIsEnabled().performClick()

        assertEquals(1, continues)
    }

    @Test
    fun a_blocked_attempt_shows_that_a_name_is_required() {
        show(OnboardingUiState(name = "", nameRequiredAttempts = 1), initialPage = lastPage)

        compose.onNodeWithText(context.getString(R.string.name_field_required)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun a_name_also_continues() {
        show(OnboardingUiState(name = "Matthew"), initialPage = lastPage)

        compose.onNodeWithTag(OnboardingTestTags.CONTINUE).assertIsEnabled().performClick()

        assertEquals(1, continues)
    }

    @Test
    fun get_started_is_disabled_while_saving() {
        show(OnboardingUiState(name = "Matthew", isSaving = true), initialPage = lastPage)

        compose.onNodeWithTag(OnboardingTestTags.CONTINUE).assertIsNotEnabled()
    }

    @Test
    fun typing_reports_the_text_to_the_view_model() {
        show(OnboardingUiState(), initialPage = lastPage)

        compose.onNodeWithTag(OnboardingTestTags.NAME_FIELD).performScrollTo().performTextInput("Sam")

        assertEquals(listOf("Sam"), typed)
    }

    @Test
    fun a_failed_save_is_reported() {
        show(OnboardingUiState(name = "Matthew", saveFailed = true), initialPage = lastPage)

        compose.onNodeWithTag(OnboardingTestTags.SAVE_ERROR).performScrollTo().assertIsDisplayed()
    }
}
