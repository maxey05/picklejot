package com.maxeydev.picklejot.domain.settings

interface AppCache {
    suspend fun sizeBytes(): Long

    suspend fun clear()
}
