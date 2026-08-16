package sqz.checklist.reminder.impl.assign

import sqz.checklist.common.NotificationScheduler
import sqz.checklist.data.database.TaskReminder
import sqz.checklist.data.database.model.ReminderViewData
import sqz.checklist.data.database.repository.reminder.TaskReminderRepository
import sqz.checklist.reminder.api.assign.ReminderAssign
import kotlin.random.Random

internal class ReminderAssignImpl(
    private val reminderRepository: TaskReminderRepository,
    private val scheduler: NotificationScheduler,
) : ReminderAssign {

    override suspend fun getCurrentReminder(taskId: Long): ReminderViewData? {
        val notifyId = reminderRepository.getReminder(taskId)?.id ?: return null
        return reminderRepository.getReminderView(notifyId)
    }

    override suspend fun setReminder(taskId: Long, remindAtMillis: Long) {
        reminderRepository.getReminder(taskId)?.let { existing ->
            scheduler.cancel(existing.id)
            reminderRepository.deleteRemindedInfo(existing.taskId)
        }

        val notificationId = createNotificationId()
        reminderRepository.insertReminder(
            TaskReminder(
                id = notificationId,
                taskId = taskId,
                reminderTime = remindAtMillis,
                extraData = null,
            )
        )
        scheduler.schedule(notificationId, remindAtMillis)
    }

    override suspend fun cancelReminder(taskId: Long): Boolean {
        val reminder = reminderRepository.getReminder(taskId) ?: return false
        scheduler.cancel(reminder.id)
        reminderRepository.deleteReminder(reminder.id)
        return true
    }

    override suspend fun checkUnsentReminderError(): Boolean {
        val list = reminderRepository.getReminderViewList(noRemindedOnly = true).also {
            if (it.isEmpty()) return false
        }
        list.forEach { item ->
            if (!scheduler.isExisted(item.reminder.id)) {
                return true
            }
        }
        return false
    }

    private suspend fun createNotificationId(): Int {
        val usedIds = reminderRepository.getReminderViewList()
            .mapTo(mutableSetOf()) { it.reminder.id }
        while (true) {
            val candidate = Random.nextInt(Int.MIN_VALUE, Int.MAX_VALUE)
            if (candidate != 0 && candidate !in usedIds) {
                return candidate
            }
        }
    }
}
