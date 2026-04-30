package sqz.checklist.task.impl.list

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sqz.checklist.data.database.model.TaskViewData
import sqz.checklist.data.database.repository.history.TaskHistoryRepository
import sqz.checklist.data.database.repository.reminder.TaskReminderRepository
import sqz.checklist.data.database.repository.task.TaskRepository
import sqz.checklist.task.api.list.TaskList
import sqz.checklist.task.api.list.model.TaskItemModel
import sqz.checklist.task.impl.list.item.TaskItem
import sqz.checklist.task.impl.list.item.UndoProcesser
import kotlin.time.Clock

internal class TaskListImpl(
    private val config: StateFlow<TaskList.Config>,
    private val taskHistoryRepository: TaskHistoryRepository,
    private val taskRepository: TaskRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob())
) : TaskList {

    private val _listInventory: MutableStateFlow<TaskList.Inventory> = MutableStateFlow(
        TaskList.Inventory.Loading
    )

    override val getTaskListInventory: StateFlow<TaskList.Inventory> = _listInventory.asStateFlow()

    private val _undoProcesser = UndoProcesser(
        scope = scope,
        enableUndo = config.map { it.enableUndo }.stateIn(
            scope, SharingStarted.Eagerly, config.value.enableUndo
        ),
    ) { // onReset
        if (config.value.autoDelIsHistoryTaskNumber == null) return@UndoProcesser
        scope.launch {
            taskHistoryRepository.deleteOldHistoryTask(
                numOfAllowedHistory = config.value.autoDelIsHistoryTaskNumber!!
            )
        }
    }

    override val getUndoState: StateFlow<Boolean> = _undoProcesser.undoState

    private val _externalRequest = MutableStateFlow<TaskItemModel.ExternalRequest>(
        TaskItemModel.ExternalRequest.None
    )

    override val getExternalRequest = _externalRequest.asStateFlow()

    override fun resetExternalRequest() {
        _externalRequest.update { TaskItemModel.ExternalRequest.None }
    }

    private fun mapTaskList(taskView: Flow<List<TaskViewData>>): Flow<List<TaskItemModel>> {
        return taskView.map {
            if (it.isEmpty()) {
                return@map listOf()
            }
            return@map it.map { taskView ->
                TaskItem(
                    taskRepository = taskRepository,
                    scope = scope,
                    _externalRequest = _externalRequest,
                    _undoProcesser = _undoProcesser,
                    taskViewData = taskView,
                )
            }
        }
    }

    private fun setDefault(): TaskList.Inventory.Default {
        val default = TaskList.Inventory.Default(
            primaryList = this.mapTaskList(taskRepository.getTaskList()),
            remindedList = this.mapTaskList(taskRepository.getRemindedTaskList()),
            pinnedList = this.mapTaskList(taskRepository.getPinnedTaskList()),
        )
        return default
    }

    private fun setSearch(searchQuery: String): TaskList.Inventory.Search {
        val search = TaskList.Inventory.Search(
            searchQuery = searchQuery, inSearchList = this.mapTaskList(
                taskRepository.getSearchedList(searchQuery)
            )
        )
        return search
    }

    override suspend fun updateList() {
        if (_listInventory.value is TaskList.Inventory.Loading) _listInventory.update {
            this.setDefault()
        }.also {
            if (config.value.autoDelIsHistoryTaskNumber == null) return
            scope.launch {
                taskHistoryRepository.deleteOldHistoryTask(
                    numOfAllowedHistory = config.value.autoDelIsHistoryTaskNumber!!
                )
            }
            return
        }
        if (_listInventory.value is TaskList.Inventory.Search) {
            val searchQuery = (_listInventory.value as TaskList.Inventory.Search).searchQuery
            _listInventory.update { TaskList.Inventory.Loading }
            val search = this.setSearch(searchQuery)
            _listInventory.update { search }
            return
        }
        _listInventory.update { TaskList.Inventory.Loading }
        val default = this.setDefault()
        _listInventory.update { default }
    }

    override suspend fun removeRemindedInfoByTime(
        dbReminder: TaskReminderRepository,
        removeNotification: suspend (taskId: Long) -> Boolean?,
        onRemoveNotification: suspend (taskId: Long) -> Unit,
    ): Int? {
        val recentlyRemindedKeepTime = config.value.recentlyRemindedKeepTime.also {
            if (it <= 0L) return null
        }
        taskRepository.getRemindedTaskList().stateIn(scope).let { list ->
            for (data in list.value) {
                val timeMillisData = data.reminderTime ?: continue
                val delReminderTime: Boolean =
                    timeMillisData < (Clock.System.now().toEpochMilliseconds() - recentlyRemindedKeepTime)
                val removeNotification = removeNotification(data.task.id)
                if (delReminderTime) {
                    if (removeNotification == null || removeNotification) {
                        onRemoveNotification(data.task.id)
                        dbReminder.deleteRemindedInfo(data.task.id)
                    }
                }
            }
            return list.value.size
        }
    }

    override fun requestUndo(onUndoTaskId: (Long) -> Unit) {
        val taskId = _undoProcesser.getUndoTaskId()
        scope.launch {
            taskHistoryRepository.restoreTaskFromHistoryList(taskId)
            onUndoTaskId(taskId)
            _undoProcesser.resetUndoProcessor()
        }
    }

    override fun setUndoBreakFactor(any: Any) {
        _undoProcesser.setUndoBreakFactor(any)
    }

    override fun onSearchRequest(query: String?) {
        if (_listInventory.value !is TaskList.Inventory.Search) {
            if (query != null) _listInventory.update {
                this@TaskListImpl.setSearch(query)
            }
            return
        }
        if (query == null) {
            _listInventory.update { this@TaskListImpl.setDefault() }
            return
        }
        _listInventory.update { this@TaskListImpl.setSearch(query) }
    }

    override fun isInventoryEmpty(): Flow<Boolean> {
        if (_listInventory.value is TaskList.Inventory.Loading) {
            throw NullPointerException("Inventory cannot be loading state!")
        }
        return taskRepository.isTaskListEmpty()
    }
}
