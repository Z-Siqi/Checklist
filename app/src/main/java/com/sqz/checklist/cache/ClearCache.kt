package com.sqz.checklist.cache

import android.content.Context

/**
 * Clear old cache files. Default: clear one day before cache files
 */
fun clearExpiredCache(context: Context, expirationTimeMillis: Long = ONE_DAY_LONG) {
    val cacheDir = context.cacheDir
    val currentTime = System.currentTimeMillis()
    if (cacheDir.exists()) {
        val files = cacheDir.listFiles()
        files?.forEach { file ->
            if (file.isFile) {
                val lastModifiedTime = file.lastModified()
                if (currentTime - lastModifiedTime > expirationTimeMillis) {
                    file.delete()
                }
            }
        }
    }
}

/**
 * Clear old cache when cache dir more than 50MB and more than one file
 */
fun clearOldCacheIfNeeded(context: Context) {
    val cacheDir = context.cacheDir
    if (cacheDir.exists() && cacheDir.isDirectory) {
        val files = cacheDir.listFiles()?.filter { it.isFile } ?: return
        if (files.size > 1) {
            val totalSize = files.sumOf { it.length() }
            if (totalSize > DATA_SIZE_50MB) {
                val sortedFiles = files.sortedByDescending { it.lastModified() }
                sortedFiles.drop(1).forEach { file ->
                    file.delete()
                }
            }
        }
    }
}
