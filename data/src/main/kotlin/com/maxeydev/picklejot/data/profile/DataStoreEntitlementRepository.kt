package com.maxeydev.picklejot.data.profile

import androidx.datastore.core.DataStore
import com.maxeydev.picklejot.domain.entitlement.EntitlementRules
import com.maxeydev.picklejot.domain.entitlement.EntitlementSignal
import com.maxeydev.picklejot.domain.profile.Entitlement
import com.maxeydev.picklejot.domain.profile.EntitlementRepository
import com.maxeydev.picklejot.domain.profile.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DataStoreEntitlementRepository(
    private val dataStore: DataStore<UserProfile>,
) : EntitlementRepository {
    override fun observeEntitlement(): Flow<Entitlement> =
        dataStore.data.map { profile -> profile.entitlement }.distinctUntilChanged()

    override suspend fun record(signal: EntitlementSignal) {
        dataStore.updateData { profile ->
            val next = EntitlementRules.next(profile.entitlement, signal)
            if (next == profile.entitlement) profile else profile.copy(entitlement = next)
        }
    }
}
