package com.maxeydev.picklejot.data.billing

import com.maxeydev.picklejot.domain.billing.ReviewerOutcome
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import kotlin.time.Clock
import kotlin.time.Instant

private const val REVIEWER_PACKAGE = "com.maxeydev.picklejot"
private val REVIEWER_ENCODER = Base64.getUrlEncoder().withoutPadding()

private fun reviewerKeyPair(): KeyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()

private fun reviewerClaims(
    nonce: String,
    status: String = "granted",
    kind: String = "reviewer",
    product: String = PRO_PRODUCT_ID,
    pkg: String = REVIEWER_PACKAGE,
) = """{"status":"$status","kind":"$kind","clientNonce":"$nonce","productId":"$product","packageName":"$pkg"}"""

private fun reviewerEnvelope(
    claims: String,
    signer: KeyPair,
): String {
    val payload = REVIEWER_ENCODER.encodeToString(claims.toByteArray())
    val signature =
        Signature.getInstance("SHA256withECDSA").run {
            initSign(signer.private)
            update(payload.toByteArray())
            sign()
        }
    return """{"payload":"$payload","sig":"${REVIEWER_ENCODER.encodeToString(signature)}"}"""
}

class ReviewerCodeRedeemerTest {
    private val serverKeys = reviewerKeyPair()
    private val config =
        LicenseServerConfig(
            "https://verify.example/",
            Base64.getEncoder().encodeToString(serverKeys.public.encoded),
        )
    private val entitlements = InMemoryEntitlementRepository()
    private val clock =
        object : Clock {
            override fun now(): Instant = Instant.fromEpochSeconds(1_800_000_000)
        }

    private var posts = 0
    private var postedUrl: String? = null
    private var sentCode: String? = null

    private fun redeemer(
        cfg: LicenseServerConfig = config,
        reply: (clientNonce: String) -> HttpReply,
    ) = ReviewerCodeRedeemer(
        config = cfg,
        packageName = REVIEWER_PACKAGE,
        entitlements = entitlements,
        clock = clock,
        post = { url, body ->
            posts++
            postedUrl = url
            val request = Json.parseToJsonElement(body).jsonObject
            sentCode = request.getValue("code").jsonPrimitive.content
            reply(request.getValue("clientNonce").jsonPrimitive.content)
        },
    )

    private fun granted(
        signer: KeyPair = serverKeys,
        nonceOverride: String? = null,
        status: String = "granted",
        kind: String = "reviewer",
        product: String = PRO_PRODUCT_ID,
        pkg: String = REVIEWER_PACKAGE,
    ): (String) -> HttpReply =
        { nonce ->
            HttpReply(200, reviewerEnvelope(reviewerClaims(nonceOverride ?: nonce, status, kind, product, pkg), signer))
        }

    @Test
    fun `a correctly signed grant for this attempt unlocks pro`() =
        runTest {
            assertEquals(ReviewerOutcome.Unlocked, redeemer(reply = granted()).redeem("good-code"))

            assertTrue(entitlements.current.isPro)
            assertEquals(ReviewerCodeRedeemer.REVIEWER_TOKEN, entitlements.current.purchaseToken)
            assertEquals(Instant.fromEpochSeconds(1_800_000_000), entitlements.current.lastVerifiedAt)
        }

    @Test
    fun `the code is trimmed and posted to the reviewer endpoint of the verifier`() =
        runTest {
            redeemer(reply = granted()).redeem("  good-code \n")

            assertEquals("https://verify.example/v1/reviewer", postedUrl)
            assertEquals("good-code", sentCode)
        }

    @Test
    fun `a blank code is a wrong code and never leaves the device`() =
        runTest {
            assertEquals(ReviewerOutcome.WrongCode, redeemer(reply = granted()).redeem("   "))

            assertEquals(0, posts)
            assertFalse(entitlements.current.isPro)
        }

    @Test
    fun `the server refusing the code is a wrong code and unlocks nothing`() =
        runTest {
            val refusals = listOf(403 to """{"error":"wrong_code"}""", 400 to """{"error":"bad_request"}""")
            refusals.forEach { (status, body) ->
                assertEquals(ReviewerOutcome.WrongCode, redeemer { HttpReply(status, body) }.redeem("nope"))
            }

            assertFalse(entitlements.current.isPro)
            assertTrue(entitlements.signals.isEmpty())
        }

    @Test
    fun `a disabled server, a busy server and an outage are unavailable not wrong`() =
        runTest {
            for (status in listOf(404, 408, 429, 500, 502, 503)) {
                assertEquals(ReviewerOutcome.Unavailable, redeemer { HttpReply(status, "{}") }.redeem("code"))
            }

            assertFalse(entitlements.current.isPro)
        }

    @Test
    fun `no network is unavailable`() =
        runTest {
            val offline =
                ReviewerCodeRedeemer(
                    config = config,
                    packageName = REVIEWER_PACKAGE,
                    entitlements = entitlements,
                    clock = clock,
                    post = { _, _ -> throw IOException("offline") },
                )

            assertEquals(ReviewerOutcome.Unavailable, offline.redeem("code"))
            assertFalse(entitlements.current.isPro)
        }

    @Test
    fun `an app built without a usable verifier is unavailable and sends nothing`() =
        runTest {
            val unusable = redeemer(cfg = LicenseServerConfig("", ""), reply = granted())

            assertEquals(ReviewerOutcome.Unavailable, unusable.redeem("code"))
            assertEquals(0, posts)
        }

    @Test
    fun `a reply signed by some other key is rejected`() =
        runTest {
            val forged = redeemer(reply = granted(signer = reviewerKeyPair()))

            assertEquals(ReviewerOutcome.WrongCode, forged.redeem("code"))

            assertFalse(entitlements.current.isPro)
        }

    @Test
    fun `a recorded grant for another attempt cannot be replayed`() =
        runTest {
            assertEquals(
                ReviewerOutcome.WrongCode,
                redeemer(reply = granted(nonceOverride = "AAAAAAAAAAAAAAAAAAAAAAAA")).redeem("code"),
            )

            assertFalse(entitlements.current.isPro)
        }

    @Test
    fun `a signed reply that is not a reviewer grant for this app and product is rejected`() =
        runTest {
            val replies =
                listOf(
                    granted(status = "refunded"),
                    granted(kind = "purchase"),
                    granted(product = "other_product"),
                    granted(pkg = "com.someone.else"),
                )

            replies.forEach { reply ->
                assertEquals(ReviewerOutcome.WrongCode, redeemer(reply = reply).redeem("code"))
            }
            assertFalse(entitlements.current.isPro)
        }

    @Test
    fun `a malformed reply is rejected`() =
        runTest {
            val bodies = listOf("not json", """{"payload":"%%","sig":"%%"}""")
            bodies.forEach { body ->
                assertEquals(ReviewerOutcome.WrongCode, redeemer { HttpReply(200, body) }.redeem("code"))
            }

            assertNull(entitlements.current.purchaseToken)
        }
}
