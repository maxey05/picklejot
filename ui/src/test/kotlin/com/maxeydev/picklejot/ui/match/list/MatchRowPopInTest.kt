package com.maxeydev.picklejot.ui.match.list

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MatchRowPopInTest {
    @Test
    fun noSavedMatchMeansNothingPopsIn() {
        assertNull(matchRowPopInTarget(savedMatchId = null, poppedInMatchId = null))
        assertNull(matchRowPopInTarget(savedMatchId = null, poppedInMatchId = "a"))
    }

    @Test
    fun freshlySavedMatchPopsIn() {
        assertEquals("a", matchRowPopInTarget(savedMatchId = "a", poppedInMatchId = null))
        assertEquals("b", matchRowPopInTarget(savedMatchId = "b", poppedInMatchId = "a"))
    }

    @Test
    fun alreadyPoppedMatchIsNotReplayed() {
        assertNull(matchRowPopInTarget(savedMatchId = "a", poppedInMatchId = "a"))
    }
}
