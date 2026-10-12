package com.maxeydev.picklejot.data.profile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.maxeydev.picklejot.domain.profile.ProfileRepository
import com.maxeydev.picklejot.domain.profile.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import kotlin.time.Clock

const val USER_PROFILE_FILE_NAME = "user-profile.json"

fun createUserProfileDataStore(
    context: Context,
    scope: CoroutineScope,
    clock: Clock = Clock.System,
): DataStore<UserProfile> =
    DataStoreFactory.create(
        serializer = UserProfileSerializer(clock),
        scope = scope,
        produceFile = { File(context.applicationContext.filesDir, USER_PROFILE_FILE_NAME) },
    )

class DataStoreProfileRepository(
    private val dataStore: DataStore<UserProfile>,
) : ProfileRepository {
    private val initialisation = Mutex()

    @Volatile
    private var createdAtPersisted = false

    override fun observeProfile(): Flow<UserProfile> =
        flow {
            persistCreatedAtOnce()
            emitAll(dataStore.data)
        }

    override suspend fun updateDisplayName(displayName: String) {
        dataStore.updateData { profile -> profile.copy(displayName = displayName) }
        createdAtPersisted = true
    }

    private suspend fun persistCreatedAtOnce() {
        if (createdAtPersisted) {
            return
        }
        initialisation.withLock {
            if (!createdAtPersisted) {
                dataStore.updateData { profile -> profile }
                createdAtPersisted = true
            }
        }
    }
}
