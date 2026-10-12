@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.match.FilterState
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.stats.AdvancedMatchLine
import com.maxeydev.picklejot.domain.stats.AdvancedStats
import com.maxeydev.picklejot.domain.stats.BasicStats
import com.maxeydev.picklejot.domain.stats.WinLoss
import com.maxeydev.picklejot.domain.streak.StreakResult
import com.maxeydev.picklejot.ui.fakes.FakeDependencies
import com.maxeydev.picklejot.ui.fakes.FakeMatchRepository
import com.maxeydev.picklejot.ui.fakes.FakeProfileRepository
import com.maxeydev.picklejot.ui.navigation.PicklejotNavHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val WAIT_MILLIS = 5_000L
private const val PIXEL_TOLERANCE = 1f

@RunWith(AndroidJUnit4::class)
class DashboardUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val record =
        BasicStats(
            overall = WinLoss(wins = 8, losses = 4),
            singles = WinLoss(wins = 8, losses = 4),
            doubles = WinLoss.NONE,
        )

    private fun state(
        filter: FilterState = FilterState.NONE,
        streak: StreakResult = StreakResult(current = 3, longest = 5),
        displayName: String = "Matty",
        stats: BasicStats = record,
        overallStats: BasicStats = record,
        recentResults: List<MatchResult> = emptyList(),
        hasAnyMatches: Boolean = true,
        isPro: Boolean = false,
        advanced: AdvancedStats = AdvancedStats.EMPTY,
        peopleNames: Map<Uuid, String> = emptyMap(),
        skipsHeld: Int = 0,
    ): DashboardUiState =
        DashboardUiState(
            isLoading = false,
            displayName = displayName,
            stats = stats,
            overallStats = overallStats,
            recentResults = recentResults,
            streak = streak,
            hasAnyMatches = hasAnyMatches,
            filter = filter,
            isPro = isPro,
            advanced = advanced,
            peopleNames = peopleNames,
            skipsHeld = skipsHeld,
        )

    private val dave = Uuid.random()
    private val casey = Uuid.random()
    private val names = mapOf(dave to "Dave", casey to "Casey")

    private fun line(
        date: String,
        result: MatchResult,
        vararg opponents: Uuid,
    ): AdvancedMatchLine =
        AdvancedMatchLine(
            date = AppDate.parse(date),
            format = MatchFormat.SINGLES,
            result = result,
            opponentIds = opponents.toList(),
        )

    private fun advancedOf(vararg lines: AdvancedMatchLine): AdvancedStats =
        AdvancedStats.from(lines.toList(), AppDate.parse("2026-09-30"))

    private fun showHeader(
        state: DashboardUiState,
        fontScale: Float = 1f,
        isCollapsed: Boolean = false,
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MaterialTheme {
                    Box(modifier = Modifier.width(360.dp)) {
                        DashboardHeader(state = state, isCollapsed = isCollapsed, onOpenStats = {})
                    }
                }
            }
        }
    }

    private fun spokenHeader(): String {
        val node = compose.onNodeWithTag(DashboardTestTags.HEADER).fetchSemanticsNode()
        val descriptions = node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        val texts = node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        return (texts + descriptions).joinToString(", ")
    }

    private fun SemanticsNodeInteraction.assertTextFits() {
        val results = mutableListOf<TextLayoutResult>()
        val node = fetchSemanticsNode()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        val layout = results.single()
        val widestLineEnd = (0 until layout.lineCount).maxOf { layout.getLineRight(it) }
        val textHeight = layout.multiParagraph.height
        val measured = "'${layout.layoutInput.text}' is ${layout.size}, lines end at $widestLineEnd, height $textHeight"
        assertTrue("$measured: cut off at the side", widestLineEnd <= layout.size.width + PIXEL_TOLERANCE)
        assertTrue("$measured: cut off at the bottom", textHeight <= layout.size.height + PIXEL_TOLERANCE)
        val header = compose.onNodeWithTag(DashboardTestTags.HEADER).fetchSemanticsNode().boundsInRoot
        val bounds = node.boundsInRoot
        assertTrue("$measured escapes the header $header", bounds.bottom <= header.bottom + PIXEL_TOLERANCE)
        assertTrue("$measured escapes the header $header", bounds.right <= header.right + PIXEL_TOLERANCE)
    }

    private fun SemanticsNodeInteraction.assertInsideHeader() {
        val header = compose.onNodeWithTag(DashboardTestTags.HEADER).fetchSemanticsNode().boundsInRoot
        val bounds = fetchSemanticsNode().boundsInRoot
        assertTrue("$bounds escapes the header $header", bounds.bottom <= header.bottom + PIXEL_TOLERANCE)
        assertTrue("$bounds escapes the header $header", bounds.right <= header.right + PIXEL_TOLERANCE)
    }

    @Test
    fun the_header_shows_name_record_win_rate_and_streak() {
        showHeader(state())

        compose.onNodeWithTag(DashboardTestTags.NAME, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("8W – 4L", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("67%", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("WR", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("3-week streak", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun a_zero_streak_says_so_in_words() {
        showHeader(state(streak = StreakResult.NONE))

        compose.onNodeWithText("No active streak", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun the_header_figures_ignore_the_active_filter() {
        val filteredRecord =
            BasicStats(
                overall = WinLoss(wins = 1, losses = 0),
                singles = WinLoss(wins = 1, losses = 0),
                doubles = WinLoss.NONE,
            )
        showHeader(
            state(
                filter = FilterState(format = MatchFormat.SINGLES, result = MatchResult.WIN),
                stats = filteredRecord,
            ),
        )

        compose.onNodeWithText("8W – 4L", useUnmergedTree = true).assertIsDisplayed()
        compose.onAllNodesWithTag(DashboardTestTags.FILTER_INDICATOR, useUnmergedTree = true).assertCountEquals(0)
        assertFalse(spokenHeader().contains("filtered"))
    }

    @Test
    fun the_collapsed_header_keeps_the_name_streak_win_rate_and_record() {
        showHeader(state(), isCollapsed = true)

        compose.onNodeWithTag(DashboardTestTags.NAME, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("3 wk", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("67%", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("8W – 4L", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun the_last_matches_show_only_when_there_are_some() {
        showHeader(state(recentResults = listOf(MatchResult.WIN, MatchResult.LOSS, MatchResult.WIN)))

        compose.onNodeWithText("Last 3", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun zero_matches_show_no_header_at_all() {
        showHeader(state(stats = BasicStats.EMPTY, overallStats = BasicStats.EMPTY, hasAnyMatches = false))

        compose.onAllNodesWithTag(DashboardTestTags.HEADER, useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithTag(DashboardTestTags.WIN_PERCENT, useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodes(hasText("0%", substring = true), useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodes(hasText("NaN", substring = true), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun an_empty_display_name_leaves_no_label_behind() {
        showHeader(state(displayName = ""))

        compose.onAllNodesWithTag(DashboardTestTags.NAME, useUnmergedTree = true).assertCountEquals(0)
        assertFalse(spokenHeader().startsWith(","))
    }

    @Test
    fun the_header_reads_as_one_sentence_for_talkback() {
        showHeader(state(recentResults = listOf(MatchResult.WIN, MatchResult.LOSS, MatchResult.WIN)))

        val spoken = spokenHeader()
        listOf(
            "Matty",
            "8 won, 4 lost",
            "67 percent won",
            "current streak 3 weeks, counting all matches",
            "last 3 matches, oldest first: Win, Loss, Win",
        ).forEach { part -> assertTrue("missing '$part' in: $spoken", spoken.contains(part)) }
    }

    @Test
    fun at_double_font_size_nothing_in_the_header_is_clipped() {
        showHeader(
            state(
                filter = FilterState(format = MatchFormat.DOUBLES, result = MatchResult.LOSS, location = "Court A"),
                displayName = "Maximiliano Alejandro de la Cruz",
            ),
            fontScale = 2f,
        )

        listOf(DashboardTestTags.NAME, DashboardTestTags.RECORD).forEach { tag ->
            compose.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed().assertTextFits()
        }
        listOf(DashboardTestTags.WIN_PERCENT, DashboardTestTags.STREAK).forEach { tag ->
            compose.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed().assertInsideHeader()
        }
    }

    @Test
    fun the_stats_screen_says_when_the_current_streak_is_the_longest() {
        compose.setContent {
            MaterialTheme {
                ExpandedStatsScreen(
                    state = state(streak = StreakResult(current = 4, longest = 4)),
                    onBack = {},
                    onSeePro = {},
                )
            }
        }

        compose.onNodeWithText("Your longest streak ever", useUnmergedTree = true).assertIsDisplayed()
        compose.onAllNodesWithTag(DashboardTestTags.STATS_LONGEST_STREAK).assertCountEquals(0)
    }

    @Test
    fun the_stats_screen_shows_a_dash_for_a_format_with_no_matches_and_locked_pro_rows() {
        compose.setContent {
            MaterialTheme {
                ExpandedStatsScreen(state = state(), onBack = {}, onSeePro = {})
            }
        }

        compose.onNodeWithTag(DashboardTestTags.STATS_DOUBLES).assert(hasText("—", substring = true))
        compose.onNodeWithTag(DashboardTestTags.STATS_DOUBLES).assert(hasText("No matches", substring = true))
        compose.onNodeWithTag(DashboardTestTags.STATS_SINGLES).assert(hasText("67%", substring = true))
        compose.onNodeWithTag(DashboardTestTags.STATS_CURRENT_STREAK).assert(hasText("3 weeks", substring = true))
        compose.onNodeWithTag(DashboardTestTags.STATS_LONGEST_STREAK).assert(hasText("5 weeks", substring = true))
        compose.onNodeWithTag(DashboardTestTags.proRow(0)).performScrollToAndAssertLocked()
    }

    @Test
    fun a_free_user_sees_a_locked_preview_naming_the_top_opponent_without_the_record() {
        var openedPro = 0
        val advanced =
            advancedOf(
                line("2026-09-01", MatchResult.WIN, dave),
                line("2026-09-08", MatchResult.WIN, dave),
                line("2026-09-15", MatchResult.WIN, dave),
                line("2026-09-16", MatchResult.LOSS, casey),
            )
        compose.setContent {
            MaterialTheme {
                ExpandedStatsScreen(
                    state = state(advanced = advanced, peopleNames = names),
                    onBack = {},
                    onSeePro = { openedPro++ },
                )
            }
        }

        compose.onNodeWithTag(DashboardTestTags.STATS_LOCKED_PREVIEW).assertIsDisplayed()
        val name = compose.onNodeWithTag(DashboardTestTags.STATS_LOCKED_PREVIEW_NAME, useUnmergedTree = true)
        name.assert(hasText("You vs Dave"))
        compose.onNodeWithText("3–0", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("100%", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag(DashboardTestTags.STATS_LOCKED_PREVIEW).performClick()
        assertEquals(1, openedPro)
    }

    @Test
    fun a_free_user_with_no_named_opponent_sees_no_preview_card() {
        compose.setContent {
            MaterialTheme {
                ExpandedStatsScreen(state = state(), onBack = {}, onSeePro = {})
            }
        }

        compose.onAllNodesWithTag(DashboardTestTags.STATS_LOCKED_PREVIEW).assertCountEquals(0)
        compose.onNodeWithTag(DashboardTestTags.STATS_PRO_SECTION).assertExists()
    }

    @Test
    fun a_pro_user_sees_small_samples_as_raw_tallies_and_larger_ones_as_percentages() {
        val advanced =
            advancedOf(
                line("2026-09-01", MatchResult.WIN, dave),
                line("2026-09-02", MatchResult.WIN, casey),
                line("2026-09-03", MatchResult.WIN, casey),
                line("2026-09-04", MatchResult.LOSS, casey),
            )
        compose.setContent {
            MaterialTheme {
                ExpandedStatsScreen(
                    state = state(isPro = true, advanced = advanced, peopleNames = names),
                    onBack = {},
                    onSeePro = {},
                )
            }
        }

        val group = DashboardTestTags.GROUP_HEAD_TO_HEAD
        val first = compose.onNodeWithTag(DashboardTestTags.advancedRow(group, 0))
        first.performScrollTo()
        first.assert(hasText("Casey", substring = true))
        first.assert(hasText("67%", substring = true))
        val second = compose.onNodeWithTag(DashboardTestTags.advancedRow(group, 1))
        second.performScrollTo()
        second.assert(hasText("Dave", substring = true))
        second.assert(hasText("1–0", substring = true))
        second.assert(hasText("Too few matches", substring = true))
        compose.onNodeWithText("100%", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onAllNodesWithTag(DashboardTestTags.STATS_PRO_SECTION).assertCountEquals(0)
    }

    @Test
    fun a_pro_user_with_no_matches_sees_one_empty_state_instead_of_empty_charts() {
        compose.setContent {
            MaterialTheme {
                ExpandedStatsScreen(state = state(isPro = true), onBack = {}, onSeePro = {})
            }
        }

        compose.onNodeWithTag(DashboardTestTags.STATS_ADVANCED_EMPTY).performScrollTo().assertIsDisplayed()
        val headToHeadRows = DashboardTestTags.advancedRow(DashboardTestTags.GROUP_HEAD_TO_HEAD, 0)
        val monthRows = DashboardTestTags.advancedRow(DashboardTestTags.GROUP_MONTH, 0)
        compose.onAllNodesWithTag(headToHeadRows).assertCountEquals(0)
        compose.onAllNodesWithTag(monthRows).assertCountEquals(0)
    }

    @Test
    fun a_month_with_no_matches_is_a_gap_not_a_zero() {
        val advanced =
            advancedOf(
                line("2026-07-10", MatchResult.WIN, dave),
                line("2026-09-10", MatchResult.WIN, dave),
            )
        compose.setContent {
            MaterialTheme {
                ExpandedStatsScreen(
                    state = state(isPro = true, advanced = advanced, peopleNames = names),
                    onBack = {},
                    onSeePro = {},
                )
            }
        }

        compose.onNodeWithText("Month by month").performScrollTo()
        val gap = compose.onNodeWithTag(DashboardTestTags.advancedRow(DashboardTestTags.GROUP_MONTH, 1))
        gap.assert(hasContentDescription("no matches", substring = true))
        gap.assert(hasText("—", substring = true))
    }

    @Test
    fun the_pro_key_numbers_show_the_last_ten_and_a_dash_when_the_trend_is_unknown() {
        val advanced =
            advancedOf(
                line("2026-09-01", MatchResult.WIN, dave),
                line("2026-09-02", MatchResult.WIN, casey),
                line("2026-09-03", MatchResult.WIN, casey),
                line("2026-09-04", MatchResult.LOSS, casey),
            )
        compose.setContent {
            MaterialTheme {
                ExpandedStatsScreen(
                    state = state(isPro = true, advanced = advanced, peopleNames = names),
                    onBack = {},
                    onSeePro = {},
                )
            }
        }

        val lastTen = compose.onNodeWithTag(DashboardTestTags.STATS_PRO_LAST_TEN)
        lastTen.performScrollTo()
        lastTen.assert(hasText("3–1", substring = true))
        val trend = compose.onNodeWithTag(DashboardTestTags.STATS_PRO_TREND)
        trend.performScrollTo()
        trend.assert(hasText("—", substring = true))
    }

    @Test
    fun the_activity_graph_switches_between_six_months_and_a_year() {
        val advanced = advancedOf(line("2026-09-01", MatchResult.WIN, dave))
        compose.setContent {
            MaterialTheme {
                ExpandedStatsScreen(
                    state = state(isPro = true, advanced = advanced, peopleNames = names),
                    onBack = {},
                    onSeePro = {},
                )
            }
        }

        val sixMonths = compose.onNodeWithTag(DashboardTestTags.STATS_PRO_HEATMAP_SIX_MONTHS)
        val year = compose.onNodeWithTag(DashboardTestTags.STATS_PRO_HEATMAP_YEAR)
        sixMonths.performScrollTo()
        sixMonths.assertIsSelected()
        year.assertIsNotSelected()
        year.performClick()
        year.assertIsSelected()
        sixMonths.assertIsNotSelected()
    }

    @Test
    fun a_pro_user_sees_how_many_skips_are_held() {
        compose.setContent {
            MaterialTheme {
                ExpandedStatsScreen(state = state(isPro = true, skipsHeld = 1), onBack = {}, onSeePro = {})
            }
        }

        val skips = compose.onNodeWithTag(DashboardTestTags.STATS_SKIPS_HELD)
        skips.performScrollTo()
        skips.assert(hasText("1 held", substring = true))
    }

    @Test
    fun a_free_user_is_not_shown_a_skip_balance() {
        compose.setContent {
            MaterialTheme {
                ExpandedStatsScreen(state = state(), onBack = {}, onSeePro = {})
            }
        }

        compose.onAllNodesWithTag(DashboardTestTags.STATS_SKIPS_HELD).assertCountEquals(0)
    }

    private fun SemanticsNodeInteraction.performScrollToAndAssertLocked() {
        performScrollTo()
        assert(hasText("Pro", substring = true))
    }

    @Test
    fun tapping_the_header_opens_the_stats_screen_with_the_same_filtered_figures() {
        val day = AppDate.parse("2026-09-01")
        val stored =
            listOf(MatchResult.WIN, MatchResult.LOSS, MatchResult.WIN).mapIndexed { index, result ->
                Match(
                    id = Uuid.random(),
                    format = if (index == 1) MatchFormat.DOUBLES else MatchFormat.SINGLES,
                    date = day,
                    result = result,
                    createdAt = AppInstant.fromEpochMilliseconds(index.toLong()),
                    updatedAt = AppInstant.fromEpochMilliseconds(index.toLong()),
                )
            }
        val dependencies =
            FakeDependencies(
                matchRepository = FakeMatchRepository(stored),
                profileRepository = FakeProfileRepository(displayName = "Matty"),
            )
        compose.setContent { MaterialTheme { PicklejotNavHost(dependencies = dependencies) } }
        compose.waitUntil(WAIT_MILLIS) {
            compose.onAllNodesWithTag(DashboardTestTags.HEADER).fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithTag(DashboardTestTags.HEADER).performClick()

        compose.waitUntil(WAIT_MILLIS) {
            compose.onAllNodesWithTag(DashboardTestTags.STATS_SCREEN).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(DashboardTestTags.STATS_MATCHES).assert(hasText("3"))
        compose.onNodeWithTag(DashboardTestTags.STATS_RECORD).assert(hasText("2–1"))
    }
}
