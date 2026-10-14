package com.maxeydev.picklejot.ui.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import com.maxeydev.picklejot.domain.share.CardRatio
import com.maxeydev.picklejot.domain.share.CardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

internal val targetContext
    get() = InstrumentationRegistry.getInstrumentation().targetContext

internal fun <T> onMain(block: () -> T): T = runBlocking { withContext(Dispatchers.Main) { block() } }

internal fun photoDataUri(
    base: Int,
    spread: Int,
    width: Int = 1600,
    height: Int = 1200,
): String {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint()
    val random = Random(base)
    val block = 40
    for (y in 0 until height step block) {
        for (x in 0 until width step block) {
            val value = (base + random.nextInt(-spread, spread + 1)).coerceIn(0, 255)
            paint.color = Color.rgb(value, value, value)
            canvas.drawRect(x.toFloat(), y.toFloat(), (x + block).toFloat(), (y + block).toFloat(), paint)
        }
    }
    val bytes =
        ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            output.toByteArray()
        }
    bitmap.recycle()
    return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
}

internal fun sampleCard(
    brand: String = "Picklejot",
    displayName: String = "Matty",
    opponents: List<String> = listOf("Ana", "Ben"),
    location: String? = "Ayala Triangle Gardens",
    photo: String? = null,
    ratio: CardRatio = CardRatio.TALL,
    theme: CardTheme = CardTheme.DARK,
): CardData =
    CardData(
        brand = brand,
        displayName = displayName,
        meta = "Doubles · Sep 20, 2026",
        partner = CardEntry("With", listOf("Cy")),
        time = CardEntry("Time", listOf("1h 16m")),
        opponents = opponents.takeIf { it.isNotEmpty() }?.let { CardEntry("Against", it) },
        games = CardEntry("Games", listOf("11–9", "8–11", "11–7")),
        location = location,
        photo = photo,
        ratio = ratio,
        theme = theme,
    )

private fun channel(value: Int): Double {
    val normalized = value / 255.0
    return if (normalized <= 0.03928) normalized / 12.92 else ((normalized + 0.055) / 1.055).pow(2.4)
}

internal fun luminance(pixel: Int): Double =
    0.2126 * channel(Color.red(pixel)) + 0.7152 * channel(Color.green(pixel)) + 0.0722 * channel(Color.blue(pixel))

internal fun contrastAgainstWhite(pixel: Int): Double = 1.05 / (luminance(pixel) + 0.05)

internal fun contrastBetween(
    first: Int,
    second: Int,
): Double {
    val lighter = maxOf(luminance(first), luminance(second))
    val darker = minOf(luminance(first), luminance(second))
    return (lighter + 0.05) / (darker + 0.05)
}

internal val LIGHT_THEME_TEXT: Int = Color.rgb(0x14, 0x23, 0x1d)

internal fun goldenDirectory(): File = File(targetContext.getExternalFilesDir(null), "card-goldens")

internal fun JSONObject.rect(id: String): JSONObject = getJSONObject("rects").getJSONObject(id)

internal fun saveForReview(
    name: String,
    bitmap: Bitmap,
) {
    val directory = goldenDirectory().apply { mkdirs() }
    File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
}

internal fun countInkWithin(
    bitmap: Bitmap,
    rect: JSONObject,
    background: Int,
    minimumDistance: Int = 150,
): Int {
    val left = rect.getDouble("left").toInt().coerceAtLeast(0)
    val right = rect.getDouble("right").toInt().coerceAtMost(bitmap.width - 1)
    val top = rect.getDouble("top").toInt().coerceAtLeast(0)
    val bottom = rect.getDouble("bottom").toInt().coerceAtMost(bitmap.height - 1)
    var ink = 0
    for (y in top..bottom step 2) {
        for (x in left..right step 2) {
            val pixel = bitmap.getPixel(x, y)
            val distance =
                abs(Color.red(pixel) - Color.red(background)) +
                    abs(Color.green(pixel) - Color.green(background)) +
                    abs(Color.blue(pixel) - Color.blue(background))
            if (distance >= minimumDistance) {
                ink++
            }
        }
    }
    return ink
}
