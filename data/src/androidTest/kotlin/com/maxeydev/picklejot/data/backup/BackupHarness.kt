@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.maxeydev.picklejot.data.db.PicklejotDatabase
import com.maxeydev.picklejot.data.match.RoomMatchRepository
import com.maxeydev.picklejot.data.person.RoomPersonRepository
import com.maxeydev.picklejot.data.photo.PhotoStore
import com.maxeydev.picklejot.domain.backup.ExportPromptState
import com.maxeydev.picklejot.domain.backup.ExportPromptStore
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.datetime.plusDays
import com.maxeydev.picklejot.domain.entitlement.EntitlementSignal
import com.maxeydev.picklejot.domain.match.GameScore
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.person.Person
import com.maxeydev.picklejot.domain.photo.PhotoRef
import com.maxeydev.picklejot.domain.profile.Entitlement
import com.maxeydev.picklejot.domain.profile.EntitlementRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.InputStream
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class TestEntitlements(
    isPro: Boolean,
) : EntitlementRepository {
    private val state = MutableStateFlow(Entitlement(isPro = isPro))

    override fun observeEntitlement(): Flow<Entitlement> = state

    override suspend fun record(signal: EntitlementSignal) {
        throw UnsupportedOperationException("Backup tests never record entitlement signals.")
    }
}

class InMemoryExportPromptStore : ExportPromptStore {
    val state = MutableStateFlow(ExportPromptState())

    override fun observe(): Flow<ExportPromptState> = state

    override suspend fun update(transform: (ExportPromptState) -> ExportPromptState) {
        state.value = transform(state.value)
    }
}

class FixedClock(
    var current: AppInstant,
) : Clock {
    override fun now(): AppInstant = current
}

class BackupHarness(
    isPro: Boolean = false,
    openSource: ((String) -> InputStream?)? = null,
) {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val workDirectory = File(context.cacheDir, "backup-test-${Uuid.random()}").apply { mkdirs() }
    val photoRoot = File(workDirectory, "files").apply { mkdirs() }
    val exportDirectory = File(workDirectory, "exports")
    val database: PicklejotDatabase = Room.inMemoryDatabaseBuilder(context, PicklejotDatabase::class.java).build()
    val photoStore = PhotoStore(photoRoot)
    val matches = RoomMatchRepository(database, photoStore, Dispatchers.IO)
    val people = RoomPersonRepository(database, Dispatchers.IO)
    val entitlements = TestEntitlements(isPro)
    val promptStore = InMemoryExportPromptStore()
    val clock = FixedClock(AppInstant.fromEpochMilliseconds(1_790_000_000_000L))
    val repository =
        RoomBackupRepository(
            database = database,
            photoStore = photoStore,
            entitlements = entitlements,
            promptStore = promptStore,
            exportDirectory = exportDirectory,
            openSource = openSource ?: { path -> File(path).inputStream() },
            clock = clock,
            ioDispatcher = Dispatchers.IO,
        )

    fun close() {
        database.close()
        workDirectory.deleteRecursively()
    }

    fun photoFile(relativePath: String): File =
        photoStore.resolve(relativePath).also {
            it.parentFile?.mkdirs()
            it.writeBytes(ByteArray(PHOTO_BYTES) { index -> index.toByte() })
        }

    fun writeImport(
        name: String,
        text: String,
    ): String = File(workDirectory, name).also { it.writeText(text) }.path

    suspend fun seed(
        count: Int,
        opponent: String = "Sam Rivera",
        firstDate: String = "2024-01-01",
    ): List<Uuid> {
        val rival = people.findOrCreatePerson(opponent)
        val start = AppDate.parse(firstDate)
        return (0 until count).map { index ->
            val id = Uuid.random()
            matches.saveMatch(
                Match(
                    id = id,
                    format = MatchFormat.SINGLES,
                    date = start.plusDays(index),
                    result = if (index % 2 == 0) MatchResult.WIN else MatchResult.LOSS,
                    createdAt = AppInstant.fromEpochMilliseconds(1_000L + index),
                    updatedAt = AppInstant.fromEpochMilliseconds(1_000L + index),
                    opponents = listOf(rival),
                    games = listOf(GameScore(1, 11, 7)),
                ),
            )
            id
        }
    }

    suspend fun seedRich(): List<Uuid> {
        val ana = people.findOrCreatePerson("Ana Cruz")
        val ben = people.findOrCreatePerson("Ben Ortiz")
        val cy = people.findOrCreatePerson("Cy Lim")
        val firstPhoto = "photos/${Uuid.random()}.jpg"
        photoFile(firstPhoto)
        val richId = Uuid.random()
        matches.saveMatch(
            Match(
                id = richId,
                format = MatchFormat.DOUBLES,
                date = AppDate.parse("2026-08-15"),
                result = MatchResult.WIN,
                createdAt = AppInstant.fromEpochMilliseconds(5_000),
                updatedAt = AppInstant.fromEpochMilliseconds(6_000),
                startTime = AppTime.parse("18:30"),
                endTime = AppTime.parse("19:45"),
                location = "Ayala Triangle",
                opponents = listOf(ana, ben),
                partner = cy,
                games = listOf(GameScore(1, 11, 8), GameScore(2, 9, 11), GameScore(3, 11, 6)),
                paddle = "Selkirk \"Vanguard\"",
                notes = "Windy, with a comma, a \"quote\", ünïcödé 🏓 and\na second line.",
                photos = listOf(PhotoRef(Uuid.random(), firstPhoto, 800, 600, PHOTO_BYTES.toLong(), 0)),
            ),
        )
        val plainId = Uuid.random()
        matches.saveMatch(
            Match(
                id = plainId,
                format = MatchFormat.SINGLES,
                date = AppDate.parse("2026-08-20"),
                result = MatchResult.LOSS,
                createdAt = AppInstant.fromEpochMilliseconds(7_000),
                updatedAt = AppInstant.fromEpochMilliseconds(7_000),
                opponents = listOf(ana),
                games = listOf(GameScore(1, 4, 11)),
            ),
        )
        return listOf(richId, plainId)
    }

    suspend fun allMatches(): List<Match> =
        database
            .matchDao()
            .allMatchIds()
            .map { id -> requireNotNull(matches.observeById(Uuid.parse(id)).first()) }
            .sortedBy { it.id.toString() }

    suspend fun allPeople(): List<Person> = people.observeAll().first().sortedBy { it.id.toString() }

    suspend fun matchCount(): Int = database.matchDao().matchCount()

    private companion object {
        const val PHOTO_BYTES = 2_048
    }
}
