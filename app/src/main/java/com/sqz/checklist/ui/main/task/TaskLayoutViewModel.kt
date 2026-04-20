package com.sqz.checklist.ui.main.task

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sqz.checklist.MainActivity
import com.sqz.checklist.notification.NotifyManager
import com.sqz.checklist.presentation.task.info.TaskInfoState
import com.sqz.checklist.presentation.task.modify.TaskModifyState
import sqz.checklist.data.database.repository.DatabaseRepository
import sqz.checklist.data.preferences.PreferencesInCache
import sqz.checklist.data.preferences.PrimaryPreferences
import com.sqz.checklist.ui.main.task.handler.ReminderHandler
import com.sqz.checklist.ui.main.task.layout.NavConnectData
import com.sqz.checklist.ui.main.task.layout.TopBarMenuClickType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sqz.checklist.data.database.Task
import sqz.checklist.data.database.model.TaskViewData
import sqz.checklist.data.database.repository.Table
import sqz.checklist.data.storage.audioMediaPath
import sqz.checklist.data.storage.pictureMediaPath
import sqz.checklist.data.storage.videoMediaPath
import sqz.checklist.task.api.list.TaskList
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

open class TaskLayoutViewModel : ViewModel() {
    open fun database(): DatabaseRepository = DatabaseRepository(
        MainActivity.taskDatabase.getDatabase()
    )

    private val _navExtendedConnectData = MutableStateFlow(NavConnectData())
    val navExtendedConnector: MutableStateFlow<NavConnectData> = _navExtendedConnectData
    fun updateNavConnector(data: NavConnectData, updateSet: NavConnectData) {
        _navExtendedConnectData.update { nav ->
            if (updateSet.searchState) searchView(data.searchState)
            nav.copy(
                canScroll = if (updateSet.canScroll) data.canScroll else nav.canScroll,
                scrollToFirst = if (updateSet.scrollToFirst) data.scrollToFirst else nav.scrollToFirst,
                scrollToBottom = if (updateSet.scrollToBottom) data.scrollToBottom else nav.scrollToBottom,
                canScrollForward = if (updateSet.canScrollForward) data.canScrollForward else nav.canScrollForward
            )
        }
    }

    fun onTopBarMenuClick(type: TopBarMenuClickType, context: Context) = when (type) {
        TopBarMenuClickType.History -> resetUndo(context)
        TopBarMenuClickType.Search -> searchView(!_navExtendedConnectData.value.searchState)
        TopBarMenuClickType.BackupRestore -> resetUndo(context)
        TopBarMenuClickType.Settings -> resetUndo(context)
    }

    private var _init = MutableStateFlow(false)

    // TODO: Finish refactoring this
    private val _onSearchRequest = MutableStateFlow<Boolean?>(null)

    fun onResetSearchRequest() {
        _onSearchRequest.value = null
    }

    val onSearchRequest = _onSearchRequest.asStateFlow()

    private fun searchView(setter: Boolean) { //Connect top bar & nav bar search actions
        _onSearchRequest.value = setter
        _navExtendedConnectData.update { it.copy(searchState = setter) }
    }

    /** Reminder handler **/
    var reminderHandler: ReminderHandler

    init {
        this.let { viewModel ->
            reminderHandler = ReminderHandler.instance(viewModel, _init)
        }
        Log.d("TaskLayoutViewModel", "ViewModel is init")
    }

    private fun primaryPreferences(context: Context): PrimaryPreferences {
        return PrimaryPreferences(context)
    }

    fun resetUndo(context: Context) { //TODO: Finish refactoring cancelHistoryReminder
        reminderHandler.cancelHistoryReminder(context = context)
    }

    /** Remind task. autoDel and id (del reminder info) **/
    fun autoDeleteRemindedTaskInfo(remindedList: List<TaskViewData>, context: Context) { //TODO
        viewModelScope.launch(Dispatchers.IO) {
            val primaryPreferences = PrimaryPreferences(context)
            if (primaryPreferences.recentlyRemindedKeepTime() > 0L) try {
                for (data in remindedList) {
                    try {
                        val getReminder = database().getReminderData(data.task.id)!!
                        val timeMillisData = getReminder.reminderTime
                        val delReminderTime: Boolean =
                            timeMillisData < (System.currentTimeMillis() - primaryPreferences.recentlyRemindedKeepTime())
                        if (delReminderTime && primaryPreferences.removeNoticeInAutoDelReminded()) {
                            reminderHandler.notifyManager.removeShowedNotification(
                                getReminder.id, context
                            )
                        }
                        if (delReminderTime) database().deleteReminderData(data.task.id)
                    } catch (_: NullPointerException) {
                    }
                }
                //updateListState()
            } catch (_: NoSuchFieldException) {
                Log.w("DeleteReminderData", "Noting need to delete")
            }
            if (_init.value && !_removeInvalidFile) removeInvalidFile(context)
        }
    }

    /** Remove invalid media file from error **/
    private var _removeInvalidFile by mutableStateOf(false)
    private fun removeInvalidFile(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        val cache = PreferencesInCache(context)
        val list = listOf(pictureMediaPath, videoMediaPath, audioMediaPath)
        if (cache.errFileNameSaver() != null) cache.errFileNameSaver()?.let {
            for (data in list) {
                val mediaDir = File(context.filesDir, data)
                val file = File(mediaDir, it)
                if (file.exists()) file.delete().also { cache.errFileNameSaver(null) }
            }
        }
        _removeInvalidFile = true
    }

    private val _listConfig = MutableStateFlow(TaskList.Config())
    val listConfig = _listConfig.asStateFlow()

    fun updateListConfig(prefs: PrimaryPreferences) {
        fun Int.prefsLimit(): Int? = this.let {
            if (it >= 21) null else it
        }
        val config = TaskList.Config(
            enableUndo = !prefs.disableUndoButton(),
            autoDelIsHistoryTaskNumber = prefs.allowedNumberOfHistory().prefsLimit()
        )
        _listConfig.update { config }
    }

    fun onCloseNotification(taskId: Long, context: Context) {
        //TODO: Finish refactoring this
        viewModelScope.launch {
            val reminder = database().getReminderData(taskId)
            if (!primaryPreferences(context).disableRemoveNotifyInReminded()) try {
                reminderHandler.notifyManager.removeShowedNotification(reminder!!.id, context)
                database().deleteReminderData(taskId)
            } catch (e: Exception) {
                Log.w("RemoveShowedNotify", "Exception: $e")
            } else try {
                database().deleteReminderData(taskId)
            } catch (e: Exception) {
                Log.w("RemoveRemindedNotify", "Exception: $e")
            }
        }
    }

    private val _isTaskInfo: MutableStateFlow<TaskInfoState?> = MutableStateFlow(null)
    val isTaskInfo: StateFlow<TaskInfoState?> = _isTaskInfo.asStateFlow()

    fun requestTaskInfo(state: TaskInfoState?) {
        _isTaskInfo.update { state }
    }

    private val _isModify: MutableStateFlow<TaskModifyState?> = MutableStateFlow(null)
    val isModify: StateFlow<TaskModifyState?> = _isModify.asStateFlow()

    fun requestModify(state: TaskModifyState?) {
        _isModify.update { state }
    }

    fun requestReminder(id: Long) {
        reminderHandler.requestReminder(id, false)
    }

    fun updateNotification(taskId: Long, context: Context) = viewModelScope.launch {
        database().getReminderData(taskId = taskId)?.let {
            if (NotifyManager.isNotificationExist(it.id, context)) {
                val task = database().getTable(Table.Task, taskId)[0] as Task
                reminderHandler.updateNotification(it.id, task, context)
            }
        }
    }
}
