package sqz.checklist.data.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.File
import okio.Source
import okio.source

/**
 * Remember path until app exited.
 */
private object AppPath {
    var path: String? = null
    var cache: String? = null
}

/**
 * Find then remember the path in Android platform.
 *
 * Get the path of application data directory via [appInternalDirPath].
 *
 * @param context Android Context
 */
fun initInternalDirPath(context: Context) {
    if (AppPath.path != null && AppPath.cache != null) return

    AppPath.path = context.applicationContext.filesDir.absolutePath
    AppPath.cache = context.cacheDir.absolutePath

    val file = File(context.cacheDir, "/temp")
    if (!file.isDirectory) {
        file.mkdirs()
    } else {
        file.delete()
        file.mkdirs()
    }
}

/**
 * Get the path of application data directory.
 *
 * @param type The type of directory.
 * @return The path of application internal data directory.
 * @throws IllegalStateException If [initInternalDirPath] has not been called.
 */
actual fun appInternalDirPath(type: AppDirType): String {
    val path = AppPath.path
    val cache = AppPath.cache
    if (path == null || cache == null) throw IllegalStateException(
        "App path not initialized, please call initInternalDirPath(ctx) first"
    )
    return when (type) {
        AppDirType.Data -> path
        AppDirType.Cache -> cache
        AppDirType.Temp -> "$cache/temp"
    }
}

/**
 * Open a [Source] from a [Uri].
 *
 * @param uri The [Uri] to open.
 */
fun ContentResolver.openUriSource(uri: Uri): Source {
    val inputStream = openInputStream(uri)
        ?: error("Cannot open InputStream for uri=$uri")
    return inputStream.source()
}

/**
 * Get the file name from a [Uri].
 *
 * @param uri The [Uri] to get the file name from.
 */
fun ContentResolver.getFileName(uri: Uri): String {
    query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst())
            return cursor.getString(index)
    }
    return "unknown"
}
