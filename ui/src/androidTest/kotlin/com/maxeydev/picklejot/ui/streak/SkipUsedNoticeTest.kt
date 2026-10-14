package com.maxeydev.picklejot.ui.streak

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.streak.MissedSkipOpportunity
import com.maxeydev.picklejot.domain.streak.WeekKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SkipUsedNoticeTest {
    @get:Rule
    val compose = createComposeRule()

    private val week = WeekKey.of(AppDate.parse("2026-09-14"))

    @Test
    fun the_notice_states_plainly_that_a_skip_was_used_and_how_many_are_left() {
        compose.setContent {
            MaterialTheme {
                SkipUsedNotice(skippedWeek = week, skipsHeld = 1, onDismiss = {})
            }
        }

        val notice = compose.onNodeWithTag(StreakNoticeTestTags.SKIP_USED)
        notice.assertIsDisplayed()
        notice.assert(hasText("A streak skip covered the week of", substring = true))
        notice.assert(hasText("1 skip left"))
    }

    @Test
    fun the_notice_reports_zero_skips_left_rather_than_hiding_the_balance() {
        compose.setContent {
            MaterialTheme {
                SkipUsedNotice(skippedWeek = week, skipsHeld = 0, onDismiss = {})
            }
        }

        compose.onNodeWithTag(StreakNoticeTestTags.SKIP_USED).assert(hasText("0 skips left"))
    }

    @Test
    fun there_is_no_notice_when_no_skip_was_used() {
        compose.setContent {
            MaterialTheme {
                SkipUsedNotice(skippedWeek = null, skipsHeld = 2, onDismiss = {})
            }
        }

        compose.onAllNodesWithTag(StreakNoticeTestTags.SKIP_USED).assertCountEquals(0)
    }

    @Test
    fun dismissing_the_notice_reports_it() {
        var dismissed = 0
        compose.setContent {
            MaterialTheme {
                SkipUsedNotice(skippedWeek = week, skipsHeld = 1, onDismiss = { dismissed++ })
            }
        }

        compose.onNodeWithTag(StreakNoticeTestTags.SKIP_USED_DISMISS).performClick()

        assertEquals(1, dismissed)
    }

    @Test
    fun the_missed_skip_notice_names_the_streak_that_would_have_been_saved() {
        var openedPro = 0
        compose.setContent {
            MaterialTheme {
                MissedSkipNotice(
                    opportunity = MissedSkipOpportunity(missedWeek = week, brokenStreakWeeks = 5),
                    onSeePro = { openedPro++ },
                    onDismiss = {},
                )
            }
        }

        val notice = compose.onNodeWithTag(StreakNoticeTestTags.MISSED_SKIP)
        notice.assert(hasText("5-week streak", substring = true))
        notice.assert(hasText("would have kept it alive", substring = true))
        compose.onNodeWithTag(StreakNoticeTestTags.MISSED_SKIP_ACTION).performClick()
        assertEquals(1, openedPro)
    }

    @Test
    fun there_is_no_missed_skip_notice_without_an_opportunity() {
        compose.setContent {
            MaterialTheme {
                MissedSkipNotice(opportunity = null, onSeePro = {}, onDismiss = {})
            }
        }

        compose.onAllNodesWithTag(StreakNoticeTestTags.MISSED_SKIP).assertCountEquals(0)
    }
}
