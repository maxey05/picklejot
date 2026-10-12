package com.maxeydev.picklejot.domain.match

import com.maxeydev.picklejot.domain.person.normalizePersonName
import java.text.Normalizer

const val LIKE_ESCAPE_CHARACTER = '\\'

class SearchTerm private constructor(
    val text: String,
) {
    val textPattern: String = containsPattern(text)

    val namePattern: String = containsPattern(normalizePersonName(text))

    override fun equals(other: Any?): Boolean = other is SearchTerm && other.text == text

    override fun hashCode(): Int = text.hashCode()

    override fun toString(): String = "SearchTerm($text)"

    companion object {
        fun of(raw: String): SearchTerm? {
            val text = Normalizer.normalize(raw, Normalizer.Form.NFC).trim()
            return if (text.isEmpty()) null else SearchTerm(text)
        }
    }
}

fun escapeLikeWildcards(value: String): String {
    val escaped = StringBuilder(value.length)
    value.forEach { character ->
        if (character == LIKE_ESCAPE_CHARACTER || character == '%' || character == '_') {
            escaped.append(LIKE_ESCAPE_CHARACTER)
        }
        escaped.append(character)
    }
    return escaped.toString()
}

private fun containsPattern(value: String): String = "%" + escapeLikeWildcards(value) + "%"
