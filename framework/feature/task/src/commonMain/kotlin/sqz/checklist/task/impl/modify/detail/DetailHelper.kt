package sqz.checklist.task.impl.modify.detail

import sqz.checklist.common.UrlRegexHelper
import sqz.checklist.data.database.TaskDetail
import sqz.checklist.data.database.TaskDetailType
import sqz.checklist.data.platform
import sqz.checklist.data.storage.StorageHelper.isDataPath
import sqz.checklist.data.storage.StorageHelper.isMediaPath
import sqz.checklist.data.storage.StorageHelper.isTempPath
import sqz.checklist.task.api.TaskModify
import sqz.checklist.task.api.TaskModify.Detail

internal object DetailHelper {

    /**
     * Determines if a new item can be added to the task detail list based on a weighted count.
     * Different item types have different "weights" (e.g., Video=5, Picture=2, Audio=3, others=1).
     *
     * @param list The list of [TaskModify.Detail] items.
     * @return a [Boolean] emitting `true` if the total weight is more than 10, `false` otherwise.
     *   Once return `true` means should not add new item.
     */
    fun listLimit(list: List<Detail.UIState>): Boolean {
        val totalWeight = list.sumOf {
            when (it.typeState) {
                is Detail.TypeState.Video -> 5
                is Detail.TypeState.Audio -> 3
                is Detail.TypeState.Picture -> 2
                else -> 1
            }
        }
        return totalWeight >= 10
    }

    private fun checkTextValid(text: Detail.TypeState.Text): Boolean {
        return text.description.isNotBlank()
    }

    private fun checkUrlValid(url: Detail.TypeState.URL): Boolean {
        return url.url.let {
            when {
                it.matches(UrlRegexHelper.ipv4Regex) -> true
                it.matches(UrlRegexHelper.ipv4UrlRegex) -> true
                it.matches(UrlRegexHelper.ipv6UrlRegex) -> true
                it.matches(UrlRegexHelper.urlRegex) -> true
                else -> false
            }
        }
    }

    private fun checkApplicationValid(data: Detail.TypeState.Application): Boolean {
        return data.launchToken?.let { it.size > 2 } ?: false
    }

    private fun checkMediaValid(data: Detail.TypeState): Boolean {
        val path = data.getFilePath() ?: return false
        if (path.isBlank()) return false
        return path.isTempPath() || path.isDataPath() || path.isMediaPath()
    }

    /**
     * Check if the [TaskModify.Detail.TypeState] is valid.
     *
     * @return `true` if the [TaskModify.Detail.TypeState] is valid, `false` otherwise.
     */
    fun checkTypeStateValid(data: Detail.TypeState): Boolean {
        return when (data) {
            is Detail.TypeState.Text -> checkTextValid(data)
            is Detail.TypeState.URL -> checkUrlValid(data)
            is Detail.TypeState.Application -> checkApplicationValid(data)
            is Detail.TypeState.Picture -> checkMediaValid(data)
            is Detail.TypeState.Video -> checkMediaValid(data)
            is Detail.TypeState.Audio -> checkMediaValid(data)
        }
    }

    private fun Detail.TypeState.URL.format(): Detail.TypeState.URL {
        if (this.url.matches(UrlRegexHelper.ipv4Regex)) {
            return Detail.TypeState.URL(url = "http://${this.url}")
        }
        if (this.url.matches(UrlRegexHelper.ipv4UrlRegex)) {
            return this
        }
        if (this.url.matches(UrlRegexHelper.ipv6UrlRegex)) {
            return this
        }
        if (this.url.matches(UrlRegexHelper.urlRegex)) {
            if (this.url.startsWith("http://")) {
                return this
            }
            if (!this.url.startsWith("https://")) {
                return Detail.TypeState.URL(url = "https://${this.url}")
            }
            return this
        }
        throw IllegalArgumentException("Invalid URL")
    }

    /**
     * Format the [TaskModify.Detail.TypeState] to a valid format.
     *
     * @param data The [TaskModify.Detail.TypeState] to be formatted.
     * @return The formatted [TaskModify.Detail.TypeState].
     */
    fun formatData(data: Detail.TypeState): Detail.TypeState {
        return when (data) {
            is Detail.TypeState.URL -> data.format()
            else -> data
        }
    }

    /**
     * Convert the [Detail.UIState] to a [TaskDetail].
     *
     * @param data The [Detail.UIState] to be converted.
     * @return The converted [TaskDetail].
     * @throws Exception Expected throw exception if not call [checkTypeStateValid] first.
     */
    fun convertToDatabaseDetail(data: Detail.UIState): TaskDetail {
        val draftTaskDetail = TaskDetail(
            id = 0, taskId = 0,                 // No need to set here
            description = data.itemDescription, // Set item name
            type = TaskDetailType.Text,         // Draft type, must be set via copy() later
            dataString = null,                  // Draft type, must be set via copy() later
            dataByte = "".encodeToByteArray(),  // Draft type, must be set via copy() later
        )
        return when (data.typeState!!) {
            is Detail.TypeState.Text -> draftTaskDetail.copy(
                type = TaskDetailType.Text,
                dataByte = data.typeState.description.encodeToByteArray(),
            )

            is Detail.TypeState.URL -> draftTaskDetail.copy(
                type = TaskDetailType.URL,
                dataByte = data.typeState.url.encodeToByteArray(),
            )

            is Detail.TypeState.Application -> draftTaskDetail.copy(
                type = TaskDetailType.Application,
                dataString = platform(),
                dataByte = data.typeState.launchToken!!,
            )

            is Detail.TypeState.Picture -> draftTaskDetail.copy(
                type = TaskDetailType.Picture,
                dataString = data.typeState.fileName,
                dataByte = data.typeState.path.encodeToByteArray(),
            )

            is Detail.TypeState.Video -> draftTaskDetail.copy(
                type = TaskDetailType.Video,
                dataString = data.typeState.fileName,
                dataByte = data.typeState.path.encodeToByteArray(),
            )

            is Detail.TypeState.Audio -> draftTaskDetail.copy(
                type = TaskDetailType.Audio,
                dataString = data.typeState.fileName,
                dataByte = data.typeState.path.encodeToByteArray(),
            )
        }
    }
}
