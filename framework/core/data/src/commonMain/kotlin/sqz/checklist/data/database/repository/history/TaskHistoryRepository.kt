package sqz.checklist.data.database.repository.history

import kotlinx.coroutines.flow.Flow
import sqz.checklist.data.database.DatabaseProvider
import sqz.checklist.data.database.Task
import sqz.checklist.data.database.TaskDetail
import sqz.checklist.data.storage.manager.StorageManager

interface TaskHistoryRepository {

    /**
     * Get the flow of [Task] list.
     *
     * @return [Flow] of [Task] list.
     */
    fun getTaskHistoryList(): Flow<List<Task>>

    /**
     * Update [Task.isHistoryId] to 0 for remove history state.
     * This will show task from
     * [sqz.checklist.data.database.repository.task.TaskRepository.getTaskList],
     * [sqz.checklist.data.database.repository.task.TaskRepository.getPinnedTaskList],
     * [sqz.checklist.data.database.repository.task.TaskRepository.getRemindedTaskList]... lists.
     *
     * @param taskId The id of the task to update.
     * @see sqz.checklist.data.database.repository.task.TaskRepository.removeTaskFromDefaultList
     */
    suspend fun restoreTaskFromHistoryList(taskId: Long)

    /**
     * Restore all task from history list.
     *
     * @see restoreTaskFromHistoryList
     */
    suspend fun restoreAllTaskFromHistory()

    /**
     * Delete old history task.
     *
     * This method will list all isHistory tasks, then delete the history tasks by
     * smallest [Task.isHistoryId] until the size of history tasks is less than
     * [numOfAllowedHistory]` + 1`.
     *
     * @param numOfAllowedHistory The number of left history task that won't delete. `0` means
     *   all history task will be deleted.
     */
    suspend fun deleteOldHistoryTask(numOfAllowedHistory: Int)

    /**
     * Delete [Task] and [TaskDetail] that contain the task by [taskId].
     *
     * @param taskId The id of the task to delete.
     */
    suspend fun deleteFullTask(taskId: Long)

    companion object {
        fun provider(database: DatabaseProvider): TaskHistoryRepository {
            return TaskHistoryRepositoryImpl(
                db = database, storageManager = StorageManager.provider(),
            )
        }
    }
}
