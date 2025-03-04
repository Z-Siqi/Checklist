package com.sqz.checklist.ui.main.task

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.database.DatabaseRepository
import com.sqz.checklist.database.ReminderModeType
import com.sqz.checklist.database.Task
import com.sqz.checklist.database.TaskDetail
import com.sqz.checklist.database.TaskDetailType
import com.sqz.checklist.database.TaskReminder
import com.sqz.checklist.notification.NotifyManager
import com.sqz.checklist.notification.PermissionState
import com.sqz.checklist.preferences.PrimaryPreferences
import com.sqz.checklist.ui.main.task.history.arrangeHistoryId
import com.sqz.checklist.ui.main.task.layout.NavConnectData
import com.sqz.checklist.ui.main.task.layout.TopBarMenuClickType
import com.sqz.checklist.ui.main.task.layout.function.CheckDataState
import com.sqz.checklist.ui.main.task.layout.item.CardClickType
import com.sqz.checklist.ui.main.task.layout.item.EditState
import com.sqz.checklist.ui.main.task.layout.item.ListData
import com.sqz.checklist.ui.main.task.layout.item.TaskData
import com.sqz.checklist.ui.main.task.layout.function.ReminderActionType
import com.sqz.checklist.ui.main.task.layout.function.ReminderData
import com.sqz.checklist.ui.main.task.layout.function.TaskDetailData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

class TaskLayoutViewModel : ViewModel() {
    private fun database(): DatabaseRepository = DatabaseRepository(
        MainActivity.taskDatabase
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
        TopBarMenuClickType.Search -> searchView(!_listState.value.searchView)
        TopBarMenuClickType.BackupRestore -> resetUndo(context)
        TopBarMenuClickType.Settings -> resetUndo(context)
    }

    private var _init by mutableStateOf(false)
    private val _isUpdateRunning = AtomicBoolean(false)
    private val _listState = MutableStateFlow(ListData())
    val listState: StateFlow<ListData> = _listState.asStateFlow()
    private fun updateListState(init: Boolean = false) {
        if (_isUpdateRunning.get()) return else _isUpdateRunning.set(true)
        viewModelScope.launch {
            _listState.update { lists ->
                val remindedList = MainActivity.taskDatabase.taskDao().getIsRemindedList().filter {
                    if (it.reminder != null) database().getReminderData(it.reminder)!!.isReminded
                    else false
                }
                lists.copy(
                    item = MainActivity.taskDatabase.taskDao().getAll(),
                    pinnedItem = MainActivity.taskDatabase.taskDao().getAll(0),
                    isRemindedItem = remindedList,
                    unLoading = false
                )
            }.also { Log.d("ViewModel", "List is Update") }
            if (!init) updateInSearch(searchingText) else {
                _init = true
            }
            delay(220)
            _isUpdateRunning.set(false)
        }
    }

    private fun searchView(setter: Boolean) { //Connect top bar & nav bar search actions
        _listState.update { it.copy(searchView = setter) }
        _navExtendedConnectData.update { it.copy(searchState = setter) }
    }

    init {
        updateListState(init = true)
    }

    private fun primaryPreferences(context: Context): PrimaryPreferences {
        return PrimaryPreferences(context)
    }

    /**
     * ----- Reminder-related -----
     */
    private val _notificationManager = MutableStateFlow(NotifyManager())

    fun notificationInitState(context: Context, init: Boolean = false): PermissionState {
        val requestPermission = _notificationManager.value.requestPermission(context)
        fun makeToast() = Toast.makeText(
            context, context.getString(R.string.permission_lost_toast), Toast.LENGTH_LONG
        ).show()
        if (init && _init && requestPermission != PermissionState.Both) viewModelScope.launch {
            database().getIsRemindedNum(false).collect {
                if (it >= 1) { // If no permission to send notification for reminder
                    val reCheck = _notificationManager.value.requestPermission(context)
                    if (reCheck == PermissionState.Null || reCheck == PermissionState.Alarm) makeToast()
                    else if (reCheck != PermissionState.Both &&
                        database().getModeNumWithNoReminded(ReminderModeType.AlarmManager) >= 1
                    ) makeToast()
                }
            }
        }
        if (_init) _init = false
        return requestPermission
    }

    fun isAlarmPermission(): Boolean = _notificationManager.value.getAlarmPermission()

    /** Send a delayed notification to user **/
    private var _notifyId = 0
    suspend fun setReminder(
        delayDuration: Long, timeUnit: TimeUnit, id: Long, description: String, context: Context
    ) {
        _notifyId = Random.nextInt(Int.MIN_VALUE, Int.MAX_VALUE) // Make a random notify id
        suspend fun checkRandomId() { // If notify id is already exist
            for (data in database().getReminderData()) {
                if (data.id == _notifyId || _notifyId == 0) {
                    _notifyId = Random.nextInt(Int.MIN_VALUE, Int.MAX_VALUE)
                    checkRandomId()
                }
            }
        }
        checkRandomId()
        val delayTime = if (!isAlarmPermission()) delayDuration else {
            System.currentTimeMillis() + delayDuration
        }
        val notification = _notificationManager.value.createNotification(
            channelId = context.getString(R.string.tasks),
            channelName = context.getString(R.string.task_reminder),
            channelDescription = context.getString(R.string.description),
            description = description, notifyId = _notifyId,
            delayDuration = delayTime, timeUnit = timeUnit,
            context = context
        )
        try { // remove old data first
            cancelReminder(id, database().getReminderData(id)!!.id, context)
            database().deleteReminderData(id)
        } catch (e: Exception) {
            if (e is NoSuchFieldException || e is NullPointerException) {
                Log.d("SetReminder", "New reminder is setting")
            } else throw e
        }
        viewModelScope.launch {
            val notify = notification.also { Log.i("Notification", "Reminder is setting") }
            val now = Calendar.getInstance()
            val remindTime =
                if (isAlarmPermission()) delayTime else now.timeInMillis + delayDuration
            val mode = if (isAlarmPermission()) ReminderModeType.AlarmManager else {
                ReminderModeType.Worker
            }
            val taskReminder = TaskReminder(
                _notifyId, description, remindTime, mode, extraData = notify
            )
            database().insertReminderData(id, taskReminder)
            updateListState()
        }
    }

    /** Cancel reminder by id. If cancelHistory = true, cancel all reminder which in history **/
    fun cancelReminder(
        id: Long = -1L, reminder: Int?, context: Context, cancelHistory: Boolean = false
    ) {
        if (!cancelHistory && id != -1L) viewModelScope.launch {
            try { // Cancel sent notification
                if (reminder != 0 && reminder != null) {
                    val data = MainActivity.taskDatabase.taskReminderDao().getAll(reminder)
                    if (data != null) when (data.mode) {
                        ReminderModeType.AlarmManager -> _notificationManager.value.cancelNotification(
                            data.id.toString(), context, reminder
                        )

                        ReminderModeType.Worker -> _notificationManager.value.cancelNotification(
                            data.extraData!!, context, reminder
                        )
                    }
                } else {
                    Log.d("CancelFailed", "No reminder data need to cancel")
                }
                // Delete reminder info
                database().deleteReminderData(id)
                updateListState()
            } catch (e: NoSuchFieldException) {
                Log.d("DeleteReminderData", "Noting need to delete")
            } catch (e: IllegalStateException) {
                Log.w("TaskLayoutViewModel", "$e")
            } catch (e: Exception) {
                Log.e("ERROR", "$e")
            }
        } else viewModelScope.launch {
            val allIsHistoryIdList = MainActivity.taskDatabase.taskDao().getAllOrderByIsHistoryId()
            for (data in allIsHistoryIdList) {
                if (data.reminder != 0 && data.reminder != null) cancelReminder(
                    data.id, data.reminder, context
                )
            }
        }
    }

    suspend fun getReminderData(taskId: Int): TaskReminder {
        return database().getReminderData(taskId)!!
    }

    fun getIsRemindedNum(): Flow<Int> = database().getIsRemindedNum(true)

    fun reminderActionCaller(
        id: Long, info: Int?, set: Boolean, description: String
    ) = _taskData.update {
        val booleanToType = if (set) ReminderActionType.Set else ReminderActionType.Cancel
        it.copy(reminder = ReminderData(id, info, booleanToType, description))
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

    fun requestUpdateList() = this.updateListState()

    fun taskChecked(id: Long, context: Context) = _undo.update { // when task is checked
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
    fun onTaskItemClick(task: Task, type: CardClickType, reminderState: Boolean, context: Context) {
        when (type) {
            CardClickType.Pin -> pinState(task.id, !task.isPin)
            CardClickType.Detail -> taskDetailData(task.id)
            CardClickType.Close -> {
                if (!primaryPreferences(context).disableRemoveNotifyInReminded()) try {
                    _notificationManager.value.removeShowedNotification(task.reminder!!, context)
                } catch (e: Exception) {
                    Log.w("RemoveShowedNotify", "Exception: $e")
                }
                remindedState(id = task.id)
            }

            CardClickType.Reminder -> reminderActionCaller(
                task.id, task.reminder, !reminderState, task.description
            )

            CardClickType.Edit -> viewModelScope.launch {
                _taskData.update {
                    it.copy(
                        editState = EditState(
                            task, database().getDetailData(task.id), true
                        )
                    )
                }
            }
        }
    }

    private var _emptyTaskDetail = TaskDetail(0, TaskDetailType.Text, "", null)
    private var _taskDetailId = MutableStateFlow(_emptyTaskDetail)
    fun taskDetailData(setter: Long? = null): MutableStateFlow<TaskDetail> {
        if (setter != null) viewModelScope.launch {
            if (setter >= 1) _taskDetailId.update {
                val data = database().getDetailData(setter)!!
                it.copy(
                    id = data.id, type = data.type,
                    dataString = data.dataString, dataByte = data.dataByte
                )
            } else _taskDetailId.value = _emptyTaskDetail
        }
        return _taskDetailId
    }

    private val _taskDetailDataSaver = TaskDetailData.instance()
    fun taskDetailDataSaver(): TaskDetailData = _taskDetailDataSaver

    /** Set task pin **/
    private fun pinState(id: Long, set: Boolean) = viewModelScope.launch {
        val booleanToInt = if (set) 1 else 0
        MainActivity.taskDatabase.taskDao().editTaskPin(id, booleanToInt)
        updateListState()
    }

    /** Remind task. autoDel and id (del reminder info) **/
    fun remindedState(id: Long = -1L, autoDel: Boolean = false) {
        viewModelScope.launch {
            if (id != -1L) database().deleteReminderData(id)
            if (autoDel) for (data in _listState.value.isRemindedItem) {
                val timeMillisData =
                    if (data.reminder != null) database().getReminderData(data.reminder)?.reminderTime
                        ?: -1L
                    else -1L
                val delReminderTime = timeMillisData < System.currentTimeMillis() - 43200000
                if (timeMillisData != -1L && delReminderTime) {
                    database().deleteReminderData(data.id)
                }
            }
            updateListState()
        }
    }

    /** Insert task to database **/
    suspend fun insertTask(
        description: String, pin: Boolean = false,
        detailType: TaskDetailType?, detailDataString: String?, detailDataByteArray: ByteArray?
    ): Long {
        return database().insertTaskData(
            description, isPin = pin, detailType = detailType,
            detailDataString = detailDataString, dataByte = detailDataByteArray
        ).also { updateListState() }
    }

    /** Edit task **/
    fun editTask(
        id: Long, edit: String, detailType: TaskDetailType?,
        detailDataString: String?, detailDataByteArray: ByteArray?,
        context: Context
    ) {
        viewModelScope.launch {
            database().editTask(
                id, edit, detailType, detailDataString, detailDataByteArray
            )
            if (database().getReminderData(id) != null) {
                val notify = _notificationManager.value.requestPermission(context)
                if (notify == PermissionState.Notification || notify == PermissionState.Both) setReminder(
                    database().getReminderData(id)!!.reminderTime - System.currentTimeMillis(),
                    TimeUnit.MILLISECONDS, id,
                    MainActivity.taskDatabase.taskDao().getAll(id).description,
                    context
                ) else cancelReminder(id, database().getReminderData(id)!!.id, context)
            }
            _taskDetailDataSaver.releaseMemory(context)
            updateListState()
        }
    }

    /** Get Task is History or Not **/
    fun getIsHistory(id: Long): Boolean {
        var value by mutableIntStateOf(-1)
        fun getIsHistoryId(id: Long) {
            viewModelScope.launch { value = MainActivity.taskDatabase.taskDao().getIsHistory(id) }
        }
        getIsHistoryId(id)
        return value >= 1
    }

    /** Delete to history or Undo to history. Must have toHistory or undoToHistory to be @true **/
    fun changeTaskVisibility(
        id: Long, toHistory: Boolean = false, undoToHistory: Boolean = false, context: Context
    ) {
        if (toHistory) viewModelScope.launch {
            // Give an id for history sequence
            val maxId = MainActivity.taskDatabase.taskDao().getIsHistoryIdTop()
            MainActivity.taskDatabase.taskDao().setHistoryId((maxId + 1), id)
            // Update to LazyColumn
            updateListState()
        } else if (undoToHistory) viewModelScope.launch { // Actions
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
            database().deleteByHistoryId(maxRetainIdNum = start)
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
                return MainActivity.taskDatabase.taskDao().getAll()
            }
            if (reset) searchingText = ""
            return listOf()
        }
        viewModelScope.launch { _listState.update { it.copy(inSearchItem = returnList()) } }
    }
}
