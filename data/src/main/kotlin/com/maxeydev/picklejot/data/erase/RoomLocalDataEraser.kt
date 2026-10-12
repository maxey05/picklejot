package com.maxeydev.picklejot.data.erase

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.maxeydev.picklejot.data.db.PicklejotDatabase
import com.maxeydev.picklejot.data.photo.PhotoStore
import com.maxeydev.picklejot.domain.erase.LocalDataEraser
import com.maxeydev.picklejot.domain.profile.ProfileRepository
import com.maxeydev.picklejot.domain.settings.AppSettingsStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class RoomLocalDataEraser(
    private val database: PicklejotDatabase,
    private val photoStore: PhotoStore,
    private val exportDirectory: File,
    private val databaseBackupDirectory: File,
    private val preferences: DataStore<Preferences>,
    private val profileRepository: ProfileRepository,
    private val appSettings: AppSettingsStore,
    private val ioDispatcher: CoroutineDispatcher,
) : LocalDataEraser {
    override suspend fun erase() {
        withContext(ioDispatcher) {
            database.clearAllTables()
            photoStore.deleteAll()
            removeDirectory(exportDirectory)
            removeDirectory(databaseBackupDirectory)
        }
        preferences.edit { it.clear() }
        profileRepository.updateDisplayName("")
        appSettings.setOnboardingComplete(false)
    }

    private fun removeDirectory(directory: File) {
        if (directory.exists() && !directory.deleteRecursively()) {
            throw IOException("Saved data could not be removed from ${directory.name}.")
        }
    }
}
