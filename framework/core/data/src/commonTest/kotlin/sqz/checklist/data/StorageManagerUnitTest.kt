package sqz.checklist.data

import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.Path.Companion.toPath
import okio.Source
import okio.fakefilesystem.FakeFileSystem
import sqz.checklist.data.storage.AppDirType
import sqz.checklist.data.storage.manager.StorageManager
import sqz.checklist.data.storage.manager.StorageManagerImpl
import sqz.checklist.data.storage.videoMediaPath
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock

class StorageManagerUnitTest {

    private lateinit var fileSystem: FakeFileSystem
    private lateinit var storageManager: StorageManager

    private fun appInternalDirPath(type: AppDirType): String = when (type) {
        AppDirType.Data -> "/data"
        AppDirType.Cache -> "/cache"
        AppDirType.Temp -> "/temp"
    }

    @BeforeTest
    fun setup() {
        fileSystem = FakeFileSystem()
        // Reset shared state in StorageManagerImpl
        StorageManagerImpl.tempFiles.update { emptyList() }
        storageManager = StorageManagerImpl(fileSystem) { appInternalDirPath(it) }
    }

    @Test
    fun test_sanitizeFileName() {
        val manager = StorageManagerImpl(fileSystem) { appInternalDirPath(it) }
        assertEquals("8964.pdf", manager.sanitizeFileName("8964.pdf"))
        assertEquals("test_--.txt", manager.sanitizeFileName("test_<>.txt"))
    }

    @Test
    fun test_copyFileToTemp() = runTest {
        val content = "hello"
        val fileName = "test.txt"
        val source = { Buffer().writeUtf8(content) as Source }

        val (path, name) = storageManager.copyFileToTemp(source, fileName)

        assertEquals(fileName, name)
        assertTrue(fileSystem.exists(path.toPath()))
        assertEquals(content, fileSystem.read(path.toPath()) { readUtf8() })

        val tempFiles = storageManager.getTempFiles.value
        assertTrue(tempFiles.any { it.first == path && it.second == fileName })
    }

    @Test
    fun test_deleteTempFile_All() = runTest {
        val source = { Buffer().writeUtf8("data") as Source }
        storageManager.copyFileToTemp(source, "f1.txt")
        storageManager.copyFileToTemp(source, "f2.txt")

        storageManager.deleteTempFile(StorageManager.DeleteMode.All)

        assertEquals(0, storageManager.getTempFiles.value.size)
        val tempDir = appInternalDirPath(AppDirType.Temp).toPath()
        if (fileSystem.exists(tempDir)) {
            assertEquals(0, fileSystem.list(tempDir).size)
        }
    }

    @Test
    fun test_deleteTempFile_FilePath() = runTest {
        val source = { Buffer().writeUtf8("data") as Source }
        val (path, _) = storageManager.copyFileToTemp(source, "f1.txt")

        storageManager.deleteTempFile(StorageManager.DeleteMode.FilePath(path))

        assertFalse(fileSystem.exists(path.toPath()))
        assertEquals(0, storageManager.getTempFiles.value.size)
    }

    @Test
    fun test_deleteTempFile_BeforeTime() = runTest {
        val source = { Buffer().writeUtf8("data") as Source }

        val addOld = storageManager.copyFileToTemp(source, "old.txt")
        val midTime = Clock.System.now().toEpochMilliseconds()
        val renameToMockDelay = addOld.first
            .replaceAfterLast('/', "${midTime - 10000}_150old.txt")
        StorageManagerImpl.tempFiles.update {
            val list = it.toMutableList()
            list.removeAll { r -> r.first == addOld.first }
            list.add(renameToMockDelay to "old.txt")
            list.toList()
        }
        fileSystem.atomicMove(addOld.first.toPath(), renameToMockDelay.toPath())

        val (newPath, _) = storageManager.copyFileToTemp(source, "new.txt")

        storageManager.deleteTempFile(
            StorageManager.DeleteMode.BeforeTime(midTime - 1000)
        )

        assertEquals(1, storageManager.getTempFiles.value.size)
        assertEquals(newPath, storageManager.getTempFiles.value.first().first)
    }

    @Test
    fun test_moveTempFileToStorage() = runTest {
        val source = { Buffer().writeUtf8("data") as Source }
        val (tempPath, name) = storageManager.copyFileToTemp(source, "f1.txt")

        val storagePath = "${appInternalDirPath(AppDirType.Data)}/${videoMediaPath}stored.txt"
        fileSystem.createDirectories(appInternalDirPath(AppDirType.Data).toPath())

        val (movedPath, movedName) = storageManager.moveTempFileToStorage(tempPath, storagePath)

        assertEquals(storagePath, movedPath)
        assertEquals(name, movedName)
        assertFalse(fileSystem.exists(tempPath.toPath()))
        assertTrue(fileSystem.exists(storagePath.toPath()))
    }

    @Test
    fun test_copyStorageFileToCache() = runTest {
        val storageDir = appInternalDirPath(AppDirType.Data).toPath()
        fileSystem.createDirectories(storageDir)
        val filePath = storageDir / "f1.txt"
        fileSystem.write(filePath) { writeUtf8("content") }

        val (cachePath, cacheName) = storageManager.copyStorageFileToCache(
            filePath.toString(),
            "cached.txt"
        )

        assertTrue(fileSystem.exists(cachePath.toPath()))
        assertEquals("cached.txt", cacheName)
        assertEquals("content", fileSystem.read(cachePath.toPath()) { readUtf8() })
    }

    @Test
    fun test_deleteCacheFile_All() = runTest {
        val cacheDir = appInternalDirPath(AppDirType.Cache).toPath()
        fileSystem.createDirectories(cacheDir)
        fileSystem.write(cacheDir / "c1.txt") { writeUtf8("content") }

        storageManager.deleteCacheFile(StorageManager.DeleteMode.All)

        if (fileSystem.exists(cacheDir)) {
            assertEquals(0, fileSystem.list(cacheDir).size)
        }
    }

    @Test
    fun test_copyStorageFileToCache_WithName() = runTest {
        val storageDir = appInternalDirPath(AppDirType.Data).toPath()
        fileSystem.createDirectories(storageDir)
        val filePath = storageDir / "f1.txt"
        fileSystem.write(filePath) { writeUtf8("content") }

        val (cachePath, cacheName) = storageManager.copyStorageFileToCache(
            filePath.toString(),
            "cached.txt"
        )

        assertTrue(fileSystem.exists(cachePath.toPath()))
        assertEquals("cached.txt", cacheName)
        assertEquals("content", fileSystem.read(cachePath.toPath()) { readUtf8() })
    }

    @Test
    fun test_copyStorageFileToCache_NullName() = runTest {
        val storageDir = appInternalDirPath(AppDirType.Data).toPath()
        fileSystem.createDirectories(storageDir)
        val filePath = storageDir / "original.txt"
        fileSystem.write(filePath) { writeUtf8("content") }

        val (cachePath, cacheName) = storageManager.copyStorageFileToCache(
            filePath.toString(),
            null
        )
        assertEquals("original.txt", cacheName)
        assertTrue(fileSystem.exists(cachePath.toPath()))
    }

    @Test
    fun test_deleteStorageFile_All() = runTest {
        val storageDir = appInternalDirPath(AppDirType.Data).toPath()
        fileSystem.createDirectories(storageDir)
        fileSystem.write(storageDir / "s1.txt") { writeUtf8("data") }

        storageManager.deleteStorageFile(StorageManager.DeleteMode.All)

        if (fileSystem.exists(storageDir)) {
            assertEquals(0, fileSystem.list(storageDir).size)
        }
    }

    @Test
    fun test_deleteStorageFile_FilePath() = runTest {
        val storageDir = appInternalDirPath(AppDirType.Data).toPath()
        fileSystem.createDirectories(storageDir)
        val path = storageDir / "s1.txt"
        fileSystem.write(path) { writeUtf8("data") }

        storageManager.deleteStorageFile(StorageManager.DeleteMode.FilePath(path.toString()))

        assertFalse(fileSystem.exists(path))
    }

    @Test
    fun test_deleteStorageFile_BeforeTime() = runTest {
        val storageDir = appInternalDirPath(AppDirType.Data).toPath()
        fileSystem.createDirectories(storageDir)

        val oldPath = storageDir / "1000_old.txt"
        val newPath = storageDir / "2000_new.txt"

        fileSystem.write(oldPath) { writeUtf8("old") }
        fileSystem.write(newPath) { writeUtf8("new") }

        storageManager.deleteStorageFile(StorageManager.DeleteMode.BeforeTime(1500L))

        assertFalse(fileSystem.exists(oldPath))
        assertTrue(fileSystem.exists(newPath))
    }

    @Test
    fun test_deleteCacheFile_FilePath() = runTest {
        val cacheDir = appInternalDirPath(AppDirType.Cache).toPath()
        fileSystem.createDirectories(cacheDir)
        val path = cacheDir / "c1.txt"
        fileSystem.write(path) { writeUtf8("data") }

        storageManager.deleteCacheFile(StorageManager.DeleteMode.FilePath(path.toString()))

        assertFalse(fileSystem.exists(path))
    }

    @Test
    fun test_deleteCacheFile_BeforeTime() = runTest {
        val cacheDir = appInternalDirPath(AppDirType.Cache).toPath()
        fileSystem.createDirectories(cacheDir)

        val path = cacheDir / "file.txt"

        fileSystem.write(path) { writeUtf8("file") }

        try {
            storageManager.deleteCacheFile(StorageManager.DeleteMode.BeforeTime(100))
            assertFalse(fileSystem.exists(path))
        } catch (_: Exception) {
        }
    }

    @Test
    fun test_copyFileToTemp_NullName() = runTest {
        val source = { Buffer().writeUtf8("data") as Source }
        val (path, name) = storageManager.copyFileToTemp(source, null)
        assertEquals("unknown", name)
        assertTrue(fileSystem.exists(path.toPath()))
    }

    @Test
    fun test_moveTempFileToStorage_NotFound() = runTest {
        assertFailsWith<IllegalArgumentException> {
            val tPath = "${appInternalDirPath(AppDirType.Data)}/storage/path"
            storageManager.moveTempFileToStorage("/non/existent", tPath)
        }
    }
}
