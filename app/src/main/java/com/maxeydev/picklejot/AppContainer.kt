package com.maxeydev.picklejot

import android.content.Context
import androidx.annotation.MainThread
import androidx.work.WorkManager
import com.maxeydev.picklejot.data.DataLayer
import com.maxeydev.picklejot.data.photo.PhotoStore
import com.maxeydev.picklejot.data.reminder.ReminderScheduler
import com.maxeydev.picklejot.data.settings.DirectoryAppCache
import com.maxeydev.picklejot.domain.backup.BackupRepository
import com.maxeydev.picklejot.domain.billing.ProStore
import com.maxeydev.picklejot.domain.erase.EraseAllData
import com.maxeydev.picklejot.domain.match.FilterState
import com.maxeydev.picklejot.domain.match.LastUsedFormatStore
import com.maxeydev.picklejot.domain.match.MatchRepository
import com.maxeydev.picklejot.domain.match.MatchSortStore
import com.maxeydev.picklejot.domain.onboarding.Onboarding
import com.maxeydev.picklejot.domain.person.PersonRepository
import com.maxeydev.picklejot.domain.photo.PhotoImportQueue
import com.maxeydev.picklejot.domain.profile.EntitlementRepository
import com.maxeydev.picklejot.domain.profile.ProfileRepository
import com.maxeydev.picklejot.domain.reminder.ReminderStore
import com.maxeydev.picklejot.domain.reminder.StreakReminder
import com.maxeydev.picklejot.domain.settings.AppCache
import com.maxeydev.picklejot.domain.settings.AppSettingsStore
import com.maxeydev.picklejot.domain.share.CardFormatStore
import com.maxeydev.picklejot.domain.streak.InsuredStreakEngine
import com.maxeydev.picklejot.domain.streak.StreakNoticeStore
import com.maxeydev.picklejot.domain.streak.streakInsuranceStart
import com.maxeydev.picklejot.ui.PicklejotDependencies
import com.maxeydev.picklejot.ui.common.createCaptureUri
import com.maxeydev.picklejot.ui.notification.StreakReminderNotifier
import com.maxeydev.picklejot.ui.share.CardRenderer
import com.maxeydev.picklejot.ui.share.CardRendering
import com.maxeydev.picklejot.ui.share.WebViewWarmer
import com.maxeydev.picklejot.ui.sound.SoundPoolSoundEffects
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import java.io.File
import kotlin.time.Clock

class AppContainer(
    private val context: Context,
    dataLayer: DataLayer,
) : PicklejotDependencies {
    private val photoStore: PhotoStore = dataLayer.photoStore

    override val matchRepository: MatchRepository = dataLayer.matchRepository
    override val personRepository: PersonRepository = dataLayer.personRepository
    override val lastUsedFormatStore: LastUsedFormatStore = dataLayer.lastUsedFormatStore
    override val matchSortStore: MatchSortStore = dataLayer.matchSortStore
    override val profileRepository: ProfileRepository = dataLayer.profileRepository
    override val entitlementRepository: EntitlementRepository = dataLayer.entitlementRepository
    override val proStore: ProStore = dataLayer.proStore
    override val photoImportQueue: PhotoImportQueue = dataLayer.photoImportQueue
    override val cardFormatStore: CardFormatStore = dataLayer.cardFormatStore
    override val backupRepository: BackupRepository = dataLayer.backupRepository
    override val streakNoticeStore: StreakNoticeStore = dataLayer.streakNoticeStore
    override val reminderStore: ReminderStore = dataLayer.reminderStore
    override val clock: Clock = Clock.System
    override val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
    override val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    override val streakReminder: StreakReminder =
        StreakReminder(
            store = dataLayer.reminderStore,
            matchDates = {
                dataLayer.matchRepository
                    .observeStatLines(FilterState.NONE)
                    .first()
                    .map { it.date }
            },
            insuranceStart = {
                dataLayer.profileRepository
                    .observeProfile()
                    .first()
                    .entitlement
                    .streakInsuranceStart()
            },
            engine = InsuredStreakEngine(clock) { currentTimeZone() },
            scheduling = ReminderScheduler(WorkManager.getInstance(context), clock),
            notifier = StreakReminderNotifier(context),
            clock = clock,
            timeZone = { currentTimeZone() },
        )
    override val appSettingsStore: AppSettingsStore = dataLayer.appSettingsStore
    override val soundEffects: SoundPoolSoundEffects = SoundPoolSoundEffects(context)
    override val appCache: AppCache = DirectoryAppCache(context.cacheDir, Dispatchers.IO)
    override val onboarding: Onboarding =
        Onboarding(
            settings = dataLayer.appSettingsStore,
            profile = dataLayer.profileRepository,
            matchCount = { dataLayer.matchRepository.observeMatchCount().first() },
        )
    override val eraseAllData: EraseAllData = EraseAllData(streakReminder, dataLayer.localDataEraser)
    override val appVersionName: String =
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            .orEmpty()
    private val cardWarmer = WebViewWarmer(context)
    override val cardRenderer: CardRendering = CardRenderer(cardWarmer)

    override fun currentTimeZone(): TimeZone = TimeZone.currentSystemDefault()

    override fun photoFile(relativePath: String): File = photoStore.resolve(relativePath)

    @MainThread
    fun warmCardRenderer() {
        cardWarmer.warm()
    }

    override fun newCaptureUri(): String = createCaptureUri(context, clock.now().toEpochMilliseconds())
}
