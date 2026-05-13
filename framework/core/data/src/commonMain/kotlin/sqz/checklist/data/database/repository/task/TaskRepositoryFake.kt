package sqz.checklist.data.database.repository.task

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import sqz.checklist.data.database.Task
import sqz.checklist.data.database.TaskDetail
import sqz.checklist.data.database.TaskDetailType
import sqz.checklist.data.database.model.TaskViewData
import kotlin.time.Clock

class TaskRepositoryFake : TaskRepository {

    override fun getTaskList(): Flow<List<TaskViewData>> {
        val task1 = Task(
            id = 1, description = "Task 1",
            createDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
            doingState = null, isPin = true,
        )
        val task2 = task1.copy(id = 2, description = "Task 2")
        val taskView1 = TaskViewData(
            task = task1, isDetailExist = false, isReminded = false, reminderTime = null
        )
        val taskView2 = taskView1.copy(task = task2)
        return flowOf(listOf(taskView1, taskView2))
    }

    override fun getRemindedTaskList(): Flow<List<TaskViewData>> {
        val task = Task(
            id = 1, description = "Task 1",
            createDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
            doingState = null, isPin = true,
        )
        val taskView = TaskViewData(task,
            isDetailExist = false, isReminded = true,
            reminderTime = Clock.System.now().toEpochMilliseconds()
        )
        return flowOf(listOf(taskView))
    }

    override fun getPinnedTaskList(): Flow<List<TaskViewData>> {
        return this.getTaskList()
    }

    override fun getSearchedList(searchQuery: String): Flow<List<TaskViewData>> {
        println("getSearchedList - searchQuery: $searchQuery")
        return this.getTaskList()
    }

    override suspend fun getTaskSum(): Long {
        return 2
    }

    override suspend fun onTaskPinChange(taskId: Long, update: Boolean) {
        println("onTaskPinChange - taskId: $taskId; update: $update")
    }

    override suspend fun removeTaskFromDefaultList(taskId: Long) {
        println("removeTaskFromDefaultList - taskId: $taskId")
    }

    override fun isTaskListEmpty(): Flow<Boolean> {
        return flowOf(false)
    }

    override suspend fun getFullTask(id: Long): Pair<Task, List<TaskDetail>?> {
        val task = Task(
            id = 1, description = "Task 1",
            createDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
            doingState = null, isPin = true,
        )
        val taskDetail = listOf(
            TaskDetail(
                id = 1, taskId = 1, type = TaskDetailType.Text,
                description = "Task 1 Detail Item Name", dataString = null,
                dataByte = "Task 1 Detail Data".encodeToByteArray()
            )
        )
        return task to taskDetail
    }

    override suspend fun modifyTask(task: Task, detail: List<TaskDetail>?): Long {
        println("modifyTask - Task: $task; Detail: $detail")
        return task.id
    }
}
