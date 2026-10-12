package com.maxeydev.picklejot.domain.person

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.Locale

class NameNormalizerTest {
    @Test
    fun `precomposed and decomposed accents normalize to the same value`() {
        val precomposed = "José"
        val decomposed = "José"
        assertNotEquals(precomposed, decomposed)
        assertEquals(normalizePersonName(precomposed), normalizePersonName(decomposed))
    }

    @Test
    fun `normalization is locale invariant`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"))
            val underTurkish = normalizePersonName("Iİi Istanbul")
            Locale.setDefault(Locale.US)
            val underUnitedStates = normalizePersonName("Iİi Istanbul")
            assertEquals(underUnitedStates, underTurkish)
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `case differences collapse to one name`() {
        assertEquals(normalizePersonName("Dave"), normalizePersonName("DAVE"))
        assertEquals(normalizePersonName("Dave"), normalizePersonName("dave"))
    }

    @Test
    fun `surrounding and repeated whitespace is collapsed`() {
        assertEquals("dave rivera", normalizePersonName("  Dave   Rivera \t"))
    }

    @Test
    fun `punctuation is preserved so Dave R is not merged into Dave`() {
        assertNotEquals(normalizePersonName("Dave R."), normalizePersonName("Dave"))
    }

    @Test
    fun `a whitespace only name normalizes to empty`() {
        assertEquals("", normalizePersonName(" \t "))
    }
}
