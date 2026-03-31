package com.sqz.checklist.cache

import android.content.Context
import java.io.File

/**
 * Clear cache file by name
 */
fun deleteCacheFileByName(context: Context, fileName: String): Boolean {
    val cacheFile = File(context.cacheDir, fileName)
    if (cacheFile.exists()) {
        val deleted = cacheFile.delete()
        return deleted
    } else {
        return false
    }
}
