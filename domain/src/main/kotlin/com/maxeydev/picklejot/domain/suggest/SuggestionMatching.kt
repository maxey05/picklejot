package com.maxeydev.picklejot.domain.suggest

import java.text.Normalizer

const val MAX_SUGGESTIONS = 5

private val COMBINING_MARKS = Regex("\\p{Mn}+")

private val WHITESPACE_RUN = Regex("\\s+")

fun suggestionKey(text: String): String =
    Normalizer
        .normalize(text, Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .trim()
        .replace(WHITESPACE_RUN, " ")
        .lowercase()

fun matchesPrefixOrWordStart(
    candidateKey: String,
    queryKey: String,
): Boolean {
    if (queryKey.isEmpty()) {
        return false
    }
    var searchFrom = 0
    while (true) {
        val found = candidateKey.indexOf(queryKey, searchFrom)
        if (found < 0) {
            return false
        }
        if (found == 0 || !candidateKey[found - 1].isLetterOrDigit()) {
            return true
        }
        searchFrom = found + 1
    }
}
