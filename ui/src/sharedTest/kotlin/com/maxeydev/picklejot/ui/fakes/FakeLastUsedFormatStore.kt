package com.maxeydev.picklejot.ui.fakes

import com.maxeydev.picklejot.domain.match.LastUsedFormatStore
import com.maxeydev.picklejot.domain.match.MatchFormat

class FakeLastUsedFormatStore(
    var format: MatchFormat = LastUsedFormatStore.FIRST_LAUNCH_FORMAT,
    var cached: MatchFormat? = null,
) : LastUsedFormatStore {
    val recorded = mutableListOf<MatchFormat>()

    override suspend fun lastUsedFormat(): MatchFormat = format

    override fun cachedLastUsedFormat(): MatchFormat? = cached

    override suspend fun recordLastUsedFormat(format: MatchFormat) {
        recorded += format
        this.format = format
    }
}
