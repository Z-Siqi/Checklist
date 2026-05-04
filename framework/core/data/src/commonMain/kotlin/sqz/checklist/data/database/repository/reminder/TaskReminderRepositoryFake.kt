package sqz.checklist.data.database.repository.reminder

import sqz.checklist.data.database.TaskReminder
import sqz.checklist.data.database.model.ReminderViewData
import sqz.checklist.data.database.model.TaskViewData

class TaskReminderRepositoryFake : TaskReminderRepository {
    override suspend fun getRemindedTaskList(): List<TaskViewData> {
        TODO("Not yet implemented")
    }

    override suspend fun getReminderViewList(): List<ReminderViewData> {
        TODO("Not yet implemented")
    }

    override suspend fun getReminderView(notifyId: Int): ReminderViewData? {
        TODO("Not yet implemented")
    }

    override suspend fun getReminder(taskId: Long): TaskReminder? {
        TODO("Not yet implemented")
    }

    override suspend fun updateRemindedState(notifyId: Int, isReminded: Boolean) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteRemindedInfo(taskId: Long) {
        TODO("Not yet implemented")
    }
}
