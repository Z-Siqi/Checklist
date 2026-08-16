package sqz.checklist.reminder.impl.runtime

import sqz.checklist.common.NotificationScheduler
import sqz.checklist.data.database.TaskReminder
import sqz.checklist.data.database.model.ReminderViewData
import sqz.checklist.data.database.repository.reminder.TaskReminderRepository
import sqz.checklist.reminder.api.runtime.ReminderRuntime

internal class ReminderRuntimeImpl(
    private val taskReminderRepository: TaskReminderRepository,
    private val scheduler: NotificationScheduler,
) : ReminderRuntime {

    override suspend fun getReminder(taskId: Long): ReminderViewData? {
        val reminder = taskReminderRepository.getReminder(taskId) ?: return null
        return taskReminderRepository.getReminderView(notifyId = reminder.id)
    }

    override suspend fun removeReminder(taskId: Long): TaskReminder? {
        val reminder = taskReminderRepository.getReminder(taskId) ?: return null
        taskReminderRepository.deleteReminder(reminder.id)
        return reminder
    }

    override suspend fun hasPendingReminder(): Boolean {
        return taskReminderRepository.getReminderViewList(noRemindedOnly = true).any {
            !it.reminder.isReminded
        }
    }
}
