package sqz.checklist.task.list

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import sqz.checklist.data.database.Task
import sqz.checklist.data.database.TaskDetail
import sqz.checklist.data.database.model.TaskViewData
import sqz.checklist.data.database.repository.history.TaskHistoryRepositoryFake
import sqz.checklist.data.database.repository.task.TaskRepository
import sqz.checklist.task.api.list.TaskList
import sqz.checklist.task.api.list.model.TaskItemModel
import sqz.checklist.task.impl.list.TaskListImpl
import sqz.checklist.task.impl.list.item.UndoProcesser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListUnitTest {

    private val today = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    private class FakeTaskRepository : TaskRepository {
        val taskListFlow = MutableStateFlow<List<TaskViewData>>(emptyList())
        val remindedListFlow = MutableStateFlow<List<TaskViewData>>(emptyList())
        val pinnedListFlow = MutableStateFlow<List<TaskViewData>>(emptyList())
        val searchedListFlow = MutableStateFlow<List<TaskViewData>>(emptyList())
        var removedTaskId: Long? = null
        var pinnedTaskId: Long? = null
        var pinnedUpdate: Boolean? = null

        val taskDetails = mutableMapOf<Long, List<TaskDetail>>()

        override fun getTaskList(): Flow<List<TaskViewData>> = taskListFlow
        override fun getPinnedTaskList(): Flow<List<TaskViewData>> = pinnedListFlow
        override fun getRemindedTaskList(): Flow<List<TaskViewData>> = remindedListFlow
        override fun getSearchedList(searchQuery: String): Flow<List<TaskViewData>> = searchedListFlow
        override suspend fun getTaskSum(): Long {
            return taskListFlow.value.size.toLong()
        }

        override suspend fun onTaskPinChange(taskId: Long, update: Boolean) {
            pinnedTaskId = taskId
            pinnedUpdate = update
        }

        override suspend fun removeTaskFromDefaultList(taskId: Long) {
            removedTaskId = taskId
        }

        override fun isTaskListEmpty(): Flow<Boolean> {
            return flowOf(false)
        }

        override suspend fun getFullTask(id: Long): Pair<Task, List<TaskDetail>?> {
            val task = (taskListFlow.value + remindedListFlow.value + pinnedListFlow.value + searchedListFlow.value)
                .find { it.task.id == id }?.task
                ?: throw NullPointerException("Task not found")
            return task to taskDetails[id]
        }

        override suspend fun modifyTask(task: Task, detail: List<TaskDetail>?): Long = 0
    }

    @Test
    fun taskListImpl_resetExternalRequest_should_set_request_to_None() = runTest {
        val repository = FakeTaskRepository()
        val taskList = TaskListImpl(
            MutableStateFlow(TaskList.Config()), TaskHistoryRepositoryFake(), repository
        )
        taskList.resetExternalRequest()
        assertEquals(TaskItemModel.ExternalRequest.None, taskList.getExternalRequest.value)
    }

    @Test
    fun taskListImpl_updateList_should_change_inventory_from_Loading_to_Default() = runTest {
        val repository = FakeTaskRepository()
        val taskList = TaskListImpl(
            MutableStateFlow(TaskList.Config()), TaskHistoryRepositoryFake(), repository
        )
        taskList.updateList()
        assertTrue(taskList.getTaskListInventory.value is TaskList.Inventory.Default)
    }

    @Test
    fun taskListImpl_onSearchRequest_should_update_inventory_to_Search() = runTest {
        val repository = FakeTaskRepository()
        val taskList = TaskListImpl(
            MutableStateFlow(TaskList.Config()), TaskHistoryRepositoryFake(), repository
        )
        taskList.onSearchRequest("test")
        advanceUntilIdle()
        yield()
        taskList.getTaskListInventory.test {
            val inventory = (awaitItem() as TaskList.Inventory.Search)
            assertEquals("test", inventory.searchQuery)
        }
        val inventory = taskList.getTaskListInventory.value
        assertTrue(inventory is TaskList.Inventory.Search)
        assertEquals("test", inventory.searchQuery)
    }

    @Test
    fun taskListImpl_onSearchRequest_with_null_should_return_to_Default() = runTest {
        val repository = FakeTaskRepository()
        val taskList = TaskListImpl(
            MutableStateFlow(TaskList.Config()), TaskHistoryRepositoryFake(), repository
        )
        taskList.onSearchRequest("test")
        taskList.getTaskListInventory.test {
            assertTrue(awaitItem() is TaskList.Inventory.Search)
        }
        taskList.onSearchRequest(null)
        taskList.getTaskListInventory.test {
            assertTrue(awaitItem() is TaskList.Inventory.Default)
        }
    }

    @Test
    fun taskItem_onEditRequest_should_update_externalRequest() = runTest {
        val repository = FakeTaskRepository()
        val taskList = TaskListImpl(
            MutableStateFlow(TaskList.Config()), TaskHistoryRepositoryFake(), repository
        )
        val task = Task(id = 10, description = "Test", createDate = today)
        repository.taskListFlow.value = listOf(TaskViewData(task,
            isDetailExist = false,
            isReminded = false,
            reminderTime = null
        ))
        taskList.updateList()
        val inventory = taskList.getTaskListInventory.value as TaskList.Inventory.Default
        val items = inventory.primaryList.first()
        val item = items[0]
        item.onEditRequest()
        assertEquals(TaskItemModel.ExternalRequest.Edit(10), taskList.getExternalRequest.value)
    }

    @Test
    fun taskItem_onRemoveAction_should_call_repository_remove() = runTest {
        val repository = FakeTaskRepository()
        val taskList = TaskListImpl(
            MutableStateFlow(TaskList.Config(enableUndo = true)),
            TaskHistoryRepositoryFake(),
            repository
        )
        val task = Task(id = 20, description = "Test", createDate = today)
        repository.taskListFlow.value = listOf(TaskViewData(task,
            isDetailExist = false,
            isReminded = false,
            reminderTime = null
        ))
        taskList.updateList()
        val inventory = taskList.getTaskListInventory.value as TaskList.Inventory.Default
        val items = inventory.primaryList.first()
        val item = items[0]
        item.onRemoveAction()
        advanceUntilIdle()
        assertEquals(20L, repository.removedTaskId)
    }

    @Test
    fun undoProcesser_undo_state_should_change_when_requested() = runTest {
        val scope = CoroutineScope(SupervisorJob())
        val undoProcesser = UndoProcesser(scope, MutableStateFlow(true))
        undoProcesser.requestUndoState(30)
        while (!undoProcesser.undoState.value) {
            delay(10000)
            if (undoProcesser.undoState.value) break
        }
        assertTrue(undoProcesser.undoState.value)
        assertEquals(30L, undoProcesser.getUndoTaskId())
    }

    @Test
    fun taskListImpl_requestUndo_should_call_repository_restore() = runTest {
        val repository = FakeTaskRepository()
        val taskList = TaskListImpl(
            MutableStateFlow(TaskList.Config(enableUndo = true)),
            TaskHistoryRepositoryFake(),
            repository
        )
        val task = Task(id = 50, description = "Undo Test", createDate = today)
        repository.taskListFlow.value = listOf(TaskViewData(task,
            isDetailExist = false,
            isReminded = false,
            reminderTime = null
        ))
        taskList.updateList()
        val inventory = taskList.getTaskListInventory.value as TaskList.Inventory.Default
        val items = inventory.primaryList.first()
        val item = items[0]

        item.onRemoveAction()
        taskList.getUndoState.test {
            assertTrue(awaitItem())
            var undoId: Long? = null
            taskList.requestUndo { undoId = it }
            awaitItem()
            assertEquals(50L, undoId)
        }
    }
}
