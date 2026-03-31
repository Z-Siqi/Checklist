package sqz.checklist.data.storage

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.Source
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.posix.stat

// not implemented

@OptIn(ExperimentalForeignApi::class)
actual fun appInternalDirPath(type: AppDirType): String {
    val fm = NSFileManager.defaultManager
    val dir = when (type) {
        AppDirType.Data -> fm.URLsForDirectory(NSApplicationSupportDirectory, NSUserDomainMask)

        AppDirType.Cache -> fm.URLsForDirectory(NSCachesDirectory, NSUserDomainMask)

        AppDirType.Temp -> listOf(NSURL.fileURLWithPath(NSTemporaryDirectory()))
    }
    val url = dir.firstOrNull() as? NSURL ?: error("Directory not found")
    val path = url.path!!
    fm.createDirectoryAtPath(path, true, null, null)
    return path
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun getFileLastModified(path: Path): Long {
    memScoped {
        val statBuf = alloc<stat>()
        if (stat(path.toString(), statBuf.ptr) == 0) {
            return statBuf.st_mtimespec.tv_sec
        }
    }
    error("Failed to get last modified time of file: $path")
}

fun sourceFromPath(path: String): Source {
    return FileSystem.SYSTEM.source(path.toPath())
}

@Suppress("unused")
fun sourceFromPath(url: NSURL): Source {
    val path = url.path ?: error("NSURL has no path")
    return sourceFromPath(path)
}
