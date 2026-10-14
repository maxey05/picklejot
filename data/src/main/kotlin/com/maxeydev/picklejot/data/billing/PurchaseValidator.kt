package com.maxeydev.picklejot.data.billing

/**
 * Outcome of asking the verifier whether a Play purchase is genuine.
 *
 * Only [VALID] grants and only [REFUNDED] revokes (R58). [REJECTED] means "do not grant" and
 * changes nothing already stored; [UNREACHABLE] means "could not check" and changes nothing.
 */
enum class ValidationResult { VALID, REFUNDED, REJECTED, UNREACHABLE }

fun interface PurchaseValidator {
    suspend fun validate(purchase: PlayPurchase): ValidationResult
}
