@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.domain.stats

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.uuid.ExperimentalUuidApi

class ProInsightsTest {
    private val today = AppDate.parse("2026-09-30")

    private fun line(
        date: String,
        result: MatchResult = MatchResult.WIN,
        format: MatchFormat = MatchFormat.SINGLES,
    ): AdvancedMatchLine = AdvancedMatchLine(date = AppDate.parse(date), format = format, result = result)

    private fun insights(vararg lines: AdvancedMatchLine): ProInsights = ProInsights.from(lines.toList(), today)

    private fun septemberDay(day: Int): String = "2026-09-" + day.toString().padStart(2, '0')

    @Test
    fun `no matches gives empty insights that still know today`() {
        val result = insights()

        assertEquals(ProInsights.EMPTY.copy(today = today), result)
        assertEquals(0, result.daysPlayed)
        assertEquals(emptyList<Int>(), result.form)
    }

    @Test
    fun `advanced stats with no matches keep the empty insights`() {
        assertEquals(AdvancedStats.EMPTY, AdvancedStats.from(emptyList(), today))
    }

    @Test
    fun `advanced stats carry the insights for the same matches`() {
        val lines = listOf(line("2026-09-01"), line("2026-09-02", MatchResult.LOSS))

        assertEquals(ProInsights.from(lines, today), AdvancedStats.from(lines, today).insights)
    }

    @Test
    fun `matches are counted per day and days played counts distinct days`() {
        val result =
            insights(
                line("2026-10-01"),
                line("2026-10-02"),
                line("2026-10-03"),
                line("2026-10-05"),
                line("2026-10-05", MatchResult.LOSS),
            )

        assertEquals(4, result.daysPlayed)
        assertEquals(2, result.matchesOn(AppDate.parse("2026-10-05")))
        assertEquals(1, result.matchesOn(AppDate.parse("2026-10-01")))
        assertEquals(0, result.matchesOn(AppDate.parse("2026-10-04")))
    }

    @Test
    fun `the longest run counts consecutive days with a match`() {
        val result =
            insights(
                line("2026-10-01"),
                line("2026-10-02"),
                line("2026-10-03"),
                line("2026-10-05"),
                line("2026-10-06"),
            )

        assertEquals(3, result.longestDayRun)
    }

    @Test
    fun `a single match is a run of one day`() {
        assertEquals(1, insights(line("2026-09-10")).longestDayRun)
    }

    @Test
    fun `weekday records put Monday first and Sunday last`() {
        val result =
            insights(
                line("2026-10-05", MatchResult.WIN),
                line("2026-10-05", MatchResult.LOSS),
                line("2026-10-10", MatchResult.WIN),
                line("2026-10-11", MatchResult.LOSS),
            )

        assertEquals(7, result.weekdays.size)
        assertEquals(WinLoss(wins = 1, losses = 1), result.weekdays[0].record)
        assertEquals(WinLoss(wins = 1, losses = 0), result.weekdays[5].record)
        assertEquals(WinLoss(wins = 0, losses = 1), result.weekdays[6].record)
        assertEquals(WinLoss.NONE, result.weekdays[2].record)
    }

    @Test
    fun `the weekday index of a known Monday is zero`() {
        assertEquals(0, ProInsights.weekdayIndexOf(AppDate.parse("2026-10-05")))
        assertEquals(6, ProInsights.weekdayIndexOf(AppDate.parse("2026-10-11")))
    }

    @Test
    fun `the last ten matches ignore everything older`() {
        val older = listOf(line(septemberDay(1), MatchResult.LOSS), line(septemberDay(2), MatchResult.LOSS))
        val newer = (3..12).map { line(septemberDay(it), MatchResult.WIN) }

        val result = ProInsights.from(older + newer, today)

        assertEquals(WinLoss(wins = 10, losses = 0), result.lastTen)
        assertEquals(WinLoss(wins = 10, losses = 2), result.overall)
    }

    @Test
    fun `doubles baseline counts only doubles matches`() {
        val result =
            insights(
                line("2026-09-01", MatchResult.WIN, MatchFormat.DOUBLES),
                line("2026-09-02", MatchResult.LOSS, MatchFormat.DOUBLES),
                line("2026-09-03", MatchResult.WIN, MatchFormat.SINGLES),
            )

        assertEquals(WinLoss(wins = 1, losses = 1), result.doubles)
    }

    @Test
    fun `form needs more than one window of matches`() {
        val tenMatches = (1..10).map { line(septemberDay(it)) }

        assertEquals(emptyList<Int>(), ProInsights.from(tenMatches, today).form)
    }

    @Test
    fun `form is the rolling win rate over ten matches ending on each recent match`() {
        val lines =
            listOf(line(septemberDay(1), MatchResult.LOSS), line(septemberDay(2), MatchResult.LOSS)) +
                (3..12).map { line(septemberDay(it), MatchResult.WIN) }

        assertEquals(listOf(80, 90, 100), ProInsights.from(lines, today).form)
    }

    @Test
    fun `form keeps only the last thirty matches`() {
        val lines = (1..45).map { line("2026-08-" + it.coerceAtMost(28).toString().padStart(2, '0')) }

        assertEquals(30, ProInsights.from(lines, today).form.size)
    }

    @Test
    fun `trend compares the last ninety days with the ninety before`() {
        val result =
            insights(
                line("2026-06-01", MatchResult.WIN),
                line("2026-06-02", MatchResult.LOSS),
                line("2026-06-03", MatchResult.LOSS),
                line("2026-09-01", MatchResult.WIN),
                line("2026-09-02", MatchResult.WIN),
                line("2026-09-03", MatchResult.LOSS),
            )

        assertEquals(34, result.trendPoints)
    }

    @Test
    fun `trend is unknown when either period has fewer than three matches`() {
        val result =
            insights(
                line("2026-06-01", MatchResult.WIN),
                line("2026-09-01", MatchResult.WIN),
                line("2026-09-02", MatchResult.WIN),
                line("2026-09-03", MatchResult.LOSS),
            )

        assertNull(result.trendPoints)
    }

    @Test
    fun `matches per week divides by the weeks since the first match`() {
        val oneWeek =
            insights(line("2026-09-28"), line("2026-09-29"), line("2026-09-29"), line("2026-09-30"))
        val twoWeeks =
            insights(line("2026-09-21"), line("2026-09-22"), line("2026-09-29"), line("2026-09-30"))

        assertEquals(4.0, oneWeek.matchesPerWeek, 0.001)
        assertEquals(2.0, twoWeeks.matchesPerWeek, 0.001)
    }

    @Test
    fun `points above compares a record with a baseline`() {
        val baseline = WinLoss(wins = 1, losses = 1)

        assertEquals(25, WinLoss(wins = 3, losses = 1).pointsAbove(baseline))
        assertEquals(-17, WinLoss(wins = 1, losses = 2).pointsAbove(baseline))
    }

    @Test
    fun `points above is unknown for a small sample or an empty baseline`() {
        assertNull(WinLoss(wins = 2, losses = 0).pointsAbove(WinLoss(wins = 1, losses = 1)))
        assertNull(WinLoss(wins = 3, losses = 0).pointsAbove(WinLoss.NONE))
    }
}
