package sqz.checklist.task.api.info

import kotlinx.coroutines.flow.StateFlow
import sqz.checklist.data.database.Task

interface TaskInfo {

    /**
     * Clear the data from [TaskInfoData].
     */
    fun clearTaskInfo()

    /**
     * Set the data for the external method to use.
     *
     * @param config [Config] to control what [TaskInfoData] need provide.
     * @param taskId The id of the task to retrieve.
     */
    suspend fun setTaskInfo(config: Config, taskId: Long)

    /**
     * Get the data for the external method to use.
     *
     * @return [TaskInfoData]
     */
    fun getTaskInfo(): StateFlow<TaskInfoData>

    /**
     * Set the [sqz.checklist.data.database.Task.isPin] to opposite value.
     *
     * @throws IllegalStateException if `pinChangeAllowed == false`
     * @throws NullPointerException if [TaskInfoData] is [TaskInfoData.None]
     */
    fun onPinChangeRequest()

    /**
     * Set [Config] to control what [TaskInfoData] need provide.
     *
     * Note: Expected to use `taskId` in [setTaskInfo] for getting [sqz.checklist.data.database.Task]
     *   or Detail information.
     *
     * @param TaskOnly only provide [TaskInfoData.TaskOnly].
     * @param DetailOnly only provide [TaskInfoData.DetailOnly].
     * @param TaskAndDetail provide both [TaskInfoData.TaskOnly] and [TaskInfoData.DetailOnly].
     */
    sealed interface Config {

        data class TaskOnly(val pinChangeAllowed: Boolean = false) : Config

        data object DetailOnly : Config

        data class TaskAndDetail(val pinChangeAllowed: Boolean = false) : Config
    }

    /** The data for the external method to use. **/
    sealed interface TaskInfoData {

        data object None : TaskInfoData

        data class TaskOnly(
            val task: Task,
            val pinChangeAllowed: Boolean,
        ) : TaskInfoData

        data class DetailOnly(
            val detail: List<DetailInfoState>,
        ) : TaskInfoData

        data class TaskAndDetail(
            val task: Task,
            val detail: List<DetailInfoState>,
            val pinChangeAllowed: Boolean,
        ) : TaskInfoData
    }

    /** The detail type state that convert from database. **/
    data class DetailInfoState(
        val detailDescription: String?,
        val detailType: DetailType,
    ) {
        sealed interface DetailType {

            data class Text(val description: String) : DetailType

            data class URL(val url: String) : DetailType

            @Suppress("ArrayInDataClass")
            data class Application(val launchToken: ByteArray?) : DetailType

            data class Picture(val fileName: String, val path: String) : DetailType

            data class Video(val fileName: String, val path: String) : DetailType

            data class Audio(val fileName: String, val path: String) : DetailType
        }
    }
}
