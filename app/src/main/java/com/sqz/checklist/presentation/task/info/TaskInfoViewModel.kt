package com.sqz.checklist.presentation.task.info

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import sqz.checklist.data.database.repository.task.TaskRepository
import sqz.checklist.task.api.info.TaskInfo
import sqz.checklist.task.api.taskInfoProvider

class TaskInfoViewModel(
    taskRepository: TaskRepository
) : ViewModel() {

    private val _taskInfo = taskInfoProvider(taskRepository = taskRepository)

    val taskInfo: StateFlow<TaskInfo.TaskInfoData> = _taskInfo.getTaskInfo()

    private suspend fun getTaskInfo(state: TaskInfoState) {
        val taskId = state.taskId
        val config = when (state.config) {
            is TaskInfoState.Config.TaskOnly -> TaskInfo.Config.TaskOnly(
                state.config.pinChangeAllowed
            )

            is TaskInfoState.Config.TaskAndDetail -> TaskInfo.Config.TaskAndDetail(
                state.config.pinChangeAllowed
            )

            is TaskInfoState.Config.DetailOnly -> TaskInfo.Config.DetailOnly
        }
        _taskInfo.setTaskInfo(config, taskId)
    }

    private val _isVisible = MutableStateFlow(false)

    suspend fun onStateChanged(state: TaskInfoState, onDismissed: () -> Unit) {
        if (taskInfo.value is TaskInfo.TaskInfoData.None) {
            if (_isVisible.value) {
                onDismissed().also { _isVisible.value = false }
            } else {
                this.getTaskInfo(state).also { _isVisible.value = true }
            }
        }
    }

    fun onPinChange() {
        _taskInfo.onPinChangeRequest()
    }

    fun clearTaskInfo() {
        _taskInfo.clearTaskInfo()
    }

    override fun onCleared() {
        super.onCleared()
        this.clearTaskInfo()
    }
}
