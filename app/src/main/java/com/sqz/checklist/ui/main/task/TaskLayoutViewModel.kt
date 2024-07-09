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
     * Reminder-related
     * **/
    var cancelReminderAction by mutableStateOf(false)

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
            taskData = MainActivity.taskDatabase.taskDao().getAll(withoutHistory = 1)
        }
    }

    // Cancel reminder by id
    fun cancelReminder(id: Int, context: Context) {
        viewModelScope.launch {
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
        }
    }

    fun cancelHistoryReminder(context: Context) {
        viewModelScope.launch {
            val allIsHistoryIdList = MainActivity.taskDatabase.taskDao().getAllIsHistoryId()
            for (data in allIsHistoryIdList) {
                cancelReminder(data.id, context)
            }
        }
    }

    /**
     * Task-related
     * **/
    var checkTaskAction by mutableStateOf(false)
    var undoActionId by mutableIntStateOf(-0)
    var undoTaskAction by mutableStateOf(false)

    // Load saved task
    private var taskData by mutableStateOf(listOf<Task>())
    fun loadTaskData(dao: TaskDao): List<Task> {
        viewModelScope.launch {
            taskData = dao.getAll(withoutHistory = 1)
        }
        return taskData
    }

    // Reminded task
    private var isRemindedData by mutableStateOf(listOf<Task>())
    fun remindedState(load: Boolean = false): List<Task> {
        viewModelScope.launch {
            if (load) {
                for (data in MainActivity.taskDatabase.taskDao().getIsRemindedList()) {
                    data.reminder?.let {
                        val parts = it.split(":")
                        if (parts.size >= 2) {
                            parts[0]
                            val time = parts[1].toLong()
                            if (time < System.currentTimeMillis() && !isRemindedData.contains(data)) {
                                isRemindedData += data
                            }
                        }
                    }
                }
            } else {
                for (data in isRemindedData) {
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
            }
        }
        return isRemindedData
    }

    // Set task pin
    private var isPinTaskData by mutableStateOf(listOf<Task>())
    fun pinState(id: Int = 0, set: Int = 0, load: Boolean = false): List<Task> {
        viewModelScope.launch {
            if (load) {
                isPinTaskData = MainActivity.taskDatabase.taskDao().getAll(1, 0)
            } else {
                MainActivity.taskDatabase.taskDao().editTaskPin(id, set)
                taskData = MainActivity.taskDatabase.taskDao().getAll(withoutHistory = 1)
                isPinTaskData = MainActivity.taskDatabase.taskDao().getAll(1, 0)
            }
        }
        return isPinTaskData
    }

    // Insert task to database
    fun insertTask(description: String) {
        viewModelScope.launch {
            val insert = Task(description = description, createDate = LocalDate.now())
            MainActivity.taskDatabase.taskDao().insertAll(insert)
            taskData = MainActivity.taskDatabase.taskDao().getAll(withoutHistory = 1)
            isPinTaskData = MainActivity.taskDatabase.taskDao().getAll(1, 0)
        }
    }

    // Edit task
    fun editTask(id: Int, edit: String) {
        viewModelScope.launch {
            MainActivity.taskDatabase.taskDao().editTask(id, edit)
            taskData = MainActivity.taskDatabase.taskDao().getAll(withoutHistory = 1)
            isPinTaskData = MainActivity.taskDatabase.taskDao().getAll(1, 0)
        }
    }

    // Load history task
    private var taskHistoryData by mutableStateOf(listOf<Task>())
    fun loadTaskHistoryData(dao: TaskDao): List<Task> {
        viewModelScope.launch {
            taskHistoryData = dao.getAllOrderByIsHistoryId()
        }
        return taskHistoryData
    }

    // Get Task is History or Not
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

    // Delete to History
    fun deleteTaskToHistory(id: Int) {
        viewModelScope.launch {
            MainActivity.taskDatabase.taskDao().setHistory(1, id)
            // Give an id for history sequence
            val maxId = MainActivity.taskDatabase.taskDao().getIsHistoryIdTop()
            MainActivity.taskDatabase.taskDao().setHistoryId((maxId + 1), id)
            // Update to LazyColumn
            taskData = MainActivity.taskDatabase.taskDao().getAll(withoutHistory = 1)
            isPinTaskData = MainActivity.taskDatabase.taskDao().getAll(1, 0)
        }
    }

    // Auto Delete History Task
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

    // Undo to History
    fun undoTaskToHistory(id: Int) {
        viewModelScope.launch {
            // Actions
            MainActivity.taskDatabase.taskDao().setHistory(0, id)
            MainActivity.taskDatabase.taskDao().setHistoryId(0, id)
            arrangeIsHistoryId()
            undoActionId = -0
            cancelReminderAction = true
            // Update to LazyColumn
            taskData = MainActivity.taskDatabase.taskDao().getAll(withoutHistory = 1)
            taskHistoryData = MainActivity.taskDatabase.taskDao().getAllOrderByIsHistoryId()
            isPinTaskData = MainActivity.taskDatabase.taskDao().getAll(1, 0)
        }
    }

    // Redo all task from history
    fun redoAll() {
        viewModelScope.launch {
            // Actions
            MainActivity.taskDatabase.taskDao().setAllNotHistory()
            // Update to LazyColumn
            taskHistoryData = MainActivity.taskDatabase.taskDao().getAll(withoutHistory = 0)
        }
    }

    // Delete action
    fun deleteTask(id: Int) = viewModelScope.launch {
        // Actions
        MainActivity.taskDatabase.taskDao().delete(
            Task(id = id, description = "", createDate = LocalDate.MIN)
        )
        arrangeIsHistoryId()
        // Update to LazyColumn
        taskHistoryData = MainActivity.taskDatabase.taskDao().getAllOrderByIsHistoryId()
    }

    // Delete all task from history
    fun deleteAll() {
        viewModelScope.launch {
            // Actions
            MainActivity.taskDatabase.taskDao().deleteAllHistory()
            // Update to LazyColumn
            taskHistoryData = MainActivity.taskDatabase.taskDao().getAll(withoutHistory = 0)
        }
    }

    // Arrange IsHistoryId
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

    /**
     * Task History select state
     * **/
    var selectedId by mutableIntStateOf(-0)
    var onSelect by mutableStateOf(false)
    fun selectTask(id: Int) {
        if (!onSelect) {
            selectedId = id
            onSelect = true
        } else if (selectedId == id) {
            selectedId = -0
            onSelect = false
        } else selectedId = id
    }

    var hideSelected by mutableStateOf(false)
}