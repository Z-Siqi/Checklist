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
import com.sqz.checklist.database.DatabaseRepository
import com.sqz.checklist.database.Task
import com.sqz.checklist.database.TaskDetail
import com.sqz.checklist.database.TaskDetailType
import com.sqz.checklist.preferences.PreferencesInCache
import com.sqz.checklist.preferences.PrimaryPreferences
import com.sqz.checklist.ui.main.task.handler.ModifyHandler
import com.sqz.checklist.ui.main.task.handler.ReminderHandler
import com.sqz.checklist.ui.main.task.layout.NavConnectData
import com.sqz.checklist.ui.main.task.layout.TopBarMenuClickType
import com.sqz.checklist.ui.main.task.layout.function.CheckDataState
import com.sqz.checklist.ui.main.task.layout.item.CardClickType
import com.sqz.checklist.ui.main.task.layout.item.ListData
import com.sqz.checklist.ui.common.media.audioMediaPath
import com.sqz.checklist.ui.common.media.pictureMediaPath
import com.sqz.checklist.ui.common.media.videoMediaPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

open class TaskLayoutViewModel : ViewModel() {
    open fun database(): DatabaseRepository = DatabaseRepository(
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

    private val _requestUpdate = MutableStateFlow(false)

    private var _init = MutableStateFlow(false)
    private val _isUpdateRunning = AtomicBoolean(false)
    private val _listState = MutableStateFlow(ListData())
    val listState: StateFlow<ListData> = _listState.asStateFlow()
    private fun updateListState(init: Boolean = false) {
        if (_isUpdateRunning.get()) return else _isUpdateRunning.set(true)
        viewModelScope.launch {
            _listState.update { lists ->
                val remindedList = MainActivity.taskDatabase.taskDao().getIsRemindedList().filter {
                    if (it.reminder != null) database().getReminderData(it.reminder)?.isReminded
                        ?: false
                    else false
                }
                lists.copy(
                    item = MainActivity.taskDatabase.taskDao().getAll(),
                    pinnedItem = MainActivity.taskDatabase.taskDao().getAll(0),
                    isRemindedItem = remindedList,
                    unLoading = false
                )
            }.also { Log.d("ViewModel", "List is Update") }
            if (!init) updateInSearch(searchingText) else _init.value = true
            delay(220)
            _isUpdateRunning.set(false)
        }
    }

    private fun searchView(setter: Boolean) { //Connect top bar & nav bar search actions
        _listState.update { it.copy(searchView = setter) }
        _navExtendedConnectData.update { it.copy(searchState = setter) }
    }

    /** Reminder handler **/
    var reminderHandler: ReminderHandler

    /** Modify the task **/
    var modifyHandler: ModifyHandler

    init {
        this.let { viewModel ->
            reminderHandler = ReminderHandler.instance(viewModel, _requestUpdate, _init)
            modifyHandler = ModifyHandler.instance(viewModel, _requestUpdate)
        }
        if (!_init.value) updateListState(init = true)
        viewModelScope.launch {
            _requestUpdate.collect { state ->
                if (state) {
                    updateListState()
                    _requestUpdate.update { false }
                }
            }
        }
        Log.d("TaskLayoutViewModel", "ViewModel is init")
    }

    private fun primaryPreferences(context: Context): PrimaryPreferences {
        return PrimaryPreferences(context)
    }

    private val _undo = MutableStateFlow(CheckDataState())
    val undo: MutableStateFlow<CheckDataState> = _undo
    fun resetUndo(context: Context? = null) { // reset undo state
        if (context != null) reminderHandler.cancelHistoryReminder(context = context)
        _undo.value = CheckDataState()
    }

    fun taskChecked(id: Long) = _undo.update { // when task is checked
        modifyHandler.onTaskChecked(id)
        resetUndo()
        it.copy(onCheckTask = true, toUndoId = id)
    }

    /** Process undo button state **/
    fun undoButtonProcess(lazyState: LazyListState, context: Context): Boolean {
        if (!primaryPreferences(context).disableUndoButton()) {
            if (_undo.value.onCheckTask) viewModelScope.launch {
                if (_undo.value.rememberScroll == null && _undo.value.rememberScrollIndex == null) {
                    _undo.update {
                        it.copy(
                            rememberScroll = lazyState.firstVisibleItemScrollOffset,
                            rememberScrollIndex = lazyState.firstVisibleItemIndex,
                        )
                    }
                    _undo.value.let { // process timeout
                        if (it.rememberScroll != null && it.rememberScrollIndex != null) {
                            if (!_undo.value.onCheckTask) this.cancel()
                            delay(1500)
                            val isTimeout =
                                it.rememberScroll > lazyState.firstVisibleItemScrollOffset + 10 || it.rememberScroll < lazyState.firstVisibleItemScrollOffset - 10
                            for (i in 1..7) {
                                if (!_undo.value.onCheckTask) this.cancel()
                                delay(500)
                                if (it.rememberScrollIndex != lazyState.firstVisibleItemIndex || isTimeout) break
                            }
                            resetUndo(context)
                        }
                    }
                }
            }
            return _undo.value.onCheckTask
        } else return false.also { resetUndo(context) }
    }

    /** Task click action **/
    fun onTaskItemClick(task: Task, type: CardClickType, context: Context) {
        when (type) {
            CardClickType.Pin -> modifyHandler.pinState(task.id, !task.isPin)
            CardClickType.Detail -> this.taskDetailData(task.id)
            CardClickType.Reminder -> reminderHandler.requestReminder(task.id)
            CardClickType.Edit -> modifyHandler.requestEditTask(task)
            CardClickType.Close -> viewModelScope.launch {
                if (!primaryPreferences(context).disableRemoveNotifyInReminded()) try {
                    reminderHandler.notifyManager.removeShowedNotification(task.reminder!!, context)
                    database().deleteReminderData(task.id)
                    updateListState()
                } catch (e: Exception) {
                    Log.w("RemoveShowedNotify", "Exception: $e")
                } else try {
                    database().deleteReminderData(task.id)
                    updateListState()
                } catch (e: Exception) {
                    Log.w("RemoveRemindedNotify", "Exception: $e")
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

    /** Remind task. autoDel and id (del reminder info) **/
    fun autoDeleteRemindedTaskInfo(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val primaryPreferences = PrimaryPreferences(context)
            if (primaryPreferences.recentlyRemindedKeepTime() > 0L) try {
                for (data in _listState.value.isRemindedItem) {
                    val timeMillisData =
                        if (data.reminder != null) database().getReminderData(data.reminder)?.reminderTime
                            ?: -1L
                        else -1L
                    val delReminderTime =
                        timeMillisData < System.currentTimeMillis() - primaryPreferences.recentlyRemindedKeepTime()
                    if (timeMillisData != -1L && delReminderTime) {
                        if (primaryPreferences.removeNoticeInAutoDelReminded()) {
                            reminderHandler.notifyManager.removeShowedNotification(
                                data.reminder!!, context
                            )
                        }
                        database().deleteReminderData(data.id)
                    }
                }
                updateListState()
            } catch (e: NoSuchFieldException) {
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

    /** Get Task is History or Not **/
    fun getIsHistory(id: Long): Boolean {
        var value by mutableIntStateOf(-1)
        fun getIsHistoryId(id: Long) {
            viewModelScope.launch { value = MainActivity.taskDatabase.taskDao().getIsHistory(id) }
        }
        getIsHistoryId(id)
        return value >= 1
    }

    /** Auto Delete History Task **/
    fun autoDeleteHistoryTask(start: Int) {
        viewModelScope.launch(Dispatchers.IO) {
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
