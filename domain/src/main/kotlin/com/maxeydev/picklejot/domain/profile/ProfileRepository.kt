package com.maxeydev.picklejot.domain.profile

import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile>

    suspend fun updateDisplayName(displayName: String)
}
