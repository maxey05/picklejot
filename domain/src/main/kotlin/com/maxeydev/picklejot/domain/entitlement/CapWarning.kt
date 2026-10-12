package com.maxeydev.picklejot.domain.entitlement

import com.maxeydev.picklejot.domain.profile.Entitlement

enum class CapWarning {
    NONE,
    APPROACHING,
    IMMINENT,
    ;

    companion object {
        fun forCount(
            savedMatches: Int,
            entitlement: Entitlement,
        ): CapWarning =
            when {
                entitlement.isPro -> NONE
                savedMatches >= FreeTier.FINAL_WARNING_AT -> IMMINENT
                savedMatches >= FreeTier.FIRST_WARNING_AT -> APPROACHING
                else -> NONE
            }

        fun remainingFreeMatches(savedMatches: Int): Int = (FreeTier.MATCH_LIMIT - savedMatches).coerceAtLeast(0)
    }
}
