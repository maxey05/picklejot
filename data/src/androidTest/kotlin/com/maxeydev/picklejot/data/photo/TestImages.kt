package com.maxeydev.picklejot.data.photo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.ExifInterface
import java.io.File
import kotlin.random.Random

internal fun writeTestJpeg(
    file: File,
    width: Int,
    height: Int,
    orientation: Int = ExifInterface.ORIENTATION_NORMAL,
    seed: Int = 1,
): File {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val random = Random(seed)
    val paint = Paint()
    val block = 16
    for (y in 0 until height step block) {
        for (x in 0 until width step block) {
            paint.color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
            canvas.drawRect(x.toFloat(), y.toFloat(), (x + block).toFloat(), (y + block).toFloat(), paint)
        }
    }
    paint.color = Color.RED
    canvas.drawRect(0f, 0f, width / 4f, height / 4f, paint)
    file.parentFile?.mkdirs()
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
    bitmap.recycle()
    if (orientation != ExifInterface.ORIENTATION_NORMAL) {
        ExifInterface(file.path).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            saveAttributes()
        }
    }
    return file
}
