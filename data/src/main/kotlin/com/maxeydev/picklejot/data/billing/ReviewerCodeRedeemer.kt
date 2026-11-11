package com.maxeydev.picklejot.data.billing

import com.maxeydev.picklejot.domain.billing.ReviewerAccess
import com.maxeydev.picklejot.domain.billing.ReviewerOutcome
import com.maxeydev.picklejot.domain.entitlement.EntitlementSignal
import com.maxeydev.picklejot.domain.profile.EntitlementRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.security.SecureRandom
import java.util.Base64
import kotlin.time.Clock

class ReviewerCodeRedeemer(
    private val config: LicenseServerConfig,
    private val packageName: String,
    private val entitlements: EntitlementRepository,
    private val clock: Clock,
    private val post: suspend (url: String, body: String) -> HttpReply = { url, body ->
        ServerPurchaseValidator.httpPost(url, body)
    },
    private val random: SecureRandom = SecureRandom(),
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ReviewerAccess {
    override suspend fun redeem(code: String): ReviewerOutcome =
        try {
            attempt(code.trim())
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: Exception) {
            ReviewerOutcome.Unavailable
        }

    private suspend fun attempt(code: String): ReviewerOutcome {
        if (code.isEmpty()) return ReviewerOutcome.WrongCode
        if (!config.isUsable) return ReviewerOutcome.Unavailable
        val nonceBytes = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val clientNonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes)
        val request = Json.encodeToString(Request(code, clientNonce))
        val reply =
            try {
                post(config.url.trimEnd('/') + REVIEWER_PATH, request)
            } catch (failure: IOException) {
                return ReviewerOutcome.Unavailable
            }
        return when (reply.status) {
            HttpURLConnection.HTTP_OK -> {
                val granted = withContext(computeDispatcher) { grantedBy(reply.body, clientNonce) }
                if (granted) {
                    entitlements.record(
                        EntitlementSignal.PurchaseVerified(purchaseToken = REVIEWER_TOKEN, verifiedAt = clock.now()),
                    )
                    ReviewerOutcome.Unlocked
                } else {
                    ReviewerOutcome.WrongCode
                }
            }
            HttpURLConnection.HTTP_BAD_REQUEST, HttpURLConnection.HTTP_FORBIDDEN -> ReviewerOutcome.WrongCode
            else -> ReviewerOutcome.Unavailable
        }
    }

    private fun grantedBy(
        body: String,
        clientNonce: String,
    ): Boolean =
        try {
            val envelope = LENIENT.decodeFromString<Envelope>(body)
            if (!SignedReply.holds(config.publicKey, envelope.payload, envelope.sig)) {
                false
            } else {
                val claims = LENIENT.decodeFromString<Claims>(String(Base64.getUrlDecoder().decode(envelope.payload)))
                claims.status == "granted" &&
                    claims.kind == "reviewer" &&
                    claims.clientNonce == clientNonce &&
                    claims.productId == PRO_PRODUCT_ID &&
                    claims.packageName == packageName
            }
        } catch (malformed: IllegalArgumentException) {
            false
        }

    @Serializable
    private data class Request(
        val code: String,
        val clientNonce: String,
    )

    @Serializable
    private data class Envelope(
        val payload: String,
        val sig: String,
    )

    @Serializable
    private data class Claims(
        val status: String,
        val kind: String,
        val clientNonce: String,
        val productId: String,
        val packageName: String,
    )

    companion object {
        const val REVIEWER_TOKEN = "reviewer-access"
        private val LENIENT = Json { ignoreUnknownKeys = true }
        private const val REVIEWER_PATH = "/v1/reviewer"
        private const val NONCE_BYTES = 24
    }
}
