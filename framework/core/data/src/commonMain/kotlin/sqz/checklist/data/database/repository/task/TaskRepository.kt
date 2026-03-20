package sqz.checklist.data.database.repository.task

import sqz.checklist.data.database.Task
import sqz.checklist.data.database.TaskDatabase
import sqz.checklist.data.database.TaskDetail
import sqz.checklist.data.storage.manager.StorageManager

interface TaskRepository {

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
        fun provider(database: TaskDatabase): TaskRepository {
            return TaskRepositoryImpl(
                taskDao = database.taskDao(), storageManager = StorageManager.provider(),
            )
        }
    }
}
