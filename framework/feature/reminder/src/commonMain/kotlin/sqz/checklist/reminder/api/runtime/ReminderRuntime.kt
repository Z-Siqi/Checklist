package sqz.checklist.reminder.api.runtime

import sqz.checklist.data.database.TaskReminder
import sqz.checklist.data.database.model.ReminderViewData

/**
 * Exposes reminder lifecycle data needed outside the set or cancel dialog flow.
 */
interface ReminderRuntime {
    /**
     * Loads reminder data for a task.
     */
    suspend fun getReminder(taskId: Long): ReminderViewData?

    /**
     * Deletes reminder data for a task and returns the removed data.
     */
    suspend fun removeReminder(taskId: Long, removeDisplayedNotification: Boolean): TaskReminder?

    /**
     * Checks whether any not-yet-triggered reminder still exists.
     */
    suspend fun hasPendingReminder(): Boolean
}
