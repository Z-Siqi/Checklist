package sqz.checklist.data.database.repository.history

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import sqz.checklist.data.database.Task
import kotlin.time.Clock

class TaskHistoryRepositoryFake : TaskHistoryRepository {

    override fun getTaskHistoryList(): Flow<List<Task>> {
        val task1 = Task(
            id = 1, description = "Task 1",
            createDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
            doingState = null, isPin = true,
        )
        val task2 = task1.copy(id = 2, description = "Task 2")
        return flowOf(listOf(task1, task2))
    }

    override suspend fun restoreTaskFromHistoryList(taskId: Long) {
        println("restoreTaskFromHistoryList - taskId: $taskId")
    }

    override suspend fun restoreAllTaskFromHistory() {
        println("restoreAllTaskFromHistory called")
    }

    override suspend fun deleteOldHistoryTask(numOfAllowedHistory: Int) {
        println("deleteOldHistoryTask - numOfAllowedHistory: $numOfAllowedHistory")
    }

    override suspend fun deleteFullTask(taskId: Long) {
        println("deleteFullTask - taskId: $taskId")
    }
}
