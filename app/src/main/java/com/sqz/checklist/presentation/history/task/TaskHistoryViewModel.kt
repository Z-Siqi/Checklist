package com.sqz.checklist.presentation.history.task

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import sqz.checklist.data.database.repository.history.TaskHistoryRepository
import sqz.checklist.history.api.task.TaskHistory
import sqz.checklist.history.api.taskHistoryProvider

class TaskHistoryViewModel(
    config: StateFlow<TaskHistory.Config>,
    taskHistoryRepository: TaskHistoryRepository,
) : ViewModel() {

    private val _taskHistory = taskHistoryProvider(
        config = config,
        taskHistoryRepository = taskHistoryRepository,
        scope = viewModelScope,
    )

    val historyInventory: StateFlow<TaskHistory.Inventory> = _taskHistory.getHistoryInventory

    fun isInventoryListEmpty(): Flow<Boolean> {
        return _taskHistory.isInventoryEmpty()
    }

    fun singleSelectTask(taskId: Long) {
        val def = (_taskHistory.getHistoryInventory.value as? TaskHistory.Inventory.Default).let {
            if (it == null) {
                Log.e("TaskHistoryViewModel", "Invalid state during select!")
                return
            }
            return@let it
        }
        try {
            if (def.selectedTaskId == taskId) {
                _taskHistory.deselectTask()
                return
            }
            _taskHistory.selectTask(taskId)
        } catch (e: IllegalStateException) {
            Log.e("TaskHistoryViewModel", "Invalid select action: $e")
        }
    }

    private val _secondConfirmationDialog = MutableStateFlow<SecondConfirmationState?>(null)

    enum class SecondConfirmationState {
        DeleteAll, RedoAll
    }

    fun onSecondConfirmation(state: SecondConfirmationState?) {
        if (this.historyInventory.value !is TaskHistory.Inventory.Default) {
            return
        }
        when (state) {
            SecondConfirmationState.DeleteAll -> _secondConfirmationDialog.update {
                _taskHistory.deleteAllHistory()
                null
            }

            SecondConfirmationState.RedoAll -> _secondConfirmationDialog.update {
                _taskHistory.redoAllHistory()
                null
            }

            null -> _secondConfirmationDialog.update { null }
        }
    }

    val secondConfirmationState: StateFlow<SecondConfirmationState?> = _secondConfirmationDialog

    fun onExternalState(state: TaskHistoryState, onFailed: () -> Unit) {
        val history = (this.historyInventory.value as? TaskHistory.Inventory.Default).let {
            if (it == null) onFailed()
            it ?: return
        }
        when (state) {
            is TaskHistoryState.Delete -> try {
                if (history.selectedTaskId != null) {
                    _taskHistory.deleteSelectedTask()
                } else if (this.historyInventory.value is TaskHistory.Inventory.Default) {
                    _secondConfirmationDialog.update { SecondConfirmationState.DeleteAll }
                }
            } catch (_: IllegalStateException) {
                onFailed()
            }

            is TaskHistoryState.Redo -> try {
                if (history.selectedTaskId != null) {
                    _taskHistory.redoSelectedTask()
                } else if (this.historyInventory.value is TaskHistory.Inventory.Default) {
                    _secondConfirmationDialog.update { SecondConfirmationState.RedoAll }
                }
            } catch (_: IllegalStateException) {
                onFailed()
            }

            is TaskHistoryState.None -> return
        }
    }
}
