package com.sqz.checklist.database

import java.time.LocalDate

class DatabaseRepository(
    private val databaseInstance: TaskDatabase?
) : Exception() {
    suspend fun insertTaskData(
        description: String, doingState: String? = null, isPin: Boolean = false,
        detailType: TaskDetailType? = null, detailDataString: String? = null,
    ) {
        val task = Task(
            description = description,
            createDate = LocalDate.now(),
            doingState = doingState,
            isPin = isPin,
            detail = detailType != null && detailDataString != null,
        )
        if (detailType == null || detailDataString == null) {
            this.databaseInstance!!.taskDao().insertAll(task)
        } else {
            val taskDetail = TaskDetail(
                id = this.databaseInstance!!.taskDao().insertAll(task),
                type = detailType,
                dataString = detailDataString
            )
            this.databaseInstance.taskDao().insertAll(taskDetail)
        }
    }

    suspend fun insertReminderData(taskId: Long, taskReminder: TaskReminder) {
        this.databaseInstance!!.taskReminderDao().insertAll(taskReminder)
        this.databaseInstance.taskDao().insertReminder(taskId, taskReminder.id)
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
        val value = this.databaseInstance!!.taskDao().getIsHistorySum()
        if (value > maxRetainIdNum) for (i in 1..(value - maxRetainIdNum)) {
            val id = this.databaseInstance.taskDao().getIsHistoryBottomKeyId()
            this.databaseInstance.taskDao().delete(
                Task(id = id, description = "", createDate = LocalDate.MIN)
            )
            arrangeHistoryId()
        }
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
