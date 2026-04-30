package sqz.checklist.data.database.repository.reminder

import sqz.checklist.data.database.DatabaseProvider
import sqz.checklist.data.database.Task
import sqz.checklist.data.database.TaskDetail
import sqz.checklist.data.database.TaskReminder
import sqz.checklist.data.database.model.ReminderViewData
import sqz.checklist.data.database.model.TaskViewData
import sqz.checklist.data.storage.manager.StorageManager

interface TaskReminderRepository {

    /**
     * Get [TaskViewData] list in reminded. Not include history task.
     *
     * @return [TaskViewData] list
     *
     * @see [Task]
     * @see [TaskDetail]
     * @see [sqz.checklist.data.database.TaskReminder]
     */
    suspend fun getRemindedTaskList(): List<TaskViewData>

    /**
     * Get [ReminderViewData] list for showing the notification.
     *
     * @return [ReminderViewData] list
     *
     * @see [TaskReminder]
     * @see [Task.description]
     */
    suspend fun getReminderViewList(): List<ReminderViewData>

    /**
     * Get [TaskReminder] by taskId
     *
     * @return [TaskReminder] or `null` if [taskId] not found.
     */
    suspend fun getReminder(taskId: Long): TaskReminder?

    /**
     * Delete [TaskReminder] by task ID. Expected to remove the reminder data and the list for
     *   showing the reminded tasks should no longer visible the task.
     *
     * Note: This method will not affect the primary task which is from [Task].
     *
     * @throws IllegalArgumentException if [TaskReminder.isReminded] not `true`.
     * @throws NullPointerException if [TaskReminder] not found.
     */
    suspend fun deleteRemindedInfo(taskId: Long)

    //TODO

    companion object {
        fun provider(database: DatabaseProvider): TaskReminderRepository {
            return TaskReminderRepositoryImpl(
                db = database, storageManager = StorageManager.provider(),
            )
        }
    }
}