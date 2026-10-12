package com.maxeydev.picklejot.data.profile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklejot.domain.entitlement.EntitlementSignal
import com.maxeydev.picklejot.domain.profile.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.time.Instant

private const val TOKEN = "purchase-token"

@RunWith(AndroidJUnit4::class)
class DataStoreEntitlementRepositoryTest {
    private lateinit var directory: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        directory = File(context.cacheDir, "entitlement-test-${System.nanoTime()}").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun open(job: Job): DataStore<UserProfile> =
        DataStoreFactory.create(
            serializer = UserProfileSerializer(),
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { File(directory, USER_PROFILE_FILE_NAME) },
        )

    private suspend fun proRepository(job: Job): DataStoreEntitlementRepository =
        DataStoreEntitlementRepository(open(job)).also {
            it.record(EntitlementSignal.PurchaseVerified(TOKEN, Instant.fromEpochMilliseconds(1_000)))
        }

    @Test
    fun a_verified_purchase_survives_an_app_restart() =
        runBlocking {
            val first = SupervisorJob()
            proRepository(first)
            first.cancelAndJoin()

            val second = SupervisorJob()
            val reopened = DataStoreEntitlementRepository(open(second)).observeEntitlement().first()

            assertTrue(reopened.isPro)
            assertEquals(TOKEN, reopened.purchaseToken)
            second.cancelAndJoin()
        }

    @Test
    fun failed_unavailable_and_empty_checks_leave_the_stored_pro_untouched_on_disk() =
        runBlocking {
            val job = SupervisorJob()
            val repository = proRepository(job)
            val before = repository.observeEntitlement().first()

            repository.record(EntitlementSignal.CheckFailed)
            repository.record(EntitlementSignal.CheckUnavailable)
            repository.record(EntitlementSignal.NoPurchaseFound)

            assertEquals(before, repository.observeEntitlement().first())
            job.cancelAndJoin()
        }

    @Test
    fun only_a_confirmed_refund_of_this_purchase_revokes_and_the_display_name_survives_it() =
        runBlocking {
            val job = SupervisorJob()
            val dataStore = open(job)
            DataStoreProfileRepository(dataStore).updateDisplayName("Matthew")
            val repository = DataStoreEntitlementRepository(dataStore)
            repository.record(EntitlementSignal.PurchaseVerified(TOKEN, Instant.fromEpochMilliseconds(1_000)))

            repository.record(EntitlementSignal.RefundConfirmed("a-different-purchase"))
            assertTrue(repository.observeEntitlement().first().isPro)

            repository.record(EntitlementSignal.RefundConfirmed(TOKEN))

            assertFalse(repository.observeEntitlement().first().isPro)
            assertEquals("Matthew", dataStore.data.first().displayName)
            job.cancelAndJoin()
        }
}
