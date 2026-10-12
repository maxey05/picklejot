package com.maxeydev.picklejot.ui.match.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.match.GameScore
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.ui.match.formatMatchDateLong
import com.maxeydev.picklejot.ui.match.formatMatchDateShort
import com.maxeydev.picklejot.ui.match.todayInDeviceZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

private val PHONE_WIDTH: Dp = 360.dp
private const val PIXEL_TOLERANCE = 0.5f

@RunWith(AndroidJUnit4::class)
class MatchRowTest {
    @get:Rule
    val compose = createComposeRule()

    private val date = AppDate.parse("2025-03-05")

    private val fullRow =
        MatchRowUiState(
            id = "full",
            date = date,
            format = MatchFormat.DOUBLES,
            result = MatchResult.WIN,
            opponentNames = listOf("Ana", "Ben"),
            games = listOf(GameScore(1, 11, 7), GameScore(2, 9, 11)),
            thumbnailPath = "/nonexistent/photos/full.jpg",
            partnerName = "Cy",
            location = "Rec Center",
        )

    private val bareRow =
        MatchRowUiState(
            id = "bare",
            date = date,
            format = MatchFormat.SINGLES,
            result = MatchResult.LOSS,
            opponentNames = emptyList(),
            games = emptyList(),
            thumbnailPath = null,
        )

    private val locale: Locale
        get() =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext.resources.configuration.locales[0]

    private val shortDate: String
        get() = formatMatchDateShort(date, todayInDeviceZone(), locale)

    private fun show(
        vararg rows: MatchRowUiState,
        fontScale: Float = 1f,
        onClick: (String) -> Unit = {},
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MaterialTheme {
                    Box(modifier = Modifier.width(PHONE_WIDTH)) {
                        Column {
                            rows.forEach { row -> MatchRow(state = row, onClick = { onClick(row.id) }) }
                        }
                    }
                }
            }
        }
    }

    private fun part(
        rowId: String,
        tag: String,
    ): SemanticsNodeInteraction =
        compose.onNode(
            hasTestTag(tag) and hasAnyAncestor(hasTestTag(MatchListTestTags.row(rowId))),
            useUnmergedTree = true,
        )

    private fun bounds(interaction: SemanticsNodeInteraction): Rect = interaction.fetchSemanticsNode().boundsInRoot

    private fun textLayout(interaction: SemanticsNodeInteraction): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        interaction
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(results)
        return results.single()
    }

    private fun scoreEntries(rowId: String): SemanticsNodeInteractionCollection =
        compose.onAllNodes(
            hasTestTag(MatchListTestTags.SCORE_ENTRY) and hasAnyAncestor(hasTestTag(MatchListTestTags.row(rowId))),
            useUnmergedTree = true,
        )

    private fun assertTextNotClipped(
        label: String,
        interaction: SemanticsNodeInteraction,
    ) {
        val layout = textLayout(interaction)
        val widestLineEnd = (0 until layout.lineCount).maxOf { layout.getLineRight(it) }
        val textHeight = layout.multiParagraph.height
        val measured =
            "$label is ${layout.size.width}x${layout.size.height}px, its widest line ends at ${widestLineEnd}px " +
                "and its ${layout.lineCount} line(s) are ${textHeight}px tall"
        assertTrue("$measured, so it is cut off at the side", widestLineEnd <= layout.size.width + PIXEL_TOLERANCE)
        assertTrue("$measured, so it is cut off at the bottom", textHeight <= layout.size.height + PIXEL_TOLERANCE)
    }

    @Test
    fun `a_full_row_shows_result_date_opponents_partner_location_scores_and_a_thumbnail`() {
        show(fullRow)

        part("full", MatchListTestTags.HEADLINE).assertTextEquals("vs Ana & Ben")
        part("full", MatchListTestTags.DETAILS).assertTextEquals("Doubles · with Cy")
        part("full", MatchListTestTags.LOCATION).assertTextEquals("Rec Center")
        part("full", MatchListTestTags.ROW_DATE).assertTextEquals(shortDate)
        val scores = scoreEntries("full")
        scores.assertCountEquals(2)
        scores[0].assertTextEquals("11–7")
        scores[1].assertTextEquals("9–11")
        part("full", MatchListTestTags.THUMBNAIL).assertExists()
        compose
            .onNode(
                hasText("W") and hasAnyAncestor(hasTestTag(MatchListTestTags.RESULT_BADGE)),
                useUnmergedTree = true,
            ).assertExists()
    }

    @Test
    fun `missing_scores_opponents_and_photo_are_simply_absent_with_no_placeholder`() {
        show(bareRow)

        part("bare", MatchListTestTags.HEADLINE).assertTextEquals("Singles match")
        part("bare", MatchListTestTags.DETAILS).assertDoesNotExist()
        part("bare", MatchListTestTags.LOCATION).assertDoesNotExist()
        part("bare", MatchListTestTags.ROW_DATE).assertTextEquals(shortDate)
        part("bare", MatchListTestTags.SCORES).assertDoesNotExist()
        part("bare", MatchListTestTags.THUMBNAIL).assertDoesNotExist()
        listOf("Unknown", "—", "–", "vs").forEach { placeholder ->
            compose
                .onAllNodes(hasText(placeholder, substring = true, ignoreCase = true), useUnmergedTree = true)
                .assertCountEquals(0)
        }
    }

    @Test
    fun `two_long_opponent_names_truncate_to_one_line_instead_of_doubling_the_row_height`() {
        val longNames =
            fullRow.copy(
                id = "long",
                opponentNames = listOf("Maximiliana Concepcion-Villanueva", "Bartholomew Fitzgerald-Montgomery"),
            )
        show(fullRow, longNames)

        val headline = textLayout(part("long", MatchListTestTags.HEADLINE))
        assertEquals(1, headline.lineCount)
        assertTrue(headline.isLineEllipsized(0))
        assertEquals(
            bounds(compose.onNodeWithTag(MatchListTestTags.row("full"))).height,
            bounds(compose.onNodeWithTag(MatchListTestTags.row("long"))).height,
        )
    }

    @Test
    fun `tapping_a_row_reports_which_match_was_tapped`() {
        val tapped = mutableListOf<String>()
        show(fullRow, bareRow, onClick = { tapped += it })

        compose.onNodeWithTag(MatchListTestTags.row("bare")).performClick()

        assertEquals(listOf("bare"), tapped)
    }

    @Test
    fun `talkback_reads_a_full_row_as_one_sentence`() {
        show(fullRow)

        val row = compose.onNodeWithTag(MatchListTestTags.row("full")).fetchSemanticsNode()

        val expected =
            "Win, Doubles, ${formatMatchDateLong(date, locale)}, " +
                "against Ana and Ben, with Cy, at Rec Center, scores 11 to 7, 9 to 11, with photo"
        assertEquals(listOf(expected), row.config[SemanticsProperties.ContentDescription])
        compose.onNode(hasText("vs Ana & Ben")).assertExists()
        compose.onAllNodes(hasText("vs Ana & Ben")).assertCountEquals(1)
        compose.onNode(hasText("vs Ana & Ben") and hasTestTag(MatchListTestTags.row("full"))).assertExists()
    }

    @Test
    fun `talkback_reads_a_bare_row_without_mentioning_what_is_missing`() {
        show(bareRow)

        val row = compose.onNodeWithTag(MatchListTestTags.row("bare")).fetchSemanticsNode()

        assertEquals(
            listOf("Loss, Singles, ${formatMatchDateLong(date, locale)}"),
            row.config[SemanticsProperties.ContentDescription],
        )
    }

    @Test
    fun `win_and_loss_each_carry_a_text_label_not_only_a_colour`() {
        show(fullRow, bareRow)

        compose
            .onNode(
                hasText("W") and hasAnyAncestor(hasTestTag(MatchListTestTags.row("full"))),
                useUnmergedTree = true,
            ).assertExists()
        compose
            .onNode(
                hasText("L") and hasAnyAncestor(hasTestTag(MatchListTestTags.row("bare"))),
                useUnmergedTree = true,
            ).assertExists()
    }

    @Test
    fun `at_200_percent_font_scale_the_row_grows_instead_of_clipping_or_overlapping`() {
        show(fullRow, fontScale = 2f)

        val row = bounds(compose.onNodeWithTag(MatchListTestTags.row("full")))
        val badge = bounds(part("full", MatchListTestTags.RESULT_BADGE))
        val text = bounds(part("full", MatchListTestTags.TEXT_COLUMN))
        val thumbnail = bounds(part("full", MatchListTestTags.THUMBNAIL))
        val scoreColumn = bounds(part("full", MatchListTestTags.SCORES))
        listOf(badge, text, thumbnail, scoreColumn).forEach { child ->
            assertTrue("$child escapes the row $row", child.left >= row.left && child.right <= row.right)
            assertTrue("$child escapes the row $row", child.top >= row.top && child.bottom <= row.bottom)
        }
        assertTrue("badge $badge overlaps text $text", badge.right <= text.left)
        assertTrue("text $text overlaps thumbnail $thumbnail", text.right <= thumbnail.left)
        assertTrue("thumbnail $thumbnail overlaps scores $scoreColumn", thumbnail.right <= scoreColumn.left)

        val headline = textLayout(part("full", MatchListTestTags.HEADLINE))
        assertEquals(1, headline.lineCount)
        assertTextNotClipped(MatchListTestTags.DETAILS, part("full", MatchListTestTags.DETAILS))
        val scores = scoreEntries("full")
        scores.assertCountEquals(2)
        repeat(2) { index -> assertTextNotClipped("score ${index + 1}", scores[index]) }
        listOf(MatchListTestTags.HEADLINE, MatchListTestTags.DETAILS, MatchListTestTags.LOCATION).forEach { tag ->
            val line = bounds(part("full", tag))
            val isInsideColumn = line.top >= text.top && line.bottom <= text.bottom
            assertTrue("$tag $line is clipped by the text column $text", isInsideColumn)
        }
    }
}
