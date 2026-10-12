package com.maxeydev.picklejot.domain.entitlement

import com.maxeydev.picklejot.domain.profile.Entitlement
import com.maxeydev.picklejot.domain.profile.EntitlementRepository
import kotlinx.coroutines.flow.first

class CanAddPhoto(
    private val entitlements: EntitlementRepository,
) {
    suspend fun remainingFor(photosOnMatch: Int): Int =
        remaining(photosOnMatch, entitlements.observeEntitlement().first())

    suspend fun hasPro(): Boolean = entitlements.observeEntitlement().first().isPro

    companion object {
        fun limitFor(entitlement: Entitlement): Int =
            if (entitlement.isPro) {
                ProTier.PHOTOS_PER_MATCH
            } else {
                FreeTier.PHOTOS_PER_MATCH
            }

        fun remaining(
            photosOnMatch: Int,
            entitlement: Entitlement,
        ): Int = (limitFor(entitlement) - photosOnMatch).coerceAtLeast(0)
    }
}
