package sqz.checklist.task.impl.info

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sqz.checklist.data.database.repository.task.TaskRepository
import sqz.checklist.task.api.info.TaskInfo

internal class TaskInfoImpl(
    private val taskRepository: TaskRepository,
) : TaskInfo {

    private lateinit var scope: CoroutineScope

    private val _taskInfo = MutableStateFlow<TaskInfo.TaskInfoData>(
        TaskInfo.TaskInfoData.None
    )

    override fun clearTaskInfo() {
        _taskInfo.value = TaskInfo.TaskInfoData.None
        scope.cancel()
    }

    override suspend fun setTaskInfo(config: TaskInfo.Config, taskId: Long) {
        if (_taskInfo.value != TaskInfo.TaskInfoData.None) {
            throw IllegalStateException("Task info is not null")
        }

        scope = CoroutineScope(SupervisorJob())
        val fullTask = taskRepository.getFullTask(taskId)

        when (config) {
            is TaskInfo.Config.DetailOnly -> {
                if (fullTask.second.isNullOrEmpty()) {
                    throw IllegalArgumentException("No detail!")
                }
                val toUpdate = fullTask.second!!.convertTaskDetail()
                _taskInfo.update { TaskInfo.TaskInfoData.DetailOnly(toUpdate) }
            }

            is TaskInfo.Config.TaskOnly -> {
                val toUpdate = TaskInfo.TaskInfoData.TaskOnly(
                    fullTask.first, config.pinChangeAllowed
                )
                _taskInfo.update { toUpdate }
            }

            is TaskInfo.Config.TaskAndDetail -> {
                val toUpdate = TaskInfo.TaskInfoData.TaskAndDetail(
                    task = fullTask.first,
                    detail = fullTask.second.convertTaskDetail(),
                    pinChangeAllowed = config.pinChangeAllowed,
                )
                _taskInfo.update { toUpdate }
            }
        }
    }

    override fun getTaskInfo(): StateFlow<TaskInfo.TaskInfoData> {
        return _taskInfo.asStateFlow()
    }

    override fun onPinChangeRequest() {
        if (_taskInfo.value is TaskInfo.TaskInfoData.None) {
            throw NullPointerException("Task info is null")
        }
        if (!_taskInfo.value.isPinChangeAllowed()) {
            throw IllegalStateException("Pin change is not allowed")
        }
        fun onPinStateChanged() = _taskInfo.update {
            when (it) {
                is TaskInfo.TaskInfoData.None -> throw RuntimeException()
                is TaskInfo.TaskInfoData.DetailOnly -> throw RuntimeException()

                is TaskInfo.TaskInfoData.TaskOnly -> it.copy(
                    task = it.task.copy(isPin = !it.task.isPin)
                )

                is TaskInfo.TaskInfoData.TaskAndDetail -> it.copy(
                    task = it.task.copy(isPin = !it.task.isPin)
                )
            }
        }
        scope.launch {
            val task = _taskInfo.value.findTaskData()!!
            taskRepository.onTaskPinChange(task.id, !task.isPin)
            onPinStateChanged()
        }
    }
}
