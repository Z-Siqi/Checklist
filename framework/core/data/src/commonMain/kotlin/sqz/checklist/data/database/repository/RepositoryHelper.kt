package sqz.checklist.data.database.repository

import okio.IOException
import okio.Path.Companion.toPath
import sqz.checklist.data.database.TaskDetail
import sqz.checklist.data.database.pathStringConverter
import sqz.checklist.data.storage.AppDirType
import sqz.checklist.data.storage.StorageHelper.isTempPath
import sqz.checklist.data.storage.StorageHelper.platformDataPathToSafetyPath
import sqz.checklist.data.storage.StorageHelper.toInternalMediaPath
import sqz.checklist.data.storage.appInternalDirPath
import sqz.checklist.data.storage.manager.StorageManager

/**
 * Delete [TaskDetail] storage file. Ignore delete if this [TaskDetail] don't contain storage file.
 *
 * @param storageManager Storage manager for delete file.
 * @param verifyList The list to ensure this [TaskDetail] path not in it.
 *   Stop delete if found same [TaskDetail] path in the [verifyList].
 * @see pathStringConverter
 */
internal suspend fun TaskDetail.deleteTaskDetailStorageFile(
    storageManager: StorageManager, verifyList: List<TaskDetail>? = null
) {
    this.type.pathStringConverter(this.dataByte)?.let {
        if (verifyList != null) {
            val isSamePathFind = verifyList.any { find ->
                val getListItemPath = find.type.pathStringConverter(find.dataByte) ?: false
                return@any getListItemPath == it
            }
            if (isSamePathFind) return
        }
        try {
            val toStr = it.let { let ->
                if (let.startsWith("file:///") || let.startsWith("content://")) {
                    return@let let.replaceBefore("media", "")
                }
                return@let let
            }
            val path: String = toStr.let { let ->
                if (let.startsWith(appInternalDirPath(AppDirType.Data))) {
                    return@let let
                }
                "${appInternalDirPath(AppDirType.Data)}/${let}"
            }
            val delMode = StorageManager.DeleteMode.FilePath(path)
            storageManager.deleteStorageFile(delMode)
        } catch (e: IOException) {
            println("Storage file may deleted before this called, which is unexpected!")
            e.printStackTrace()
        }
    }
}

/**
 * Move temp file to internal storage. Ignore move if this [TaskDetail] don't contain temp file or
 * it already in internal storage.
 *
 * @see pathStringConverter
 */
internal suspend fun TaskDetail.moveTempToInternalStorage(
    storageManager: StorageManager
): String? {
    this.type.pathStringConverter(this.dataByte)?.let {
        if (it.isTempPath()) {
            val mediaPath = this.type.toInternalMediaPath()
                ?: throw IllegalArgumentException("Unknown type")
            val fileName: String = it.toPath().name
            val fullPath = "${appInternalDirPath(AppDirType.Data)}/${mediaPath}${fileName}"
            val moveTo =
                storageManager.moveTempFileToStorage(it, fullPath).first
            return moveTo.platformDataPathToSafetyPath()
        }
    }
    return null
}
