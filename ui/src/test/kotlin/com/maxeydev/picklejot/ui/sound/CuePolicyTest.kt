package com.maxeydev.picklejot.ui.sound

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CuePolicyTest {
    private val policy = CuePolicy()

    private fun CueDecision.isPlay(): Boolean = this is CueDecision.Play

    @Test
    fun `tap cues are dropped while touch exploration is on`() {
        assertEquals(CueDecision.Drop, policy.decide(Cue.POCK, 0, touchExploration = true))
        assertTrue(policy.decide(Cue.MATCH_SAVED, 0, touchExploration = true).isPlay())
    }

    @Test
    fun `the same cue inside its minimum gap is dropped`() {
        assertTrue(policy.decide(Cue.POCK, 1_000, touchExploration = false).isPlay())
        assertEquals(CueDecision.Drop, policy.decide(Cue.POCK, 1_079, touchExploration = false))
        assertTrue(policy.decide(Cue.POCK, 1_080, touchExploration = false).isPlay())
    }

    @Test
    fun `duck cues use the longer gap`() {
        assertTrue(policy.decide(Cue.DUCK_PET_1, 0, touchExploration = false).isPlay())
        assertEquals(CueDecision.Drop, policy.decide(Cue.DUCK_PET_1, 119, touchExploration = false))
        assertTrue(policy.decide(Cue.DUCK_PET_1, 120, touchExploration = false).isPlay())
    }

    @Test
    fun `different cues do not share a gap`() {
        assertTrue(policy.decide(Cue.TICK_SELECT, 0, touchExploration = false).isPlay())
        assertTrue(policy.decide(Cue.TICK_CLEAR, 10, touchExploration = false).isPlay())
    }

    @Test
    fun `a confirmation is dropped while a celebration is playing and allowed after it ends`() {
        assertTrue(policy.decide(Cue.PRO_UNLOCKED, 0, touchExploration = false).isPlay())
        assertEquals(CueDecision.Drop, policy.decide(Cue.MATCH_SAVED, 1_000, touchExploration = false))
        assertTrue(policy.decide(Cue.MATCH_SAVED, Cue.PRO_UNLOCKED.durationMillis, touchExploration = false).isPlay())
    }

    @Test
    fun `taps are never blocked by a celebration`() {
        assertTrue(policy.decide(Cue.STREAK_MILESTONE, 0, touchExploration = false).isPlay())
        assertTrue(policy.decide(Cue.TICK_SELECT, 100, touchExploration = false).isPlay())
    }

    @Test
    fun `a higher ranked celebration replaces a lower one`() {
        assertTrue(policy.decide(Cue.STREAK_UP, 0, touchExploration = false).isPlay())
        val decision = policy.decide(Cue.STREAK_MILESTONE, 100, touchExploration = false)
        assertEquals(CueDecision.Play(volume = 1f, stopsActiveCelebration = true), decision)
    }

    @Test
    fun `a lower ranked celebration is dropped while a higher one is playing`() {
        assertTrue(policy.decide(Cue.STREAK_MILESTONE, 0, touchExploration = false).isPlay())
        assertEquals(CueDecision.Drop, policy.decide(Cue.STREAK_UP, 100, touchExploration = false))
        assertEquals(CueDecision.Drop, policy.decide(Cue.FIRST_MATCH, 200, touchExploration = false))
    }

    @Test
    fun `a celebration plays again after the previous one has finished`() {
        assertTrue(policy.decide(Cue.STREAK_UP, 0, touchExploration = false).isPlay())
        val later = Cue.STREAK_UP.durationMillis + 1
        assertEquals(
            CueDecision.Play(volume = 1f, stopsActiveCelebration = false),
            policy.decide(Cue.FIRST_MATCH, later, touchExploration = false),
        )
    }

    @Test
    fun `celebrations are ducked while touch exploration is on`() {
        val decision = policy.decide(Cue.PRO_UNLOCKED, 0, touchExploration = true)
        assertEquals(CueDecision.Play(volume = 0.6f, stopsActiveCelebration = false), decision)
    }

    @Test
    fun `milestone cues outrank the streak burst and the first match`() {
        assertTrue(Cue.STREAK_MILESTONE.rank > Cue.FIRST_MATCH.rank)
        assertTrue(Cue.FIRST_MATCH.rank > Cue.STREAK_UP.rank)
        assertTrue(Cue.PRO_UNLOCKED.rank > Cue.PRO_RESTORED.rank)
        assertTrue(Cue.PRO_RESTORED.rank > Cue.STREAK_MILESTONE.rank)
    }
}
