package com.maxeydev.picklejot.data.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlin.math.max
import kotlin.math.roundToInt

const val MAX_LONG_EDGE_PX = 2048
const val JPEG_QUALITY = 80

class ImageCompressor(
    private val openSource: (String) -> InputStream?,
    private val maxLongEdge: Int = MAX_LONG_EDGE_PX,
    private val jpegQuality: Int = JPEG_QUALITY,
) {
    fun compress(
        source: String,
        target: File,
    ): CompressionResult =
        try {
            compressOrNull(source, target)?.let { (width, height) ->
                CompressionResult.Compressed(width = width, height = height, byteSize = target.length())
            } ?: CompressionResult.Unreadable
        } catch (unreadable: IOException) {
            target.delete()
            CompressionResult.Unreadable
        } catch (notPermitted: SecurityException) {
            target.delete()
            CompressionResult.Unreadable
        } catch (malformed: IllegalArgumentException) {
            target.delete()
            CompressionResult.Unreadable
        }

    private fun compressOrNull(
        source: String,
        target: File,
    ): Pair<Int, Int>? {
        val bounds = readBounds(source) ?: return null
        val orientation = readOrientation(source)
        val decoded = decodeSampled(source, bounds) ?: return null
        val scaled = scaleToFit(decoded)
        val oriented = applyOrientation(scaled, orientation)
        try {
            writeJpeg(oriented, target)
            return oriented.width to oriented.height
        } finally {
            oriented.recycle()
        }
    }

    private fun readBounds(source: String): Pair<Int, Int>? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(source).use { stream -> BitmapFactory.decodeStream(stream, null, options) }
        return if (options.outWidth > 0 && options.outHeight > 0) {
            options.outWidth to options.outHeight
        } else {
            null
        }
    }

    private fun readOrientation(source: String): Int =
        open(source).use { stream ->
            ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }

    private fun decodeSampled(
        source: String,
        bounds: Pair<Int, Int>,
    ): Bitmap? {
        val options =
            BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(max(bounds.first, bounds.second))
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        return open(source).use { stream -> BitmapFactory.decodeStream(stream, null, options) }
    }

    private fun sampleSizeFor(longEdge: Int): Int {
        var sampleSize = 1
        while (longEdge / (sampleSize * 2) >= maxLongEdge) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun scaleToFit(bitmap: Bitmap): Bitmap {
        val longEdge = max(bitmap.width, bitmap.height)
        if (longEdge <= maxLongEdge) {
            return bitmap
        }
        val ratio = maxLongEdge.toDouble() / longEdge
        val width = (bitmap.width * ratio).roundToInt().coerceIn(1, maxLongEdge)
        val height = (bitmap.height * ratio).roundToInt().coerceIn(1, maxLongEdge)
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        if (scaled !== bitmap) {
            bitmap.recycle()
        }
        return scaled
    }

    private fun applyOrientation(
        bitmap: Bitmap,
        orientation: Int,
    ): Bitmap {
        val matrix = orientationMatrix(orientation) ?: return bitmap
        val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (oriented !== bitmap) {
            bitmap.recycle()
        }
        return oriented
    }

    private fun orientationMatrix(orientation: Int): Matrix? =
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> Matrix().apply { postRotate(QUARTER_TURN) }
            ExifInterface.ORIENTATION_ROTATE_180 -> Matrix().apply { postRotate(HALF_TURN) }
            ExifInterface.ORIENTATION_ROTATE_270 -> Matrix().apply { postRotate(THREE_QUARTER_TURN) }
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Matrix().apply { postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> Matrix().apply { postScale(1f, -1f) }
            ExifInterface.ORIENTATION_TRANSPOSE ->
                Matrix().apply {
                    postRotate(QUARTER_TURN)
                    postScale(-1f, 1f)
                }
            ExifInterface.ORIENTATION_TRANSVERSE ->
                Matrix().apply {
                    postRotate(THREE_QUARTER_TURN)
                    postScale(-1f, 1f)
                }
            else -> null
        }

    private fun writeJpeg(
        bitmap: Bitmap,
        target: File,
    ) {
        target.parentFile?.mkdirs()
        val partial = File(target.parentFile, "${target.name}.partial")
        partial.outputStream().use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, output)) {
                throw IOException("The photo could not be encoded as a JPEG.")
            }
        }
        if (!partial.renameTo(target)) {
            partial.delete()
            throw IOException("The compressed photo could not be moved into place.")
        }
    }

    private fun open(source: String): InputStream =
        openSource(source) ?: throw IOException("The photo source could not be opened.")

    private companion object {
        const val QUARTER_TURN = 90f
        const val HALF_TURN = 180f
        const val THREE_QUARTER_TURN = 270f
    }
}
