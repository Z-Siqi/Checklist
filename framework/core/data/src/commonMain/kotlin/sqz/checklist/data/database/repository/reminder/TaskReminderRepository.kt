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
     * Get [ReminderViewData] list for showing the notification. Exclude [Task.isHistoryId] not `0`.
     *
     * @param [noRemindedOnly] only [TaskReminder.isReminded]` = false` will in the list if
     *   [noRemindedOnly] is `true`; otherwise the list will include all.
     * @return [ReminderViewData] list
     *
     * @see [TaskReminder]
     * @see [Task.description]
     */
    suspend fun getReminderViewList(noRemindedOnly: Boolean = false): List<ReminderViewData>

    /**
     * Get [ReminderViewData] by notifyId which is the [TaskReminder.id] (primary key).
     *
     * @return [ReminderViewData] or `null` if [notifyId] not found.
     */
    suspend fun getReminderView(notifyId: Int): ReminderViewData?

    /**
     * Get [TaskReminder] by taskId
     *
     * @return [TaskReminder] or `null` if [taskId] not found.
     */
    suspend fun getReminder(taskId: Long): TaskReminder?

    /**
     * Update [TaskReminder.isReminded] state.
     *
     * @param notifyId [TaskReminder.id] (primary key)
     * @param isReminded [TaskReminder.isReminded]
     */
    suspend fun updateRemindedState(notifyId: Int, isReminded: Boolean)

    /**
     * Delete [TaskReminder] by task ID. Expected to remove the reminder data and the list for
     *   showing the reminded tasks should no longer visible the task.
     *
     * Note: This method will not affect the primary task which is from [Task], delayed notification
     *   is also will not be affect.
     *
     * @return the removed reminder.
     *
     * @throws IllegalArgumentException if [TaskReminder.isReminded] not `true`.
     * @throws NullPointerException if [TaskReminder] not found.
     */
    suspend fun deleteRemindedInfo(taskId: Long): TaskReminder?

    /**
     * Delete [TaskReminder] by [TaskReminder.id].
     *
     * @throws NullPointerException if [TaskReminder.id] not existed.
     */
    suspend fun deleteReminder(notifyId: Int)

    /**
     * Add a new [TaskReminder] to database, or replace when [TaskReminder.id] already existed.
     *
     * @param reminder the instance of [TaskReminder].
     */
    suspend fun insertReminder(reminder: TaskReminder)

    //TODO

    companion object {
        fun provider(database: DatabaseProvider): TaskReminderRepository {
            return TaskReminderRepositoryImpl(
                db = database, storageManager = StorageManager.provider(),
            )
        }
    }
}
