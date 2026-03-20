package com.sqz.checklist.common.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.IntRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import androidx.core.graphics.scale
import kotlin.math.max

/**
 * Select the compression format based on the file extension, and write the compressed result
 * back to the original path.
 *
 * Supported file extensions:
 * - jpg / jpeg -> JPEG
 * - png        -> PNG
 * - webp       -> WEBP
 *
 * Example：
 * - compression = 0   -> quality = 100 (return before process)
 * - compression = 1   -> quality = 99  (almost not compress)
 * - compression = 50  -> quality = 50  (compress half)
 * - compression = 100 -> quality = 0   (100% compress)
 *
 * @return
 * - `true`: Compression and overwrite success.
 * - `false`: Failed, or failed due to unsupported extension name.
 */
suspend fun compressImageInPlace(
    path: Path,
    @IntRange(0, 100) compression: Int,
    fileSystem: FileSystem = FileSystem.SYSTEM,
): Boolean = withContext(Dispatchers.IO) {
    // Only compression levels 1 to 100 are accepted.
    if (compression !in 1..100) return@withContext false

    // The higher the compression value, the lower the quality.
    val quality = 100 - compression

    // The compression format is determined by the file extension.
    val format = path.toCompressFormat() ?: return@withContext false

    // Use the temporary file path in the same directory to replace the original file
    // after successful installation, to avoid directly overwriting and corrupting the source file.
    val tempPath = buildTempPath(path)

    var bitmap: Bitmap? = null

    try {
        // First, read the original file completely into memory, then decode it into a Bitmap.
        // This avoids the overwriting problem that can occur when
        // "reading and writing to the original file simultaneously".
        val bytes = fileSystem.read(path) { readByteArray() }

        bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: return@withContext false

        // First write to a temporary file
        val writeSuccess = fileSystem.write(tempPath) {
            if (format == Bitmap.CompressFormat.PNG) {
                val maxSide = getValueByPercentage(quality / 100.0)
                bitmap = bitmap.scaledDownIfNeeded(maxSide.toInt())
            }
            bitmap.compress(format, quality, outputStream())
        }

        if (!writeSuccess) {
            // If compress() returns false, it means the encoding failed.
            fileSystem.delete(tempPath, mustExist = false)
            return@withContext false
        }

        // Replace the original file with a temporary file.
        // On some platforms, `atomicMove(target already exists)` may fail, so delete the
        // original file before moving it.
        fileSystem.delete(path, mustExist = false)
        fileSystem.atomicMove(tempPath, path)

        true
    } catch (e: Exception) {
        e.printStackTrace()

        // Try to clean up temporary files when the process fails.
        try {
            fileSystem.delete(tempPath, mustExist = false)
        } catch (_: Exception) {
        }

        false
    } finally {
        // Release native memory as soon as possible
        bitmap?.recycle()
    }
}

private fun getValueByPercentage(percent: Double): Double {
    val start = 1680.0
    val end = 4808.0
    return start + (end - start) * percent
}

/**
 * Maps the path extension to the corresponding Bitmap compression format.
 *
 * Returning null indicates that the current file extension is not supported.
 */
private fun Path.toCompressFormat(): Bitmap.CompressFormat? {
    val extension = name.substringAfterLast('.', "").lowercase()

    return when (extension) {
        "jpg", "jpeg" -> Bitmap.CompressFormat.JPEG

        // PNG is a lossless format, so quality is usually ignored.
        "png" -> Bitmap.CompressFormat.PNG

        // The .webp extension only tells you that it is a webp file; it doesn't tell you whether
        // it was originally a lossy or lossless file.
        // The reason for choosing lossy here is that your compression/quality semantics are
        // inherently "lossy compression".
        "webp" -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
        }

        else -> null
    }
}

/**
 * Shrink the Bitmap by its maximum side length.
 *
 * Rule:
 * - maxSide <= 0: Return to the original image without scaling.
 * - If the original image has both width and height <= maxSide: Return to the original image
 *   without scaling.
 * - Otherwise, scale down proportionally to avoid stretching and deformation.
 */
private fun Bitmap.scaledDownIfNeeded(maxSide: Int): Bitmap {
    if (maxSide <= 0) return this

    val srcWidth = width
    val srcHeight = height
    val srcMaxSide = max(srcWidth, srcHeight)

    // no need scaled down if the max side is not larger than the original
    if (srcMaxSide <= maxSide) return this

    val scale = maxSide.toFloat() / srcMaxSide.toFloat()
    val targetWidth = (srcWidth * scale).toInt().coerceAtLeast(1)
    val targetHeight = (srcHeight * scale).toInt().coerceAtLeast(1)

    return this.scale(targetWidth, targetHeight)
}

/**
 * Generate a temporary file path in the same directory as the target file.
 *
 * Such as:
 * /a/b/c.jpg -> /a/b/c.jpg.tmp
 */
private fun buildTempPath(path: Path): Path {
    val parent = path.parent
    val tempName = "${path.name}.tmp"

    return if (parent != null) {
        parent / tempName
    } else {
        tempName.toPath()
    }
}
