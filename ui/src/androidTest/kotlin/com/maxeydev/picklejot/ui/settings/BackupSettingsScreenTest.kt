package com.maxeydev.picklejot.ui.settings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklejot.domain.backup.ExportPromptReason
import com.maxeydev.picklejot.domain.backup.ImportSummary
import com.maxeydev.picklejot.domain.datetime.AppInstant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupSettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var exportRequests = 0
    private var exportConfirmations = 0

    private val actions =
        BackupActions(
            onBack = {},
            onExportRequested = { exportRequests++ },
            onExportConfirmed = { exportConfirmations++ },
            onExportDialogDismissed = {},
            onShareLaunched = {},
            onShareFailed = {},
            onImportPicked = {},
            onImportSummaryDismissed = {},
            onMessageDismissed = {},
        )

    private fun show(state: BackupUiState) {
        compose.setContent { BackupSettingsScreen(state = state, actions = actions) }
    }

    @Test
    fun the_screen_says_automatic_backup_may_not_be_running() {
        show(BackupUiState())

        compose.onNodeWithTag(BackupTestTags.HONESTY).assertIsDisplayed()
    }

    @Test
    fun it_says_when_nothing_has_ever_been_exported() {
        show(BackupUiState(lastExportAt = null))

        compose.onNodeWithTag(BackupTestTags.LAST_EXPORT, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun it_shows_the_last_export_date_once_there_is_one() {
        show(BackupUiState(lastExportAt = AppInstant.parse("2026-09-30T04:00:00Z")))

        compose.onNodeWithTag(BackupTestTags.LAST_EXPORT, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun tapping_export_asks_for_confirmation_through_the_view_model() {
        show(BackupUiState())

        compose.onNodeWithTag(BackupTestTags.EXPORT).performClick()

        assertEquals(1, exportRequests)
        assertEquals(0, exportConfirmations)
    }

    @Test
    fun the_export_dialog_states_plainly_that_photos_are_not_included() {
        show(BackupUiState(isExportDialogVisible = true))

        compose.onNodeWithTag(BackupTestTags.EXPORT_DIALOG_PHOTOS).assertIsDisplayed()
        compose.onNodeWithTag(BackupTestTags.EXPORT_DIALOG_CONFIRM).performClick()

        assertEquals(1, exportConfirmations)
    }

    @Test
    fun the_screen_says_photos_are_not_included_before_any_export() {
        show(BackupUiState())

        compose.onNodeWithText("Photos aren't included").assertIsDisplayed()
    }

    @Test
    fun a_capped_import_reports_how_many_were_held_back() {
        val summary = ImportSummary(added = 50, alreadyPresent = 0, heldBack = 10, photosNotRestored = 0)
        show(BackupUiState(importSummary = summary))

        compose.onNodeWithTag(BackupTestTags.IMPORT_HELD_BACK).assertIsDisplayed()
    }

    @Test
    fun an_import_that_held_nothing_back_shows_no_held_back_line() {
        val summary = ImportSummary(added = 5, alreadyPresent = 1, heldBack = 0, photosNotRestored = 0)
        show(BackupUiState(importSummary = summary))

        compose.onNodeWithTag(BackupTestTags.IMPORT_SUMMARY).assertIsDisplayed()
        compose.onAllNodesWithTag(BackupTestTags.IMPORT_HELD_BACK).assertCountEquals(0)
    }

    @Test
    fun both_buttons_are_disabled_while_an_export_is_running() {
        show(BackupUiState(isExporting = true))

        compose.onNodeWithTag(BackupTestTags.EXPORT).assertIsNotEnabled()
        compose.onNodeWithTag(BackupTestTags.IMPORT).assertIsNotEnabled()
    }

    @Test
    fun a_failed_import_shows_its_message() {
        show(BackupUiState(message = BackupMessage.CORRUPT))

        compose.onNodeWithTag(BackupTestTags.MESSAGE).assertIsDisplayed()
    }

    @Test
    fun the_reminder_banner_shows_only_when_a_prompt_is_pending() {
        compose.setContent {
            ExportPromptBanner(reason = ExportPromptReason.PERIODIC, onExport = {}, onDismiss = {})
        }

        compose.onNodeWithTag(BackupTestTags.PROMPT_BANNER).assertIsDisplayed()
        compose.onNodeWithTag(BackupTestTags.PROMPT_DISMISS).assertIsDisplayed()
    }
}
