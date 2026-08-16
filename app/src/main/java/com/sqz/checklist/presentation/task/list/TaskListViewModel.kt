package com.sqz.checklist.presentation.task.list

import android.content.Context
import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sqz.checklist.notification.NotifyManager
import com.sqz.checklist.notification.PermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sqz.checklist.data.database.TaskReminder
import sqz.checklist.data.database.repository.history.TaskHistoryRepository
import sqz.checklist.data.database.repository.reminder.TaskReminderRepository
import sqz.checklist.data.database.repository.task.TaskRepository
import sqz.checklist.task.api.list.TaskList
import sqz.checklist.task.api.list.model.TaskItemModel
import sqz.checklist.task.api.taskListProvider

class TaskListViewModel(
    config: StateFlow<TaskList.Config>,
    taskHistoryRepository: TaskHistoryRepository,
    private val taskReminderRepo: TaskReminderRepository,
    private val taskRepository: TaskRepository,
) : ViewModel() {

    private val _taskList = taskListProvider(
        config = config,
        taskHistoryRepository = taskHistoryRepository,
        taskRepository = taskRepository,
        scope = viewModelScope
    )

    private val _notifyManager = NotifyManager()

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

    private var _undoReminder: TaskReminder? = null

    fun onFinished(
        task: TaskItemModel,
        lazyListState: LazyListState,
        androidContext: Context,
    ) {
        task.taskViewData.let {
            if (it.reminderTime != null) viewModelScope.launch {
                val reminder = taskReminderRepo.getReminder(it.task.id)
                val notifyId = reminder?.id ?: return@launch
                _notifyManager.cancelNotification(
                    notifyId = notifyId,
                    context = androidContext,
                    delShowedByNotifyId = true,
                )
                if (it.isReminded) {
                    _undoReminder = taskReminderRepo.deleteRemindedInfo(it.task.id)
                }
            }
        }
        task.onRemoveAction()
        viewModelScope.launch {
            delay(500)
            this@TaskListViewModel.setUndoBreakFactor(lazyListState)
        }
    }

    fun resetUndoReminder() {
        _undoReminder = null
    }

    suspend fun removeRemindedInfoByTime(
        androidContext: Context,
    ) = withContext(Dispatchers.Unconfined) {
        while (true) {
            Log.d("TaskListViewModel", "removeRemindedInfoByTime executed")
            _taskList.removeRemindedInfoByTime(
                dbReminder = taskReminderRepo,
                removeNotification = {
                    val reminder = taskReminderRepo.getReminder(it)
                    val notifyId = reminder?.id ?: return@removeRemindedInfoByTime true
                    return@removeRemindedInfoByTime !NotifyManager.isNotificationDisplayed(
                        notifyId = notifyId, context = androidContext
                    )
                }
            ) { // onRemoveNotification
                val reminder = taskReminderRepo.getReminder(it)
                val notifyId = reminder?.id ?: return@removeRemindedInfoByTime
                _notifyManager.removeShowedNotification(notifyId, androidContext)
                Log.d("removeRemindedInfoByTime", "onRemoveNotification: $it")
            }.also {
                when {
                    it == null -> delay(60_000L)
                    it > 20 -> delay(120_000L)
                    it > 10 -> delay(80_000L)
                    it <= 10 -> delay(60_000L)
                }
            }
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

    fun onUndoClick(context: Context) {
        _taskList.requestUndo {
            viewModelScope.launch {
                if (!_notifyManager.hasNotificationPermission(context)) {
                    return@launch
                }
                val reminder = taskReminderRepo.getReminder(it) ?: return@launch
                if (reminder.isReminded) return@launch
                if (!_notifyManager.isDelayedNotificationExist(reminder.id, context)) {
                    Log.e("onUndoClick", "Skipped restore scheduled reminder!")
                    return@launch
                }
                _notifyManager.createNotification(
                    notifyId = reminder.id, targetTime = reminder.reminderTime, context = context
                )
            }
        }
        if (_undoReminder != null) viewModelScope.launch {
            /*TODO: also undo notification*/
            try {
                _undoReminder?.let { taskReminderRepo.insertReminder(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _undoReminder = null
        }
    }

    fun onSearchStateChange(): Boolean {
        if (_taskList.getTaskListInventory.value is TaskList.Inventory.Search) {
            _taskList.onSearchRequest(null)
            return false
        }
        _taskList.onSearchRequest("")
        return true
    }

    fun setSearchState(request: Boolean) {
        if (_taskList.getTaskListInventory.value is TaskList.Inventory.Search) {
            if (request) return
            _taskList.onSearchRequest(null)
            return
        }
        if (request) _taskList.onSearchRequest("")
        else _taskList.onSearchRequest(null)
    }
}
