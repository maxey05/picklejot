package com.maxeydev.picklejot.ui.match.edit

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maxeydev.picklejot.ui.common.PermissionTestTags
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoPickerSectionTest {
    @get:Rule
    val compose = createComposeRule()

    private val moves = mutableListOf<Pair<String, Int>>()
    private val removed = mutableListOf<String>()
    private var errorDismissed = false

    private val actions =
        PhotoPickerActions(
            newCaptureUri = { "content://picklejot.test/camera/x.jpg" },
            onPhotosPicked = {},
            onPhotoCaptured = {},
            onPhotoMoved = { key, offset -> moves += key to offset },
            onPhotoRemoved = { removed += it },
            onPhotoErrorDismissed = { errorDismissed = true },
        )

    private fun show(
        photos: List<PhotoUiState>,
        hasPhotoError: Boolean = false,
    ) {
        compose.setContent {
            MaterialTheme {
                PhotoPickerSection(photos = photos, hasPhotoError = hasPhotoError, actions = actions)
            }
        }
    }

    @Test
    fun the_first_photo_is_the_cover_and_cannot_move_earlier() {
        show(listOf(PhotoUiState("a", "/files/a.jpg"), PhotoUiState("b", "/files/b.jpg")))

        compose.onNodeWithContentDescription("Photo 1 of 2, used as the cover").assertIsDisplayed()
        compose.onNodeWithTag(MatchEditTestTags.photoMoveEarlier("a")).assertIsNotEnabled()
        compose.onNodeWithTag(MatchEditTestTags.photoMoveLater("b")).assertIsNotEnabled()
        compose.onNodeWithTag(MatchEditTestTags.photoMoveEarlier("b")).assertIsEnabled().performClick()
        compose.onNodeWithTag(MatchEditTestTags.photoRemove("a")).performClick()

        assertEquals(listOf("b" to -1), moves)
        assertEquals(listOf("a"), removed)
    }

    @Test
    fun a_photo_still_processing_says_so() {
        show(listOf(PhotoUiState("a", null)))

        compose.onNodeWithContentDescription("Photo 1 of 1, still processing").assertIsDisplayed()
    }

    @Test
    fun an_unreadable_photo_message_can_be_dismissed() {
        show(emptyList(), hasPhotoError = true)

        compose.onNodeWithTag(MatchEditTestTags.PHOTO_ERROR).assertIsDisplayed()
        compose.onNodeWithTag(MatchEditTestTags.PHOTO_ERROR_DISMISS).performClick()

        assertEquals(true, errorDismissed)
    }

    @Test
    fun taking_a_photo_explains_why_before_any_system_dialog_and_declining_keeps_the_picker_working() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue(context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY))
        assumeTrue(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED,
        )
        show(emptyList())

        compose.onNodeWithTag(MatchEditTestTags.TAKE_PHOTO).performClick()

        compose.onNodeWithTag(PermissionTestTags.RATIONALE).assertIsDisplayed()
        compose.onNodeWithTag(PermissionTestTags.RATIONALE_CONTINUE).assertIsDisplayed()
        compose.onNodeWithText("Not now").performClick()
        compose.onAllNodesWithTag(PermissionTestTags.RATIONALE).assertCountEquals(0)
        compose.onNodeWithTag(MatchEditTestTags.ADD_PHOTOS).assertIsEnabled()
    }

    @Test
    fun adding_photos_from_the_gallery_needs_no_permission_prompt() {
        show(emptyList())

        compose.onNodeWithTag(MatchEditTestTags.ADD_PHOTOS).assertIsDisplayed().assertIsEnabled()
        compose.onAllNodesWithTag(PermissionTestTags.RATIONALE).assertCountEquals(0)
    }
}
