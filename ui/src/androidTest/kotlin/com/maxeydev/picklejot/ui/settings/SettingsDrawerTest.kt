package com.maxeydev.picklejot.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsDrawerTest {
    @get:Rule
    val compose = createComposeRule()

    private val darkChoices = mutableListOf<Boolean>()
    private var saves = 0
    private var eraseConfirmations = 0
    private var aboutOpens = 0
    private var supportOpens = 0
    private var backupOpens = 0
    private var privacyOpens = 0
    private var proOpens = 0
    private var cacheClears = 0
    private var closes = 0

    private val actions =
        SettingsActions(
            onClose = { closes++ },
            onSeePro = { proOpens++ },
            onOpenBackup = { backupOpens++ },
            onOpenPrivacy = { privacyOpens++ },
            onOpenAbout = { aboutOpens++ },
            onRateUs = {},
            onOpenSupport = { supportOpens++ },
            onClearCache = { cacheClears++ },
            onEnableReminder = {},
            onDisableReminder = {},
            onReminderTimeChanged = {},
            onNameChanged = {},
            onSaveName = { saves++ },
            onNameEditCancelled = {},
            onDarkThemeChanged = { darkChoices += it },
            onEraseConfirmed = { eraseConfirmations++ },
            onEraseFailureDismissed = {},
        )

    private fun show(state: SettingsUiState) {
        compose.setContent { SettingsDrawer(state = state, versionName = "1.0.0", actions = actions) }
    }

    private fun showHost(open: Boolean) {
        compose.setContent {
            SettingsDrawerHost(
                open = open,
                onDismiss = { closes++ },
                drawer = { modifier ->
                    SettingsDrawer(
                        state = SettingsUiState(),
                        versionName = "1.0.0",
                        actions = actions,
                        modifier = modifier,
                    )
                },
            ) {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }

    @Test
    fun the_drawer_is_absent_until_it_is_opened() {
        showHost(open = false)

        compose.onAllNodesWithTag(SettingsTestTags.SCREEN).assertCountEquals(0)
    }

    @Test
    fun tapping_outside_the_open_drawer_closes_it() {
        showHost(open = true)

        compose.onNodeWithTag(SettingsTestTags.SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(SettingsTestTags.SCRIM).performTouchInput { click(percentOffset(0.02f, 0.5f)) }

        assertEquals(1, closes)
    }

    @Test
    fun the_close_button_closes_the_drawer() {
        show(SettingsUiState())

        compose.onNodeWithTag(SettingsTestTags.CLOSE).performClick()

        assertEquals(1, closes)
    }

    @Test
    fun a_free_user_sees_their_match_count_and_can_open_pro_details() {
        show(SettingsUiState(savedMatches = 32))

        compose.onNodeWithTag(SettingsTestTags.PRO_STATUS, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag(SettingsTestTags.SEE_PRO).performClick()

        assertEquals(1, proOpens)
    }

    @Test
    fun a_pro_user_is_not_offered_pro_details() {
        show(SettingsUiState(hasPro = true))

        compose.onAllNodesWithTag(SettingsTestTags.SEE_PRO).assertCountEquals(0)
    }

    @Test
    fun the_dark_mode_switch_reflects_an_explicit_dark_choice() {
        show(SettingsUiState(darkTheme = true))

        compose.onNodeWithTag(SettingsTestTags.DARK_THEME_TOGGLE).performScrollTo().assertIsOn()
    }

    @Test
    fun the_dark_mode_switch_reflects_an_explicit_light_choice() {
        show(SettingsUiState(darkTheme = false))

        compose.onNodeWithTag(SettingsTestTags.DARK_THEME_TOGGLE).performScrollTo().assertIsOff()
    }

    @Test
    fun flipping_the_switch_reports_the_new_choice() {
        show(SettingsUiState(darkTheme = false))

        compose.onNodeWithTag(SettingsTestTags.DARK_THEME_TOGGLE).performScrollTo().performClick()

        assertEquals(listOf(true), darkChoices)
    }

    @Test
    fun saving_the_name_is_disabled_when_there_is_nothing_new_to_save() {
        show(SettingsUiState(displayName = "Matthew", nameDraft = "Matthew"))
        compose.onNodeWithTag(SettingsTestTags.NAME_OPEN).performScrollTo().performClick()

        compose.onNodeWithTag(SettingsTestTags.NAME_SAVE).assertIsNotEnabled()
    }

    @Test
    fun an_edited_name_can_be_saved() {
        show(SettingsUiState(displayName = "Matthew", nameDraft = "Matt"))
        compose.onNodeWithTag(SettingsTestTags.NAME_OPEN).performScrollTo().performClick()

        compose.onNodeWithTag(SettingsTestTags.NAME_SAVE).assertIsEnabled().performClick()

        assertEquals(1, saves)
        compose.onAllNodesWithTag(SettingsTestTags.NAME_FIELD).assertCountEquals(0)
    }

    @Test
    fun a_blank_name_cannot_be_saved() {
        show(SettingsUiState(displayName = "Matthew", nameDraft = ""))
        compose.onNodeWithTag(SettingsTestTags.NAME_OPEN).performScrollTo().performClick()

        compose.onNodeWithTag(SettingsTestTags.NAME_SAVE).assertIsNotEnabled()
    }

    @Test
    fun the_reminder_time_is_only_offered_while_the_reminder_is_on() {
        show(SettingsUiState(reminderEnabled = false))
        compose.onAllNodesWithTag(SettingsTestTags.REMINDER_TIME).assertCountEquals(0)
    }

    @Test
    fun the_reminder_time_appears_once_the_reminder_is_on() {
        show(SettingsUiState(reminderEnabled = true))

        compose.onNodeWithTag(SettingsTestTags.REMINDER_TIME).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun export_details_open_from_the_more_info_link() {
        show(SettingsUiState())

        compose.onNodeWithTag(SettingsTestTags.BACKUP_MORE_INFO).performScrollTo().performClick()

        assertEquals(1, backupOpens)
    }

    @Test
    fun clearing_the_cache_is_reported_and_its_size_is_shown() {
        show(SettingsUiState(cacheBytes = 12_000_000L))

        compose.onNodeWithTag(SettingsTestTags.CACHE_SIZE, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(SettingsTestTags.CLEAR_CACHE).performClick()

        assertEquals(1, cacheClears)
    }

    @Test
    fun the_cache_cannot_be_cleared_twice_at_once() {
        show(SettingsUiState(cacheBytes = 1_000L, isClearingCache = true))

        compose.onNodeWithTag(SettingsTestTags.CLEAR_CACHE).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun the_privacy_policy_opens_inside_the_app() {
        show(SettingsUiState())

        compose.onNodeWithTag(SettingsTestTags.PRIVACY_OPEN).performScrollTo().performClick()

        assertEquals(1, privacyOpens)
    }

    @Test
    fun about_opens_from_settings() {
        show(SettingsUiState())

        compose.onNodeWithTag(SettingsTestTags.ABOUT_OPEN).performScrollTo().performClick()

        assertEquals(1, aboutOpens)
    }

    @Test
    fun support_opens_from_settings() {
        show(SettingsUiState())

        compose.onNodeWithTag(SettingsTestTags.SUPPORT_OPEN).performScrollTo().performClick()

        assertEquals(1, supportOpens)
    }

    @Test
    fun the_first_erase_prompt_does_not_erase_and_leads_to_a_final_confirmation() {
        show(SettingsUiState(savedMatches = 3))

        compose.onNodeWithTag(SettingsTestTags.ERASE_OPEN).performScrollTo().performClick()
        compose.onNodeWithTag(SettingsTestTags.ERASE_CONFIRM).assertIsEnabled().performClick()

        assertEquals(0, eraseConfirmations)
        compose.onNodeWithTag(SettingsTestTags.ERASE_FINAL_DIALOG).assertIsDisplayed()
        compose.onNodeWithText("Your 3 matches", substring = true).assertIsDisplayed()
    }

    @Test
    fun confirming_the_final_prompt_reports_the_erase() {
        show(SettingsUiState(savedMatches = 3))
        compose.onNodeWithTag(SettingsTestTags.ERASE_OPEN).performScrollTo().performClick()
        compose.onNodeWithTag(SettingsTestTags.ERASE_CONFIRM).performClick()

        compose.onNodeWithTag(SettingsTestTags.ERASE_FINAL_CONFIRM).performClick()

        assertEquals(1, eraseConfirmations)
    }

    @Test
    fun cancelling_the_final_prompt_keeps_the_data() {
        show(SettingsUiState(savedMatches = 3))
        compose.onNodeWithTag(SettingsTestTags.ERASE_OPEN).performScrollTo().performClick()
        compose.onNodeWithTag(SettingsTestTags.ERASE_CONFIRM).performClick()

        compose.onNodeWithTag(SettingsTestTags.ERASE_FINAL_CANCEL).performClick()

        assertEquals(0, eraseConfirmations)
        compose.onAllNodesWithTag(SettingsTestTags.ERASE_FINAL_DIALOG).assertCountEquals(0)
    }

    @Test
    fun the_final_prompt_uses_the_singular_for_one_match() {
        show(SettingsUiState(savedMatches = 1))
        compose.onNodeWithTag(SettingsTestTags.ERASE_OPEN).performScrollTo().performClick()
        compose.onNodeWithTag(SettingsTestTags.ERASE_CONFIRM).performClick()

        compose.onNodeWithText("Your 1 match,", substring = true).assertIsDisplayed()
    }

    @Test
    fun the_erase_dialog_offers_an_export_first() {
        show(SettingsUiState())
        compose.onNodeWithTag(SettingsTestTags.ERASE_OPEN).performScrollTo().performClick()

        compose.onNodeWithTag(SettingsTestTags.ERASE_EXPORT_FIRST).performClick()

        assertEquals(1, backupOpens)
    }

    @Test
    fun a_failed_erase_is_explained_inside_the_final_dialog() {
        show(SettingsUiState(eraseFailed = true))
        compose.onNodeWithTag(SettingsTestTags.ERASE_OPEN).performScrollTo().performClick()
        compose.onNodeWithTag(SettingsTestTags.ERASE_CONFIRM).performClick()

        compose.onNodeWithTag(SettingsTestTags.ERASE_ERROR).assertIsDisplayed()
    }
}
