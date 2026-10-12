package com.maxeydev.picklejot.ui.share

import com.maxeydev.picklejot.domain.share.CardLayout
import com.maxeydev.picklejot.domain.share.CardRatio
import com.maxeydev.picklejot.domain.share.CardTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardGoldenCoverageTest {
    @Test
    fun `every declared ratio theme and layout combination has a golden`() {
        val declared =
            CardRatio.entries.flatMap { ratio ->
                CardTheme.entries.flatMap { theme ->
                    CardLayout.entries.map { layout -> Triple(ratio, theme, layout) }
                }
            }
        val covered =
            CARD_GOLDENS
                .filter { it.content == GoldenContent.STANDARD }
                .map { Triple(it.ratio, it.theme, it.layout) }

        val missing = declared - covered.toSet()
        assertTrue("variants with no golden: $missing", missing.isEmpty())
        assertEquals("a variant combination has more than one standard golden", declared.size, covered.size)
    }

    @Test
    fun `the golden set is the deliberate eighteen and every name is unique`() {
        assertEquals(18, CARD_GOLDENS.size)
        assertEquals(CARD_GOLDENS.size, CARD_GOLDENS.map { it.name }.toSet().size)
    }

    @Test
    fun `the content edge cases run at tall dark photo only`() {
        val edgeCases = CARD_GOLDENS.filter { it.content != GoldenContent.STANDARD }

        assertEquals(setOf(GoldenContent.LONG_NAMES, GoldenContent.EMOJI_NAMES), edgeCases.map { it.content }.toSet())
        edgeCases.forEach {
            assertEquals(CardRatio.TALL, it.ratio)
            assertEquals(CardTheme.DARK, it.theme)
            assertEquals(CardLayout.PHOTO, it.layout)
        }
    }
}
