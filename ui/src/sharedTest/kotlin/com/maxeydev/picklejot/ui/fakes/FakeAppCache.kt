package com.maxeydev.picklejot.ui.fakes

import com.maxeydev.picklejot.domain.settings.AppCache

class FakeAppCache(
    var bytes: Long = 0L,
) : AppCache {
    var clearCount = 0
        private set

    override suspend fun sizeBytes(): Long = bytes

    override suspend fun clear() {
        clearCount++
        bytes = 0L
    }
}
