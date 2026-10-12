package com.maxeydev.picklejot.ui.match

private val WHITESPACE = Regex("\\s+")

fun playerInitials(displayName: String): String {
    val words = displayName.trim().split(WHITESPACE).filter { it.isNotEmpty() }
    return when (words.size) {
        0 -> ""
        1 -> firstSymbol(words.first())
        else -> firstSymbol(words.first()) + firstSymbol(words.last())
    }
}

private fun firstSymbol(word: String): String = word.substring(0, word.offsetByCodePoints(0, 1)).uppercase()
