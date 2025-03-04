package com.sqz.checklist.database

import com.sqz.checklist.ui.main.task.layout.function.toUri
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.time.LocalDate

class DatabaseRepository(
    private val databaseInstance: TaskDatabase?
) : Exception() {
    suspend fun insertTaskData(
        description: String, doingState: String? = null, isPin: Boolean = false,
        detailType: TaskDetailType? = null,
        detailDataString: String? = null, dataByte: ByteArray? = null
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
                dataString = detailDataString,
                dataByte = dataByte
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
        detailType: TaskDetailType?, detailDataString: String?, dataByte: ByteArray? = null
    ) {
        val taskDao = this.databaseInstance!!.taskDao()
        suspend fun deleteTaskDetail() {
            if (taskDao.matchTaskDetail(taskId) >= 1) {
                this.deleteDetail(taskId, taskDao, true)
            }
        }
        if (detailType != null && detailDataString != null) {
            deleteTaskDetail()
            taskDao.insertAll(TaskDetail(taskId, detailType, detailDataString, dataByte))
        } else deleteTaskDetail()
        taskDao.editTask(taskId, edit, detailType != null && detailDataString != null)
    }

    suspend fun deleteReminderData(taskId: Long) {
        val reminderId = this.databaseInstance!!.taskDao().getAll(taskId).reminder
        val reminderDao = this.databaseInstance.taskReminderDao()

        for (data in this.databaseInstance.taskReminderDao().getAll()) { // remove error item
            if (this.databaseInstance.taskDao().matchReminder(data.id) < 1) {
                val getter = reminderDao.getAll(data.id)
                if (getter != null) reminderDao.delete(getter)
            }
        }
        if (reminderId == 0 || reminderId == null) throw NoSuchFieldException("No id data!")
        val getter = reminderDao.getAll(reminderId)
        if (getter != null) reminderDao.delete(getter)
        this.databaseInstance.taskDao().deleteReminder(taskId)
    }

    suspend fun deleteByHistoryId(maxRetainIdNum: Int) {
        val taskDao = this.databaseInstance!!.taskDao()
        val value = taskDao.getIsHistorySum()
        if (value > maxRetainIdNum) for (i in 1..(value - maxRetainIdNum)) {
            val id = taskDao.getIsHistoryBottomKeyId()
            this.deleteDetail(id, taskDao)
            for (data in taskDao.getTaskDetail()) { // remove error item
                if (taskDao.matchTask(data.id) < 1) {
                    this.deleteDetail(data.id, taskDao, true)
                }
            }
            taskDao.delete(Task(id = id, description = "", createDate = LocalDate.MIN))
            arrangeHistoryId()
        }
    }

    suspend fun deleteTask(taskId: Long) {
        val taskDao = this.databaseInstance!!.taskDao()
        this.deleteDetail(taskId, taskDao)
        taskDao.delete(Task(id = taskId, description = "", createDate = LocalDate.MIN))
    }

    suspend fun deleteAllHistory() {
        val taskDao = this.databaseInstance!!.taskDao()
        for (data in taskDao.getAllOrderByIsHistoryId()) {
            this.deleteDetail(data.id, taskDao)
        }
        taskDao.deleteAllHistory()
    }

    private suspend fun deleteDetail(taskId: Long, taskDao: TaskDao, ignoreCheck: Boolean = false) {
        if (taskDao.getAll(taskId).detail || ignoreCheck) {
            try {
                val data = taskDao.getTaskDetail(taskId).dataByte?.toUri()
                val file = File(data?.path!!)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
            taskDao.delete(
                TaskDetail(taskId, TaskDetailType.Text, "")
            )
        }
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

    suspend fun getReminderData(reminderId: Int): TaskReminder? {
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
