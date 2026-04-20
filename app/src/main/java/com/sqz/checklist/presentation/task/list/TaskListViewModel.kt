package com.sqz.checklist.presentation.task.list

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sqz.checklist.data.database.repository.history.TaskHistoryRepository
import sqz.checklist.data.database.repository.task.TaskRepository
import sqz.checklist.task.api.list.TaskList
import sqz.checklist.task.api.list.model.TaskItemModel
import sqz.checklist.task.api.taskListProvider

class TaskListViewModel(
    config: StateFlow<TaskList.Config>,
    taskHistoryRepository: TaskHistoryRepository,
    private val taskRepository: TaskRepository,
) : ViewModel() {

    private val _taskList = taskListProvider(
        config = config,
        taskHistoryRepository = taskHistoryRepository,
        taskRepository = taskRepository,
    )

    val listInventory: StateFlow<TaskList.Inventory> = _taskList.getTaskListInventory

    fun updateList() {
        viewModelScope.launch {
            _taskList.updateList()
        }
    }

    init {
        this.updateList()
    }

    fun isListEmpty(): Flow<Boolean> {
        return _taskList.isInventoryEmpty()
    }

    @Stable
    fun safeTaskItemModel(instance: TaskItemModel): TaskItemModel {
        fun printErrLog() {
            val message = "Failed to execute due to ExternalRequest already exist!"
            Log.w("TaskListViewModel", message)
        }

        val safeMethod = object : TaskItemModel by instance {
            override fun onDetailRequest() {
                try {
                    instance.onDetailRequest()
                } catch (_: IllegalStateException) {
                    printErrLog()
                }
            }

            override fun onEditRequest() {
                try {
                    instance.onEditRequest()
                } catch (_: IllegalStateException) {
                    printErrLog()
                }
            }

            override fun onInfoRequest(type: TaskList.ListType) {
                try {
                    instance.onInfoRequest(type)
                } catch (_: IllegalStateException) {
                    printErrLog()
                }
            }

            override fun onRemindedRemoveRequest() {
                try {
                    instance.onRemindedRemoveRequest()
                } catch (_: IllegalStateException) {
                    printErrLog()
                }
            }

            override fun onReminderRequest() {
                try {
                    instance.onReminderRequest()
                } catch (_: IllegalStateException) {
                    printErrLog()
                }
            }
        }
        return safeMethod
    }

    fun onFinished(task: TaskItemModel, lazyListState: LazyListState) {
        task.onRemoveAction()
        viewModelScope.launch {
            delay(500)
            this@TaskListViewModel.setUndoBreakFactor(lazyListState)
        }
    }

    fun onSearchQueryChange(query: String) {
        _taskList.onSearchRequest(query)
    }

    fun setUndoBreakFactor(lazyListState: LazyListState?) {
        if (lazyListState == null) {
            _taskList.setUndoBreakFactor("Meow~~~")
            return
        }
        val factor1 = lazyListState.firstVisibleItemScrollOffset
        val factor2 = lazyListState.firstVisibleItemIndex
        _taskList.setUndoBreakFactor(factor1 + factor2)
    }

    val externalRequest: StateFlow<TaskItemModel.ExternalRequest> = _taskList.getExternalRequest

    fun resetExternalRequest() {
        _taskList.resetExternalRequest()
    }

    val undoState: StateFlow<Boolean> = _taskList.getUndoState

    fun onUndoClick() {
        _taskList.requestUndo { /*TODO: also undo notification*/ }
    }

    fun onSearchStateChange() {
        if (_taskList.getTaskListInventory.value is TaskList.Inventory.Search) {
            _taskList.onSearchRequest(null)
            return
        }
        _taskList.onSearchRequest("")
    }

    fun setSearchState(request: Boolean) { //TODO: Finish refactoring this
        if (_taskList.getTaskListInventory.value is TaskList.Inventory.Search) {
            if (request) return
            _taskList.onSearchRequest(null)
            return
        }
        if (request) _taskList.onSearchRequest("")
        else _taskList.onSearchRequest(null)
    }
}
