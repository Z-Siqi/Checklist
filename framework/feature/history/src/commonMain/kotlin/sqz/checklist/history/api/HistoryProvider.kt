package sqz.checklist.history.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import sqz.checklist.data.database.repository.history.TaskHistoryRepository
import sqz.checklist.history.api.task.TaskHistory
import sqz.checklist.history.impl.task.TaskHistoryImpl

fun taskHistoryProvider(
    config: StateFlow<TaskHistory.Config>,
    taskHistoryRepository: TaskHistoryRepository,
    scope: CoroutineScope = CoroutineScope(SupervisorJob())
): TaskHistory {
    return TaskHistoryImpl(
        config = config,
        taskHistoryRepository = taskHistoryRepository,
        scope = scope,
    )
}
