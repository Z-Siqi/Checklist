package com.sqz.checklist.common.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
