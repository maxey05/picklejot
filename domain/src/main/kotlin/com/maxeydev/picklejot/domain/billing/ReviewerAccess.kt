package com.maxeydev.picklejot.domain.billing

fun interface ReviewerAccess {
    suspend fun redeem(code: String): ReviewerOutcome
}

sealed interface ReviewerOutcome {
    data object Unlocked : ReviewerOutcome

    data object WrongCode : ReviewerOutcome

    data object Unavailable : ReviewerOutcome
}
