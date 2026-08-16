package sqz.checklist.reminder.api

import sqz.checklist.common.NotificationScheduler
import sqz.checklist.data.database.repository.reminder.TaskReminderRepository
import sqz.checklist.reminder.api.assign.ReminderAssign
import sqz.checklist.reminder.api.runtime.ReminderRuntime
import sqz.checklist.reminder.impl.assign.ReminderAssignImpl
import sqz.checklist.reminder.impl.runtime.ReminderRuntimeImpl

/**
 * Creates the default reminder dialog use case backed by the shared database.
 */
fun reminderAssignProvider(
    taskReminderRepository: TaskReminderRepository,
    scheduler: NotificationScheduler,
): ReminderAssign {
    return ReminderAssignImpl(
        reminderRepository = taskReminderRepository,
        scheduler = scheduler,
    )
}

/**
 * Creates the default reminder runtime use case backed by the shared database.
 */
fun reminderRuntimeProvider(
    taskReminderRepository: TaskReminderRepository,
    scheduler: NotificationScheduler,
): ReminderRuntime {
    return ReminderRuntimeImpl(
        taskReminderRepository = taskReminderRepository,
        scheduler = scheduler,
    )
}
