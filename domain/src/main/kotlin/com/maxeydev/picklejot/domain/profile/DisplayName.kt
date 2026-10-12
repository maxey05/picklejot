package com.maxeydev.picklejot.domain.profile

object DisplayName {
    const val MAX_LENGTH = 30

    fun clean(raw: String): String = raw.trim()

    fun limit(raw: String): String {
        if (raw.length <= MAX_LENGTH) {
            return raw
        }
        val cut = raw.take(MAX_LENGTH)
        return if (cut.last().isHighSurrogate()) cut.dropLast(1) else cut
    }

    fun isValid(raw: String): Boolean {
        val name = clean(raw)
        return name.isNotEmpty() && name.length <= MAX_LENGTH
    }
}
