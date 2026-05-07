package sqz.checklist.data.storage.manager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.Source
import okio.buffer
import okio.use
import sqz.checklist.data.storage.AppDirType
import sqz.checklist.data.storage.StorageHelper.illegalFileNameCharsRegex
import sqz.checklist.data.storage.StorageHelper.reservedFileNames
import sqz.checklist.data.storage.getFileLastModified
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal class StorageManagerImpl(
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val appInternalDirPath: (type: AppDirType) -> String,
) : StorageManager {
    companion object {
        /**
         * The file only for copy to app internal storage
         *
         * `Pair<filePath, fileName>`
         */
        val tempFiles: MutableStateFlow<List<Pair<String, String>>> = MutableStateFlow(
            value = mutableListOf()
        )
    }

    internal fun sanitizeFileName(fileName: String): String {
        var sanitizedFileName = fileName.replace(illegalFileNameCharsRegex, "-")
        if (sanitizedFileName.uppercase() in reservedFileNames) {
            sanitizedFileName = "file-$sanitizedFileName"
        }
        if (fileName.length > 253) {
            val deleteLength = fileName.substring(fileName.length - 250, fileName.length)
            return this.sanitizeFileName("...$deleteLength")
        }
        return sanitizedFileName
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun copyFileToTemp(
        inputSource: () -> Source,
        originalFileName: String?,
    ): Pair<String, String> = withContext(Dispatchers.Default) {

        val dir: Path = appInternalDirPath(AppDirType.Temp).toPath()

        val fileName = sanitizeFileName(originalFileName ?: "unknown")

        fileSystem.createDirectories(dir, mustCreate = false)

        val timestamp = Clock.System.now().toEpochMilliseconds()
        val salt = (0..999).random().toString().padStart(3, '0')

        val storedFileName = "${timestamp}_${salt}_${fileName.replace(" ", "-")}".let {
            if (it.length > 253) {
                it.substring(0, 253)
            } else it
        }
        val dest = dir / storedFileName

        inputSource().buffer().use { src ->
            fileSystem.sink(dest).buffer().use { dst ->
                dst.writeAll(src)
            }
        }

        val pair = dest.toString() to fileName
        tempFiles.update {
            val list = it.toMutableList()
            list.add(pair)
            list.toList()
        }
        return@withContext pair
    }

    override suspend fun deleteTempFile(mode: StorageManager.DeleteMode) {
        when (mode) {
            is StorageManager.DeleteMode.All -> {
                tempFiles.value = listOf()
                val dir: Path = appInternalDirPath(AppDirType.Temp).toPath()
                fileSystem.deleteRecursively(dir, false)
            }

            is StorageManager.DeleteMode.FilePath -> {
                if (!mode.path.startsWith(appInternalDirPath(AppDirType.Temp))) {
                    throw IllegalArgumentException("Invalid path: ${mode.path}")
                }
                val dir: Path = mode.path.toPath()
                tempFiles.update { temp ->
                    val list = temp.toMutableList()
                    list.removeAll { it.first == mode.path }
                    list.toList()
                }
                fileSystem.delete(dir, true)
            }

            is StorageManager.DeleteMode.BeforeTime -> {
                val time = mode.deleteAllFileBefore
                tempFiles.update { temp ->
                    val list = temp.toMutableList()
                    list.removeAll {
                        if (!it.first.contains('/')) {
                            throw IllegalArgumentException("Invalid path: ${it.first}")
                        }
                        val fileTime = it.first
                            .substringAfterLast("/")
                            .substringBefore("_")
                            .toLong()
                        if (fileTime < time) {
                            val dir: Path = it.first.toPath()
                            fileSystem.delete(dir, true)
                        }
                        fileTime < time
                    }
                    list
                }
            }
        }
    }

    override suspend fun moveTempFileToStorage(
        tempFilePath: String,
        storagePath: String,
    ): Pair<String, String> {
        if (!storagePath.startsWith(appInternalDirPath(AppDirType.Data))) {
            throw IllegalArgumentException("Invalid storage path: $storagePath")
        }
        if (!tempFilePath.startsWith(appInternalDirPath(AppDirType.Temp))) {
            throw IllegalArgumentException("Invalid temp path: $tempFilePath")
        }
        var pair: Pair<String, String>? = null
        tempFiles.update { temp ->
            val mList = temp.toMutableList()
            mList.removeAll {
                if (it.first == tempFilePath) {
                    val dir: Path = it.first.toPath()
                    val pathDir = storagePath.toPath().parent!!
                    fileSystem.createDirectories(pathDir, false)
                    fileSystem.atomicMove(dir, storagePath.toPath())
                    pair = storagePath to it.second
                }
                it.first == tempFilePath
            }
            mList.toList()
        }
        return pair!!
    }

    override suspend fun deleteStorageFile(mode: StorageManager.DeleteMode) {
        when (mode) {
            is StorageManager.DeleteMode.All -> {
                val dir: Path = appInternalDirPath(AppDirType.Data).toPath()
                fileSystem.deleteRecursively(dir, false)
            }

            is StorageManager.DeleteMode.FilePath -> {
                if (!mode.path.startsWith(appInternalDirPath(AppDirType.Data))) {
                    if (mode.path.startsWith("media/")) {
                        val errText =
                            "Invalid path due to may not start with `appInternalDirPath(AppDirType.Data)`: ${mode.path}"
                        throw IllegalArgumentException(errText)
                    }
                    throw IllegalArgumentException("Invalid path: ${mode.path}")
                }
                val dir: Path = mode.path.toPath()
                fileSystem.delete(dir, true)
            }

            is StorageManager.DeleteMode.BeforeTime -> {
                val time = mode.deleteAllFileBefore
                val dir: Path = appInternalDirPath(AppDirType.Data).toPath()
                fileSystem.listRecursively(dir).forEach { path ->
                    if (!path.toString().contains('/')) {
                        throw IllegalArgumentException("Invalid path: $path")
                    }
                    val fileTime = path.toString()
                        .substringAfterLast("/")
                        .substringBefore("_")
                        .toLongOrNull()
                    if (fileTime != null && fileTime < time) {
                        fileSystem.delete(path, true)
                    }
                }
            }
        }
    }

    override suspend fun copyStorageFileToCache(
        filePath: String,
        fileSourceName: String?,
    ): Pair<String, String> {
        val cacheDir: Path = appInternalDirPath(AppDirType.Cache).toPath()
        fileSystem.createDirectories(cacheDir, mustCreate = false)
        if (fileSourceName != null) {
            fileSystem.copy(filePath.toPath(), cacheDir / fileSourceName)
            return (cacheDir / fileSourceName).toString() to fileSourceName
        } else {
            val fileName = filePath.substringAfterLast("/")
            fileSystem.copy(filePath.toPath(), cacheDir / fileName)
            return (cacheDir / fileName).toString() to fileName
        }
    }

    override suspend fun deleteCacheFile(mode: StorageManager.DeleteMode) {
        when (mode) {
            is StorageManager.DeleteMode.All -> {
                val dir: Path = appInternalDirPath(AppDirType.Cache).toPath()
                fileSystem.deleteRecursively(dir, false)
            }

            is StorageManager.DeleteMode.FilePath -> {
                if (!mode.path.startsWith(appInternalDirPath(AppDirType.Cache))) {
                    throw IllegalArgumentException("Invalid path: ${mode.path}")
                }
                val dir: Path = mode.path.toPath()
                fileSystem.delete(dir, true)
            }

            is StorageManager.DeleteMode.BeforeTime -> {
                val time = mode.deleteAllFileBefore
                val dir: Path = appInternalDirPath(AppDirType.Cache).toPath()
                fileSystem.listRecursively(dir).forEach { path ->
                    if (!path.toString().contains('/')) {
                        throw IllegalArgumentException("Invalid path: $path")
                    }
                    if (getFileLastModified(path) < time) {
                        fileSystem.delete(path, true)
                    }
                }
            }
        }
    }

    override suspend fun getDirResList(dirType: AppDirType): List<String> {
        val dir: Path = appInternalDirPath(dirType).toPath()
        return fileSystem.listRecursively(dir).toList().map {
            it.toString()
        }
    }

    override val getTempFiles: StateFlow<List<Pair<String, String>>> = tempFiles.asStateFlow()
}
