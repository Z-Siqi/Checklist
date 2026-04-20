package sqz.checklist.data.storage.manager

import kotlinx.coroutines.flow.StateFlow
import okio.FileSystem
import okio.SYSTEM
import okio.Source
import sqz.checklist.data.storage.AppDirType
import sqz.checklist.data.storage.appInternalDirPath

interface StorageManager {

    /**
     * Copy file to app internal storage
     *
     * @param inputSource Source
     * @param originalFileName the full of the file, e.g. `"xxx.jpg"` / `"xxx"`
     * @return `Pair(fullPathString, fileName)`
     */
    suspend fun copyFileToTemp(
        inputSource: () -> Source,
        originalFileName: String?,
    ): Pair<String, String>

    /**
     * Delete temp file
     *
     * @param mode DeleteMode
     */
    suspend fun deleteTempFile(mode: DeleteMode)

    /**
     * Move temp file to storage.
     *
     * [storagePath] must be start with `appInternalDirPath(AppDirType.Data)`: e.g.
     * ```
     * val path = "${appInternalDirPath(AppDirType.Data)}/${pictureMediaPath}file.png"
     * ```
     *
     * @param tempFilePath the path of the file to move
     * @param storagePath the path of the storage to move to
     * @return `Pair(pathStringAfterMove, fileName)`
     * @throws NullPointerException if the file path is invalid
     * @throws IllegalArgumentException if the storage or temp path is invalid
     */
    suspend fun moveTempFileToStorage(
        tempFilePath: String,
        storagePath: String
    ): Pair<String, String>

    /**
     * Delete storage file
     *
     * @param mode DeleteMode
     * @throws IllegalArgumentException if the path is invalid
     */
    suspend fun deleteStorageFile(mode: DeleteMode)

    /**
     * Copy storage file to cache
     *
     * @param filePath the path of the file to copy
     * @param fileSourceName the name of the file to copy,
     *     if null will use the name from [filePath].
     * @return `Pair(fullPathString, fileName)`
     */
    suspend fun copyStorageFileToCache(
        filePath: String,
        fileSourceName: String?,
    ): Pair<String, String>

    /**
     * Delete cache file
     *
     * @param mode DeleteMode
     * @throws okio.IOException if the delete file not existed.
     * @throws IllegalArgumentException if the path is invalid.
     */
    suspend fun deleteCacheFile(mode: DeleteMode)

    /**
     * File delete mode
     */
    sealed interface DeleteMode {

        object All : DeleteMode

        data class FilePath(val path: String) : DeleteMode

        data class BeforeTime(val deleteAllFileBefore: Long) : DeleteMode
    }

    /**
     * Get the temp files. only keep it until app exited.
     *
     * @return A stateFlow of `List<Pair<filePathString, fileNameString>>`.
     */
    val getTempFiles: StateFlow<List<Pair<String, String>>>

    suspend fun getDirResList(dirType: AppDirType): List<String>

    // Provider
    companion object {
        fun provider(fileSystem: FileSystem = FileSystem.SYSTEM): StorageManager {
            return StorageManagerImpl(fileSystem) { appInternalDirPath(it) }
        }
    }
}
