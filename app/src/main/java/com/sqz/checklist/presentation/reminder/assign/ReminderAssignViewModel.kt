package com.sqz.checklist.presentation.reminder.assign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sqz.checklist.notification.PermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sqz.checklist.common.NotificationScheduler
import sqz.checklist.data.database.model.ReminderViewData
import sqz.checklist.data.database.repository.reminder.TaskReminderRepository
import sqz.checklist.reminder.api.assign.ReminderAssign
import sqz.checklist.reminder.api.reminderAssignProvider

class ReminderAssignViewModel(
    taskReminderRepository: TaskReminderRepository,
    private val scheduler: NotificationScheduler,
) : ViewModel() {

    private val _currentAction = MutableStateFlow<ReminderAssign.Action?>(null)

    val currentAction: StateFlow<ReminderAssign.Action?> = _currentAction.asStateFlow()

    private val _taskId = MutableStateFlow<Long?>(null)

    private val reminder = reminderAssignProvider(
        taskReminderRepository = taskReminderRepository,
        scheduler = scheduler,
    )

    private val _currentReminder = MutableStateFlow<ReminderViewData?>(null)

    val currentReminder: StateFlow<ReminderViewData?> = _currentReminder.asStateFlow()

    private val _noFullPermission = MutableStateFlow(false)

    val noFullPermission: StateFlow<Boolean> = _noFullPermission.asStateFlow()

    private suspend fun noPermissionGeneralCheck() {
        if (reminder.checkUnsentReminderError()) {
            _currentAction.value = ReminderAssign.Action.Error
            return
        }
        _currentAction.value = ReminderAssign.Action.NoPermission
    }

    private suspend fun scheduledInSystemCheck(info: ReminderViewData) {
        val isExistedInSystem = scheduler.isExisted(info.reminder.id)
        if (!isExistedInSystem) {
            _currentAction.value = ReminderAssign.Action.Error
        }
    }

    fun setAction(taskId: Long, currentPermissionState: PermissionState) {
        if (_currentAction.value != null) {
            this.resetAction()
        }
        _noFullPermission.value = false
        _taskId.value = taskId
        viewModelScope.launch {
            // No permission situation
            when (currentPermissionState) { // initial assign permission
                PermissionState.Null -> noPermissionGeneralCheck()
                PermissionState.Alarm -> noPermissionGeneralCheck()
                PermissionState.Both -> Unit
                PermissionState.Notification -> _noFullPermission.value = true
            }
            if (_currentAction.value != null) {
                return@launch
            }
            // Assign actions
            val getInfo = reminder.getCurrentReminder(taskId = taskId).let {
                if (it == null) { // set a new one
                    _currentAction.value = ReminderAssign.Action.Set
                    return@launch
                }
                return@let it
            }
            _currentReminder.value = getInfo
            if (getInfo.reminder.isReminded) {
                _currentAction.value = ReminderAssign.Action.Set // reminded, set a new one
                return@launch
            }
            scheduledInSystemCheck(info = getInfo).also {
                if (_currentAction.value == ReminderAssign.Action.Error) return@launch
            }
            _currentAction.value = ReminderAssign.Action.Cancel // is existed
        }
    }

    suspend fun setReminder(remindAtMillis: Long) {
        reminder.setReminder(_taskId.value!!, remindAtMillis)
    }

    suspend fun cancelReminder() {
        reminder.cancelReminder(_taskId.value!!)
    }

    fun resetAction() {
        _currentAction.value = null
        _taskId.value = null
        _currentReminder.value = null
        _noFullPermission.value = false
    }
}
