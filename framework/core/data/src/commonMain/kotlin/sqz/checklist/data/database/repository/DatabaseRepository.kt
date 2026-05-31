package sqz.checklist.data.database.repository

import kotlinx.coroutines.flow.Flow
import sqz.checklist.data.database.ReminderModeType
import sqz.checklist.data.database.TaskDatabase
import sqz.checklist.data.database.TaskReminder
import sqz.checklist.data.database.model.ReminderViewData

class DatabaseRepository(
    private val databaseInstance: TaskDatabase?
) {

    /**
     * Inserts a new task reminder into the database.
     *
     * @param taskReminder The [sqz.checklist.data.database.TaskReminder] object to insert.
     */
    suspend fun insertReminderData(taskReminder: TaskReminder) {
        this.databaseInstance!!.taskReminderDao().insertAll(taskReminder)
    }

    /**
     * Retrieves a list of records from a specified table based on a task ID.
     *
     * @param table The [Table] enum specifying which table to query.
     * @param taskId The ID of the task to filter by.
     * @return A list of results, cast to `List<*>`. The actual type depends on the table queried.
     */
    suspend fun getTable(table: Table, taskId: Long): List<*> {
        val db: TaskDatabase = this.databaseInstance!!
        return when (table) {
            Table.Task -> listOf(db.taskDaoOld().getTask(id = taskId))
            Table.TaskDetail -> db.taskDaoOld().getTaskDetail(taskId = taskId)
            Table.TaskReminder -> listOf(db.taskReminderDao().getByTaskId(taskId = taskId)!!)
        }
    }

    /**
     * Deletes the reminder data associated with a specific task ID.
     *
     * @param taskId The ID of the task whose reminder should be deleted.
     * @throws kotlin.NullPointerException if no reminder is found for the given task ID.
     */
    suspend fun deleteReminderData(taskId: Long) {
        val db: TaskDatabase = this.databaseInstance!!
        val reminderDao = db.taskReminderDao()
        val reminder =
            reminderDao.getByTaskId(taskId = taskId) ?: throw NullPointerException("No id data!")
        reminderDao.delete(reminder)
    }

    /**
     * Retrieves the reminder entity for a given task ID.
     *
     * @param taskId The ID of the task.
     * @return A [TaskReminder] object, or null if not found.
     */
    suspend fun getReminderData(taskId: Long): TaskReminder? {
        val db: TaskDatabase = this.databaseInstance!!
        return db.taskReminderDao().getByTaskId(taskId = taskId)
    }

    /**
     * Retrieves a view model representation of a reminder by its own ID.
     *
     * @param reminderId The ID of the reminder.
     * @return A [ReminderViewData] object, or null if not found.
     */
    suspend fun getReminderData(reminderId: Int): ReminderViewData? {
        return this.databaseInstance!!.taskReminderDao().getAll(reminderId)
    }

    /**
     * Retrieves all reminders as a list of view model representations.
     *
     * @return A list of all [ReminderViewData] objects in the database.
     */
    suspend fun getReminderData(): List<ReminderViewData> {
        return this.databaseInstance!!.taskReminderDao().getAll()
    }

    /**
     * Gets a Flow that emits the count of reminders based on their 'isReminded' status.
     *
     * @param isReminded If true, counts reminders that have been triggered. If false, counts those that have not.
     * @return A `Flow<Int>` emitting the count, or null on error.
     */
    fun getIsRemindedNum(isReminded: Boolean): Flow<Int>? {
        return try {
            if (isReminded) this.databaseInstance!!.taskReminderDao().getIsRemindedNum(1)
            else this.databaseInstance!!.taskReminderDao().getIsRemindedNum(0)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Gets the count of reminders of a specific mode that have not yet been triggered.
     *
     * @param modeType The [sqz.checklist.data.database.ReminderModeType] to count.
     * @return The integer count of matching reminders.
     */
    suspend fun getModeNumWithNoReminded(modeType: ReminderModeType): Int {
        return this.databaseInstance!!.taskReminderDao().getModeNum(modeType, 1)
    }
}