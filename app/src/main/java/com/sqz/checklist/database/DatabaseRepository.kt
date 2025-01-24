package com.sqz.checklist.database

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class DatabaseRepository(
    private val databaseInstance: TaskDatabase?
) : Exception() {
    suspend fun insertTaskData(
        description: String, doingState: String? = null, isPin: Boolean = false,
        detailType: TaskDetailType? = null, detailDataString: String? = null,
    ): Long {
        val task = Task(
            description = description,
            createDate = LocalDate.now(),
            doingState = doingState,
            isPin = isPin,
            detail = detailType != null && detailDataString != null,
        )
        return if (detailType == null || detailDataString == null) {
            this.databaseInstance!!.taskDao().insertAll(task)
        } else {
            val insertTask = this.databaseInstance!!.taskDao().insertAll(task)
            val taskDetail = TaskDetail(
                id = insertTask,
                type = detailType,
                dataString = detailDataString
            )
            this.databaseInstance.taskDao().insertAll(taskDetail)
            insertTask
        }
    }

    suspend fun insertReminderData(taskId: Long, taskReminder: TaskReminder) {
        this.databaseInstance!!.taskReminderDao().insertAll(taskReminder)
        this.databaseInstance.taskDao().insertReminder(taskId, taskReminder.id)
    }

    suspend fun editTask(
        taskId: Long, edit: String,
        detailType: TaskDetailType?, detailDataString: String?
    ) {
        val taskDao = this.databaseInstance!!.taskDao()
        suspend fun deleteTaskDetail() {
            if (taskDao.matchTaskDetail(taskId) >= 1) taskDao.delete(
                TaskDetail(taskId, TaskDetailType.Text, "")
            )
        }
        if (detailType != null && detailDataString != null) {
            deleteTaskDetail()
            taskDao.insertAll(TaskDetail(taskId, detailType, detailDataString))
        } else deleteTaskDetail()
        taskDao.editTask(taskId, edit, detailType != null && detailDataString != null)
    }

    suspend fun deleteReminderData(taskId: Long) {
        val reminderId = this.databaseInstance!!.taskDao().getAll(taskId).reminder
        val reminderDao = this.databaseInstance.taskReminderDao()

        for (data in this.databaseInstance.taskReminderDao().getAll()) { // remove error item
            if (this.databaseInstance.taskDao().matchReminder(data.id) < 1) {
                reminderDao.delete(reminderDao.getAll(data.id))
            }
        }
        if (reminderId == 0 || reminderId == null) throw NoSuchFieldException("No id data!")
        reminderDao.delete(reminderDao.getAll(reminderId))
        this.databaseInstance.taskDao().deleteReminder(taskId)
    }

    suspend fun deleteByHistoryId(maxRetainIdNum: Int) {
        val taskDao = this.databaseInstance!!.taskDao()
        val value = taskDao.getIsHistorySum()
        if (value > maxRetainIdNum) for (i in 1..(value - maxRetainIdNum)) {
            val id = taskDao.getIsHistoryBottomKeyId()
            if (taskDao.getAll(id).detail) taskDao.delete(
                TaskDetail(id, TaskDetailType.Text, "")
            )
            for (data in taskDao.getTaskDetail()) { // remove error item
                if (taskDao.matchTask(data.id) < 1) {
                    taskDao.delete(TaskDetail(data.id, TaskDetailType.Text, ""))
                }
            }
            taskDao.delete(Task(id = id, description = "", createDate = LocalDate.MIN))
            arrangeHistoryId()
        }
    }

    suspend fun deleteTask(taskId: Long) {
        val taskDao = this.databaseInstance!!.taskDao()

        if (taskDao.getAll(taskId).detail) taskDao.delete(
            TaskDetail(taskId, TaskDetailType.Text, "")
        )
        taskDao.delete(Task(id = taskId, description = "", createDate = LocalDate.MIN))
    }

    suspend fun deleteAllHistory() {
        val taskDao = this.databaseInstance!!.taskDao()
        for (data in taskDao.getAllOrderByIsHistoryId()) {
            if (data.detail) {
                taskDao.delete(TaskDetail(data.id, TaskDetailType.Text, ""))
            }
        }
        taskDao.deleteAllHistory()
    }

    suspend fun getDetailData(detailId: Long): TaskDetail? {
        val taskDao = this.databaseInstance!!.taskDao()
        return if (taskDao.matchTaskDetail(detailId) >= 1) taskDao.getTaskDetail(detailId) else null
    }

    suspend fun getReminderData(taskId: Long): TaskReminder? {
        val reminderId = this.databaseInstance!!.taskDao().getAll(taskId).reminder
        return if (reminderId == 0 || reminderId == null) null else {
            this.databaseInstance.taskReminderDao().getAll(reminderId)
        }
    }

    suspend fun getReminderData(reminderId: Int): TaskReminder {
        return this.databaseInstance!!.taskReminderDao().getAll(reminderId)
    }

    suspend fun getReminderData(): List<TaskReminder> {
        return this.databaseInstance!!.taskReminderDao().getAll()
    }

    fun getIsRemindedNum(isReminded: Boolean): Flow<Int> {
        return if (isReminded) this.databaseInstance!!.taskReminderDao().getIsRemindedNum(1)
        else this.databaseInstance!!.taskReminderDao().getIsRemindedNum(0)
    }

    suspend fun getModeNumWithNoReminded(modeType: ReminderModeType): Int {
        return this.databaseInstance!!.taskReminderDao().getModeNum(modeType, 1)
    }

    suspend fun setIsReminded(reminderId: Int, state: Boolean) {
        if (state) this.databaseInstance!!.taskReminderDao().setIsReminded(reminderId, 1)
        else this.databaseInstance!!.taskReminderDao().setIsReminded(reminderId, 0)
    }

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
