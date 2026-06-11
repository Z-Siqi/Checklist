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
import com.sqz.checklist.ui.main.task.handler.ReminderHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sqz.checklist.data.database.Task
import sqz.checklist.data.database.repository.DatabaseRepository
import sqz.checklist.data.database.repository.Table
import sqz.checklist.data.preferences.PreferencesInCache
import sqz.checklist.data.preferences.PrimaryPreferences
import sqz.checklist.data.storage.audioMediaPath
import sqz.checklist.data.storage.pictureMediaPath
import sqz.checklist.data.storage.videoMediaPath
import sqz.checklist.task.api.list.TaskList
import java.io.File

open class TaskLayoutViewModel : ViewModel() {
    open fun database(): DatabaseRepository = DatabaseRepository(
        MainActivity.taskDatabase.getDatabase()
    )

    private var _init = MutableStateFlow(false)

    /** Reminder handler **/
    var reminderHandler: ReminderHandler

    init {
        this.let { viewModel ->
            reminderHandler = ReminderHandler.instance(viewModel, _init)
        }
        Log.d("TaskLayoutViewModel", "ViewModel is init")
        _init.value = true
    }

    private fun primaryPreferences(context: Context): PrimaryPreferences {
        return PrimaryPreferences(context)
    }

    fun resetUndo(context: Context) { //TODO: Finish refactoring cancelHistoryReminder
        reminderHandler.cancelHistoryReminder(context = context)
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
            autoDelIsHistoryTaskNumber = prefs.allowedNumberOfHistory().prefsLimit(),
            recentlyRemindedKeepTime = prefs.recentlyRemindedKeepTime(),
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

    fun requestReminder(id: Long) {
        reminderHandler.requestReminder(id, false)
    }

    fun updateNotification(taskId: Long, context: Context) = viewModelScope.launch {
        database().getReminderData(taskId = taskId)?.let {
            if (NotifyManager.isNotificationDisplayed(it.id, context)) {
                val task = database().getTable(Table.Task, taskId)[0] as Task
                reminderHandler.updateNotification(it.id, task, context)
            }
        }
    }
}
