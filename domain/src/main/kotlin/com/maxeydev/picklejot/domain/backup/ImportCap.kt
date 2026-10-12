@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.domain.backup

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.entitlement.FreeTier
import com.maxeydev.picklejot.domain.profile.Entitlement
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class ImportCandidate(
    val id: Uuid,
    val date: AppDate,
    val createdAt: AppInstant,
)

data class ImportSelection(
    val accepted: Set<Uuid>,
    val heldBack: Int,
)

object ImportCap {
    private val newestFirst =
        compareByDescending<ImportCandidate> { it.date }
            .thenByDescending { it.createdAt }
            .thenBy { it.id.toString() }

    fun select(
        candidates: List<ImportCandidate>,
        existingCount: Int,
        entitlement: Entitlement,
    ): ImportSelection {
        if (entitlement.isPro) {
            return ImportSelection(accepted = candidates.map { it.id }.toSet(), heldBack = 0)
        }
        val freeSlots = (FreeTier.MATCH_LIMIT - existingCount).coerceAtLeast(0)
        val accepted = candidates.sortedWith(newestFirst).take(freeSlots).map { it.id }.toSet()
        return ImportSelection(accepted = accepted, heldBack = candidates.size - accepted.size)
    }
}
