package com.maxeydev.picklejot.data.billing

import com.maxeydev.picklejot.domain.entitlement.EntitlementRules
import com.maxeydev.picklejot.domain.entitlement.EntitlementSignal
import com.maxeydev.picklejot.domain.profile.Entitlement
import com.maxeydev.picklejot.domain.profile.EntitlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class InMemoryEntitlementRepository(
    initial: Entitlement = Entitlement(isPro = false),
) : EntitlementRepository {
    private val state = MutableStateFlow(initial)

    val signals = mutableListOf<EntitlementSignal>()

    val current: Entitlement
        get() = state.value

    override fun observeEntitlement(): Flow<Entitlement> = state

    override suspend fun record(signal: EntitlementSignal) {
        signals += signal
        state.update { EntitlementRules.next(it, signal) }
    }
}
