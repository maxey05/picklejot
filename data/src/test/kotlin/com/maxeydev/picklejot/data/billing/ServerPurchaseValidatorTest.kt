package com.maxeydev.picklejot.data.billing

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

private const val PACKAGE = "com.maxeydev.picklejot"
private val URL_ENCODER = Base64.getUrlEncoder().withoutPadding()

private fun ecKeyPair(): KeyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()

private fun KeyPair.publicKeyBase64(): String = Base64.getEncoder().encodeToString(public.encoded)

private fun claimsJson(
    status: String,
    token: String,
    nonce: String,
    product: String = PRO_PRODUCT_ID,
    pkg: String = PACKAGE,
) = """{"status":"$status","purchaseToken":"$token","clientNonce":"$nonce",""" +
    """"productId":"$product","packageName":"$pkg"}"""

private fun envelope(
    claims: String,
    signedClaims: String = claims,
    signer: KeyPair,
): String {
    val sentPayload = URL_ENCODER.encodeToString(claims.toByteArray())
    val signedPayload = URL_ENCODER.encodeToString(signedClaims.toByteArray())
    val sig =
        Signature.getInstance("SHA256withECDSA").run {
            initSign(signer.private)
            update(signedPayload.toByteArray())
            sign()
        }
    return """{"payload":"$sentPayload","sig":"${URL_ENCODER.encodeToString(sig)}"}"""
}

class ServerPurchaseValidatorTest {
    private val serverKeys = ecKeyPair()
    private val config = LicenseServerConfig("https://verify.example", serverKeys.publicKeyBase64())
    private val purchase = proPurchase(token = "token-1")

    private var sentNonce: String? = null
    private var integrityNonce: String? = null
    private var integrityToken: String? = "integrity-token"
    private var posts = 0
    private var computeDispatcher: CoroutineDispatcher = Dispatchers.Default

    /** [reply] gets the client nonce the app chose, as the real server would. */
    private fun validator(
        cfg: LicenseServerConfig = config,
        reply: (clientNonce: String) -> HttpReply,
    ) = ServerPurchaseValidator(
        config = cfg,
        packageName = PACKAGE,
        integrity =
            IntegrityTokenProvider { nonce ->
                integrityNonce = nonce
                integrityToken
            },
        post = { _, body ->
            posts++
            val request = Json.parseToJsonElement(body).jsonObject
            val nonce = request.getValue("clientNonce").jsonPrimitive.content
            sentNonce = nonce
            assertEquals("token-1", request.getValue("purchaseToken").jsonPrimitive.content)
            assertEquals("integrity-token", request.getValue("integrityToken").jsonPrimitive.content)
            reply(nonce)
        },
        computeDispatcher = computeDispatcher,
    )

    private fun ok(
        status: String = "granted",
        token: String = "token-1",
        product: String = PRO_PRODUCT_ID,
        pkg: String = PACKAGE,
        nonceOverride: String? = null,
        signer: KeyPair = serverKeys,
    ): (String) -> HttpReply =
        { nonce ->
            HttpReply(200, envelope(claimsJson(status, token, nonceOverride ?: nonce, product, pkg), signer = signer))
        }

    @Test
    fun `a correctly signed grant for this purchase and this attempt is valid`() =
        runTest {
            assertEquals(ValidationResult.VALID, validator(reply = ok()).validate(purchase))
        }

    @Test
    fun `the signed reply is checked on the compute dispatcher not the calling thread`() =
        runTest {
            val dispatched = AtomicInteger()
            val counting =
                object : CoroutineDispatcher() {
                    override fun dispatch(
                        context: CoroutineContext,
                        block: Runnable,
                    ) {
                        dispatched.incrementAndGet()
                        Dispatchers.Default.dispatch(context, block)
                    }
                }

            computeDispatcher = counting
            assertEquals(ValidationResult.VALID, validator(reply = ok()).validate(purchase))
            assertTrue(dispatched.get() > 0)
        }

    @Test
    fun `the integrity token is requested for a nonce bound to the purchase and the attempt`() =
        runTest {
            validator(reply = ok()).validate(purchase)

            assertEquals(ServerPurchaseValidator.integrityNonce("token-1", sentNonce!!), integrityNonce)
            assertTrue(integrityNonce!!.matches(Regex("[A-Za-z0-9_-]{16,500}")))
        }

    @Test
    fun `every attempt uses a fresh nonce`() =
        runTest {
            val nonces = mutableSetOf<String>()
            repeat(5) {
                validator(
                    reply = { nonce ->
                        nonces += nonce
                        ok()(nonce)
                    },
                ).validate(purchase)
            }

            assertEquals(5, nonces.size)
        }

    @Test
    fun `a signed refund verdict is surfaced`() =
        runTest {
            assertEquals(ValidationResult.REFUNDED, validator(reply = ok(status = "refunded")).validate(purchase))
        }

    @Test
    fun `a recorded reply replayed against a new attempt is rejected`() =
        runTest {
            val replayed = validator(reply = ok(nonceOverride = "an-older-attempts-nonce"))

            assertEquals(ValidationResult.REJECTED, replayed.validate(purchase))
        }

    @Test
    fun `a genuine reply about another purchase is rejected`() =
        runTest {
            assertEquals(ValidationResult.REJECTED, validator(reply = ok(token = "token-2")).validate(purchase))
        }

    @Test
    fun `a reply for another product or another app is rejected`() =
        runTest {
            assertEquals(ValidationResult.REJECTED, validator(reply = ok(product = "other")).validate(purchase))
            assertEquals(ValidationResult.REJECTED, validator(reply = ok(pkg = "evil.app")).validate(purchase))
        }

    @Test
    fun `a reply signed by anyone but the real server is rejected`() =
        runTest {
            assertEquals(ValidationResult.REJECTED, validator(reply = ok(signer = ecKeyPair())).validate(purchase))
        }

    @Test
    fun `a payload edited after signing is rejected`() =
        runTest {
            val tampered: (String) -> HttpReply = { nonce ->
                HttpReply(
                    200,
                    envelope(
                        claims = claimsJson("granted", "token-1", nonce),
                        signedClaims = claimsJson("refunded", "token-1", nonce),
                        signer = serverKeys,
                    ),
                )
            }

            assertEquals(ValidationResult.REJECTED, validator(reply = tampered).validate(purchase))
        }

    @Test
    fun `an unknown status or an unparseable body is rejected, never granted`() =
        runTest {
            assertEquals(ValidationResult.REJECTED, validator(reply = ok(status = "maybe")).validate(purchase))
            val bodies = listOf("", "not json", "{}", """{"payload":"!!","sig":"!!"}""", """{"status":"granted"}""")
            bodies.forEach { body ->
                val result = validator(reply = { HttpReply(200, body) }).validate(purchase)
                assertEquals(body, ValidationResult.REJECTED, result)
            }
        }

    @Test
    fun `a 4xx from the server is a rejection`() =
        runTest {
            listOf(400, 403, 404).forEach { status ->
                val result = validator(reply = { HttpReply(status, """{"error":"x"}""") }).validate(purchase)
                assertEquals(ValidationResult.REJECTED, result)
            }
        }

    @Test
    fun `server trouble, throttling and timeouts are unreachable not rejections`() =
        runTest {
            listOf(408, 429, 500, 502, 503).forEach { status ->
                val result = validator(reply = { HttpReply(status, "") }).validate(purchase)
                assertEquals(ValidationResult.UNREACHABLE, result)
            }
            assertEquals(
                ValidationResult.UNREACHABLE,
                validator(reply = { throw IOException("offline") }).validate(purchase),
            )
        }

    @Test
    fun `an unexpected exception is unreachable and cannot crash the launch coroutine`() =
        runTest {
            val result = validator(reply = { throw IllegalStateException("boom") }).validate(purchase)

            assertEquals(ValidationResult.UNREACHABLE, result)
        }

    @Test
    fun `without an integrity token nothing is sent`() =
        runTest {
            integrityToken = null

            assertEquals(ValidationResult.UNREACHABLE, validator(reply = ok()).validate(purchase))
            assertEquals(0, posts)
        }

    @Test
    fun `an unconfigured or cleartext server is never contacted`() =
        runTest {
            listOf(
                LicenseServerConfig("", serverKeys.publicKeyBase64()),
                LicenseServerConfig("http://verify.example", serverKeys.publicKeyBase64()),
                LicenseServerConfig("https://verify.example", ""),
            ).forEach { cfg ->
                assertEquals(ValidationResult.UNREACHABLE, validator(cfg, ok()).validate(purchase))
            }
            assertEquals(0, posts)
            assertNull(integrityNonce)
        }

    @Test
    fun `a malformed public key rejects instead of crashing`() =
        runTest {
            val bad = LicenseServerConfig("https://verify.example", "bm90LWEta2V5")

            assertEquals(ValidationResult.REJECTED, validator(bad, ok()).validate(purchase))
        }
}
