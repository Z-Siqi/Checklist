package com.sqz.checklist.ui.main.task

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
    var setReminderState by mutableStateOf(false)
    var setReminderId by mutableIntStateOf(-0)
    var reminderCard by mutableStateOf(false)

    fun setReminder(
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
        if (setReminderState) {
            viewModelScope.launch {
                WorkManager.getInstance(context).enqueue(workRequest)
                val uuid = workRequest.id.toString()
                val now = Calendar.getInstance()
                val remindTime = now.timeInMillis + delayDuration
                val merge = "$uuid:$remindTime"
                MainActivity.taskDatabase.taskDao().insertReminder(id = id, string = merge)
            }
        }
    }

    // Cancel reminder by id
    fun cancelReminder(id: Int, context: Context) {
        viewModelScope.launch {
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
            taskData = dao.getAll(1)
        }
        return taskData
    }

    // Set task pin
    private var isPinTaskData by mutableStateOf(listOf<Task>())
    fun pinState(id: Int = 0, set: Int = 0, load: Boolean = false): List<Task> {
        viewModelScope.launch {
            if (load) {
                isPinTaskData = MainActivity.taskDatabase.taskDao().getIsPinList()
            } else {
                MainActivity.taskDatabase.taskDao().editTaskPin(id, set)
                taskData = MainActivity.taskDatabase.taskDao().getAll(1)

                isPinTaskData = MainActivity.taskDatabase.taskDao().getIsPinList()
            }
        }
        return isPinTaskData
    }

    // Insert task to database
    fun insertTask(description: String) {
        viewModelScope.launch {
            val insert = Task(description = description, createDate = LocalDate.now())
            MainActivity.taskDatabase.taskDao().insertAll(insert)
            taskData = MainActivity.taskDatabase.taskDao().getAll(1)

            isPinTaskData = MainActivity.taskDatabase.taskDao().getIsPinList()
        }
    }

    // Edit task
    fun editTask(id: Int, edit: String) {
        viewModelScope.launch {
            MainActivity.taskDatabase.taskDao().editTask(id, edit)
            taskData = MainActivity.taskDatabase.taskDao().getAll(1)

            isPinTaskData = MainActivity.taskDatabase.taskDao().getIsPinList()
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
    fun deleteTaskToHistory(id: Int, context: Context) {
        viewModelScope.launch {
            MainActivity.taskDatabase.taskDao().setHistory(1, id)
            // Cancel reminder
            cancelReminder(id, context)
            // Give an id for history sequence
            val maxId = MainActivity.taskDatabase.taskDao().getIsHistoryIdTop()
            MainActivity.taskDatabase.taskDao().setHistoryId((maxId + 1), id)
            // Update to LazyColumn
            taskData = MainActivity.taskDatabase.taskDao().getAll(1)
            isPinTaskData = MainActivity.taskDatabase.taskDao().getIsPinList()
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
                        id = id,
                        description = "",
                        createDate = LocalDate.MIN
                    )
                )
                val getBottom = MainActivity.taskDatabase.taskDao().getIsHistoryIdBottom()
                if (getBottom > 1) {
                    val allIsHistoryId = MainActivity.taskDatabase.taskDao().getAllIsHistoryId()
                    for (x in allIsHistoryId) {
                        MainActivity.taskDatabase.taskDao().setHistoryIdById((x - 1), x)
                    }
                }
            }
        }
    }

    // Undo to History
    fun undoTaskToHistory(id: Int) {
        viewModelScope.launch {
            // Actions
            arrangeIsHistoryId(id)
            MainActivity.taskDatabase.taskDao().setHistory(0, id)
            MainActivity.taskDatabase.taskDao().setHistoryId(0, id)
            // Update to LazyColumn
            taskData = MainActivity.taskDatabase.taskDao().getAll(1)
            taskHistoryData = MainActivity.taskDatabase.taskDao().getAll(0)

            isPinTaskData = MainActivity.taskDatabase.taskDao().getIsPinList()
        }
    }

    // Redo all task from history
    fun redoAll() {
        viewModelScope.launch {
            // Actions
            MainActivity.taskDatabase.taskDao().setAllNotHistory()
            // Update to LazyColumn
            taskHistoryData = MainActivity.taskDatabase.taskDao().getAll(0)
        }
    }

    // Delete action
    fun deleteTask(id: Int) = viewModelScope.launch {
        // Actions
        arrangeIsHistoryId(id)
        MainActivity.taskDatabase.taskDao().delete(
            Task(
                id = id,
                description = "",
                createDate = LocalDate.MIN
            )
        )
        // Update to LazyColumn
        taskHistoryData = MainActivity.taskDatabase.taskDao().getAll(0)
    }

    // Delete all task from history
    fun deleteAll() {
        viewModelScope.launch {
            // Actions
            MainActivity.taskDatabase.taskDao().deleteAllHistory()
            // Update to LazyColumn
            taskHistoryData = MainActivity.taskDatabase.taskDao().getAll(0)
        }
    }

    // Arrange IsHistoryId
    private fun arrangeIsHistoryId(id: Int = 0) {
        viewModelScope.launch {
            val targetMinId = MainActivity.taskDatabase.taskDao().getIsHistoryId(id)
            val allIsHistoryId = MainActivity.taskDatabase.taskDao().getAllIsHistoryId(targetMinId)
            for (x in allIsHistoryId) {
                MainActivity.taskDatabase.taskDao().setHistoryIdById((x - 1), x)
            }
        }
    }

    // For Task History select state
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
}