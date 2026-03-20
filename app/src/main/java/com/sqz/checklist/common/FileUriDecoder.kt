package com.sqz.checklist.common

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/**
 * A data class representing a decoded URI for a file.
 *
 * @property size The size of the file in bytes.
 * @property name The name of the file.
 * @property uri The URI of the file.
 */
data class FileUriDecoder(
    val size: Long,
    val name: String,
    val uri: Uri,
) {
    fun sizeToMB(): Long = this.size / 1024 / 1024
}

/**
 * Decodes the file size and name from a given URI.
 *
 * @param uri The URI of the file.
 * @return A [FileUriDecoder] object containing the file size, name, and URI.
 */
fun Context.fileUriDecoder(uri: Uri): FileUriDecoder? {
    this.contentResolver.query(
        uri, null, null, null, null
    )?.use { cursor ->
        val sizeIndex: Long? = cursor.getColumnIndex(OpenableColumns.SIZE).let {
            if (it != -1 && cursor.moveToFirst()) {
                return@let cursor.getLong(it)
            } else return@let null
        }
        val nameIndex: String? = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).let {
            if (it != -1 && cursor.moveToFirst()) {
                return@let cursor.getString(it)
            } else return@let null
        }
        if (sizeIndex == null || nameIndex == null) {
            return null
        }
        return FileUriDecoder(
            size = sizeIndex, name = nameIndex, uri = uri
        )
    }
    return null
}
