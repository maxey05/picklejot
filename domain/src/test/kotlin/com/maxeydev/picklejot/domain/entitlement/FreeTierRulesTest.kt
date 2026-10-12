package com.maxeydev.picklejot.domain.entitlement

import com.maxeydev.picklejot.domain.profile.Entitlement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeTierRulesTest {
    private val free = Entitlement(isPro = false)
    private val pro = Entitlement(isPro = true, purchaseToken = "t")

    @Test
    fun `a free account can save up to fifty matches and no more`() {
        assertTrue(CanAddMatch.allows(0, free))
        assertTrue(CanAddMatch.allows(49, free))
        assertFalse(CanAddMatch.allows(50, free))
        assertFalse(CanAddMatch.allows(80, free))
    }

    @Test
    fun `pro removes the match cap entirely`() {
        assertTrue(CanAddMatch.allows(50, pro))
        assertTrue(CanAddMatch.allows(10_000, pro))
    }

    @Test
    fun `the cap counts saved matches so deleting one frees a slot`() {
        assertFalse(CanAddMatch.allows(50, free))
        assertTrue(CanAddMatch.allows(50 - 1, free))
    }

    @Test
    fun `a free match may hold exactly one photo`() {
        assertEquals(1, CanAddPhoto.remaining(0, free))
        assertEquals(0, CanAddPhoto.remaining(1, free))
    }

    @Test
    fun `a match that already holds several photos keeps them but cannot gain more on free`() {
        assertEquals(0, CanAddPhoto.remaining(4, free))
    }

    @Test
    fun `pro allows ten photos per match`() {
        assertEquals(ProTier.PHOTOS_PER_MATCH, CanAddPhoto.remaining(0, pro))
        assertEquals(4, CanAddPhoto.remaining(6, pro))
    }

    @Test
    fun `a pro match at ten photos cannot gain more`() {
        assertEquals(0, CanAddPhoto.remaining(10, pro))
        assertEquals(0, CanAddPhoto.remaining(25, pro))
    }

    @Test
    fun `the first warning appears at forty and the prominent one at forty eight`() {
        assertEquals(CapWarning.NONE, CapWarning.forCount(39, free))
        assertEquals(CapWarning.APPROACHING, CapWarning.forCount(40, free))
        assertEquals(CapWarning.APPROACHING, CapWarning.forCount(47, free))
        assertEquals(CapWarning.IMMINENT, CapWarning.forCount(48, free))
        assertEquals(CapWarning.IMMINENT, CapWarning.forCount(50, free))
        assertEquals(CapWarning.IMMINENT, CapWarning.forCount(80, free))
    }

    @Test
    fun `pro never sees a warning`() {
        listOf(40, 48, 50, 200).forEach { assertEquals(CapWarning.NONE, CapWarning.forCount(it, pro)) }
    }

    @Test
    fun `the remaining count is what is left of fifty and never negative`() {
        assertEquals(10, CapWarning.remainingFreeMatches(40))
        assertEquals(2, CapWarning.remainingFreeMatches(48))
        assertEquals(0, CapWarning.remainingFreeMatches(50))
        assertEquals(0, CapWarning.remainingFreeMatches(80))
    }

    @Test
    fun `every count from forty up to the wall shows a warning so reaching fifty one is never a surprise`() {
        (40..50).forEach { assertTrue("no warning at $it", CapWarning.forCount(it, free) != CapWarning.NONE) }
    }
}
