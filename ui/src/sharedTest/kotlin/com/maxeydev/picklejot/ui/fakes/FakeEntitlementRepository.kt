package com.maxeydev.picklejot.ui.fakes

import com.maxeydev.picklejot.domain.entitlement.EntitlementRules
import com.maxeydev.picklejot.domain.entitlement.EntitlementSignal
import com.maxeydev.picklejot.domain.profile.Entitlement
import com.maxeydev.picklejot.domain.profile.EntitlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.CopyOnWriteArrayList

class FakeEntitlementRepository(
    isPro: Boolean = false,
) : EntitlementRepository {
    private val entitlement =
        MutableStateFlow(Entitlement(isPro = isPro, purchaseToken = if (isPro) FAKE_PURCHASE_TOKEN else null))

    val recorded: MutableList<EntitlementSignal> = CopyOnWriteArrayList()

    val current: Entitlement
        get() = entitlement.value

    override fun observeEntitlement(): Flow<Entitlement> = entitlement

    override suspend fun record(signal: EntitlementSignal) {
        recorded += signal
        entitlement.update { EntitlementRules.next(it, signal) }
    }

    companion object {
        const val FAKE_PURCHASE_TOKEN = "fake-purchase-token"
    }
}
