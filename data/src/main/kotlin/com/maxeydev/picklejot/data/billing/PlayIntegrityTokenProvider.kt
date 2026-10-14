package com.maxeydev.picklejot.data.billing

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

fun interface IntegrityTokenProvider {
    /** A Play Integrity token whose nonce is [nonce], or null if Play could not issue one. */
    suspend fun token(nonce: String): String?
}

class PlayIntegrityTokenProvider(
    context: Context,
) : IntegrityTokenProvider {
    private val manager = IntegrityManagerFactory.create(context.applicationContext)

    // The timeout matters: this runs under PlayEntitlementRepository's mutex, so a Task that never
    // completes would hang every later purchase and restore.
    override suspend fun token(nonce: String): String? =
        withTimeoutOrNull(TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                manager
                    .requestIntegrityToken(IntegrityTokenRequest.builder().setNonce(nonce).build())
                    .addOnSuccessListener { if (continuation.isActive) continuation.resume(it.token()) }
                    .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
                    .addOnCanceledListener { if (continuation.isActive) continuation.resume(null) }
            }
        }

    private companion object {
        const val TIMEOUT_MILLIS = 30_000L
    }
}
