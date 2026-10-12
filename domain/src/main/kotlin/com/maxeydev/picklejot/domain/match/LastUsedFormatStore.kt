package com.maxeydev.picklejot.domain.match

interface LastUsedFormatStore {
    suspend fun lastUsedFormat(): MatchFormat

    suspend fun recordLastUsedFormat(format: MatchFormat)

    fun cachedLastUsedFormat(): MatchFormat? = null

    companion object {
        val FIRST_LAUNCH_FORMAT: MatchFormat = MatchFormat.DOUBLES
    }
}
