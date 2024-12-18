package com.sqz.checklist.ui.main.task

import android.content.Context
import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.database.Task
import com.sqz.checklist.notification.PermissionState
import com.sqz.checklist.ui.main.task.history.arrangeHistoryId
import com.sqz.checklist.ui.main.task.layout.NavConnectData
import com.sqz.checklist.ui.main.task.layout.TopBarMenuClickType
import com.sqz.checklist.ui.main.task.layout.check.CheckDataState
import com.sqz.checklist.ui.main.task.layout.item.CardClickType
import com.sqz.checklist.ui.main.task.layout.item.EditState
import com.sqz.checklist.ui.main.task.layout.item.ListData
import com.sqz.checklist.ui.main.task.layout.item.TaskData
import com.sqz.checklist.ui.reminder.ReminderActionType
import com.sqz.checklist.ui.reminder.ReminderData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar
import java.util.concurrent.TimeUnit

class TaskLayoutViewModel : ViewModel() {

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
        TopBarMenuClickType.Search -> searchView(!_listState.value.searchView)
        TopBarMenuClickType.BackupRestore -> resetUndo(context)
    }

    private val _listState = MutableStateFlow(ListData())
    val listState: StateFlow<ListData> = _listState.asStateFlow()
    private fun updateListState(init: Boolean = false) = viewModelScope.launch {
        _listState.update { lists ->
            val remindedList = MainActivity.taskDatabase.taskDao().getIsRemindedList().filter {
                val parts = it.reminder?.split(":")
                val timeMillisData = if (parts?.size!! >= 2) parts[1].toLong() else -1L
                if (timeMillisData == -1L) Log.e("LoadingList", "Task reminder data error!")
                timeMillisData != -1L && timeMillisData < System.currentTimeMillis()
            }
            lists.copy(
                item = MainActivity.taskDatabase.taskDao().getAll(withoutHistory = 1),
                pinnedItem = MainActivity.taskDatabase.taskDao().getAll(1, 0),
                isRemindedItem = remindedList,
            )
        }
        if (!init) updateInSearch(searchingText)
    }

    private fun searchView(setter: Boolean) { //Connect top bar & nav bar search actions
        _listState.update { it.copy(searchView = setter) }
        _navExtendedConnectData.update { it.copy(searchState = setter) }
    }

    init {
        updateListState(init = true)
    }

    /**
     * ----- Reminder-related -----
     */
    private val _notificationManager = MutableStateFlow(
        com.sqz.checklist.notification.NotifyManager()
    )

    fun notificationInitState(context: Context): PermissionState {
        return _notificationManager.value.requestPermission(context)
    }

    fun isAlarmPermission(): Boolean = _notificationManager.value.getAlarmPermission()

    /** Send a delayed notification to user **/
    suspend fun setReminder(
        delayDuration: Long, timeUnit: TimeUnit, id: Int, description: String, context: Context
    ) {
        val delayTime = if (!isAlarmPermission()) delayDuration else {
            System.currentTimeMillis() + delayDuration
        }
        val notification = _notificationManager.value.createNotification(
            channelId = context.getString(R.string.tasks),
            channelName = context.getString(R.string.task_reminder),
            channelDescription = context.getString(R.string.description),
            description = description, notifyId = id,
            delayDuration = delayTime, timeUnit = timeUnit,
            context = context
        )
        viewModelScope.launch {
            val queryCharacter = notification.also { Log.i("Notification", "Reminder is setting") }
            val now = Calendar.getInstance()
            val remindTime = if (
                isAlarmPermission()) delayTime else now.timeInMillis + delayDuration
            val merge = "$queryCharacter:$remindTime"
            MainActivity.taskDatabase.taskDao().insertReminder(id = id, string = merge)
            updateListState()
        }
    }

    /** Cancel reminder by id. If cancelHistory = true, cancel all reminder which in history **/
    fun cancelReminder(
        id: Int = -1, reminder: String?, context: Context, cancelHistory: Boolean = false
    ) {
        if (!cancelHistory && id != -1) viewModelScope.launch {
            try { // Cancel sent notification
                val parts = reminder?.split(":")
                val queryCharacter = if (parts?.size!! >= 2) parts[0] else null
                if (queryCharacter != null) {
                    _notificationManager.value.cancelNotification(queryCharacter, context, id)
                }
            } catch (e: Exception) {
                Log.e("UUID", "${e.message}")
            }
            // Delete reminder info
            MainActivity.taskDatabase.taskDao().deleteReminder(id)
            updateListState()
        } else viewModelScope.launch {
            val allIsHistoryIdList = MainActivity.taskDatabase.taskDao().getAllOrderByIsHistoryId()
            for (data in allIsHistoryIdList) {
                cancelReminder(data.id, data.reminder, context)
            }
        }
    }

    /**
     * ----- Task-related -----
     */
    private val _undo = MutableStateFlow(CheckDataState())
    val undo: MutableStateFlow<CheckDataState> = _undo
    private fun resetUndo(context: Context) { // reset undo state
        cancelReminder(reminder = null, context = context, cancelHistory = true)
        _undo.value = CheckDataState()
    }

    fun taskChecked(id: Int, context: Context) = _undo.update { // when task is checked
        changeTaskVisibility(id, toHistory = true, context = context)
        it.copy(checkTaskAction = true, undoActionId = id)
    }

    fun undoTimeout(lazyState: LazyListState, context: Context): Boolean { // process undo button
        var rememberScroll by mutableIntStateOf(0)
        var rememberScrollIndex by mutableIntStateOf(0)
        val undoTimeout = { resetUndo(context) }
        if (_undo.value.checkTaskAction) viewModelScope.launch {
            while (true) {
                delay(50)
                rememberScroll = lazyState.firstVisibleItemScrollOffset
                rememberScrollIndex = lazyState.firstVisibleItemIndex
                _undo.update { it.copy(undoButtonState = true) }
                delay(1500)
                val isTimeout = rememberScroll > lazyState.firstVisibleItemScrollOffset + 10 ||
                        rememberScroll < lazyState.firstVisibleItemScrollOffset - 10
                for (i in 1..7) {
                    delay(500)
                    if (rememberScrollIndex != lazyState.firstVisibleItemIndex || isTimeout) break
                }
                undoTimeout()
                break
            }
        }
        return _undo.value.undoButtonState
    }

    private val _taskData = MutableStateFlow(TaskData())
    val taskData: MutableStateFlow<TaskData> = _taskData
    fun resetTaskData() = run { _taskData.value = TaskData() }

    /** Task click action **/
    fun onTaskItemClick(task: Task, type: CardClickType, reminderState: Boolean) {
        fun reminderAction(id: Int, info: String?, set: Boolean) = _taskData.update {
            val booleanToType = if (set) ReminderActionType.Set else ReminderActionType.Cancel
            it.copy(reminder = ReminderData(id, info, booleanToType, task.description))
        }
        when (type) {
            CardClickType.Reminder -> reminderAction(task.id, task.reminder, !reminderState)
            CardClickType.Pin -> pinState(task.id, !task.isPin)
            CardClickType.Close -> remindedState(id = task.id)
            CardClickType.Edit -> {
                _taskData.update { it.copy(editState = EditState(task.id, task.description, true)) }
            }
        }
    }

    /** Set task pin **/
    private fun pinState(id: Int, set: Boolean) = viewModelScope.launch {
        val booleanToInt = if (set) 1 else 0
        MainActivity.taskDatabase.taskDao().editTaskPin(id, booleanToInt)
        updateListState()
    }

    /** Remind task. autoDel and id (del reminder info) **/
    fun remindedState(id: Int = -1, autoDel: Boolean = false) {
        viewModelScope.launch {
            if (id != -1) MainActivity.taskDatabase.taskDao().deleteReminder(id)
            if (autoDel) for (data in _listState.value.isRemindedItem) {
                val parts = data.reminder?.split(":")
                val timeMillisData = if (parts?.size!! >= 2) parts[1].toLong() else -1L
                val delReminderTime = timeMillisData < System.currentTimeMillis() - 43200000
                if (timeMillisData != -1L && delReminderTime) {
                    MainActivity.taskDatabase.taskDao().deleteReminder(data.id)
                }
            }
            updateListState()
        }
    }

    /** Insert task to database **/
    fun insertTask(description: String) = viewModelScope.launch {
        val insert = Task(description = description, createDate = LocalDate.now())
        MainActivity.taskDatabase.taskDao().insertAll(insert)
        updateListState()
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
            viewModelScope.launch { value = MainActivity.taskDatabase.taskDao().getIsHistory(id) }
        }
        getIsHistoryId(id)
        return value == 1
    }

    /** Delete to history or Undo to history. Must have toHistory or undoToHistory to be @true **/
    fun changeTaskVisibility(
        id: Int, toHistory: Boolean = false, undoToHistory: Boolean = false, context: Context
    ) {
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
            resetUndo(context)
            // Update to LazyColumn
            updateListState()
        }
    }

    /** Auto Delete History Task **/
    fun autoDeleteHistoryTask(start: Int) {
        viewModelScope.launch {
            val value = MainActivity.taskDatabase.taskDao().getIsHistorySum()
            if (value > start) for (i in 1..(value - start)) {
                val id = MainActivity.taskDatabase.taskDao().getIsHistoryBottomKeyId()
                MainActivity.taskDatabase.taskDao().delete(
                    Task(id = id, description = "", createDate = LocalDate.MIN)
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
