package com.maxeydev.picklejot.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class BillingClientWrapper(
    context: Context,
    private val backoff: Backoff = Backoff(),
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : BillingGateway,
    PurchasesUpdatedListener {
    private val client: BillingClient =
        BillingClient
            .newBuilder(context.applicationContext)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()

    private val connection = Mutex()
    private val purchaseFlow = Mutex()

    @Volatile
    private var inFlightPurchase: CompletableDeferred<GatewayResult<List<PlayPurchase>>>? = null

    @Volatile
    private var unsolicitedListener: ((List<PlayPurchase>) -> Unit)? = null

    @Volatile
    private var cachedDetails: ProductDetails? = null

    override fun onUnsolicitedPurchases(listener: (List<PlayPurchase>) -> Unit) {
        unsolicitedListener = listener
    }

    override fun onPurchasesUpdated(
        result: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        val converted = purchases.orEmpty().map(::toPlayPurchase)
        val response = BillingResponse.fromCode(result.responseCode)
        val waiting = inFlightPurchase
        if (waiting != null && waiting.isActive) {
            waiting.complete(gatewayResult(response) { converted })
        } else if (response == BillingResponse.OK && converted.isNotEmpty()) {
            unsolicitedListener?.invoke(converted)
        }
    }

    override suspend fun queryPurchases(): GatewayResult<List<PlayPurchase>> =
        connected {
            suspendCancellableCoroutine { continuation ->
                val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
                client.queryPurchasesAsync(params) { result, purchases ->
                    val response = BillingResponse.fromCode(result.responseCode)
                    continuation.deliver(gatewayResult(response) { purchases.map(::toPlayPurchase) })
                }
            }
        }

    override suspend fun acknowledge(purchaseToken: String): BillingResponse {
        val result =
            connected<Unit> {
                suspendCancellableCoroutine { continuation ->
                    val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build()
                    client.acknowledgePurchase(params) { result ->
                        val response = BillingResponse.fromCode(result.responseCode)
                        continuation.deliver(gatewayResult(response) { })
                    }
                }
            }
        return when (result) {
            is GatewayResult.Success -> BillingResponse.OK
            is GatewayResult.Failure -> result.response
        }
    }

    override suspend fun proPrice(): GatewayResult<String> =
        when (val details = proDetails()) {
            is GatewayResult.Failure -> details
            is GatewayResult.Success ->
                details.value.oneTimePurchaseOfferDetails?.formattedPrice
                    ?.let { GatewayResult.Success(it) }
                    ?: GatewayResult.Failure(BillingResponse.ITEM_UNAVAILABLE)
        }

    override suspend fun launchProPurchase(activity: Activity): GatewayResult<List<PlayPurchase>> =
        purchaseFlow.withLock {
            when (val details = proDetails()) {
                is GatewayResult.Failure -> details
                is GatewayResult.Success -> launch(activity, details.value)
            }
        }

    private suspend fun launch(
        activity: Activity,
        details: ProductDetails,
    ): GatewayResult<List<PlayPurchase>> {
        val waiting = CompletableDeferred<GatewayResult<List<PlayPurchase>>>()
        inFlightPurchase = waiting
        try {
            val launched =
                connected<Unit> {
                    val result = withContext(mainDispatcher) { client.launchBillingFlow(activity, flowParams(details)) }
                    gatewayResult(BillingResponse.fromCode(result.responseCode)) { }
                }
            return when (launched) {
                is GatewayResult.Failure -> launched
                is GatewayResult.Success -> waiting.await()
            }
        } finally {
            inFlightPurchase = null
        }
    }

    private fun flowParams(details: ProductDetails): BillingFlowParams {
        val product = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details)
        details.oneTimePurchaseOfferDetailsList
            ?.firstOrNull()
            ?.offerToken
            ?.let { product.setOfferToken(it) }
        return BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(product.build())).build()
    }

    private suspend fun proDetails(): GatewayResult<ProductDetails> {
        cachedDetails?.let { return GatewayResult.Success(it) }
        val result =
            connected<ProductDetails> {
                suspendCancellableCoroutine { continuation ->
                    val product =
                        QueryProductDetailsParams.Product
                            .newBuilder()
                            .setProductId(PRO_PRODUCT_ID)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
                    client.queryProductDetailsAsync(params) { result, productDetails ->
                        val response = BillingResponse.fromCode(result.responseCode)
                        val details = productDetails.productDetailsList.firstOrNull { it.productId == PRO_PRODUCT_ID }
                        continuation.deliver(
                            when {
                                response != BillingResponse.OK -> GatewayResult.Failure(response)
                                details == null -> GatewayResult.Failure(BillingResponse.ITEM_UNAVAILABLE)
                                else -> GatewayResult.Success(details)
                            },
                        )
                    }
                }
            }
        if (result is GatewayResult.Success) {
            cachedDetails = result.value
        }
        return result
    }

    private suspend fun <T> connected(call: suspend () -> GatewayResult<T>): GatewayResult<T> =
        backoff.retrying {
            val ready = connect()
            if (ready == BillingResponse.OK) call() else GatewayResult.Failure(ready)
        }

    private suspend fun connect(): BillingResponse =
        connection.withLock {
            if (client.isReady) {
                return@withLock BillingResponse.OK
            }
            suspendCancellableCoroutine { continuation ->
                client.startConnection(
                    object : BillingClientStateListener {
                        override fun onBillingSetupFinished(result: BillingResult) {
                            continuation.deliver(BillingResponse.fromCode(result.responseCode))
                        }

                        override fun onBillingServiceDisconnected() {
                            continuation.deliver(BillingResponse.SERVICE_DISCONNECTED)
                        }
                    },
                )
            }
        }

    private fun toPlayPurchase(purchase: Purchase): PlayPurchase =
        PlayPurchase(
            purchaseToken = purchase.purchaseToken,
            productIds = purchase.products,
            state =
                when (purchase.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> PlayPurchaseState.PURCHASED
                    Purchase.PurchaseState.PENDING -> PlayPurchaseState.PENDING
                    else -> PlayPurchaseState.UNSPECIFIED
                },
            isAcknowledged = purchase.isAcknowledged,
            purchasedAtMillis = purchase.purchaseTime,
        )
}

private fun <T> CancellableContinuation<T>.deliver(value: T) {
    if (isActive) {
        resume(value)
    }
}

private fun <T> gatewayResult(
    response: BillingResponse,
    value: () -> T,
): GatewayResult<T> =
    if (response == BillingResponse.OK) {
        GatewayResult.Success(value())
    } else {
        GatewayResult.Failure(response)
    }
