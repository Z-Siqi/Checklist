package com.sqz.checklist.ui.main.task.layout.dialog

import androidx.lifecycle.ViewModel
import com.sqz.checklist.database.TaskData
import com.sqz.checklist.database.TaskDetailData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TaskModifyDialogViewModel : ViewModel() {
    private val _task = MutableStateFlow<TaskData?>(null)
    val taskData = _task.asStateFlow()

    private var _taskDetailList = MutableStateFlow<List<TaskDetailData>?>(null)
    val taskDetailList = _taskDetailList.asStateFlow()

    /**
     * Initializes a new task with default values.
     * This is typically used when creating a new task.
     */
    fun init() {
        this._task.value = TaskData(null, isPin = false)
    }

    /**
     * Initializes the ViewModel with existing task data and its details.
     * This is used when modifying an existing task.
     *
     * @param task The [TaskData] to be modified.
     * @param taskDetail The list of [TaskDetailData] associated with the task.
     */
    fun init(task: TaskData, taskDetail: List<TaskDetailData>?) {
        this._task.value = task
        this._taskDetailList.value = taskDetail
    }

    private var _requestReminder: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val requestReminder = _requestReminder.asStateFlow()

    private var _isChanged: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isChanged = _isChanged.asStateFlow()

    fun requestReminder() {
        _requestReminder.update { !it }
    }

    fun setTaskDetailList(list: List<TaskDetailData>?, isChanged: Boolean) {
        _isChanged.update { isChanged }
        _taskDetailList.update { list }
    }

    fun setTaskDescription(description: String) {
        _task.update { it!!.copy(description = description) }
    }

    fun updateTask(taskData: (TaskData) -> TaskData) {
        _task.update { taskData(it!!) }
    }

    @Override
    public override fun onCleared() {
        this._task.value = null
        this._taskDetailList.value = null
        this._requestReminder.value = false
        this._isChanged.value = false
        super.onCleared()
    }
}
