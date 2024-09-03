package com.sqz.checklist.ui.main.task

import android.app.NotificationManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.database.Task
import com.sqz.checklist.database.TaskDao
import com.sqz.checklist.notification.DelayedNotificationWorker
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit

class TaskLayoutViewModel : ViewModel() {
    /**
     * ----- Reminder-related -----
     * **/
    /** Send a delayed notification to user **/
    suspend fun setReminder(
        delayDuration: Long,
        timeUnit: TimeUnit,
        id: Int,
        context: Context
    ) {
        val description = taskData.find { it.id == id }?.description
        val workRequest = OneTimeWorkRequestBuilder<DelayedNotificationWorker>()
            .setInputData(
                Data.Builder()
                    .putString("channelId", context.getString(R.string.tasks))
                    .putString("channelName", context.getString(R.string.task_reminder))
                    .putString("channelDescription", context.getString(R.string.description))
                    .putString("title", description)
                    .putString("content", "")
                    .putInt("notifyId", id)
                    .build()
            )
            .setInitialDelay(delayDuration, timeUnit)
            .build()
        viewModelScope.launch {
            WorkManager.getInstance(context).enqueue(workRequest)
            val uuid = workRequest.id.toString()
            val now = Calendar.getInstance()
            val remindTime = now.timeInMillis + delayDuration
            val merge = "$uuid:$remindTime"
            MainActivity.taskDatabase.taskDao().insertReminder(id = id, string = merge)
            refreshList(true)
        }
    }

    /** Cancel reminder by id. If cancelHistory = true, cancel all reminder which in history **/
    fun cancelReminder(id: Int = -1, context: Context, cancelHistory: Boolean = false) {
        if (!cancelHistory && id != -1) viewModelScope.launch {
            // Cancel sent notification
            val uuidAndTime = MainActivity.taskDatabase.taskDao().getReminderInfo(id)
            uuidAndTime?.let {
                val parts = it.split(":")
                if (parts.size >= 2) {
                    val uuid = parts[0]
                    parts[1].toLong()
                    val workManager = WorkManager.getInstance(context)
                    workManager.cancelWorkById(UUID.fromString(uuid))
                }
            }
            // Delete notification if showed
            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.cancel(id)
            // Delete reminder info
            MainActivity.taskDatabase.taskDao().deleteReminder(id)
        } else viewModelScope.launch {
            val allIsHistoryIdList = MainActivity.taskDatabase.taskDao().getAllIsHistoryId()
            for (data in allIsHistoryIdList) {
                cancelReminder(data.id, context)
            }
        }
    }

    var cancelReminderAction by mutableStateOf(false)

    /**
     * ----- Task-related -----
     **/
    /**  Load saved task  **/
    private var taskData by mutableStateOf(listOf<Task>())
    fun loadTaskData(dao: TaskDao): List<Task> {
        viewModelScope.launch {
            taskData = dao.getAll(withoutHistory = 1)
        }
        return taskData
    }

    var checkTaskAction by mutableStateOf(false)
    var undoActionId by mutableIntStateOf(-0)
    var undoTaskAction by mutableStateOf(false)

    /** Reminded task **/
    private var isRemindedData by mutableStateOf(listOf<Task>())
    // List<Task>
    /** Remind task. Load list if load = true. If load = false, allow autoDel and id (del reminder info) to work **/
    fun remindedState(id: Int = -1, autoDel: Boolean = false, load: Boolean = false): List<Task> {
        viewModelScope.launch {
            if (load) {
                isRemindedData = MainActivity.taskDatabase.taskDao().getIsRemindedList()
                for (data in isRemindedData) {
                    data.reminder?.let {
                        val parts = it.split(":")
                        if (parts.size >= 2) {
                            parts[0]
                            val time = parts[1].toLong()
                            if (time < System.currentTimeMillis()) {
                                if (isRemindedData.any { item -> item.id == data.id }) {
                                    isRemindedData = isRemindedData.filter { find ->
                                        find.id != data.id
                                    }
                                    isRemindedData += data
                                } else isRemindedData += data
                            } else isRemindedData = isRemindedData.filter { find ->
                                find.id != data.id
                            }
                        }
                    }
                }
            } else {
                if (id != -1) MainActivity.taskDatabase.taskDao().deleteReminder(id)
                if (autoDel) for (data in isRemindedData) {
                    data.reminder?.let {
                        val parts = it.split(":")
                        if (parts.size >= 2) {
                            parts[0]
                            val time = parts[1].toLong()
                            if (time < System.currentTimeMillis() - 43200000) {
                                MainActivity.taskDatabase.taskDao().deleteReminder(data.id)
                            }
                        }
                    }
                }
                isRemindedData = remindedState(load = true)
            }
        }
        return isRemindedData
    }

    /** Set task pin. Load list if load = true. If load = false, allow set to work **/
    private var isPinTaskData by mutableStateOf(listOf<Task>())
    fun pinState(id: Int = 0, set: Int = 0, load: Boolean = false): List<Task> {
        viewModelScope.launch {
            if (load) {
                isPinTaskData = MainActivity.taskDatabase.taskDao().getAll(1, 0)
            } else {
                MainActivity.taskDatabase.taskDao().editTaskPin(id, set)
                refreshList(true)
            }
        }
        return isPinTaskData
    }

    /**  Insert task to database  **/
    fun insertTask(description: String) {
        viewModelScope.launch {
            val insert = Task(description = description, createDate = LocalDate.now())
            MainActivity.taskDatabase.taskDao().insertAll(insert)
            refreshList(true)
        }
    }

    /** Edit task **/
    fun editTask(id: Int, edit: String) {
        viewModelScope.launch {
            MainActivity.taskDatabase.taskDao().editTask(id, edit)
            refreshList(true)
        }
    }

    /** Load history task **/
    private var taskHistoryData by mutableStateOf(listOf<Task>())
    fun loadTaskHistoryData(dao: TaskDao): List<Task> {
        viewModelScope.launch {
            taskHistoryData = dao.getAllOrderByIsHistoryId()
        }
        return taskHistoryData
    }

    /** Get Task is History or Not **/
    fun getIsHistory(id: Int): Boolean {
        var value by mutableIntStateOf(-1)
        fun getIsHistoryId(id: Int) {
            viewModelScope.launch {
                value = MainActivity.taskDatabase.taskDao().getIsHistory(id)
            }
        }
        getIsHistoryId(id)
        return value == 1
    }

    /** Delete to history or Undo to history. Must have toHistory or undoToHistory to be @true **/
    fun changeTaskVisibility(id: Int, toHistory: Boolean = false, undoToHistory: Boolean = false) {
        if (toHistory) viewModelScope.launch {
            MainActivity.taskDatabase.taskDao().setHistory(1, id)
            // Give an id for history sequence
            val maxId = MainActivity.taskDatabase.taskDao().getIsHistoryIdTop()
            MainActivity.taskDatabase.taskDao().setHistoryId((maxId + 1), id)
            // Update to LazyColumn
            refreshList(true)
        } else if (undoToHistory) viewModelScope.launch { // Actions
            MainActivity.taskDatabase.taskDao().setHistory(0, id)
            MainActivity.taskDatabase.taskDao().setHistoryId(0, id)
            arrangeIsHistoryId()
            undoActionId = -0
            cancelReminderAction = true
            // Update to LazyColumn
            refreshList()
        }
    }

    /** Auto Delete History Task **/
    fun autoDeleteHistoryTask(start: Int) {
        viewModelScope.launch {
            val value = MainActivity.taskDatabase.taskDao().getIsHistorySum()
            if (value > start) {
                val id = MainActivity.taskDatabase.taskDao().getIsHistoryBottomKeyId()
                MainActivity.taskDatabase.taskDao().delete(
                    Task(
                        id = id, description = "",
                        createDate = LocalDate.MIN
                    )
                )
                arrangeIsHistoryId()
            }
        }
    }

    /** Delete action **/
    fun deleteTask(id: Int) = viewModelScope.launch {
        // Actions
        MainActivity.taskDatabase.taskDao().delete(
            Task(id = id, description = "", createDate = LocalDate.MIN)
        )
        arrangeIsHistoryId()
        // Update to LazyColumn
        refreshList(noPinTask = true, noRemindedTask = true, noNormalTask = true)
    }

    /** Redo or Delete all task from history. Must have redo or delete to be @true **/
    fun doAllTask(redo: Boolean = false, delete: Boolean = false) {
        if (redo) viewModelScope.launch { // Actions
            MainActivity.taskDatabase.taskDao().setAllNotHistory()
            // Update to LazyColumn
            refreshList(noPinTask = true, noRemindedTask = true, noNormalTask = true)
        } else if (delete) viewModelScope.launch { // Actions
            MainActivity.taskDatabase.taskDao().deleteAllHistory()
            // Update to LazyColumn
            refreshList(noPinTask = true, noRemindedTask = true, noNormalTask = true)
        }
    }

    /** Arrange IsHistoryId **/
    private fun arrangeIsHistoryId() {
        viewModelScope.launch {
            val allIsHistoryIdList = MainActivity.taskDatabase.taskDao().getAllIsHistoryId()
            val arrangeIdList = allIsHistoryIdList.mapIndexed { index, data ->
                data.copy(isHistoryId = index + 1)
            }
            for (data in arrangeIdList) {
                MainActivity.taskDatabase.taskDao().setIsHistoryId(data.isHistoryId, data.id)
            }
        }
    }

    private var inSearchData by mutableStateOf(listOf<Task>())
    fun updateInSearch(
        searchText: String = "", reset: Boolean = false, initWithAll: Boolean = false
    ): List<Task> {
        if (searchText.isNotEmpty()) viewModelScope.launch {
            inSearchData = MainActivity.taskDatabase.taskDao().searchedList(searchText)
        }
        if (initWithAll) viewModelScope.launch {
            inSearchData = MainActivity.taskDatabase.taskDao().getAll(withoutHistory = 1)
        }
        if (reset) {
            val resetList by mutableStateOf(listOf<Task>())
            inSearchData = resetList
        }
        return this.inSearchData
    }

    /**
     * Refresh List
     **/
    suspend fun refreshList(
        noHistoryTask: Boolean = false,
        noPinTask: Boolean = false,
        noRemindedTask: Boolean = false,
        noNormalTask: Boolean = false,
    ) {
        if (!noNormalTask) taskData = MainActivity.taskDatabase.taskDao().getAll(withoutHistory = 1)
        if (!noPinTask) isPinTaskData = MainActivity.taskDatabase.taskDao().getAll(1, 0)
        if (!noRemindedTask) remindedState()
        if (!noHistoryTask) {
            taskHistoryData = MainActivity.taskDatabase.taskDao().getAllOrderByIsHistoryId()
        }
    }
}
