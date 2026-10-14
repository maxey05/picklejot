package com.maxeydev.picklejot.data.profile

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.maxeydev.picklejot.domain.profile.Entitlement
import com.maxeydev.picklejot.domain.profile.UserProfile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.time.Clock
import kotlin.time.Instant

internal val userProfileJson =
    Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = true
    }

@Serializable
internal data class UserProfileJson(
    val displayName: String,
    val createdAt: Long,
    val entitlement: EntitlementJson,
)

@Serializable
internal data class EntitlementJson(
    val isPro: Boolean,
    val purchaseToken: String?,
    val lastVerifiedAt: Long?,
    val proSince: Long? = null,
)

internal fun UserProfileJson.toDomain(): UserProfile =
    UserProfile(
        displayName = displayName,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        entitlement =
            Entitlement(
                isPro = entitlement.isPro,
                purchaseToken = entitlement.purchaseToken,
                lastVerifiedAt = entitlement.lastVerifiedAt?.let(Instant::fromEpochMilliseconds),
                proSince = entitlement.proSince?.let(Instant::fromEpochMilliseconds),
            ),
    )

internal fun UserProfile.toJson(): UserProfileJson =
    UserProfileJson(
        displayName = displayName,
        createdAt = createdAt.toEpochMilliseconds(),
        entitlement =
            EntitlementJson(
                isPro = entitlement.isPro,
                purchaseToken = entitlement.purchaseToken,
                lastVerifiedAt = entitlement.lastVerifiedAt?.toEpochMilliseconds(),
                proSince = entitlement.proSince?.toEpochMilliseconds(),
            ),
    )

@OptIn(ExperimentalSerializationApi::class)
class UserProfileSerializer(
    private val clock: Clock = Clock.System,
) : Serializer<UserProfile> {
    override val defaultValue: UserProfile
        get() =
            UserProfile(
                displayName = "",
                createdAt = clock.now(),
                entitlement = Entitlement(isPro = false, purchaseToken = null, lastVerifiedAt = null),
            )

    override suspend fun readFrom(input: InputStream): UserProfile =
        try {
            userProfileJson.decodeFromStream(UserProfileJson.serializer(), input).toDomain()
        } catch (cause: SerializationException) {
            throw CorruptionException(
                "Couldn't read your saved profile — the file isn't valid Picklejot data.",
                cause,
            )
        }

    override suspend fun writeTo(
        t: UserProfile,
        output: OutputStream,
    ) {
        userProfileJson.encodeToStream(UserProfileJson.serializer(), t.toJson(), output)
    }
}
