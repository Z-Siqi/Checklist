package sqz.checklist.data.storage

import okio.Path

const val pictureMediaPath = "media/picture/"

const val videoMediaPath = "media/video/"

const val audioMediaPath = "media/audio/"

enum class AppDirType {
    Data, Cache, Temp
}

/**
 * Get the path of application data directory.
 *
 * @param type The type of directory.
 * @return The path of application internal data directory.
 * @throws IllegalStateException if platform actual method request init but call this before init.
 */
expect fun appInternalDirPath(type: AppDirType): String

/**
 * Get the last modified time of the file.
 *
 * @param path The path of the file.
 * @return The last modified time of the file.
 */
internal expect fun getFileLastModified(path: Path): Long
