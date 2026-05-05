package sqz.checklist.data.database.repository.reminder

import sqz.checklist.data.database.DatabaseProvider
import sqz.checklist.data.database.TaskReminder
import sqz.checklist.data.database.model.ReminderViewData
import sqz.checklist.data.database.model.TaskViewData
import sqz.checklist.data.storage.manager.StorageManager

internal class TaskReminderRepositoryImpl(
    private val db: DatabaseProvider,
    private val storageManager: StorageManager
) : TaskReminderRepository {

    private fun reminderDao() = db.getDatabase().taskReminderDao()

    override suspend fun getRemindedTaskList(): List<TaskViewData> {
        return this.reminderDao().getRemindedTaskList()
    }

    override suspend fun getReminderViewList(): List<ReminderViewData> {
        return try {
            this.reminderDao().getReminderViewList()
        } catch (_: IllegalArgumentException) {
            listOf()
        }
    }

    override suspend fun getReminderView(notifyId: Int): ReminderViewData? {
        return this.reminderDao().getReminderView(notifyId)
    }

    override suspend fun getReminder(taskId: Long): TaskReminder? {
        return this.reminderDao().getReminderByTaskId(taskId)
    }

    override suspend fun updateRemindedState(notifyId: Int, isReminded: Boolean) {
        val booleanToInt = if (isReminded) 1 else 0
        this.reminderDao().updateIsReminded(notifyId, booleanToInt)
    }

    override suspend fun deleteRemindedInfo(taskId: Long) {
        this.reminderDao().deleteReminder(taskId).also {
            if (it == 0) throw NullPointerException("Reminder not found!")
        }
    }
}
