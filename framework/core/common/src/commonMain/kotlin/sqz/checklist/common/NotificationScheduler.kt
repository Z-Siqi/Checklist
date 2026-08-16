package sqz.checklist.common

/**
 * General notification scheduler interface.
 *
 * Expected impl this in platform level to process the notification, but ONLY read the db.
 */
interface NotificationScheduler {
    //TODO: impl silent notification

    /**
     * Schedule a local notification for the given app-level.
     *
     * Expected only read data from database.
     *
     * @param notificationId The ID can both link to app-level notification and database notification.
     * @param remindAtMillis When should send the notification.
     */
    suspend fun schedule(
        notificationId: Int,
        remindAtMillis: Long,
        silent: Boolean = false,
    )

    /**
     * Update old pending schedule to the latest state.
     *
     * Expected only read the database.
     *
     * @param notificationId The ID can both link to app-level notification and database notification.
     */
    suspend fun reschedule(notificationId: Int, silent: Boolean = false)

    /**
     * Cancel scheduled notification (include future & current notification).
     *
     * Expected only cancel the notification, will NOT edit / delete the data.
     *
     * @param notificationId The ID can both link to app-level notification and database notification.
     * @param tryRemoveDisplayed Optionally remove already displayed notification when supported.
     */
    suspend fun cancel(
        notificationId: Int,
        tryRemoveDisplayed: Boolean = true,
    )

    /**
     * Cancel all scheduled notification (include future & current notification).
     *
     * Expected only cancel the notification, will NOT edit / delete the data
     *
     * @param tryRemoveDisplayed Optionally remove already displayed notification when supported.
     */
    suspend fun cancelAll(tryRemoveDisplayed: Boolean = true)

    /**
     * Get the notification is existed or not.
     * (Should also check both scheduled and displaying notification)
     *
     * @param notificationId The ID can both link to app-level notification and database notification.
     * @return `ture` if existed or `false` not existed.
     */
    suspend fun isExisted(notificationId: Int): Boolean
}
