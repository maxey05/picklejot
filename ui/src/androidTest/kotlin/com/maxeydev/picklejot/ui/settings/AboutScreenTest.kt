package com.maxeydev.picklejot.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun it_shows_the_app_version() {
        compose.setContent { AboutScreen(versionName = "1.2.3", onBack = {}) }

        compose.onNodeWithTag(AboutTestTags.VERSION).assertTextEquals("Version 1.2.3")
    }

    @Test
    fun it_explains_that_data_stays_on_the_device() {
        compose.setContent { AboutScreen(versionName = "1.2.3", onBack = {}) }

        compose.onNodeWithTag(AboutTestTags.PRIVACY).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun it_lists_the_open_source_software() {
        compose.setContent { AboutScreen(versionName = "1.2.3", onBack = {}) }

        compose.onNodeWithTag(AboutTestTags.LICENCES).performScrollTo().assertIsDisplayed()
    }
}
