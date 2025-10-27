package com.sqz.checklist.database

import com.sqz.checklist.MainActivity
import com.sqz.checklist.ui.common.media.toUri
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.time.LocalDate

class DatabaseRepository(
    private val databaseInstance: TaskDatabase?
) : Exception() {
    /**
     * Create and insert task to database.
     * @return the id that inserted
     * @throws NullPointerException if any value not allow to be null
     */
    suspend fun insertTask(task: TaskData, detail: List<TaskDetailData>?): Long {
        val db: TaskDatabase = this.databaseInstance!!
        val isDetailExist: Boolean = detail != null
        val task = Task(
            description = task.description!!,
            createDate = LocalDate.now(),
            doingState = task.doingState,
            isPin = task.isPin!!,
        )
        val insertTaskAndGetId = db.taskDao().insertAll(task)

        if (!isDetailExist) { // only insert task
            return insertTaskAndGetId
        }
        this.insertTaskDetail(  // it got details
            taskId = insertTaskAndGetId,
            taskDetail = detail
        )
        return insertTaskAndGetId
    }

    /**
     * Inserts a list of task details into the database for a specific task.
     * It iterates through the list and calls the single item insertion method.
     *
     * @param taskId The ID of the parent task.
     * @param taskDetail The list of [TaskDetailData] to insert.
     */
    suspend fun insertTaskDetail(taskId: Long, taskDetail: List<TaskDetailData>) {
        for (get in taskDetail) {
            this.insertTaskDetail(taskId = taskId, taskDetail = get)
        }
    }

    /**
     * Inserts a single task detail item into the database.
     *
     * @param taskId The ID of the parent task.
     * @param taskDetail The [TaskDetailData] object to insert.
     */
    suspend fun insertTaskDetail(taskId: Long, taskDetail: TaskDetailData) {
        val db: TaskDatabase = this.databaseInstance!!
        val toByteArray = taskDetail.dataByte as ByteArray
        val taskDetail = TaskDetail(
            taskId = taskId,
            type = taskDetail.type,
            description = taskDetail.description,
            dataString = taskDetail.dataString,
            dataByte = toByteArray
        )
        db.taskDao().insertAll(taskDetail)
    }

    /**
     * Inserts a new task reminder into the database.
     *
     * @param taskReminder The [TaskReminder] object to insert.
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
            Table.Task -> listOf(db.taskDao().getTask(id = taskId))
            Table.TaskDetail -> db.taskDao().getTaskDetail(taskId = taskId)!!
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
     * Deletes a specific task detail. If the detail is a media file,
     * it also deletes the corresponding file from storage.
     *
     * @param detail The [TaskDetail] object to delete.
     */
    suspend fun deleteTaskDetail(detail: TaskDetail) {
        val db: TaskDatabase = this.databaseInstance!!
        this.taskDetailType(
            withoutMedia = { db.taskDao().delete(detail) },
            withMedia = {
                val uri = detail.dataByte.toUri(MainActivity.appDir)
                val file = File(uri.path!!)
                file.delete()
                db.taskDao().delete(detail)
            },
            taskDetail = detail
        )
    }

    /**
     * Updates a task detail. If the detail is a media file, this involves deleting
     * the old file from storage before updating the database record.
     *
     * @param original The original [TaskDetail] object before the update (used to find the media file).
     * @param update The new [TaskDetail] object with the updated data.
     */
    suspend fun updateTaskDetail(original: TaskDetail, update: TaskDetail) {
        val db: TaskDatabase = this.databaseInstance!!
        this.taskDetailType(
            withoutMedia = { db.taskDao().update(update) },
            withMedia = {
                val uri = original.dataByte.toUri(MainActivity.appDir)
                val file = File(uri.path!!)
                file.delete()
                db.taskDao().update(update)
            },
            taskDetail = original
        )
    }

    /**
     * Updates a task in the database, but only if the updated version is different
     * from the original.
     *
     * @param originalTask The original [Task] object before changes.
     * @param updateTask The [Task] object with the updated data.
     */
    suspend fun updateTask(originalTask: Task, updateTask: Task) {
        val db: TaskDatabase = this.databaseInstance!!
        if (originalTask != updateTask) {
            db.taskDao().update(updateTask)
        }
    }

    /**
     * Toggles the 'pin' status of a task.
     *
     * @param id The ID of the task to update.
     * @param setter The new boolean state for the pin (true for pinned, false for unpinned).
     */
    suspend fun taskPin(id: Long, setter: Boolean) {
        fun Boolean.toInt(): Int = if (this) 1 else 0
        this.databaseInstance!!.taskDao().editTaskPin(id, setter.toInt())
    }

    /**
     * Deletes the reminder data associated with a specific task ID.
     *
     * @param taskId The ID of the task whose reminder should be deleted.
     * @throws NoSuchFieldException if no reminder is found for the given task ID.
     */
    suspend fun deleteReminderData(taskId: Long) {
        val db: TaskDatabase = this.databaseInstance!!
        val reminderDao = db.taskReminderDao()
        val reminder = reminderDao.getByTaskId(taskId = taskId)
        if (reminder == null) {
            throw NoSuchFieldException("No id data!")
        }
        reminderDao.delete(reminder)
    }

    /**
     * Deletes the oldest history tasks if the total number of history items exceeds a given limit.
     * This includes deleting any associated media files.
     *
     * @param maxRetainIdNum The maximum number of history items to retain.
     */
    suspend fun deleteByHistoryId(maxRetainIdNum: Int) {
        val taskDao = this.databaseInstance!!.taskDao()
        val value = taskDao.getIsHistorySum()
        if (value > maxRetainIdNum) (1..(value - maxRetainIdNum)).forEach { _ ->
            val toDeleteTask = taskDao.getIsHistoryBottomIdTask()
            this.deleteAllMediaByTaskId(toDeleteTask.id)
            taskDao.delete(toDeleteTask)
            arrangeHistoryId()
        }
    }

    /**
     * Deletes a task by its ID. It also deletes all associated media files and can optionally
     * re-arrange the history IDs.
     *
     * @param taskId The ID of the task to delete.
     * @param arrangeHistoryId If true, the history IDs will be re-sequenced after deletion.
     */
    suspend fun deleteTask(taskId: Long, arrangeHistoryId: Boolean = false) {
        val taskDao = this.databaseInstance!!.taskDao()
        this.deleteAllMediaByTaskId(taskId)
        taskDao.delete(Task(id = taskId, description = "", createDate = LocalDate.MIN))
        if (arrangeHistoryId) this.arrangeHistoryId()
    }

    /**
     * Deletes all tasks marked as history, including their associated media files.
     */
    suspend fun deleteAllHistory() {
        val taskDao = this.databaseInstance!!.taskDao()
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
        val taskDao = this.databaseInstance!!.taskDao()
        taskDao.getTaskDetail(taskId)?.let {
            for (getTaskDetail in it) {
                try {
                    this@DatabaseRepository.taskDetailType(
                        withoutMedia = {}, withMedia = {
                            val data = getTaskDetail.dataByte.toUri(MainActivity.appDir)
                            val file = File(data.path!!)
                            if (file.exists()) file.delete()
                        },
                        taskDetail = getTaskDetail
                    )
                } catch (_: Exception) {
                }
            }
        }
    }

    /**
     * Retrieves all task detail entities for a given task ID.
     *
     * @param taskId The ID of the parent task.
     * @return A list of [TaskDetail] objects, or null if none are found.
     */
    suspend fun getDetailData(taskId: Long): List<TaskDetail>? {
        val taskDao = this.databaseInstance!!.taskDao()
        return taskDao.getTaskDetail(taskId = taskId)
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
     * @param modeType The [ReminderModeType] to count.
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
     * Moves a task to the history list by assigning it the next available history ID.
     *
     * @param id The ID of the task to move to history.
     */
    suspend fun isHistoryIdToHistory(id: Long) {
        val dbInstance = this.databaseInstance!!.taskDao()
        val maxId = dbInstance.getIsHistoryIdTop()
        dbInstance.setHistoryId((maxId + 1), id)
    }

    /**
     * Moves a task from the history list back to the main list by setting its history ID to 0
     * and then re-sequencing the remaining history items.
     *
     * @param id The ID of the task to move out of history.
     */
    suspend fun isHistoryIdToMain(id: Long) {
        this.databaseInstance!!.taskDao().setHistoryId(0, id)
        this.arrangeHistoryId()
    }

    /**
     * Private helper to re-sequence the `isHistoryId` for all items currently in history.
     * This ensures the IDs are contiguous (1, 2, 3, ...).
     */
    private suspend fun arrangeHistoryId() {
        val allIsHistoryIdList = this.databaseInstance!!.taskDao().getAllIsHistoryId()
        val arrangeIdList = allIsHistoryIdList.mapIndexed { index, data ->
            data.copy(isHistoryId = index + 1)
        }
        for (data in arrangeIdList) {
            this.databaseInstance.taskDao().setIsHistoryId(data.isHistoryId, data.id)
        }
    }
}
