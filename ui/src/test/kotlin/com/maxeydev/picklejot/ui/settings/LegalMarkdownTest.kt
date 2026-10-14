package com.maxeydev.picklejot.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class LegalMarkdownTest {
    @Test
    fun headings_keep_their_level_and_text() {
        val blocks = LegalMarkdown.parse("# Title\n\n## 1. Who we are\n")

        assertEquals(
            listOf(LegalBlock.Heading(1, "Title"), LegalBlock.Heading(2, "1. Who we are")),
            blocks,
        )
    }

    @Test
    fun consecutive_lines_join_into_one_paragraph() {
        val blocks = LegalMarkdown.parse("First line\nsecond line\n\nNext paragraph")

        assertEquals(
            listOf(LegalBlock.Paragraph("First line second line"), LegalBlock.Paragraph("Next paragraph")),
            blocks,
        )
    }

    @Test
    fun bullet_lines_group_into_one_list() {
        val blocks = LegalMarkdown.parse("- one\n- two\n- three\n")

        assertEquals(listOf(LegalBlock.Bullets(listOf("one", "two", "three"))), blocks)
    }

    @Test
    fun a_table_drops_its_separator_row() {
        val blocks = LegalMarkdown.parse("| What | Why |\n| --- | --- |\n| Camera | Photos |\n| Alerts | Reminders |\n")

        assertEquals(
            listOf(
                LegalBlock.Table(
                    header = listOf("What", "Why"),
                    rows = listOf(listOf("Camera", "Photos"), listOf("Alerts", "Reminders")),
                ),
            ),
            blocks,
        )
    }

    @Test
    fun a_bullet_list_directly_after_a_paragraph_starts_a_new_block() {
        val blocks = LegalMarkdown.parse("You may not:\n- copy it\n- sell it\n")

        assertEquals(
            listOf(LegalBlock.Paragraph("You may not:"), LegalBlock.Bullets(listOf("copy it", "sell it"))),
            blocks,
        )
    }

    @Test
    fun bold_text_becomes_a_bold_span() {
        val spans = LegalMarkdown.inline("This is **important** news")

        assertEquals(
            listOf(LegalSpan("This is "), LegalSpan("important", bold = true), LegalSpan(" news")),
            spans,
        )
    }

    @Test
    fun a_markdown_link_becomes_a_link_span() {
        val spans = LegalMarkdown.inline("See [Google's Privacy Policy](https://policies.google.com/privacy).")

        assertEquals(
            listOf(
                LegalSpan("See "),
                LegalSpan("Google's Privacy Policy", link = "https://policies.google.com/privacy"),
                LegalSpan("."),
            ),
            spans,
        )
    }

    @Test
    fun an_email_address_becomes_a_mailto_link_without_the_trailing_full_stop() {
        val spans = LegalMarkdown.inline("Write to emgireyes.dev@gmail.com.")

        assertEquals(
            listOf(
                LegalSpan("Write to "),
                LegalSpan("emgireyes.dev@gmail.com", link = "mailto:emgireyes.dev@gmail.com"),
                LegalSpan("."),
            ),
            spans,
        )
    }

    @Test
    fun plain_text_stays_a_single_span() {
        assertEquals(listOf(LegalSpan("Nothing special here")), LegalMarkdown.inline("Nothing special here"))
    }
}
