package sqz.checklist.task.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import sqz.checklist.data.database.repository.history.TaskHistoryRepository
import sqz.checklist.data.database.repository.task.TaskRepository
import sqz.checklist.data.storage.manager.StorageManager
import sqz.checklist.task.api.info.TaskInfo
import sqz.checklist.task.api.list.TaskList
import sqz.checklist.task.api.modify.TaskModify
import sqz.checklist.task.impl.info.TaskInfoImpl
import sqz.checklist.task.impl.list.TaskListImpl
import sqz.checklist.task.impl.modify.TaskModifyImpl

fun taskInfoProvider(
    taskRepository: TaskRepository,
): TaskInfo {
    return TaskInfoImpl(taskRepository)
}

fun taskListProvider(
    config: StateFlow<TaskList.Config>,
    taskHistoryRepository: TaskHistoryRepository,
    taskRepository: TaskRepository,
    scope: CoroutineScope = CoroutineScope(SupervisorJob())
): TaskList {
    return TaskListImpl(
        config = config,
        taskHistoryRepository = taskHistoryRepository,
        taskRepository = taskRepository,
        scope = scope,
    )
}

fun taskModifyProvider(
    taskRepository: TaskRepository,
    storageManager: StorageManager,
): TaskModify {
    return TaskModifyImpl(taskRepository, storageManager)
}
