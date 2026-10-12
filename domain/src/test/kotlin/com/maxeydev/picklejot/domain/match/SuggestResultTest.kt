package com.maxeydev.picklejot.domain.match

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SuggestResultTest {
    private val won = GameScore(gameNumber = 1, myScore = 11, opponentScore = 7)
    private val lost = GameScore(gameNumber = 2, myScore = 9, opponentScore = 11)
    private val tied = GameScore(gameNumber = 3, myScore = 10, opponentScore = 10)

    @Test
    fun `an empty score list suggests nothing`() {
        assertNull(suggestedResult(emptyList()))
    }

    @Test
    fun `a majority of won games suggests a win`() {
        assertEquals(MatchResult.WIN, suggestedResult(listOf(won, lost, won)))
    }

    @Test
    fun `a majority of lost games suggests a loss`() {
        assertEquals(MatchResult.LOSS, suggestedResult(listOf(lost, won, lost)))
    }

    @Test
    fun `an even split suggests nothing`() {
        assertNull(suggestedResult(listOf(won, lost)))
    }

    @Test
    fun `tied games count for neither side`() {
        assertNull(suggestedResult(listOf(tied)))
        assertEquals(MatchResult.WIN, suggestedResult(listOf(tied, won)))
    }

    @Test
    fun `scores played to any target are counted without a scoring format`() {
        val toTwentyOne = GameScore(gameNumber = 1, myScore = 21, opponentScore = 19)
        val rallyToFifteen = GameScore(gameNumber = 2, myScore = 15, opponentScore = 14)
        assertEquals(MatchResult.WIN, suggestedResult(listOf(toTwentyOne, rallyToFifteen)))
    }

    @Test
    fun `a mismatch between the tapped result and the scores is advised`() {
        assertEquals(MatchResult.LOSS, resultAdvisory(MatchResult.WIN, listOf(lost, lost)))
        assertEquals(MatchResult.WIN, resultAdvisory(MatchResult.LOSS, listOf(won)))
    }

    @Test
    fun `agreement between the tapped result and the scores is not advised`() {
        assertNull(resultAdvisory(MatchResult.WIN, listOf(won, won)))
        assertNull(resultAdvisory(MatchResult.LOSS, listOf(lost)))
    }

    @Test
    fun `a tie or empty scores produce no advisory for either tapped result`() {
        MatchResult.entries.forEach { tapped ->
            assertNull(resultAdvisory(tapped, emptyList()))
            assertNull(resultAdvisory(tapped, listOf(won, lost)))
        }
    }
}
