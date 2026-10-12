package com.maxeydev.picklejot.domain.share

import org.junit.Assert.assertEquals
import org.junit.Test

class CardFormatTest {
    @Test
    fun `a match with a photo gets the photo layout by default`() {
        assertEquals(CardLayout.PHOTO, CardFormat.DEFAULT.layoutFor(hasPhoto = true))
    }

    @Test
    fun `a match without a photo always gets the no-photo layout`() {
        assertEquals(CardLayout.NO_PHOTO, CardFormat.DEFAULT.layoutFor(hasPhoto = false))
        assertEquals(CardLayout.NO_PHOTO, CardFormat(layoutOverride = CardLayout.PHOTO).layoutFor(hasPhoto = false))
    }

    @Test
    fun `a manual override can hide a photo the match has`() {
        assertEquals(CardLayout.NO_PHOTO, CardFormat(layoutOverride = CardLayout.NO_PHOTO).layoutFor(hasPhoto = true))
    }

    @Test
    fun `the two original themes are free and the two added themes are pro`() {
        assertEquals(setOf(CardTheme.DARK, CardTheme.LIGHT), CardTheme.entries.filterNot { it.requiresPro }.toSet())
        assertEquals(setOf(CardTheme.COURT, CardTheme.SUNSET), CardTheme.entries.filter { it.requiresPro }.toSet())
    }

    @Test
    fun `a free user is shown a stored pro theme as dark and keeps every other choice`() {
        val stored = CardFormat(CardRatio.SQUARE, CardTheme.COURT, CardLayout.NO_PHOTO)

        assertEquals(CardFormat(CardRatio.SQUARE, CardTheme.DARK, CardLayout.NO_PHOTO), stored.forEntitlement(false))
    }

    @Test
    fun `a pro user keeps a pro theme`() {
        val stored = CardFormat(theme = CardTheme.SUNSET)

        assertEquals(stored, stored.forEntitlement(true))
    }

    @Test
    fun `a free theme is never changed for anyone`() {
        val stored = CardFormat(theme = CardTheme.LIGHT)

        assertEquals(stored, stored.forEntitlement(false))
        assertEquals(stored, stored.forEntitlement(true))
    }

    @Test
    fun `the default is the tall dark story card`() {
        assertEquals(CardRatio.TALL, CardFormat.DEFAULT.ratio)
        assertEquals(CardTheme.DARK, CardFormat.DEFAULT.theme)
    }
}
