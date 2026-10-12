package com.maxeydev.picklejot.ui.settings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SupportScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var proOpens = 0
    private var rateOpens = 0
    private var shares = 0
    private var backs = 0

    private fun show(hasPro: Boolean) {
        compose.setContent {
            SupportScreen(
                hasPro = hasPro,
                onBack = { backs++ },
                onSeePro = { proOpens++ },
                onRateUs = { rateOpens++ },
                onShareApp = { shares++ },
            )
        }
    }

    @Test
    fun it_thanks_the_player_in_a_hero_card() {
        show(hasPro = false)

        compose.onNodeWithTag(SupportTestTags.HERO).assertIsDisplayed()
    }

    @Test
    fun it_offers_pro_to_a_free_player() {
        show(hasPro = false)

        compose.onNodeWithTag(SupportTestTags.PRO).performScrollTo().performClick()

        assertEquals(1, proOpens)
        compose.onAllNodesWithTag(SupportTestTags.PRO_THANKS).assertCountEquals(0)
    }

    @Test
    fun it_thanks_a_pro_player_instead_of_offering_pro_again() {
        show(hasPro = true)

        compose.onNodeWithTag(SupportTestTags.PRO_THANKS).performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithTag(SupportTestTags.PRO).assertCountEquals(0)
    }

    @Test
    fun rating_the_app_is_offered() {
        show(hasPro = false)

        compose.onNodeWithTag(SupportTestTags.RATE).performScrollTo().performClick()

        assertEquals(1, rateOpens)
    }

    @Test
    fun the_donate_row_is_shown_for_free_and_pro_players() {
        show(hasPro = false)

        compose.onNodeWithTag(SupportTestTags.DONATE).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sharing_the_app_is_offered() {
        show(hasPro = false)

        compose.onNodeWithTag(SupportTestTags.SHARE).performScrollTo().performClick()

        assertEquals(1, shares)
    }
}
