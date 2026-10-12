package com.maxeydev.picklejot.data.profile

import com.maxeydev.picklejot.domain.profile.Entitlement
import com.maxeydev.picklejot.domain.profile.UserProfile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.time.Instant

class UserProfileSerializerTest {
    private val profile =
        UserProfile(
            displayName = "Matthew",
            createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000),
            entitlement =
                Entitlement(
                    isPro = true,
                    purchaseToken = "token",
                    lastVerifiedAt = Instant.fromEpochMilliseconds(1_700_000_001_000),
                    proSince = Instant.fromEpochMilliseconds(1_699_000_000_000),
                ),
        )

    private fun write(value: UserProfile): String {
        val output = ByteArrayOutputStream()
        runBlocking { UserProfileSerializer().writeTo(value, output) }
        return output.toString(Charsets.UTF_8.name())
    }

    @Test
    fun `the serialized profile contains exactly displayName createdAt and entitlement`() {
        val keys = Json.parseToJsonElement(write(profile)).jsonObject.keys
        assertEquals(setOf("displayName", "createdAt", "entitlement"), keys)
    }

    @Test
    fun `the serialized profile contains no email password or identifier of any kind`() {
        val encoded = write(profile).lowercase()
        listOf("email", "password", "userid", "user_id", "accountid", "account_id", "uuid", "deviceid")
            .forEach { forbidden ->
                assertTrue("serialized profile must not contain '$forbidden'", !encoded.contains(forbidden))
            }
    }

    @Test
    fun `the entitlement object carries only its four declared fields`() {
        val entitlement =
            Json
                .parseToJsonElement(write(profile))
                .jsonObject["entitlement"]
                ?.jsonObject
                ?.keys
        assertEquals(setOf("isPro", "purchaseToken", "lastVerifiedAt", "proSince"), entitlement)
    }

    @Test
    fun `a profile survives a write then read round trip`() =
        runTest {
            val encoded = write(profile)
            val restored = UserProfileSerializer().readFrom(ByteArrayInputStream(encoded.encodeToByteArray()))
            assertEquals(profile, restored)
        }

    @Test
    fun `a profile saved before the pro start date existed still reads`() =
        runTest {
            val legacy =
                """{"displayName":"Matthew","createdAt":1700000000000,""" +
                    """"entitlement":{"isPro":true,"purchaseToken":"token","lastVerifiedAt":1700000001000}}"""

            val restored = UserProfileSerializer().readFrom(ByteArrayInputStream(legacy.encodeToByteArray()))

            assertEquals(true, restored.entitlement.isPro)
            assertEquals(null, restored.entitlement.proSince)
        }

    @Test
    fun `an empty display name is preserved rather than rejected`() =
        runTest {
            val anonymous = profile.copy(displayName = "")
            val encoded = write(anonymous)
            val restored = UserProfileSerializer().readFrom(ByteArrayInputStream(encoded.encodeToByteArray()))
            assertEquals("", restored.displayName)
        }
}
