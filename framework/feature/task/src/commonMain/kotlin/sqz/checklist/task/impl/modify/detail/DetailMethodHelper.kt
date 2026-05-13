package sqz.checklist.task.impl.modify.detail

import sqz.checklist.data.database.TaskDetailType
import sqz.checklist.task.api.modify.TaskModify.Detail
import sqz.checklist.task.api.modify.TaskModify.Detail.TypeState

/**
 * Convert a [TaskDetailType] to an empty [Detail.TypeState].
 */
internal fun TaskDetailType.toEmptyDetailType(): TypeState {
    return when (this) {
        TaskDetailType.Text -> TypeState.Text()
        TaskDetailType.URL -> TypeState.URL()
        TaskDetailType.Application -> TypeState.Application()
        TaskDetailType.Picture -> TypeState.Picture()
        TaskDetailType.Video -> TypeState.Video()
        TaskDetailType.Audio -> TypeState.Audio()
    }
}

/**
 * Check if the [Detail.TypeState] contains a path.
 *
 * Note: this function will not check valid of the path.
 */
internal fun TypeState.containsPath(): Boolean {
    return when (this) {
        is TypeState.Picture -> path.isNotBlank()
        is TypeState.Video -> path.isNotBlank()
        is TypeState.Audio -> path.isNotBlank()
        else -> false
    }
}

/**
 * Get the file path of the [Detail.TypeState].
 *
 * Note: this function will not check valid of the path.
 *
 * @return the file path or null if the [Detail.TypeState] does not contain a path.
 */
internal fun TypeState.getFilePath(): String? {
    val getPathString = when (this) {
        is TypeState.Picture -> path
        is TypeState.Video -> path
        is TypeState.Audio -> path
        else -> null
    }
    if (getPathString.isNullOrBlank()) {
        return null
    }
    return getPathString
}

/**
 * Get the file path of the [Detail.TypeState].
 *
 * Note: this function will not check valid of the path.
 *
 * @return `Pair<filePath, fileName>` or null if the [Detail.TypeState] does not contain a path.
 */
@Suppress("unused")
internal fun TypeState.getFileInfo(): Pair<String, String>? {
    val getFilePair = when (this) {
        is TypeState.Picture -> path to fileName
        is TypeState.Video -> path to fileName
        is TypeState.Audio -> path to fileName
        else -> null
    }
    if (getFilePair?.first.isNullOrBlank()) {
        return null
    }
    return getFilePair
}
