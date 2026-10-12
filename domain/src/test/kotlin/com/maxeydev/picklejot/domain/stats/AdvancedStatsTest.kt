@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.domain.stats

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class AdvancedStatsTest {
    private val today = AppDate.parse("2026-09-30")
    private val alex = Uuid.parse("00000000-0000-0000-0000-00000000000a")
    private val blake = Uuid.parse("00000000-0000-0000-0000-00000000000b")
    private val casey = Uuid.parse("00000000-0000-0000-0000-00000000000c")

    private fun line(
        date: String,
        result: MatchResult = MatchResult.WIN,
        opponents: List<Uuid> = emptyList(),
        partner: Uuid? = null,
        location: String? = null,
        paddle: String? = null,
    ): AdvancedMatchLine =
        AdvancedMatchLine(
            date = AppDate.parse(date),
            format = if (partner == null) MatchFormat.SINGLES else MatchFormat.DOUBLES,
            result = result,
            opponentIds = opponents,
            partnerId = partner,
            location = location,
            paddle = paddle,
        )

    private fun stats(vararg lines: AdvancedMatchLine): AdvancedStats = AdvancedStats.from(lines.toList(), today)

    @Test
    fun `no matches gives the empty stats`() {
        val result = stats()

        assertEquals(AdvancedStats.EMPTY, result)
        assertTrue(result.isEmpty)
        assertNull(result.topOpponent)
    }

    @Test
    fun `a record under three matches shows no percentage`() {
        assertNull(WinLoss(wins = 2, losses = 0).percentIfEnoughMatches())
        assertNull(WinLoss(wins = 0, losses = 0).percentIfEnoughMatches())
    }

    @Test
    fun `a record of three matches shows its percentage`() {
        assertEquals(67, WinLoss(wins = 2, losses = 1).percentIfEnoughMatches())
        assertEquals(100, WinLoss(wins = 3, losses = 0).percentIfEnoughMatches())
    }

    @Test
    fun `head to head is grouped by person id and counts each match once per opponent`() {
        val result =
            stats(
                line("2026-09-01", MatchResult.WIN, opponents = listOf(alex)),
                line("2026-09-08", MatchResult.LOSS, opponents = listOf(alex, blake)),
                line("2026-09-15", MatchResult.WIN, opponents = listOf(alex)),
            )

        assertEquals(
            listOf(
                PersonRecord(alex, WinLoss(wins = 2, losses = 1), AppDate.parse("2026-09-15")),
                PersonRecord(blake, WinLoss(wins = 0, losses = 1), AppDate.parse("2026-09-08")),
            ),
            result.headToHead,
        )
        assertEquals(alex, result.topOpponent?.personId)
    }

    @Test
    fun `head to head ties are broken by the most recent match then the id`() {
        val result =
            stats(
                line("2026-09-01", opponents = listOf(alex)),
                line("2026-09-10", opponents = listOf(blake)),
                line("2026-09-10", opponents = listOf(casey)),
            )

        assertEquals(listOf(blake, casey, alex), result.headToHead.map { it.personId })
    }

    @Test
    fun `with partner counts only doubles matches that have a partner`() {
        val result =
            stats(
                line("2026-09-01", MatchResult.WIN, partner = alex),
                line("2026-09-02", MatchResult.LOSS, partner = alex),
                line("2026-09-03", MatchResult.WIN, partner = blake),
                line("2026-09-04", MatchResult.WIN),
            )

        assertEquals(
            listOf(
                PersonRecord(alex, WinLoss(wins = 1, losses = 1), AppDate.parse("2026-09-02")),
                PersonRecord(blake, WinLoss(wins = 1, losses = 0), AppDate.parse("2026-09-03")),
            ),
            result.withPartner,
        )
    }

    @Test
    fun `locations group case and spacing variants and show the latest spelling`() {
        val result =
            stats(
                line("2026-08-01", MatchResult.WIN, location = "Riverside Courts"),
                line("2026-09-01", MatchResult.LOSS, location = "  riverside   courts "),
                line("2026-09-02", MatchResult.WIN, location = "Park"),
            )

        assertEquals(
            listOf(
                LabelRecord("riverside courts", WinLoss(wins = 1, losses = 1), AppDate.parse("2026-09-01")),
                LabelRecord("Park", WinLoss(wins = 1, losses = 0), AppDate.parse("2026-09-02")),
            ),
            result.byLocation,
        )
    }

    @Test
    fun `blank and missing labels are ignored`() {
        val result =
            stats(
                line("2026-09-01", location = "   ", paddle = ""),
                line("2026-09-02", location = null, paddle = null),
            )

        assertTrue(result.byLocation.isEmpty())
        assertTrue(result.byPaddle.isEmpty())
    }

    @Test
    fun `paddles are ranked by matches played`() {
        val result =
            stats(
                line("2026-09-01", MatchResult.WIN, paddle = "Selkirk"),
                line("2026-09-02", MatchResult.WIN, paddle = "Joola"),
                line("2026-09-03", MatchResult.LOSS, paddle = "Joola"),
            )

        assertEquals(listOf("Joola", "Selkirk"), result.byPaddle.map { it.label })
        assertEquals(WinLoss(wins = 1, losses = 1), result.byPaddle.first().record)
    }

    @Test
    fun `monthly runs newest first and marks empty months as gaps`() {
        val result =
            stats(
                line("2026-06-20", MatchResult.WIN),
                line("2026-07-03", MatchResult.LOSS),
                line("2026-07-04", MatchResult.WIN),
                line("2026-09-10", MatchResult.WIN),
            )

        assertEquals(
            listOf(
                MonthRecord(2026, 9, WinLoss(wins = 1, losses = 0)),
                MonthRecord(2026, 8, null),
                MonthRecord(2026, 7, WinLoss(wins = 1, losses = 1)),
                MonthRecord(2026, 6, WinLoss(wins = 1, losses = 0)),
            ),
            result.monthly,
        )
    }

    @Test
    fun `monthly extends to the current month when nothing was played in it`() {
        val result = stats(line("2026-07-15"))

        assertEquals(
            listOf(
                MonthRecord(2026, 9, null),
                MonthRecord(2026, 8, null),
                MonthRecord(2026, 7, WinLoss(wins = 1, losses = 0)),
            ),
            result.monthly,
        )
    }

    @Test
    fun `monthly crosses a year boundary`() {
        val result = stats(line("2025-11-30"), line("2026-01-01"))

        assertEquals(
            MonthRecord(2025, 12, null),
            result.monthly.first { it.year == 2025 && it.month == 12 },
        )
        assertEquals(MonthRecord(2025, 11, WinLoss(wins = 1, losses = 0)), result.monthly.last())
        assertEquals(MonthRecord(2026, 9, null), result.monthly.first())
        assertEquals(11, result.monthly.size)
    }
}
