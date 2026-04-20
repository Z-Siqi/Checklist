package com.sqz.checklist.common.media

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.sqz.checklist.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import sqz.checklist.data.preferences.PreferencesInCache
import sqz.checklist.data.storage.manager.StorageManager
import java.io.FileNotFoundException

/**
 * Opens an audio using an external application via an Intent.
 * It creates a temporary cache file to share the audio with other apps.
 *
 * @param audioName The name for the audio file.
 * @param audioPath The okio image path.
 * @param context The application context.
 */
suspend fun openAudioBySystem(
    audioName: String,
    audioPath: String,
    context: Context,
) = withContext(Dispatchers.IO) {
    val storageManager = StorageManager.provider()
    val name = audioName.ifBlank { "unknown_name" }

    val cache = PreferencesInCache(context)
    cache.waitingDeletedCacheName().let { getCachePath ->
        if (getCachePath != null) {
            try { // delete old cache
                val delMode = StorageManager.DeleteMode.FilePath(getCachePath)
                storageManager.deleteCacheFile(delMode)
                Log.d("openAudioBySystem", "Deleted cache: $getCachePath")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            cache.waitingDeletedCacheName(null)
        }
    }

    try {
        val toCacheFile = storageManager.copyStorageFileToCache(
            filePath = audioPath,
            fileSourceName = name
        )
        val audioToUri = (toCacheFile.first.toPath().toFile()).let { file ->
            FileProvider.getUriForFile(
                context, "${context.packageName}.provider", file
            )
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(audioToUri, "audio/*")
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
 * Get the album art from an audio [Uri].
 *
 * @param uri The [Uri] to get the album art from.
 */
suspend fun Context.getAlbumArt(uri: Uri): Bitmap? = withContext(Dispatchers.Default) {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(this@getAlbumArt, uri)
        val artBytes = retriever.embeddedPicture ?: return@withContext null
        return@withContext BitmapFactory.decodeByteArray(
            artBytes, 0, artBytes.size
        )
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    } finally {
        retriever.release()
    }
}

/**
 * Convert millisecond to minute
 */
fun Long.toMinute(): Long = this / 1000 / 60

/**
 * Convert millisecond to second without minute
 */
fun Long.toSecond(): Long = (this / 1000).let {
    fun second(second: Long): Long = if (second > 59) second(second - 60) else second
    second(it)
}

/**
 * Convert seconds to string format
 */
fun Long.toStringSecond(): String {
    val second = this.toSecond()
    return if (second < 10) "0$second" else "$second"
}
