package com.sqz.checklist.presentation.reminder.runtime

import android.content.Context
import com.sqz.checklist.R
import com.sqz.checklist.notification.NotificationChannelData
import com.sqz.checklist.notification.NotificationCreator
import com.sqz.checklist.notification.NotificationData
import com.sqz.checklist.notification.NotificationReceiver
import com.sqz.checklist.notification.NotifyManager
import sqz.checklist.reminder.api.runtime.ReminderRuntime

class ReminderNotificationController(
    private val context: Context,
    private val runtime: ReminderRuntime,
) {
    suspend fun removeReminder(taskId: Long, removeDisplayedNotification: Boolean) {
        runtime.removeReminder(taskId, removeDisplayedNotification)
    }

    suspend fun refreshDisplayedReminder(taskId: Long) {
        val reminderView = runtime.getReminder(taskId) ?: return
        if (!NotifyManager.isNotificationDisplayed(reminderView.reminder.id, context)) {
            return
        }
        NotifyManager.isNotificationDisplayed(reminderView.reminder.id, context) { channelId, postTime ->
            NotificationCreator(context).pushedNotificationCreator(
                channel = NotificationChannelData(
                    id = channelId,
                    name = context.getString(R.string.task_reminder),
                    description = context.getString(R.string.description),
                ),
                notifyData = NotificationData(
                    id = reminderView.reminder.id,
                    title = reminderView.taskDescription,
                    text = NotificationReceiver.notificationTextFormater(
                        text = reminderView.reminder.extraText,
                        remindTime = postTime,
                        ctx = context,
                    )
                )
            )
        }
    }

    suspend fun hasPendingReminder(): Boolean {
        return runtime.hasPendingReminder()
    }
}
