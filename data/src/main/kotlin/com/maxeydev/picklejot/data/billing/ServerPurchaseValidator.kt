package com.maxeydev.picklejot.data.billing

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/** [url] is the verifier's base URL (https only); [publicKey] is its base64 X.509 ECDSA P-256 key. */
data class LicenseServerConfig(
    val url: String,
    val publicKey: String,
) {
    val isUsable: Boolean get() = url.startsWith("https://") && publicKey.isNotBlank()
}

data class HttpReply(
    val status: Int,
    val body: String,
)

/**
 * Asks the verifier (server/) whether a purchase is real. The verifier checks the token with the
 * Play Developer API and the device with Play Integrity, then returns an ECDSA-signed statement
 * that echoes a nonce chosen here — so a recorded or forged reply is not accepted.
 *
 * Fails closed: any doubt about the reply is [ValidationResult.REJECTED]; any doubt about
 * reachability is [ValidationResult.UNREACHABLE]. Neither changes a stored entitlement.
 */
class ServerPurchaseValidator(
    private val config: LicenseServerConfig,
    private val packageName: String,
    private val integrity: IntegrityTokenProvider,
    private val post: suspend (url: String, body: String) -> HttpReply = { url, body -> httpPost(url, body) },
    private val random: SecureRandom = SecureRandom(),
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : PurchaseValidator {
    override suspend fun validate(purchase: PlayPurchase): ValidationResult =
        try {
            attempt(purchase)
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: Exception) {
            // Never let an unexpected failure escape into the launch-time coroutine: that crashes the app.
            ValidationResult.UNREACHABLE
        }

    private suspend fun attempt(purchase: PlayPurchase): ValidationResult {
        if (!config.isUsable) return ValidationResult.UNREACHABLE
        val clientNonce = base64Url(ByteArray(NONCE_BYTES).also(random::nextBytes))
        val integrityToken =
            integrity.token(integrityNonce(purchase.purchaseToken, clientNonce)) ?: return ValidationResult.UNREACHABLE
        val request = Json.encodeToString(Request(purchase.purchaseToken, clientNonce, integrityToken))
        val reply =
            try {
                post(config.url.trimEnd('/') + VERIFY_PATH, request)
            } catch (failure: IOException) {
                return ValidationResult.UNREACHABLE
            }
        return when {
            reply.status == HttpURLConnection.HTTP_OK ->
                withContext(computeDispatcher) { interpret(reply.body, purchase.purchaseToken, clientNonce) }
            reply.status >= 500 || reply.status == 408 || reply.status == 429 -> ValidationResult.UNREACHABLE
            else -> ValidationResult.REJECTED
        }
    }

    private fun interpret(
        body: String,
        purchaseToken: String,
        clientNonce: String,
    ): ValidationResult =
        try {
            val envelope = Json.decodeFromString<Envelope>(body)
            if (!signatureHolds(envelope)) return ValidationResult.REJECTED
            val claims = Json.decodeFromString<Claims>(String(Base64.getUrlDecoder().decode(envelope.payload)))
            val about =
                claims.purchaseToken == purchaseToken &&
                    claims.clientNonce == clientNonce &&
                    claims.productId == PRO_PRODUCT_ID &&
                    claims.packageName == packageName
            when {
                !about -> ValidationResult.REJECTED
                claims.status == "granted" -> ValidationResult.VALID
                claims.status == "refunded" -> ValidationResult.REFUNDED
                else -> ValidationResult.REJECTED
            }
        } catch (malformed: IllegalArgumentException) {
            ValidationResult.REJECTED
        } catch (malformed: kotlinx.serialization.SerializationException) {
            ValidationResult.REJECTED
        }

    private fun signatureHolds(envelope: Envelope): Boolean =
        try {
            val keySpec = X509EncodedKeySpec(Base64.getDecoder().decode(config.publicKey))
            val key = KeyFactory.getInstance("EC").generatePublic(keySpec)
            Signature.getInstance("SHA256withECDSA").run {
                initVerify(key)
                update(envelope.payload.toByteArray(Charsets.UTF_8))
                verify(Base64.getUrlDecoder().decode(envelope.sig))
            }
        } catch (failure: GeneralSecurityException) {
            false
        }

    @Serializable
    private data class Request(
        val purchaseToken: String,
        val clientNonce: String,
        val integrityToken: String,
    )

    @Serializable
    private data class Envelope(
        val payload: String,
        val sig: String,
    )

    @Serializable
    private data class Claims(
        val status: String,
        val purchaseToken: String,
        val clientNonce: String,
        val productId: String,
        val packageName: String,
    )

    companion object {
        private const val VERIFY_PATH = "/v1/verify"
        private const val NONCE_BYTES = 24
        private const val TIMEOUT_MILLIS = 15_000

        /** Binds the integrity token to this purchase and this attempt. The server recomputes it. */
        internal fun integrityNonce(
            purchaseToken: String,
            clientNonce: String,
        ): String = base64Url(MessageDigest.getInstance("SHA-256").digest("$purchaseToken\n$clientNonce".toByteArray()))

        private fun base64Url(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

        private suspend fun httpPost(
            url: String,
            body: String,
        ): HttpReply =
            withContext(Dispatchers.IO) {
                val connection = URL(url).openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "POST"
                    connection.connectTimeout = TIMEOUT_MILLIS
                    connection.readTimeout = TIMEOUT_MILLIS
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.outputStream.use { it.write(body.toByteArray()) }
                    val stream = if (connection.responseCode < 400) connection.inputStream else connection.errorStream
                    HttpReply(connection.responseCode, stream?.bufferedReader()?.use { it.readText() }.orEmpty())
                } finally {
                    connection.disconnect()
                }
            }
    }
}
