package sqz.checklist.task.modify

import kotlinx.coroutines.test.runTest
import sqz.checklist.data.database.TaskDetailType
import sqz.checklist.data.database.repository.task.TaskRepositoryFake
import sqz.checklist.data.storage.manager.StorageManagerFake
import sqz.checklist.task.api.TaskModify
import sqz.checklist.task.api.taskModifyProvider
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TaskModifyTest {

    lateinit var taskModify: TaskModify

    @BeforeTest
    fun setup() {
        taskModify = taskModifyProvider(
            taskRepository = TaskRepositoryFake(),
            storageManager = StorageManagerFake(),
        )
    }

    @Test
    fun test_add_new_task() {
        assertFailsWith(NullPointerException::class) {
            taskModify.taskHandler().updateDescription("16800")
        }
        taskModify.makeNewTask()
        assertFailsWith(IllegalStateException::class) {
            taskModify.makeNewTask()
        }
        runTest {
            assertFailsWith(IllegalArgumentException::class) {
                taskModify.confirmModify()
            }
        }
        taskModify.taskHandler().let { task ->
            task.onTypeValueChange { it.onPinClick().onReminderClick() }
            task.onTypeValueChange { it.onReminderClick() }
            task.updateDescription("TEST")
        }
        taskModify.getModifyState.value.taskState!!.let { task ->
            assertEquals("TEST", task.description)
            task.type.let {
                it as TaskModify.Task.ModifyType.NewTask
                assertEquals(true, it.isPin)
                assertEquals(false, it.withReminder)
            }
        }
        runTest {
            assertEquals(0, taskModify.confirmModify())
        }
    }

    @Test
    fun test_add_new_task_with_details() {
        taskModify.makeNewTask()
        taskModify.taskHandler().updateDescription("TEST")
        val detailHandler = taskModify.detailHandler()
        detailHandler.addItem()
        assertFailsWith(IllegalArgumentException::class) {
            detailHandler.addItem()
        }
        detailHandler.updateItemDescription(0, "A Detail")
        detailHandler.updateSelectedType(TaskDetailType.Text)
        detailHandler.updateSelectedData(
            TaskModify.Detail.TypeState.Text("TEST")
        )
        detailHandler.unselectItem()
        detailHandler.addItem()
        detailHandler.selectItem(1)
        detailHandler.updateSelectedType(TaskDetailType.URL)
        assertFailsWith(IllegalStateException::class) {
            detailHandler.unselectItem()
        }
        detailHandler.removeItem(null)
        detailHandler.addItem()
        detailHandler.selectItem(1)
        detailHandler.updateSelectedData(
            TaskModify.Detail.TypeState.URL("https://www.cho-kaguyahime.com")
        )
        assertFailsWith(IllegalStateException::class) {
            detailHandler.moveItem(0, 1)
        }
        detailHandler.unselectItem()
        detailHandler.moveItem(0, 1)
        runTest { taskModify.confirmModify() }
    }

    @Test
    fun test_empty_cancel() {
        taskModify.makeNewTask()
        taskModify.cancelModify()
    }

    @Test
    fun test_edit_task() {
        taskModify.selectTask(1)
        runTest {
            while (true) {
                if (taskModify.getModifyState.value.state != TaskModify.State.Loading) break
            }
        }
        taskModify.getModifyState.value.let {
            assertEquals("Task 1", it.taskState!!.description)
        }
        //TODO: add more test
    }
}
