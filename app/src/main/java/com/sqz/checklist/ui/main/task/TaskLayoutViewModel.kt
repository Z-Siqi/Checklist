package com.sqz.checklist.ui.main.task

import android.app.NotificationManager
import android.content.Context
import android.util.Log
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
import com.sqz.checklist.notification.DelayedNotificationWorker
import com.sqz.checklist.ui.main.task.history.arrangeHistoryId
import com.sqz.checklist.ui.main.task.layout.NavExtendedConnectData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit

class TaskLayoutViewModel : ViewModel() {

    private val _navExtendedConnectData = MutableStateFlow(NavExtendedConnectData())
    val navExtendedConnector: MutableStateFlow<NavExtendedConnectData> = _navExtendedConnectData
    fun updateNavConnector(data: NavExtendedConnectData, updateSet: NavExtendedConnectData) {
        _navExtendedConnectData.update {
            it.copy(
                canScroll = if (updateSet.canScroll) data.canScroll else it.canScroll,
                scrollToFirst = if (updateSet.scrollToFirst) data.scrollToFirst else it.scrollToFirst,
                scrollToBottom = if (updateSet.scrollToBottom) data.scrollToBottom else it.scrollToBottom,
                searchState = if (updateSet.searchState) data.searchState else it.searchState,
                canScrollForward = if (updateSet.canScrollForward) data.canScrollForward else it.canScrollForward
            )
        }
    }

    private val _listState = MutableStateFlow(ListData())
    val listState: StateFlow<ListData> = _listState.asStateFlow()
    fun updateListState(init: Boolean = false) = viewModelScope.launch {
        _listState.update { lists ->
            val remindedList = MainActivity.taskDatabase.taskDao().getIsRemindedList().dropWhile {
                val parts = it.reminder?.split(":")
                val timeMillisData = if (parts?.size!! >= 2) parts[1].toLong() else -1L
                if (timeMillisData == -1L) Log.e("LoadingList", "Task reminder data error!")
                !(timeMillisData != -1L && timeMillisData < System.currentTimeMillis())
            }
            lists.copy(
                item = MainActivity.taskDatabase.taskDao().getAll(withoutHistory = 1),
                pinnedItem = MainActivity.taskDatabase.taskDao().getAll(1, 0),
                isRemindedItem = remindedList,
            )
        }
        if (!init) updateInSearch(searchingText)
    }

    init {
        updateListState(init = true)
    }

    /**
     * ----- Reminder-related -----
     */
    /** Send a delayed notification to user **/
    suspend fun setReminder(
        delayDuration: Long,
        timeUnit: TimeUnit,
        id: Int,
        context: Context
    ) {
        val description = _listState.value.item.find { it.id == id }?.description
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
            updateListState()
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
     */
    var checkTaskAction by mutableStateOf(false)
    var undoActionId by mutableIntStateOf(-0)
    var undoTaskAction by mutableStateOf(false)

    /** Remind task. autoDel and id (del reminder info) **/
    fun remindedState(id: Int = -1, autoDel: Boolean = false) {
        viewModelScope.launch {
            if (id != -1) MainActivity.taskDatabase.taskDao().deleteReminder(id)
            if (autoDel) for (data in _listState.value.isRemindedItem) {
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
            updateListState()
        }
    }

    /** Set task pin. Load list if load = true. If load = false, allow set to work **/
    fun pinState(id: Int = 0, set: Int = 0){
        viewModelScope.launch {
            MainActivity.taskDatabase.taskDao().editTaskPin(id, set)
            updateListState()
        }
    }

    /**  Insert task to database  **/
    fun insertTask(description: String) {
        viewModelScope.launch {
            val insert = Task(description = description, createDate = LocalDate.now())
            MainActivity.taskDatabase.taskDao().insertAll(insert)
            updateListState()
        }
    }

    /** Edit task **/
    fun editTask(id: Int, edit: String) {
        viewModelScope.launch {
            MainActivity.taskDatabase.taskDao().editTask(id, edit)
            updateListState()
        }
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
            updateListState()
        } else if (undoToHistory) viewModelScope.launch { // Actions
            MainActivity.taskDatabase.taskDao().setHistory(0, id)
            MainActivity.taskDatabase.taskDao().setHistoryId(0, id)
            arrangeHistoryId()
            undoActionId = -0
            cancelReminderAction = true
            // Update to LazyColumn
            updateListState()
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
                arrangeHistoryId()
            }
        }
    }

    /** Search Task **/
    var searchingText by mutableStateOf("")
    fun updateInSearch(
        searchText: String = "", reset: Boolean = false, initWithAll: Boolean = false
    ) {
        suspend fun returnList(): List<Task> {
            if (searchText.isNotEmpty()) {
                return MainActivity.taskDatabase.taskDao().searchedList(searchText)
            }
            if (initWithAll || searchingText.isEmpty()) {
                searchingText = ""
                return MainActivity.taskDatabase.taskDao().getAll(withoutHistory = 1)
            }
            if (reset) searchingText = ""
            return listOf()
        }
        viewModelScope.launch { _listState.update { it.copy(inSearchItem = returnList()) } }
    }
}
