package sqz.checklist.data.database.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import okio.FileNotFoundException
import sqz.checklist.data.database.ReminderModeType
import sqz.checklist.data.database.Task
import sqz.checklist.data.database.TaskDatabase
import sqz.checklist.data.database.TaskDetail
import sqz.checklist.data.database.TaskDetailType
import sqz.checklist.data.database.TaskReminder
import sqz.checklist.data.database.model.ReminderViewData
import sqz.checklist.data.storage.AppDirType
import sqz.checklist.data.storage.appInternalDirPath
import sqz.checklist.data.storage.manager.StorageManager
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DatabaseRepository(
    private val databaseInstance: TaskDatabase?
) : Exception() {
    @OptIn(ExperimentalTime::class)
    private val curTime: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

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
     * A private helper function to execute different logic based on whether a [TaskDetail] contains media.
     *
     * @param withoutMedia A suspend lambda to execute for non-media types.
     * @param withMedia A suspend lambda to execute for media types.
     * @param taskDetail The [TaskDetail] to evaluate.
     * @return The result of the executed lambda.
     */
    private suspend fun taskDetailType(
        withoutMedia: suspend () -> Unit, withMedia: suspend () -> Unit, taskDetail: TaskDetail
    ) = when (taskDetail.type) {
        TaskDetailType.Text -> withoutMedia()
        TaskDetailType.URL -> withoutMedia()
        TaskDetailType.Application -> withoutMedia()
        TaskDetailType.Audio -> withMedia()
        TaskDetailType.Picture -> withMedia()
        TaskDetailType.Video -> withMedia()
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
     * Deletes a task by its ID. It also deletes all associated media files and can optionally
     * re-arrange the history IDs.
     *
     * @param taskId The ID of the task to delete.
     * @param arrangeHistoryId If true, the history IDs will be re-sequenced after deletion.
     */
    suspend fun deleteTask(taskId: Long, arrangeHistoryId: Boolean = false) {
        val taskDao = this.databaseInstance!!.taskDaoOld()
        this.deleteAllMediaByTaskId(taskId)
        taskDao.delete(Task(id = taskId, description = "", createDate = curTime))
        if (arrangeHistoryId) this.arrangeHistoryId()
    }

    /**
     * Deletes all tasks marked as history, including their associated media files.
     */
    suspend fun deleteAllHistory() {
        val taskDao = this.databaseInstance!!.taskDaoOld()
        for (data in taskDao.getAllOrderByIsHistoryId()) {
            this.deleteAllMediaByTaskId(data.id)
        }
        taskDao.deleteAllHistory()
    }

    /**
     * A private helper to delete all media files (audio, picture, video) associated with a given task ID.
     *
     * @param taskId The ID of the task whose media should be deleted.
     */
    private suspend fun deleteAllMediaByTaskId(taskId: Long) {
        val taskDao = this.databaseInstance!!.taskDaoOld()
        taskDao.getTaskDetail(taskId).let {
            if (it.isEmpty()) return
            for (getTaskDetail in it) {
                try {
                    this@DatabaseRepository.taskDetailType(
                        withoutMedia = {}, withMedia = {
                            val storageManager = StorageManager.provider()
                            val toStr = getTaskDetail.dataByte.decodeToString().let { let ->
                                if (let.startsWith("file:///") || let.startsWith("content://")) {
                                    return@let let.replaceBefore("media", "")
                                }
                                return@let let
                            }
                            val path: String = toStr.let { let ->
                                "${appInternalDirPath(AppDirType.Data)}/${let}"
                            }
                            val delMode = StorageManager.DeleteMode.FilePath(path)
                            try {
                                storageManager.deleteStorageFile(delMode)
                            } catch (_: FileNotFoundException) {
                            }
                        },
                        taskDetail = getTaskDetail
                    )
                } catch (_: Exception) {
                }
            }
        }
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

    /**
     * Sets the 'isReminded' status for a specific reminder.
     *
     * @param reminderId The ID of the reminder to update.
     * @param state The new boolean status (true for reminded, false for not reminded).
     */
    suspend fun setIsReminded(reminderId: Int, state: Boolean) {
        if (state) this.databaseInstance!!.taskReminderDao().setIsReminded(reminderId, 1)
        else this.databaseInstance!!.taskReminderDao().setIsReminded(reminderId, 0)
    }

    /**
     * Moves a task from the history list back to the main list by setting its history ID to 0
     * and then re-sequencing the remaining history items.
     *
     * @param id The ID of the task to move out of history.
     */
    suspend fun isHistoryIdToMain(id: Long) {
        this.databaseInstance!!.taskDaoOld().setHistoryId(0, id)
        this.arrangeHistoryId()
    }

    /**
     * Private helper to re-sequence the `isHistoryId` for all items currently in history.
     * This ensures the IDs are contiguous (1, 2, 3, ...).
     */
    private suspend fun arrangeHistoryId() {
        val allIsHistoryIdList = this.databaseInstance!!.taskDaoOld().getAllIsHistoryId()
        val arrangeIdList = allIsHistoryIdList.mapIndexed { index, data ->
            data.copy(isHistoryId = index + 1)
        }
        for (data in arrangeIdList) {
            this.databaseInstance.taskDaoOld().setIsHistoryId(data.isHistoryId, data.id)
        }
    }
}