package sqz.checklist.data.database.repository.task

import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import sqz.checklist.data.database.Task
import sqz.checklist.data.database.TaskDetail
import sqz.checklist.data.database.TaskDetailType
import kotlin.time.Clock

class TaskRepositoryFake : TaskRepository {
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
        println("Task: $task; Detail: $detail")
        return task.id
    }
}
