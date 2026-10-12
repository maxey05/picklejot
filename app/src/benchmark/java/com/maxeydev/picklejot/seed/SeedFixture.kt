@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.seed

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.match.GameScore
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchRepository
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.person.Person
import com.maxeydev.picklejot.domain.person.PersonRepository
import com.maxeydev.picklejot.domain.photo.PhotoRef
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import java.io.File
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

const val SEEDED_MATCH_COUNT = 1_000

private const val RANDOM_SEED = 4
private const val SPAN_DAYS = 1_826
private const val SOURCE_PHOTO_COUNT = 24
private const val PHOTO_WIDTH = 2048
private const val PHOTO_HEIGHT = 1536
private const val JPEG_QUALITY = 80
private const val CIRCLES_PER_PHOTO = 12
private const val MATCH_ID_HIGH_BITS = 0x5EED_0000_0000_0001L
private const val PHOTO_ID_HIGH_BITS = 0x5EED_0000_0000_0002L
private const val MILLIS_PER_DAY = 86_400_000L
private const val WINNING_SCORE = 11
private const val MAX_LOSING_SCORE = 10
private const val MAX_GAMES = 3
private val FIRST_DATE: AppDate = AppDate.parse("2021-09-26")
private val PLAYER_NAMES =
    (
        "Ana Ben Carla Dave Elena Franco Gia Hugo Isabel Jonas Kaye Luis Mara Nico Olivia " +
            "Paolo Quinn Rosa Sam Tessa Uma Vince Wes Xandra Yuri Zoe Aaron Bea Cris Dina"
    ).split(" ")

class SeedFixture(
    private val matchRepository: MatchRepository,
    private val personRepository: PersonRepository,
    private val filesDirectory: File,
    private val markerFile: File,
) {
    suspend fun seedOnce(): Int {
        if (markerFile.exists()) {
            return markerFile.readText().trim().toInt()
        }
        val sourceDirectory = File(filesDirectory, "seed-sources")
        val sources = writeSourcePhotos(sourceDirectory)
        val players = PLAYER_NAMES.map { personRepository.findOrCreatePerson(it) }
        val random = Random(RANDOM_SEED)
        repeat(SEEDED_MATCH_COUNT) { index ->
            matchRepository.saveMatch(seededMatch(index, random, players, sources))
        }
        sourceDirectory.deleteRecursively()
        markerFile.parentFile?.mkdirs()
        markerFile.writeText(SEEDED_MATCH_COUNT.toString())
        return SEEDED_MATCH_COUNT
    }

    private fun seededMatch(
        index: Int,
        random: Random,
        players: List<Person>,
        sources: List<File>,
    ): Match {
        val format = if (index % 4 == 0) MatchFormat.SINGLES else MatchFormat.DOUBLES
        val shuffled = players.shuffled(random)
        val opponentCount =
            when {
                index % 10 == 0 -> 0
                format == MatchFormat.SINGLES -> 1
                index % 7 == 0 -> 1
                else -> 2
            }
        val partner = if (format == MatchFormat.DOUBLES && index % 3 != 0) shuffled[opponentCount] else null
        val date = FIRST_DATE.plus(index * SPAN_DAYS / SEEDED_MATCH_COUNT, DateTimeUnit.DAY)
        val loggedAt = AppInstant.fromEpochMilliseconds(date.toEpochDays() * MILLIS_PER_DAY + index)
        return Match(
            id = Uuid.fromLongs(MATCH_ID_HIGH_BITS, index.toLong()),
            format = format,
            date = date,
            result = if (random.nextBoolean()) MatchResult.WIN else MatchResult.LOSS,
            createdAt = loggedAt,
            updatedAt = loggedAt,
            opponents = shuffled.take(opponentCount),
            partner = partner,
            games = List(random.nextInt(MAX_GAMES + 1)) { game -> seededGame(game + 1, random) },
            photos = if (index % 10 == 9) emptyList() else listOf(seededPhoto(index, sources[index % sources.size])),
        )
    }

    private fun seededGame(
        gameNumber: Int,
        random: Random,
    ): GameScore {
        val losingScore = random.nextInt(MAX_LOSING_SCORE)
        return if (random.nextBoolean()) {
            GameScore(gameNumber, WINNING_SCORE, losingScore)
        } else {
            GameScore(gameNumber, losingScore, WINNING_SCORE)
        }
    }

    private fun seededPhoto(
        index: Int,
        source: File,
    ): PhotoRef {
        val relativePath = "photos/seed-$index.jpg"
        val destination = File(filesDirectory, relativePath)
        destination.parentFile?.mkdirs()
        source.copyTo(destination, overwrite = true)
        return PhotoRef(
            id = Uuid.fromLongs(PHOTO_ID_HIGH_BITS, index.toLong()),
            relativePath = relativePath,
            width = PHOTO_WIDTH,
            height = PHOTO_HEIGHT,
            byteSize = destination.length(),
            sortIndex = 0,
        )
    }

    private fun writeSourcePhotos(directory: File): List<File> {
        directory.mkdirs()
        return List(SOURCE_PHOTO_COUNT) { index ->
            val file = File(directory, "source-$index.jpg")
            val bitmap = Bitmap.createBitmap(PHOTO_WIDTH, PHOTO_HEIGHT, Bitmap.Config.ARGB_8888)
            paintSourcePhoto(Canvas(bitmap), index)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            bitmap.recycle()
            file
        }
    }

    private fun paintSourcePhoto(
        canvas: Canvas,
        index: Int,
    ) {
        val width = PHOTO_WIDTH.toFloat()
        val height = PHOTO_HEIGHT.toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, width, height, hue(index, 0), hue(index, 1), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width, height, paint)
        paint.shader = null
        val random = Random(index)
        repeat(CIRCLES_PER_PHOTO) { circle ->
            paint.color = hue(index, circle + 2)
            val centerX = random.nextFloat() * width
            val centerY = random.nextFloat() * height
            val radius = random.nextFloat() * height / 3
            canvas.drawCircle(centerX, centerY, radius, paint)
        }
    }

    private fun hue(
        index: Int,
        step: Int,
    ): Int = Color.HSVToColor(floatArrayOf(((index * 37 + step * 53) % 360).toFloat(), 0.55f, 0.85f))
}
