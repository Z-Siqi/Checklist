package sqz.checklist.task.api

import sqz.checklist.data.database.repository.task.TaskRepository
import sqz.checklist.data.storage.manager.StorageManager
import sqz.checklist.task.impl.modify.TaskModifyImpl

fun taskModifyProvider(
    taskRepository: TaskRepository,
    storageManager: StorageManager
): TaskModify {
    return TaskModifyImpl(taskRepository, storageManager)
}
