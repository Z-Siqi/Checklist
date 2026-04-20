package com.sqz.checklist.common.media

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.util.Log
import android.widget.Toast
import androidx.annotation.IntRange
import androidx.core.content.FileProvider
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy
import com.sqz.checklist.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import sqz.checklist.data.preferences.PreferencesInCache
import sqz.checklist.data.storage.manager.StorageManager
import java.io.FileNotFoundException
import kotlin.coroutines.resume

/**
 * Opens a video using an external application via an Intent.
 * It creates a temporary cache file to share the video with other apps.
 *
 * @param videoName The name for the image file.
 * @param videoPath The okio image path.
 * @param context The application context.
 */
suspend fun openVideoBySystem(
    videoName: String,
    videoPath: String,
    context: Context,
) = withContext(Dispatchers.IO) {
    val storageManager = StorageManager.provider()
    val name = videoName.ifBlank { "unknown_name" }

    val cache = PreferencesInCache(context)
    cache.waitingDeletedCacheName().let { getCachePath ->
        if (getCachePath != null) {
            try { // delete old cache
                val delMode = StorageManager.DeleteMode.FilePath(getCachePath)
                storageManager.deleteCacheFile(delMode)
                Log.d("openVideoBySystem", "Deleted cache: $getCachePath")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            cache.waitingDeletedCacheName(null)
        }
    }

    try {
        val toCacheFile = storageManager.copyStorageFileToCache(
            filePath = videoPath,
            fileSourceName = name
        )
        val videoToUri = (toCacheFile.first.toPath().toFile()).let { file ->
            FileProvider.getUriForFile(
                context, "${context.packageName}.provider", file
            )
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(videoToUri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.open_with))
        )
        cache.waitingDeletedCacheName(toCacheFile.first)
    } catch (_: FileNotFoundException) {
        Toast.makeText(context, context.getString(R.string.failed_open), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Compresses a video file in place.
 *
 * Behavior:
 * - compressionRate == 0: no transcoding is performed, returns success immediately
 * - compressionRate in 1..100: transcode the source video to a temporary file,
 *   then replaces the original file if transcoding succeeds
 *
 * Important:
 * - This function is designed for local file paths, not Uri input.
 * - The original file is only replaced after a successful transcode.
 * - If transcoding fails or is canceled, the original file is left untouched.
 *
 * Return value:
 * - true  -> success
 * - false -> invalid parameter, unsupported input, canceled, or transcode failure
 *
 * Progress:
 * - onProgress receives values in the approximate range 0.0..100.0
 */
suspend fun compressVideoInPlace(
    sourcePath: Path,
    @IntRange(from = 0, to = 100) compressionRate: Int,
    onProgress: (Double) -> Unit = {},
    fileSystem: FileSystem = FileSystem.SYSTEM,
): Boolean = suspendCancellableCoroutine { continuation ->
    // Accept only the same range as the old implementation.
    if (compressionRate !in 0..100) {
        continuation.resume(false)
        return@suspendCancellableCoroutine
    }

    // If no compression is requested, treat this as success.
    // Since the source and destination are the same path, no copy is needed.
    if (compressionRate == 0) {
        onProgress(100.0)
        continuation.resume(true)
        return@suspendCancellableCoroutine
    }

    // This implementation replaces the original file only after success.
    val tempPath = buildTempVideoPath(sourcePath)

    try {
        // Ensure the parent directory exists.
        sourcePath.parent?.let { parent ->
            if (!fileSystem.exists(parent)) {
                fileSystem.createDirectories(parent)
            }
        }

        // Remove any stale temp file from a previous failed attempt.
        fileSystem.delete(tempPath, mustExist = false)

        // Read the source bitrate. Fall back to a default value if metadata is unavailable.
        val sourceBitrate = readVideoBitrateOrDefault(
            sourcePath = sourcePath,
            defaultBitrate = 5_800_000L,
        )

        // Keep the original recursive bitrate reduction strategy.
        val targetBitrate = computeTargetBitrate(
            bitrate = sourceBitrate,
            rate = compressionRate,
        ).coerceAtLeast(1L)

        val future = Transcoder.into(tempPath.toString())
            .addDataSource(sourcePath.toString())
            .setVideoTrackStrategy(
                DefaultVideoStrategy.Builder()
                    .bitRate(targetBitrate)
                    .build()
            )
            .setListener(object : TranscoderListener {

                override fun onTranscodeProgress(progress: Double) {
                    onProgress(progress * 100.0)
                }

                override fun onTranscodeCompleted(successCode: Int) {
                    try {
                        replacePath(
                            from = tempPath,
                            to = sourcePath,
                            fileSystem = fileSystem,
                        )
                        onProgress(100.0)
                        if (continuation.isActive) continuation.resume(true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        try {
                            fileSystem.delete(tempPath, mustExist = false)
                        } catch (_: Exception) {
                        }
                        if (continuation.isActive) continuation.resume(false)
                    }
                }

                override fun onTranscodeCanceled() {
                    try {
                        fileSystem.delete(tempPath, mustExist = false)
                    } catch (_: Exception) {
                    }
                    if (continuation.isActive) continuation.resume(false)
                }

                override fun onTranscodeFailed(exception: Throwable) {
                    exception.printStackTrace()
                    try {
                        fileSystem.delete(tempPath, mustExist = false)
                    } catch (_: Exception) {
                    }
                    if (continuation.isActive) continuation.resume(false)
                }
            })
            .transcode()

        // Cancel the underlying transcode task if the coroutine is canceled.
        continuation.invokeOnCancellation {
            try {
                future.cancel(true)
            } catch (_: Exception) {
            }
            try {
                fileSystem.delete(tempPath, mustExist = false)
            } catch (_: Exception) {
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        try {
            fileSystem.delete(tempPath, mustExist = false)
        } catch (_: Exception) {
        }
        if (continuation.isActive) continuation.resume(false)
    }
}

/**
 * Reads the average bitrate from a local video file.
 *
 * If metadata cannot be read, returns the provided default bitrate.
 */
@Suppress("SameParameterValue")
private fun readVideoBitrateOrDefault(
    sourcePath: Path,
    defaultBitrate: Long,
): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(sourcePath.toString())
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            ?.toLongOrNull()
            ?: defaultBitrate
    } catch (_: Exception) {
        defaultBitrate
    } finally {
        try {
            retriever.release()
        } catch (_: Exception) {
        }
    }
}

/**
 * Keeps the original bitrate reduction behavior from the old implementation.
 *
 * Higher rate values produce lower target bitrates.
 */
private fun computeTargetBitrate(
    bitrate: Long,
    rate: Int,
): Long {
    val recursion = (rate / 1.5).toInt()
    return if (recursion > 1) {
        computeTargetBitrate(
            bitrate = bitrate * (100 - rate) / 100,
            rate = recursion,
        )
    } else {
        bitrate
    }
}

/**
 * Replaces the target file with the source file.
 *
 * The target is deleted first to avoid issues on file systems where atomicMove
 * fails if the destination already exists.
 */
private fun replacePath(
    from: Path,
    to: Path,
    fileSystem: FileSystem,
) {
    fileSystem.delete(to, mustExist = false)
    fileSystem.atomicMove(from, to)
}

/**
 * Builds a temporary file path in the same directory as the original file.
 *
 * Example:
 * /a/b/video.mp4 -> /a/b/video.mp4.tmp
 */
private fun buildTempVideoPath(path: Path): Path {
    val parent = path.parent
    val tempName = "${path.name}.tmp"
    return if (parent != null) {
        parent / tempName
    } else {
        tempName.toPath()
    }
}
