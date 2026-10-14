package com.maxeydev.picklejot.ui.settings

sealed interface LegalBlock {
    data class Heading(
        val level: Int,
        val text: String,
    ) : LegalBlock

    data class Paragraph(
        val text: String,
    ) : LegalBlock

    data class Bullets(
        val items: List<String>,
    ) : LegalBlock

    data class Table(
        val header: List<String>,
        val rows: List<List<String>>,
    ) : LegalBlock
}

data class LegalSpan(
    val text: String,
    val bold: Boolean = false,
    val link: String? = null,
)

object LegalMarkdown {
    private val HEADING = Regex("""^(#{1,3})\s+(.+)$""")
    private val SEPARATOR_CELL = Regex("""^:?-{3,}:?$""")
    private val INLINE =
        Regex(
            """\*\*(.+?)\*\*""" +
                """|\[([^\]]+)]\(([^)]+)\)""" +
                """|([A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,})""",
        )

    private enum class Kind { NONE, PARAGRAPH, BULLET, TABLE }

    fun parse(source: String): List<LegalBlock> {
        val blocks = mutableListOf<LegalBlock>()
        val paragraph = mutableListOf<String>()
        val bullets = mutableListOf<String>()
        val tableLines = mutableListOf<String>()
        var kind = Kind.NONE

        fun flush() {
            if (paragraph.isNotEmpty()) {
                blocks += LegalBlock.Paragraph(paragraph.joinToString(" "))
                paragraph.clear()
            }
            if (bullets.isNotEmpty()) {
                blocks += LegalBlock.Bullets(bullets.toList())
                bullets.clear()
            }
            if (tableLines.isNotEmpty()) {
                table(tableLines)?.let { blocks += it }
                tableLines.clear()
            }
            kind = Kind.NONE
        }

        for (raw in source.lines()) {
            val line = raw.trim()
            val heading = HEADING.matchEntire(line)
            when {
                line.isEmpty() -> flush()
                heading != null -> {
                    flush()
                    blocks += LegalBlock.Heading(heading.groupValues[1].length, heading.groupValues[2].trim())
                }
                line.startsWith("- ") -> {
                    if (kind != Kind.BULLET) flush()
                    bullets += line.removePrefix("- ").trim()
                    kind = Kind.BULLET
                }
                line.startsWith("|") -> {
                    if (kind != Kind.TABLE) flush()
                    tableLines += line
                    kind = Kind.TABLE
                }
                else -> {
                    if (kind != Kind.PARAGRAPH) flush()
                    paragraph += line
                    kind = Kind.PARAGRAPH
                }
            }
        }
        flush()
        return blocks
    }

    fun inline(text: String): List<LegalSpan> {
        val spans = mutableListOf<LegalSpan>()
        var cursor = 0
        for (match in INLINE.findAll(text)) {
            if (match.range.first > cursor) {
                spans += LegalSpan(text.substring(cursor, match.range.first))
            }
            val bold = match.groups[1]?.value
            val label = match.groups[2]?.value
            val url = match.groups[3]?.value
            spans +=
                when {
                    bold != null -> LegalSpan(bold, bold = true)
                    label != null && url != null -> LegalSpan(label, link = url)
                    else -> LegalSpan(match.value, link = "mailto:${match.value}")
                }
            cursor = match.range.last + 1
        }
        if (cursor < text.length) {
            spans += LegalSpan(text.substring(cursor))
        }
        return spans
    }

    private fun table(lines: List<String>): LegalBlock.Table? {
        val rows =
            lines
                .map(::cells)
                .filterNot { row -> row.all { cell -> SEPARATOR_CELL.matches(cell) } }
        if (rows.isEmpty()) return null
        return LegalBlock.Table(header = rows.first(), rows = rows.drop(1))
    }

    private fun cells(line: String): List<String> =
        line
            .trim()
            .removePrefix("|")
            .removeSuffix("|")
            .split("|")
            .map { cell -> cell.trim() }
}
