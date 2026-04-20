package sqz.checklist.data.database.repository.task

import kotlinx.coroutines.flow.Flow
import sqz.checklist.data.database.DatabaseProvider
import sqz.checklist.data.database.Task
import sqz.checklist.data.database.TaskDetail
import sqz.checklist.data.database.model.TaskViewData
import sqz.checklist.data.storage.manager.StorageManager

interface TaskRepository {

    /**
     * Get [TaskViewData] list. Not include history task.
     *
     * @return [Flow] of [TaskViewData] list.
     * @see [Task]
     * @see [TaskDetail]
     * @see [sqz.checklist.data.database.TaskReminder]
     */
    fun getTaskList(): Flow<List<TaskViewData>>

    /**
     * Get [TaskViewData] list in pinned. Not include history task.
     *
     * @return [Flow] of [TaskViewData] list
     * @see [Task]
     * @see [TaskDetail]
     * @see [sqz.checklist.data.database.TaskReminder]
     */
    fun getPinnedTaskList(): Flow<List<TaskViewData>>

    /**
     * Get [TaskViewData] list in reminded. Not include history task.
     *
     * @return [Flow] of [TaskViewData] list
     * @see [Task]
     * @see [TaskDetail]
     * @see [sqz.checklist.data.database.TaskReminder]
     */
    fun getRemindedTaskList(): Flow<List<TaskViewData>>

    /**
     * Get [TaskViewData] list in search keywords. Not include history task.
     *
     * @param searchQuery The search keywords.
     * @see [Task]
     * @see [TaskDetail]
     * @see [sqz.checklist.data.database.TaskReminder]
     */
    fun getSearchedList(searchQuery: String): Flow<List<TaskViewData>>

    /**
     * Update [Task.isPin] by id.
     *
     * @param taskId The id of the task to update.
     * @param update The new value of [Task.isPin].
     */
    suspend fun onTaskPinChange(taskId: Long, update: Boolean)

    /**
     * Update [Task.isHistoryId] to 1 or max isHistoryId + 1 for a history state.
     * This will hide task from [getTaskList], [getPinnedTaskList], [getRemindedTaskList].. lists,
     * but not delete from database.
     *
     * @param taskId The id of the task to update.
     * @see sqz.checklist.data.database.repository.history.TaskHistoryRepository.restoreTaskFromHistoryList
     */
    suspend fun removeTaskFromDefaultList(taskId: Long)

    /**
     * Check if [Task] list is empty. This will not include history task.
     *
     * @return `true` if [Task] list is empty
     */
    fun isTaskListEmpty(): Flow<Boolean>

    /**
     * Get [Task] and [TaskDetail] list by id in a Pair format.
     *
     * Expect `List<TaskDetail>` can never be empty, only `null` when no task details.
     *
     * @param id The id of the task to entity.
     * @return [Task] and [TaskDetail] list; Pair.first is [Task], `Pair.second` is [TaskDetail] list.
     * @throws NullPointerException if no task match the id
     */
    suspend fun getFullTask(id: Long): Pair<Task, List<TaskDetail>?>

    /**
     * Modify [Task] and [TaskDetail] list.
     *
     * @param task Update or insert [Task] to database. [Task.id]` = 0` to insert a new task
     *   (default = `0`), otherwise update [Task] by [Task.id].
     * @param detail The [TaskDetail] list to modify. [TaskDetail.id]` = 0` to insert a new task
     *   detail (default = `0`), otherwise update [TaskDetail] by [TaskDetail.id];
     *   [TaskDetail.taskId] expect same with [Task.id] or `0` in insert task case. For delete
     *   non-existed [TaskDetail] case, [Task.id] which from [task] will use to search out-date
     *   [TaskDetail] and delete it.
     * @return The [Task.id] that modified.
     * @throws IllegalArgumentException if [Task.id] or [TaskDetail.taskId] not `0` or not existed
     *   in database; [TaskDetail.id] is illegal will also throw this exception.
     */
    suspend fun modifyTask(task: Task, detail: List<TaskDetail>?): Long

    companion object {
        fun provider(database: DatabaseProvider): TaskRepository {
            return TaskRepositoryImpl(
                db = database, storageManager = StorageManager.provider(),
            )
        }
    }
}
