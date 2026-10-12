package com.maxeydev.picklejot.ui

import com.maxeydev.picklejot.domain.backup.BackupRepository
import com.maxeydev.picklejot.domain.billing.ProStore
import com.maxeydev.picklejot.domain.erase.EraseAllData
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
import com.maxeydev.picklejot.domain.streak.StreakNoticeStore
import com.maxeydev.picklejot.ui.share.CardRendering
import com.maxeydev.picklejot.ui.sound.SoundEffects
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.datetime.TimeZone
import java.io.File
import kotlin.time.Clock

interface PicklejotDependencies {
    val matchRepository: MatchRepository
    val personRepository: PersonRepository
    val lastUsedFormatStore: LastUsedFormatStore
    val matchSortStore: MatchSortStore
    val profileRepository: ProfileRepository
    val entitlementRepository: EntitlementRepository
    val proStore: ProStore
    val photoImportQueue: PhotoImportQueue
    val cardRenderer: CardRendering
    val cardFormatStore: CardFormatStore
    val backupRepository: BackupRepository
    val streakNoticeStore: StreakNoticeStore
    val reminderStore: ReminderStore
    val streakReminder: StreakReminder
    val appSettingsStore: AppSettingsStore
    val soundEffects: SoundEffects
    val appCache: AppCache
    val onboarding: Onboarding
    val eraseAllData: EraseAllData
    val appVersionName: String
    val clock: Clock
    val defaultDispatcher: CoroutineDispatcher
    val ioDispatcher: CoroutineDispatcher

    fun currentTimeZone(): TimeZone

    fun photoFile(relativePath: String): File

    fun newCaptureUri(): String
}
