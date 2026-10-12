package com.maxeydev.picklejot.domain.suggest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionMatchingTest {
    @Test
    fun `the key ignores case accents and repeated whitespace`() {
        assertEquals("jose garcia", suggestionKey("  JOSÉ   García "))
        assertEquals("jose garcia", suggestionKey("José García"))
    }

    @Test
    fun `a prefix of the whole name matches`() {
        assertTrue(matchesPrefixOrWordStart("dave r.", "da"))
        assertTrue(matchesPrefixOrWordStart("dave r.", "dave r"))
    }

    @Test
    fun `the start of a later word matches`() {
        assertTrue(matchesPrefixOrWordStart("dave r.", "r"))
        assertTrue(matchesPrefixOrWordStart("mary-jane", "jane"))
        assertTrue(matchesPrefixOrWordStart("bgc (court 3)", "court"))
    }

    @Test
    fun `text in the middle of a word does not match`() {
        assertFalse(matchesPrefixOrWordStart("dave r.", "av"))
        assertFalse(matchesPrefixOrWordStart("mary-jane", "ane"))
    }

    @Test
    fun `a later word start is found even when an earlier mid-word hit comes first`() {
        assertTrue(matchesPrefixOrWordStart("arran ran", "ran"))
    }

    @Test
    fun `an empty query never matches`() {
        assertFalse(matchesPrefixOrWordStart("dave", ""))
    }
}
