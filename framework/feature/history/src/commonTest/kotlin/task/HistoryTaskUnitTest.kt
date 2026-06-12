package task

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import sqz.checklist.data.database.Task
import sqz.checklist.data.database.repository.history.TaskHistoryRepository
import sqz.checklist.history.api.task.TaskHistory
import sqz.checklist.history.impl.task.TaskHistoryImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryTaskUnitTest {

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    private class FakeTaskHistoryRepository(
        initialTasks: List<Task> = emptyList(),
        private val emptyFlow: Flow<Boolean> = flowOf(initialTasks.isEmpty()),
    ) : TaskHistoryRepository {
        val taskHistoryFlow = MutableStateFlow(initialTasks)
        var restoredTaskId: Long? = null
        var deletedTaskId: Long? = null
        var deleteOldHistoryArg: Int? = null
        var restoreAllCalled = false

        override fun getTaskHistoryList(): Flow<List<Task>> = taskHistoryFlow

        override fun isTaskHistoryListEmpty(): Flow<Boolean> = emptyFlow

        override suspend fun restoreTaskFromHistoryList(taskId: Long) {
            restoredTaskId = taskId
        }

        override suspend fun restoreAllTaskFromHistory() {
            restoreAllCalled = true
        }

        override suspend fun deleteOldHistoryTask(numOfAllowedHistory: Int) {
            deleteOldHistoryArg = numOfAllowedHistory
        }

        override suspend fun deleteFullTask(taskId: Long) {
            deletedTaskId = taskId
        }
    }

    @Test
    fun taskHistoryImpl_init_should_change_inventory_from_Loading_to_Default() = runTest {
        val repository = FakeTaskHistoryRepository(
            initialTasks = listOf(Task(id = 1, description = "Task 1", createDate = today))
        )
        val history = TaskHistoryImpl(
            config = MutableStateFlow(TaskHistory.Config()),
            taskHistoryRepository = repository,
            scope = this,
        )

        assertIs<TaskHistory.Inventory.Loading>(history.getHistoryInventory.value)

        advanceUntilIdle()

        val inventory = assertIs<TaskHistory.Inventory.Default>(history.getHistoryInventory.value)
        assertEquals(repository.taskHistoryFlow, inventory.historyList)
        assertNull(inventory.selectedTaskId)
    }

    @Test
    fun taskHistoryImpl_isInventoryEmpty_should_delegate_to_repository() = runTest {
        val repository = FakeTaskHistoryRepository(emptyFlow = flowOf(true))
        val history = TaskHistoryImpl(
            config = MutableStateFlow(TaskHistory.Config()),
            taskHistoryRepository = repository,
            scope = this,
        )

        assertTrue(history.isInventoryEmpty().first())
    }

    @Test
    fun taskHistoryImpl_selectTask_should_update_selectedTaskId() = runTest {
        val repository = FakeTaskHistoryRepository(
            initialTasks = listOf(Task(id = 7, description = "Task 7", createDate = today))
        )
        val history = TaskHistoryImpl(
            config = MutableStateFlow(TaskHistory.Config()),
            taskHistoryRepository = repository,
            scope = this,
        )
        advanceUntilIdle()

        history.selectTask(7)

        val inventory = assertIs<TaskHistory.Inventory.Default>(history.getHistoryInventory.value)
        assertEquals(7L, inventory.selectedTaskId)
    }

    @Test
    fun taskHistoryImpl_deselectTask_should_clear_selectedTaskId() = runTest {
        val repository = FakeTaskHistoryRepository(
            initialTasks = listOf(Task(id = 8, description = "Task 8", createDate = today))
        )
        val history = TaskHistoryImpl(
            config = MutableStateFlow(TaskHistory.Config()),
            taskHistoryRepository = repository,
            scope = this,
        )
        advanceUntilIdle()
        history.selectTask(8)

        history.deselectTask()

        val inventory = assertIs<TaskHistory.Inventory.Default>(history.getHistoryInventory.value)
        assertNull(inventory.selectedTaskId)
    }

    @Test
    fun taskHistoryImpl_selectTask_when_loading_should_throw() = runTest {
        val history = TaskHistoryImpl(
            config = MutableStateFlow(TaskHistory.Config()),
            taskHistoryRepository = FakeTaskHistoryRepository(),
            scope = this,
        )

        assertFailsWith<IllegalStateException> {
            history.selectTask(1)
        }
    }

    @Test
    fun taskHistoryImpl_deleteSelectedTask_should_call_repository_and_clear_selection() = runTest {
        val repository = FakeTaskHistoryRepository(
            initialTasks = listOf(Task(id = 11, description = "Task 11", createDate = today))
        )
        val history = TaskHistoryImpl(
            config = MutableStateFlow(TaskHistory.Config()),
            taskHistoryRepository = repository,
            scope = this,
        )
        advanceUntilIdle()
        history.selectTask(11)

        history.deleteSelectedTask()
        advanceUntilIdle()

        val inventory = assertIs<TaskHistory.Inventory.Default>(history.getHistoryInventory.value)
        assertNull(inventory.selectedTaskId)
        assertEquals(11L, repository.deletedTaskId)
    }

    @Test
    fun taskHistoryImpl_redoSelectedTask_should_call_repository_and_clear_selection() = runTest {
        val repository = FakeTaskHistoryRepository(
            initialTasks = listOf(Task(id = 12, description = "Task 12", createDate = today))
        )
        val history = TaskHistoryImpl(
            config = MutableStateFlow(TaskHistory.Config()),
            taskHistoryRepository = repository,
            scope = this,
        )
        advanceUntilIdle()
        history.selectTask(12)

        history.redoSelectedTask()
        advanceUntilIdle()

        val inventory = assertIs<TaskHistory.Inventory.Default>(history.getHistoryInventory.value)
        assertNull(inventory.selectedTaskId)
        assertEquals(12L, repository.restoredTaskId)
    }

    @Test
    fun taskHistoryImpl_deleteAllHistory_should_reload_inventory_and_call_repository() = runTest {
        val repository = FakeTaskHistoryRepository(
            initialTasks = listOf(Task(id = 21, description = "Task 21", createDate = today))
        )
        val history = TaskHistoryImpl(
            config = MutableStateFlow(TaskHistory.Config()),
            taskHistoryRepository = repository,
            scope = this,
        )
        advanceUntilIdle()
        repository.taskHistoryFlow.value = emptyList()

        history.deleteAllHistory()
        withTimeout(2_000) {
            while (repository.deleteOldHistoryArg == null) {
                delay(10)
            }
        }

        assertEquals(0, repository.deleteOldHistoryArg)
        val inventory = assertIs<TaskHistory.Inventory.Default>(history.getHistoryInventory.value)
        assertEquals(emptyList(), inventory.historyList.first())
    }

    @Test
    fun taskHistoryImpl_redoAllHistory_should_call_repository() = runTest {
        val repository = FakeTaskHistoryRepository(
            initialTasks = listOf(Task(id = 31, description = "Task 31", createDate = today))
        )
        val history = TaskHistoryImpl(
            config = MutableStateFlow(TaskHistory.Config()),
            taskHistoryRepository = repository,
            scope = this,
        )
        advanceUntilIdle()

        history.redoAllHistory()
        withTimeout(2_000) {
            while (!repository.restoreAllCalled) {
                delay(10)
            }
        }

        assertTrue(repository.restoreAllCalled)
    }

    @Test
    fun taskHistoryImpl_redoAllHistory_when_loading_should_throw() = runTest {
        val history = TaskHistoryImpl(
            config = MutableStateFlow(TaskHistory.Config()),
            taskHistoryRepository = FakeTaskHistoryRepository(),
            scope = this,
        )

        assertFailsWith<IllegalStateException> {
            history.redoAllHistory()
        }
    }
}
