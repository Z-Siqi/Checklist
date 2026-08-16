package sqz.checklist.reminder.api.assign

import sqz.checklist.data.database.model.ReminderViewData

/**
 * Coordinates reminder dialog actions across persistence and platform scheduling.
 */
interface ReminderAssign {

    suspend fun getCurrentReminder(taskId: Long): ReminderViewData?

    /**
     * Creates or replaces a reminder for the given task.
     */
    suspend fun setReminder(taskId: Long, remindAtMillis: Long)

    /**
     * Cancels the existing reminder for the given task.
     *
     * @return `true` when a reminder existed and was removed.
     */
    suspend fun cancelReminder(taskId: Long): Boolean

    /**
     * Check all unsent reminder whether existed in the system.
     *
     * @return `true` if found unsent reminder but not scheduled in the system.
     */
    suspend fun checkUnsentReminderError(): Boolean

    /**
     * Describes which reminder action the UI should present.
     */
    enum class Action {

        /**
         * When no permission to push a notification
         * (expected only happen in set a new situation, otherwise it should be `Error` action).
         */
        NoPermission,

        /**
         * Set a new reminder.
         */
        Set,

        /**
         * To cancel current scheduled reminder.
         */
        Cancel,

        /**
         * Expected when the reminder lost such as accidentally the delayed notification gone.
         */
        Error,
    }
}
