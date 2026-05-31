package sqz.checklist.history.impl.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sqz.checklist.data.database.repository.history.TaskHistoryRepository
import sqz.checklist.history.api.task.TaskHistory

internal class TaskHistoryImpl(
    private val config: StateFlow<TaskHistory.Config>,
    private val taskHistoryRepository: TaskHistoryRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob())
) : TaskHistory {

    private val _inventory = MutableStateFlow<TaskHistory.Inventory>(
        TaskHistory.Inventory.Loading
    )

    override val getHistoryInventory: StateFlow<TaskHistory.Inventory> = _inventory.asStateFlow()

    init {
        scope.launch { // load list
            val history = taskHistoryRepository.getTaskHistoryList()
            history.first().also { // update until got the first element emitted
                _inventory.update { TaskHistory.Inventory.Default(historyList = history) }
            }
        }
    }

    override fun isInventoryEmpty(): Flow<Boolean> {
        return taskHistoryRepository.isTaskHistoryListEmpty()
    }

    override fun selectTask(taskId: Long) {
        if (_inventory.value is TaskHistory.Inventory.Loading) {
            throw IllegalStateException("TaskHistory.Inventory is still loading!")
        }
        when (_inventory.value) {
            is TaskHistory.Inventory.Default -> _inventory.update { current ->
                (current as TaskHistory.Inventory.Default).copy(selectedTaskId = taskId)
            }

            else -> throw NullPointerException("Unknown TaskHistory.Inventory state!")
        }
    }

    override fun deselectTask() {
        if (_inventory.value is TaskHistory.Inventory.Loading) {
            throw IllegalStateException("TaskHistory.Inventory is still loading!")
        }
        when (_inventory.value) {
            is TaskHistory.Inventory.Default -> _inventory.update { current ->
                (current as TaskHistory.Inventory.Default).copy(selectedTaskId = null)
            }

            else -> throw NullPointerException("Unknown TaskHistory.Inventory state!")
        }
    }

    override fun deleteSelectedTask() {
        if (_inventory.value is TaskHistory.Inventory.Loading) {
            throw IllegalStateException("TaskHistory.Inventory is still loading!")
        }
        when (_inventory.value) {
            is TaskHistory.Inventory.Default -> _inventory.update { current ->
                val current = current as TaskHistory.Inventory.Default
                scope.launch {
                    val taskId = current.selectedTaskId ?: throw NullPointerException(
                        "No selected task!"
                    )
                    taskHistoryRepository.deleteFullTask(taskId)
                }
                current.copy(selectedTaskId = null)
            }

            else -> throw NullPointerException("Unknown TaskHistory.Inventory state!")
        }
    }

    override fun redoSelectedTask() {
        if (_inventory.value is TaskHistory.Inventory.Loading) {
            throw IllegalStateException("TaskHistory.Inventory is still loading!")
        }
        when (_inventory.value) {
            is TaskHistory.Inventory.Default -> _inventory.update { current ->
                val current = current as TaskHistory.Inventory.Default
                scope.launch {
                    val taskId = current.selectedTaskId ?: throw NullPointerException(
                        "No selected task!"
                    )
                    taskHistoryRepository.restoreTaskFromHistoryList(taskId)
                }
                current.copy(selectedTaskId = null)
            }

            else -> throw NullPointerException("Unknown TaskHistory.Inventory state!")
        }
    }

    override fun deleteAllHistory() {
        if (_inventory.value !is TaskHistory.Inventory.Default) {
            if (_inventory.value is TaskHistory.Inventory.Loading) {
                println("Ignored due to state is TaskHistory.Inventory.Loading")
                return
            }
            throw IllegalStateException("Only TaskHistory.Inventory.Default allowed this!")
        }
        scope.launch(Dispatchers.IO) {
            _inventory.value = TaskHistory.Inventory.Loading
            taskHistoryRepository.deleteOldHistoryTask(0)
            val history = taskHistoryRepository.getTaskHistoryList()
            _inventory.update { TaskHistory.Inventory.Default(historyList = history) }
        }
    }

    override fun redoAllHistory() {
        if (_inventory.value !is TaskHistory.Inventory.Default) {
            throw IllegalStateException("Only TaskHistory.Inventory.Default allowed this!")
        }
        scope.launch(Dispatchers.IO) {
            taskHistoryRepository.restoreAllTaskFromHistory()
        }
    }
}
