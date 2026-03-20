package sqz.checklist.data.storage.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okio.Source
import sqz.checklist.data.storage.AppDirType

class StorageManagerFake : StorageManager {
    override suspend fun copyFileToTemp(
        inputSource: () -> Source,
        originalFileName: String?
    ): Pair<String, String> {
        return "temp/851000168_851168.txt" to "168.txt"
    }

    override suspend fun deleteTempFile(mode: StorageManager.DeleteMode) {
    }

    override suspend fun moveTempFileToStorage(
        tempFilePath: String,
        storagePath: String
    ): Pair<String, String> {
        return "data/851000168_851168.txt" to "168.txt"
    }

    override suspend fun deleteStorageFile(mode: StorageManager.DeleteMode) {
    }

    override suspend fun copyStorageFileToCache(
        filePath: String,
        fileSourceName: String?
    ): Pair<String, String> {
        return "cache/851000168_851168.txt" to "168.txt"
    }

    override suspend fun deleteCacheFile(mode: StorageManager.DeleteMode) {
    }

    override val getTempFiles: StateFlow<List<Pair<String, String>>> = MutableStateFlow(
        value = emptyList()
    )

    override suspend fun getDirResList(dirType: AppDirType): List<String> {
        return listOf()
    }
}
