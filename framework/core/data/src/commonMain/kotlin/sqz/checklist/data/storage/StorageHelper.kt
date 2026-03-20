package sqz.checklist.data.storage

import okio.Path
import okio.Path.Companion.toPath
import sqz.checklist.data.database.TaskDetailType
import sqz.checklist.data.database.model.Platform
import sqz.checklist.data.platform

object StorageHelper {
    /**
     * Check if the path is a temp path.
     *
     * @return true if the path is a temp path, false otherwise.
     */
    fun String.isTempPath(): Boolean {
        try {
            val baseDir: Path = appInternalDirPath(AppDirType.Temp).toPath()
            val thisDir: Path = this.toPath()
            val baseStr = baseDir.toString()
            val thisStr = thisDir.toString()
            return thisStr.startsWith(baseStr)
        } catch (_: IllegalStateException) {
            return false
        }
    }

    fun String.isCachePath(): Boolean {
        try {
            val baseDir: Path = appInternalDirPath(AppDirType.Cache).toPath()
            val thisDir: Path = this.toPath()
            val baseStr = baseDir.toString().also {
                val temp = appInternalDirPath(AppDirType.Temp)
                if (it.startsWith(temp)) {
                    return false
                }
                if (temp.startsWith(it)) {
                    return false
                }
            }
            val thisStr = thisDir.toString()
            return thisStr.startsWith(baseStr)
        } catch (_: IllegalStateException) {
            return false
        }
    }

    /**
     * Check if the path is a data path.
     *
     * Warn: Each platform can have different data path.
     * E.g. Android: `data/data/<package name>/files/`;
     *      iOS: `var/mobile/Containers/Data/Application/<bundle id>/data/`;
     *      ...
     *
     * @return true if the path is a data path, false otherwise.
     */
    fun String.isDataPath(): Boolean {
        try {
            val baseDir: Path = appInternalDirPath(AppDirType.Data).toPath()
            val thisDir: Path = this.toPath()
            val baseStr = baseDir.toString()
            val thisStr = thisDir.toString()
            return thisStr.startsWith(baseStr)
        } catch (_: IllegalStateException) {
            return false
        }
    }

    /**
     * Check if the path is a media path.
     *
     * Expect the path is the key part to store in database for correctly retrieve the file in
     * multiplatform environment. e.g. `/media/<type>/<file_name>`
     *
     * Note: this function will not check valid of whole path.
     *
     * @return true if the path is a media path, false otherwise.
     */
    fun String.isMediaPath(): Boolean {
        val regex = Regex("""^(?!.*/media/.*/media/).*/media/[^/]+/[^/]+/?$""")
        return regex.matches(this)
    }

    /**
     * A set of file names that are reserved for the platform use.
     */
    val reservedFileNames = setOf(
        "CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6","COM7",
        "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",
    )

    /**
     * Regex to check if the file name is illegal.
     *
     * Note: Path String is definitely illegal in this regex.
     */
    val illegalFileNameCharsRegex = Regex("""[\\/:*?"<>|\x00]""")

    /**
     * Convert [TaskDetailType] to internal media path.
     *
     * @return internal path String, e.g. `media/picture/`; or `null` in non-media type
     */
    fun TaskDetailType.toInternalMediaPath(): String? {
        return when (this) {
            TaskDetailType.Text -> null
            TaskDetailType.Application -> null
            TaskDetailType.URL -> null
            TaskDetailType.Picture -> pictureMediaPath
            TaskDetailType.Video -> videoMediaPath
            TaskDetailType.Audio -> audioMediaPath
        }
    }

    /**
     * Convert platform path to safety path which is for multiplatform support.
     *
     * The result can safety save to database.
     *
     * E.g.
     * `/data/user/0/com.sqz.checklist/files/media/audio/1773742731174_168_Reply.mp3`
     *  =>
     * `media/audio/1773742731174_168_Reply.mp3`
     *
     * @return safety path String, platform-specific path will be formatted.
     * @throws UnsupportedOperationException if the path is a temp path or cache path.
     */
    fun String.platformDataPathToSafetyPath(): String {
        if (this.startsWith(appInternalDirPath(AppDirType.Temp))) {
            throw UnsupportedOperationException("Temp path can not be converted to safety path!")
        }
        if (this.startsWith(appInternalDirPath(AppDirType.Cache))) {
            throw UnsupportedOperationException("Cache path can not be converted to safety path!")
        }
        if (this.startsWith(appInternalDirPath(AppDirType.Data))) {
            val startStr = "${appInternalDirPath(AppDirType.Data)}/"
            return this.replace(startStr, "")
        }
        if (platform() == Platform.Android.name) {
            if (this.startsWith("content://") || this.startsWith("file://")) {
                return this.replaceBefore("media", "")
            }
        }
        return this
    }
}
