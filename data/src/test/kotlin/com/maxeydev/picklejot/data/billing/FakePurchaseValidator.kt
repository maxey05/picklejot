package com.maxeydev.picklejot.data.billing

class FakePurchaseValidator(
    var result: ValidationResult = ValidationResult.VALID,
) : PurchaseValidator {
    val validated = mutableListOf<String>()

    override suspend fun validate(purchase: PlayPurchase): ValidationResult {
        validated += purchase.purchaseToken
        return result
    }
}
