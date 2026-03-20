package sqz.checklist.data.storage

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
