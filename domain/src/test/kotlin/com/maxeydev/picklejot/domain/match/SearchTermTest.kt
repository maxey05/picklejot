package com.maxeydev.picklejot.domain.match

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchTermTest {
    @Test
    fun `blank input is no search at all`() {
        assertNull(SearchTerm.of(""))
        assertNull(SearchTerm.of("   \t "))
    }

    @Test
    fun `surrounding whitespace is ignored`() {
        assertEquals("ana", SearchTerm.of("  ana  ")?.text)
    }

    @Test
    fun `the text pattern matches the term anywhere as a substring`() {
        assertEquals("%Ayala%", SearchTerm.of("Ayala")?.textPattern)
    }

    @Test
    fun `the name pattern uses the same normalization as stored person names`() {
        assertEquals("%peña cruz%", SearchTerm.of("  PEÑA   Cruz ")?.namePattern)
    }

    @Test
    fun `like wildcards typed by the user are matched literally`() {
        assertEquals("%100\\%%", SearchTerm.of("100%")?.textPattern)
        assertEquals("%a\\_b%", SearchTerm.of("a_b")?.textPattern)
        assertEquals("%c:\\\\d%", SearchTerm.of("c:\\d")?.textPattern)
    }

    @Test
    fun `decomposed accents are composed so they match stored text`() {
        val decomposed = "Pen\u0303a"

        assertEquals("Peña", SearchTerm.of(decomposed)?.text)
    }

    @Test
    fun `two terms with the same text are equal`() {
        assertEquals(SearchTerm.of("ana"), SearchTerm.of(" ana"))
    }
}
