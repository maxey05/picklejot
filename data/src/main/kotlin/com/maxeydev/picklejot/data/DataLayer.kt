package com.maxeydev.picklejot.data

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.maxeydev.picklejot.data.backup.DataStoreExportPromptStore
import com.maxeydev.picklejot.data.backup.RoomBackupRepository
import com.maxeydev.picklejot.data.billing.BillingClientWrapper
import com.maxeydev.picklejot.data.billing.BillingStartupCheck
import com.maxeydev.picklejot.data.billing.LicenseServerConfig
import com.maxeydev.picklejot.data.billing.PlayEntitlementRepository
import com.maxeydev.picklejot.data.billing.PlayIntegrityTokenProvider
import com.maxeydev.picklejot.data.billing.PlayProStore
import com.maxeydev.picklejot.data.billing.PurchaseAcknowledger
import com.maxeydev.picklejot.data.billing.ServerPurchaseValidator
import com.maxeydev.picklejot.data.db.DB_BACKUP_DIRECTORY
import com.maxeydev.picklejot.data.db.createPicklejotDatabase
import com.maxeydev.picklejot.data.erase.RoomLocalDataEraser
import com.maxeydev.picklejot.data.match.DataStoreLastUsedFormatStore
import com.maxeydev.picklejot.data.match.DataStoreMatchSortStore
import com.maxeydev.picklejot.data.match.RoomMatchRepository
import com.maxeydev.picklejot.data.match.createMatchPreferencesDataStore
import com.maxeydev.picklejot.data.person.RoomPersonRepository
import com.maxeydev.picklejot.data.photo.ImageCompressor
import com.maxeydev.picklejot.data.photo.PhotoStore
import com.maxeydev.picklejot.data.photo.QueuedPhotoImporter
import com.maxeydev.picklejot.data.profile.DataStoreEntitlementRepository
import com.maxeydev.picklejot.data.profile.DataStoreProfileRepository
import com.maxeydev.picklejot.data.profile.createUserProfileDataStore
import com.maxeydev.picklejot.data.reminder.DataStoreReminderStore
import com.maxeydev.picklejot.data.settings.DataStoreAppSettingsStore
import com.maxeydev.picklejot.data.settings.createAppSettingsDataStore
import com.maxeydev.picklejot.data.share.DataStoreCardFormatStore
import com.maxeydev.picklejot.data.streak.DataStoreStreakNoticeStore
import com.maxeydev.picklejot.domain.backup.BackupRepository
import com.maxeydev.picklejot.domain.billing.ProStore
import com.maxeydev.picklejot.domain.erase.LocalDataEraser
import com.maxeydev.picklejot.domain.match.LastUsedFormatStore
import com.maxeydev.picklejot.domain.match.MatchRepository
import com.maxeydev.picklejot.domain.match.MatchSortStore
import com.maxeydev.picklejot.domain.person.PersonRepository
import com.maxeydev.picklejot.domain.photo.PhotoImportQueue
import com.maxeydev.picklejot.domain.profile.EntitlementRepository
import com.maxeydev.picklejot.domain.profile.ProfileRepository
import com.maxeydev.picklejot.domain.reminder.ReminderStore
import com.maxeydev.picklejot.domain.settings.AppSettingsStore
import com.maxeydev.picklejot.domain.share.CardFormatStore
import com.maxeydev.picklejot.domain.streak.StreakNoticeStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Clock

const val EXPORT_CACHE_DIRECTORY = "exports"

const val ORPHAN_PHOTO_AGE_MILLIS = 24L * 60 * 60 * 1000

class DataLayer(
    private val roomMatchRepository: RoomMatchRepository,
    val personRepository: PersonRepository,
    val lastUsedFormatStore: LastUsedFormatStore,
    val matchSortStore: MatchSortStore,
    val photoStore: PhotoStore,
    val profileRepository: ProfileRepository,
    val entitlementRepository: EntitlementRepository,
    val proStore: ProStore,
    val billingStartupCheck: BillingStartupCheck,
    val photoImportQueue: PhotoImportQueue,
    val cardFormatStore: CardFormatStore,
    val backupRepository: BackupRepository,
    val streakNoticeStore: StreakNoticeStore,
    val reminderStore: ReminderStore,
    val appSettingsStore: AppSettingsStore,
    val localDataEraser: LocalDataEraser,
    private val ioDispatcher: CoroutineDispatcher,
) {
    val matchRepository: MatchRepository = roomMatchRepository

    suspend fun sweepOrphanPhotos(nowMillis: Long): Int {
        val referenced = roomMatchRepository.referencedPhotoPaths()
        return withContext(ioDispatcher) {
            photoStore.deleteOrphans(referenced, olderThanMillis = nowMillis - ORPHAN_PHOTO_AGE_MILLIS).size
        }
    }
}

suspend fun createDataLayer(
    context: Context,
    applicationScope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
    licenseServer: LicenseServerConfig,
    currentActivity: () -> Activity?,
): DataLayer {
    val appContext = context.applicationContext
    val database = createPicklejotDatabase(appContext, ioDispatcher)
    val preferences = createMatchPreferencesDataStore(appContext, applicationScope + ioDispatcher)
    val photoStore = PhotoStore(appContext.filesDir)
    val profileStore = createUserProfileDataStore(appContext, applicationScope + ioDispatcher)
    val profileRepository = DataStoreProfileRepository(profileStore)
    val appSettings =
        DataStoreAppSettingsStore(createAppSettingsDataStore(appContext, applicationScope + ioDispatcher))
    val matchRepository = RoomMatchRepository(database, photoStore, ioDispatcher)
    val billing = BillingClientWrapper(appContext)
    val entitlements =
        PlayEntitlementRepository(
            local = DataStoreEntitlementRepository(profileStore),
            gateway = billing,
            acknowledger = PurchaseAcknowledger(billing),
            validator =
                ServerPurchaseValidator(
                    config = licenseServer,
                    packageName = appContext.packageName,
                    integrity = PlayIntegrityTokenProvider(appContext),
                ),
            clock = Clock.System,
        )
    val resolver = appContext.contentResolver
    val importer =
        QueuedPhotoImporter(
            scope = applicationScope,
            ioDispatcher = ioDispatcher,
            compressor = ImageCompressor(openSource = { uri -> resolver.openInputStream(Uri.parse(uri)) }),
            photoStore = photoStore,
            matchRepository = matchRepository,
            deleteTemporaryCapture = { uri -> resolver.delete(Uri.parse(uri), null, null) },
        )
    val backupRepository =
        RoomBackupRepository(
            database = database,
            photoStore = photoStore,
            entitlements = entitlements,
            promptStore = DataStoreExportPromptStore(preferences),
            exportDirectory = File(appContext.cacheDir, EXPORT_CACHE_DIRECTORY),
            openSource = { uri -> resolver.openInputStream(Uri.parse(uri)) },
            clock = Clock.System,
            ioDispatcher = ioDispatcher,
        )
    return DataLayer(
        roomMatchRepository = matchRepository,
        personRepository = RoomPersonRepository(database, ioDispatcher),
        lastUsedFormatStore = DataStoreLastUsedFormatStore(preferences, applicationScope),
        matchSortStore = DataStoreMatchSortStore(preferences),
        photoStore = photoStore,
        profileRepository = profileRepository,
        entitlementRepository = entitlements,
        proStore = PlayProStore(billing, entitlements) { currentActivity()?.let { billing.launchProPurchase(it) } },
        billingStartupCheck = BillingStartupCheck(entitlements, billing, applicationScope),
        photoImportQueue = importer,
        cardFormatStore = DataStoreCardFormatStore(preferences),
        backupRepository = backupRepository,
        streakNoticeStore = DataStoreStreakNoticeStore(preferences),
        reminderStore = DataStoreReminderStore(preferences),
        appSettingsStore = appSettings,
        localDataEraser =
            RoomLocalDataEraser(
                database = database,
                photoStore = photoStore,
                exportDirectory = File(appContext.cacheDir, EXPORT_CACHE_DIRECTORY),
                databaseBackupDirectory = File(appContext.noBackupFilesDir, DB_BACKUP_DIRECTORY),
                preferences = preferences,
                profileRepository = profileRepository,
                appSettings = appSettings,
                ioDispatcher = ioDispatcher,
            ),
        ioDispatcher = ioDispatcher,
    )
}
