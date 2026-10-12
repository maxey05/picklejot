package com.maxeydev.picklejot.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.maxeydev.picklejot.data.match.GameScoreEntity
import com.maxeydev.picklejot.data.match.MatchDao
import com.maxeydev.picklejot.data.match.MatchEntity
import com.maxeydev.picklejot.data.match.MatchPersonEntity
import com.maxeydev.picklejot.data.person.PersonDao
import com.maxeydev.picklejot.data.person.PersonEntity
import com.maxeydev.picklejot.data.photo.PhotoEntity
import kotlinx.coroutines.CoroutineDispatcher

const val PICKLEJOT_DB_NAME = "picklejot.db"

const val PICKLEJOT_DB_VERSION = 1

@Database(
    entities = [
        MatchEntity::class,
        PersonEntity::class,
        MatchPersonEntity::class,
        GameScoreEntity::class,
        PhotoEntity::class,
    ],
    version = PICKLEJOT_DB_VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PicklejotDatabase : RoomDatabase() {
    abstract fun matchDao(): MatchDao

    abstract fun personDao(): PersonDao
}

suspend fun createPicklejotDatabase(
    context: Context,
    ioDispatcher: CoroutineDispatcher,
): PicklejotDatabase {
    DatabaseBackup(context, ioDispatcher).backupIfMigrationPending(PICKLEJOT_DB_VERSION)
    return Room
        .databaseBuilder(
            context.applicationContext,
            PicklejotDatabase::class.java,
            PICKLEJOT_DB_NAME,
        ).build()
}
