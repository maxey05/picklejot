package com.maxeydev.picklejot.domain.entitlement

import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.profile.Entitlement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

private const val TOKEN = "purchase-token-1"

private const val RULES_PATH = "domain/src/main/kotlin/com/maxeydev/picklejot/domain/entitlement/EntitlementRules.kt"

private val VERIFIED_AT = AppInstant.parse("2026-09-28T09:00:00Z")

private val PRO = Entitlement(isPro = true, purchaseToken = TOKEN, lastVerifiedAt = VERIFIED_AT)

private val FREE = Entitlement(isPro = false)

class EntitlementInvariantTest {
    private val everySignal: List<EntitlementSignal> =
        listOf(
            EntitlementSignal.PurchaseVerified(TOKEN, VERIFIED_AT),
            EntitlementSignal.RefundConfirmed(TOKEN),
            EntitlementSignal.RefundConfirmed("some-other-token"),
            EntitlementSignal.NoPurchaseFound,
            EntitlementSignal.CheckFailed,
            EntitlementSignal.CheckUnavailable,
        )

    private fun isExplicitRefundOfThisPurchase(signal: EntitlementSignal): Boolean =
        when (signal) {
            is EntitlementSignal.RefundConfirmed -> signal.purchaseToken == TOKEN
            is EntitlementSignal.PurchaseVerified -> false
            EntitlementSignal.NoPurchaseFound -> false
            EntitlementSignal.CheckFailed -> false
            EntitlementSignal.CheckUnavailable -> false
        }

    @Test
    fun `only an explicit successful refund of this purchase ever turns pro off`() {
        everySignal.forEach { signal ->
            val after = EntitlementRules.next(PRO, signal)

            assertEquals("signal $signal", !isExplicitRefundOfThisPurchase(signal), after.isPro)
        }
    }

    @Test
    fun `no sequence of failures ever revokes pro`() {
        val failures =
            listOf(
                EntitlementSignal.CheckFailed,
                EntitlementSignal.CheckUnavailable,
                EntitlementSignal.NoPurchaseFound,
            )
        var entitlement = PRO
        repeat(100) { round ->
            entitlement = EntitlementRules.next(entitlement, failures[round % failures.size])
        }

        assertEquals(PRO, entitlement)
    }

    @Test
    fun `pro has no expiry however long ago it was last verified`() {
        val ancient = PRO.copy(lastVerifiedAt = AppInstant.parse("2000-01-01T00:00:00Z"))

        assertTrue(EntitlementRules.next(ancient, EntitlementSignal.CheckUnavailable).isPro)
    }

    @Test
    fun `a verified purchase grants pro and records the token`() {
        val after = EntitlementRules.next(FREE, EntitlementSignal.PurchaseVerified(TOKEN, VERIFIED_AT))

        assertEquals(PRO.copy(proSince = VERIFIED_AT), after)
    }

    @Test
    fun `a refund signal for a free user changes nothing`() {
        assertEquals(FREE, EntitlementRules.next(FREE, EntitlementSignal.RefundConfirmed(TOKEN)))
    }

    @Test
    fun `a refund keeps the purchase token so the record stays auditable`() {
        val after = EntitlementRules.next(PRO, EntitlementSignal.RefundConfirmed(TOKEN))

        assertFalse(after.isPro)
        assertEquals(TOKEN, after.purchaseToken)
    }

    @Test
    fun `no production source outside the rules writes isPro false`() {
        val root = File(System.getProperty("user.dir")).parentFile
        val allowed =
            setOf(
                RULES_PATH,
                "data/src/main/kotlin/com/maxeydev/picklejot/data/profile/UserProfileSerializer.kt",
            )
        val revocation = Regex("""isPro\s*=\s*false|copy\(\s*isPro\s*=|isPro\s*=\s*!""")
        val offenders =
            listOf("app", "data", "domain", "ui")
                .map { File(root, "$it/src/main") }
                .filter { it.isDirectory }
                .flatMap { directory -> directory.walkTopDown().filter { it.extension == "kt" }.toList() }
                .map { it.relativeTo(root).invariantSeparatorsPath to it.readText() }
                .filter { (path, text) -> path !in allowed && revocation.containsMatchIn(text) }
                .map { it.first }

        assertTrue("sources that write isPro = false outside EntitlementRules: $offenders", offenders.isEmpty())
        assertTrue(
            "the scan found no sources at all, so it proves nothing",
            File(root, RULES_PATH).isFile,
        )
    }

    @Test
    fun `the rules file revokes in exactly one place and only for a refund`() {
        val root = File(System.getProperty("user.dir")).parentFile
        val rules = File(root, RULES_PATH).readText()

        assertEquals(1, Regex("""isPro\s*=\s*false""").findAll(rules).count())
        val revokingFunction = rules.substringAfter("private fun revokedBy(")
        assertTrue(revokingFunction.contains("isPro = false"))
        assertFalse(rules.substringBefore("private fun revokedBy(").contains("isPro = false"))
    }
}
