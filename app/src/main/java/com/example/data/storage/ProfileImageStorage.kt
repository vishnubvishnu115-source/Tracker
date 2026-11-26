package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Native Android profile photo pipeline that decodes with two-pass subsampling,
 * corrects EXIF orientation, scales within 512x512, and compresses to a lightweight JPEG (~30-60 KB)
 * without requiring Coil or any external image-loading/caching dependency.
 */
object ProfileImageStorage {

    const val AVATAR_FILE_NAME = "operator_profile_avatar.jpg"
    const val LEGACY_AVATAR_FILE_NAME = "operator_profile_avatar.img"

    private const val MAX_AVATAR_DIMENSION_PX = 512
    private const val AVATAR_JPEG_QUALITY = 82

    fun saveCompressedAvatar(context: Context, sourceUri: Uri): String? {
        val resolver = context.contentResolver

        // Pass 1: Measure dimensions without allocating pixel memory
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        resolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, boundsOptions)
        } ?: return null

        val srcWidth = boundsOptions.outWidth
        val srcHeight = boundsOptions.outHeight
        if (srcWidth <= 0 || srcHeight <= 0) return null

        // Read EXIF orientation safely (API 24+ supports ExifInterface(InputStream))
        val exifOrientation = runCatching {
            resolver.openInputStream(sourceUri)?.use { exifStream ->
                ExifInterface(exifStream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        // Pass 2: Decode with power-of-two subsampling to prevent OOM on 48MP+ camera photos
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(srcWidth, srcHeight, MAX_AVATAR_DIMENSION_PX)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val sampledBitmap = resolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        } ?: return null

        var transformedBitmap: Bitmap? = null
        try {
            transformedBitmap = orientAndScaleBitmap(
                source = sampledBitmap,
                exifOrientation = exifOrientation,
                maxDimensionPx = MAX_AVATAR_DIMENSION_PX,
            )

            val tempFile = File(context.filesDir, "$AVATAR_FILE_NAME.tmp")
            val targetFile = File(context.filesDir, AVATAR_FILE_NAME)

            FileOutputStream(tempFile).use { output ->
                val ok = transformedBitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    AVATAR_JPEG_QUALITY,
                    output,
                )
                if (!ok) return null
                output.flush()
            }

            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            // Clean up any legacy uncompressed avatar file from older installs
            val legacyFile = File(context.filesDir, LEGACY_AVATAR_FILE_NAME)
            if (legacyFile.exists()) {
                legacyFile.delete()
            }

            return if (targetFile.exists() && targetFile.length() > 0L) {
                targetFile.absolutePath
            } else {
                null
            }
        } finally {
            if (transformedBitmap != null && transformedBitmap !== sampledBitmap) {
                transformedBitmap.recycle()
            }
            sampledBitmap.recycle()
        }
    }

    fun decodeAvatarBitmap(path: String): Bitmap? {
        val file = File(path)
        if (!file.exists() || file.length() <= 0L) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(
                width = bounds.outWidth,
                height = bounds.outHeight,
                maxDim = MAX_AVATAR_DIMENSION_PX,
            )
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(path, options)
    }

    fun deleteAvatar(context: Context) {
        runCatching { File(context.filesDir, AVATAR_FILE_NAME).delete() }
        runCatching { File(context.filesDir, LEGACY_AVATAR_FILE_NAME).delete() }
        runCatching { File(context.filesDir, "$AVATAR_FILE_NAME.tmp").delete() }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var inSampleSize = 1
        var halfW = width / 2
        var halfH = height / 2
        while (halfW >= maxDim && halfH >= maxDim) {
            inSampleSize *= 2
            halfW /= 2
            halfH /= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun orientAndScaleBitmap(
        source: Bitmap,
        exifOrientation: Int,
        maxDimensionPx: Int,
    ): Bitmap {
        val matrix = Matrix()
        when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
        }

        val maxSide = max(source.width, source.height)
        if (maxSide > maxDimensionPx) {
            val scale = maxDimensionPx.toFloat() / maxSide.toFloat()
            matrix.postScale(scale, scale)
        }

        if (matrix.isIdentity) return source

        val result = Bitmap.createBitmap(
            source,
            0,
            0,
            source.width,
            source.height,
            matrix,
            true,
        )
        // Guard against tiny floating-point rounding slightly exceeding maxDimensionPx
        if (max(result.width, result.height) > maxDimensionPx + 2) {
            val scale = maxDimensionPx.toFloat() / max(result.width, result.height).toFloat()
            val targetW = (result.width * scale).roundToInt().coerceAtLeast(1)
            val targetH = (result.height * scale).roundToInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(result, targetW, targetH, true)
            if (scaled !== result && result !== source) {
                result.recycle()
            }
            return scaled
        }
        return result
    }
}
