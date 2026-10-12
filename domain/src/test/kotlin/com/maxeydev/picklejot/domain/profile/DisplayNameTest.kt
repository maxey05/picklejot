package com.maxeydev.picklejot.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayNameTest {
    @Test
    fun `an ordinary name is valid`() {
        assertTrue(DisplayName.isValid("Matthew"))
    }

    @Test
    fun `an empty or whitespace only name is not valid`() {
        assertFalse(DisplayName.isValid(""))
        assertFalse(DisplayName.isValid("   "))
        assertFalse(DisplayName.isValid("\t\n"))
    }

    @Test
    fun `surrounding whitespace is ignored when checking and cleaning`() {
        assertTrue(DisplayName.isValid("  Matthew  "))
        assertEquals("Matthew", DisplayName.clean("  Matthew  "))
    }

    @Test
    fun `a name of exactly the limit is valid and one over is not`() {
        assertTrue(DisplayName.isValid("x".repeat(DisplayName.MAX_LENGTH)))
        assertFalse(DisplayName.isValid("x".repeat(DisplayName.MAX_LENGTH + 1)))
    }

    @Test
    fun `limit cuts a long name at the maximum`() {
        assertEquals(DisplayName.MAX_LENGTH, DisplayName.limit("x".repeat(DisplayName.MAX_LENGTH + 20)).length)
    }

    @Test
    fun `limit leaves a short name alone`() {
        assertEquals("Matthew", DisplayName.limit("Matthew"))
    }

    @Test
    fun `limit never leaves half of an emoji at the cut`() {
        val grinning = "😀"
        val raw = "x".repeat(DisplayName.MAX_LENGTH - 1) + grinning

        val limited = DisplayName.limit(raw)

        assertEquals("x".repeat(DisplayName.MAX_LENGTH - 1), limited)
    }
}
