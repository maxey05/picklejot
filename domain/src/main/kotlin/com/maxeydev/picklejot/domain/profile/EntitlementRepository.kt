package com.maxeydev.picklejot.domain.profile

import com.maxeydev.picklejot.domain.entitlement.EntitlementSignal
import kotlinx.coroutines.flow.Flow

interface EntitlementRepository {
    fun observeEntitlement(): Flow<Entitlement>

    suspend fun record(signal: EntitlementSignal)
}
