package com.sqz.checklist.presentation.history.task

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sqz.checklist.notification.NotifyManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import sqz.checklist.data.database.model.ReminderViewData
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

    private val _notifyManager = NotifyManager()

    private fun rescheduleReminder(data: ReminderViewData, context: Context) {
        if (!_notifyManager.isDelayedNotificationExist(data.reminder.id, context)) {
            Log.w("TaskHistoryViewModel", "Trying to force restore scheduled reminder!")
            _notifyManager.cancelNotification(data.reminder.id, context)
        }
        _notifyManager.createNotification(
            notifyId = data.reminder.id,
            targetTime = data.reminder.reminderTime,
            context = context,
        )
    }

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

    fun onSecondConfirmation(state: SecondConfirmationState?, context: Context) {
        if (this.historyInventory.value !is TaskHistory.Inventory.Default) {
            return
        }
        when (state) {
            SecondConfirmationState.DeleteAll -> _secondConfirmationDialog.update {
                _taskHistory.deleteAllHistory()
                null
            }

            SecondConfirmationState.RedoAll -> _secondConfirmationDialog.update {
                val hasNotificationPermission = _notifyManager.hasNotificationPermission(context)
                _taskHistory.redoAllHistory {
                    if (!hasNotificationPermission) return@redoAllHistory
                    this.rescheduleReminder(it, context)
                }
                null
            }

            null -> _secondConfirmationDialog.update { null }
        }
    }

    val secondConfirmationState: StateFlow<SecondConfirmationState?> = _secondConfirmationDialog

    fun onExternalState(state: TaskHistoryState, onFailed: () -> Unit, context: Context) {
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
                    _taskHistory.redoSelectedTask {
                        if (!_notifyManager.hasNotificationPermission(context)) {
                            return@redoSelectedTask
                        }
                        this.rescheduleReminder(it, context)
                    }
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
