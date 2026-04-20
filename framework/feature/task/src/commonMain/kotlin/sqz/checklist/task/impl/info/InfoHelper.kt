package sqz.checklist.task.impl.info

import sqz.checklist.data.database.Task
import sqz.checklist.data.database.TaskDetail
import sqz.checklist.data.database.TaskDetailType
import sqz.checklist.task.api.info.TaskInfo

internal fun List<TaskDetail>?.convertTaskDetail(): List<TaskInfo.DetailInfoState> {
    if (this == null) return listOf()
    return this.let { taskDetails ->
        val list = mutableListOf<TaskInfo.DetailInfoState>()
        for (data in taskDetails) {
            val typeState = when (data.type) {
                TaskDetailType.Text -> TaskInfo.DetailInfoState(
                    detailDescription = data.description,
                    detailType = TaskInfo.DetailInfoState.DetailType.Text(
                        description = data.dataByte.decodeToString()
                    )
                )

                TaskDetailType.URL -> TaskInfo.DetailInfoState(
                    detailDescription = data.description,
                    detailType = TaskInfo.DetailInfoState.DetailType.URL(
                        url = data.dataByte.decodeToString()
                    )
                )

                TaskDetailType.Application -> TaskInfo.DetailInfoState(
                    detailDescription = data.description,
                    detailType = TaskInfo.DetailInfoState.DetailType.Application(
                        launchToken = data.dataByte
                    )
                )

                TaskDetailType.Picture -> TaskInfo.DetailInfoState(
                    detailDescription = data.description,
                    detailType = TaskInfo.DetailInfoState.DetailType.Picture(
                        fileName = data.dataString ?: "unknown",
                        path = data.dataByte.decodeToString()
                    )
                )

                TaskDetailType.Video -> TaskInfo.DetailInfoState(
                    detailDescription = data.description,
                    detailType = TaskInfo.DetailInfoState.DetailType.Video(
                        fileName = data.dataString ?: "unknown",
                        path = data.dataByte.decodeToString()
                    )
                )

                TaskDetailType.Audio -> TaskInfo.DetailInfoState(
                    detailDescription = data.description,
                    detailType = TaskInfo.DetailInfoState.DetailType.Audio(
                        fileName = data.dataString ?: "unknown",
                        path = data.dataByte.decodeToString()
                    )
                )
            }
            list.add(typeState)
        }
        list.toList()
    }
}

internal fun TaskInfo.TaskInfoData.findTaskData(): Task? {
    return when (this) {
        is TaskInfo.TaskInfoData.TaskOnly -> task
        is TaskInfo.TaskInfoData.TaskAndDetail -> task
        is TaskInfo.TaskInfoData.None -> null
        is TaskInfo.TaskInfoData.DetailOnly -> null
    }
}

internal fun TaskInfo.TaskInfoData.isPinChangeAllowed(): Boolean {
    return when (this) {
        is TaskInfo.TaskInfoData.TaskOnly -> pinChangeAllowed
        is TaskInfo.TaskInfoData.TaskAndDetail -> pinChangeAllowed
        is TaskInfo.TaskInfoData.None -> false
        is TaskInfo.TaskInfoData.DetailOnly -> false
    }
}
