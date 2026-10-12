package com.maxeydev.picklejot.domain.person

import java.text.Normalizer

private val WHITESPACE_RUN = Regex("\\s+")

fun normalizePersonName(displayName: String): String =
    Normalizer
        .normalize(displayName, Normalizer.Form.NFC)
        .trim()
        .replace(WHITESPACE_RUN, " ")
        .lowercase()
