package com.maxeydev.picklejot.domain.stats

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BasicStatsTest {
    private val day = AppDate.parse("2026-09-01")

    private fun line(
        format: MatchFormat,
        result: MatchResult,
    ): MatchStatLine = MatchStatLine(date = day, format = format, result = result)

    @Test
    fun `no matches gives no win percentage rather than zero or NaN`() {
        val stats = BasicStats.from(emptyList())

        assertEquals(0, stats.totalMatches)
        assertNull(stats.overall.winPercent)
        assertNull(stats.singles.winPercent)
        assertNull(stats.doubles.winPercent)
        assertFalse(stats.overall.hasMatches)
    }

    @Test
    fun `a split with no matches on one side has no percentage while the other side does`() {
        val stats =
            BasicStats.from(
                listOf(
                    line(MatchFormat.DOUBLES, MatchResult.WIN),
                    line(MatchFormat.DOUBLES, MatchResult.LOSS),
                ),
            )

        assertNull(stats.singles.winPercent)
        assertFalse(stats.singles.hasMatches)
        assertEquals(50, stats.doubles.winPercent)
        assertEquals(50, stats.overall.winPercent)
    }

    @Test
    fun `wins and losses are counted per format and overall`() {
        val stats =
            BasicStats.from(
                listOf(
                    line(MatchFormat.SINGLES, MatchResult.WIN),
                    line(MatchFormat.SINGLES, MatchResult.WIN),
                    line(MatchFormat.SINGLES, MatchResult.LOSS),
                    line(MatchFormat.DOUBLES, MatchResult.LOSS),
                ),
            )

        assertEquals(WinLoss(wins = 2, losses = 1), stats.singles)
        assertEquals(WinLoss(wins = 0, losses = 1), stats.doubles)
        assertEquals(WinLoss(wins = 2, losses = 2), stats.overall)
        assertEquals(4, stats.totalMatches)
        assertEquals(67, stats.singles.winPercent)
        assertEquals(0, stats.doubles.winPercent)
    }

    @Test
    fun `a record with a loss never rounds up to one hundred percent`() {
        assertEquals(99, WinLoss(wins = 199, losses = 1).winPercent)
        assertEquals(100, WinLoss(wins = 5, losses = 0).winPercent)
    }

    @Test
    fun `a record with a win never rounds down to zero percent`() {
        assertEquals(1, WinLoss(wins = 1, losses = 199).winPercent)
        assertEquals(0, WinLoss(wins = 0, losses = 3).winPercent)
    }

    @Test
    fun `a one match record is either all or nothing`() {
        assertEquals(100, WinLoss(wins = 1, losses = 0).winPercent)
        assertEquals(0, WinLoss(wins = 0, losses = 1).winPercent)
        assertTrue(WinLoss(wins = 1, losses = 0).hasMatches)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative counts are rejected`() {
        WinLoss(wins = -1, losses = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `splits that do not add up to the overall record are rejected`() {
        BasicStats(
            overall = WinLoss(wins = 2, losses = 0),
            singles = WinLoss(wins = 1, losses = 0),
            doubles = WinLoss.NONE,
        )
    }
}
