package com.sqz.checklist.cache

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.sqz.checklist.preferences.PreferencesInCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Cleans up the in-process files path list if the path not exist.
 *
 * This function won't delete any file, just clean up inProcessFilesPath data. It check each path
 * line and remove the path that not a file exist.
 *
 * @param prefsCache The `PreferencesInCache` instance used for data storage.
 */
suspend fun dropEmptyInProcessFilesPath(prefsCache: PreferencesInCache) {
    withContext(Dispatchers.Default) {
        val lines = prefsCache.inProcessFilesPath()?.lineSequence()?.toList() ?: return@withContext
        try {
            val check = lines.filter { line ->
                val file = File(line.toUri().path ?: "")
                if (!file.exists()) {
                    Log.e(
                        "dropEmptyInProcessFilesPath",
                        "This path not exist: $line. This shouldn't happen, need to report to developer"
                    )
                    return@filter false
                }
                return@filter true
            }
            prefsCache.inProcessFilesPath(null)
            for (i in check) {
                if (!i.isBlank()) prefsCache.inProcessFilesPath(i + "\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Cleans up the in-process files path list if the path not exist.
 *
 * This function won't delete any file, just clean up inProcessFilesPath data. It check each path
 * line and remove the path that not a file exist.
 *
 * @param context The application context.
 */
suspend fun dropEmptyInProcessFilesPath(context: Context) {
    val cache = PreferencesInCache(context)
    dropEmptyInProcessFilesPath(cache)
}

/**
 * Deletes all files listed in the in-process files path.
 *
 * This function iterates through each file path stored in `inProcessFilesPath`. It attempts to
 * delete each file. If a file is successfully deleted, its path is removed from the list. If
 * deletion fails or the file doesn't exist, the path is retained for later inspection.
 *
 * @param prefsCache The `PreferencesInCache` instance used for data storage.
 */
suspend fun deleteAllFileWhichInProcessFilesPath(prefsCache: PreferencesInCache) {
    withContext(Dispatchers.IO) {
        val lines = prefsCache.inProcessFilesPath()?.lineSequence()?.toList() ?: return@withContext
        try {
            val check = lines.filter { line ->
                val file = File(line.toUri().path ?: "")
                if (file.exists()) {
                    if (file.delete()) {
                        return@filter false
                    }
                    Log.e("deleteAllFileWhichInProcessPath", "Failed to delete file: $file")
                    return@filter true
                }
                Log.e(
                    "deleteAllFileWhichInProcessPath",
                    "This path not exist: $line, this shouldn't happen, need to report to developer"
                )
                return@filter true
            }
            prefsCache.inProcessFilesPath(null)
            for (i in check) {
                if (!i.isBlank()) prefsCache.inProcessFilesPath(i + "\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Deletes all files listed in the in-process files path.
 *
 * This function iterates through each file path stored in `inProcessFilesPath`. It attempts to
 * delete each file. If a file is successfully deleted, its path is removed from the list. If
 * deletion fails or the file doesn't exist, the path is retained for later inspection.
 *
 * @param context The application context.
 */
suspend fun deleteAllFileWhichInProcessFilesPath(context: Context) {
    val cache = PreferencesInCache(context)
    deleteAllFileWhichInProcessFilesPath(cache)
}
