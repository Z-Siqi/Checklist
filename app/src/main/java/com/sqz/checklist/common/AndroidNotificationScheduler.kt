package com.sqz.checklist.common

import android.content.Context
import com.sqz.checklist.R
import com.sqz.checklist.notification.NotificationChannelData
import com.sqz.checklist.notification.NotificationCreator
import com.sqz.checklist.notification.NotificationData
import com.sqz.checklist.notification.NotificationReceiver
import com.sqz.checklist.notification.NotifyManager
import sqz.checklist.common.NotificationScheduler
import sqz.checklist.data.database.repository.reminder.TaskReminderRepository

class AndroidNotificationScheduler(
    private val context: Context,
    private val taskReminder: TaskReminderRepository,
    private val notifyManager: NotifyManager = NotifyManager(),
) : NotificationScheduler {

    override suspend fun schedule(
        notificationId: Int, remindAtMillis: Long, silent: Boolean
    ) {
        notifyManager.createNotification(
            notifyId = notificationId,
            targetTime = remindAtMillis,
            context = context,
        )
    }

    override suspend fun reschedule(notificationId: Int, silent: Boolean) {
        val reminderView = taskReminder.getReminderView(notificationId) ?: return
        val reminder = reminderView.reminder
        NotifyManager.isNotificationDisplayed(reminder.id, context) { channelId, postTime ->
            NotificationCreator(context).pushedNotificationCreator(
                channel = NotificationChannelData(
                    id = channelId,
                    name = context.getString(R.string.task_reminder),
                    description = context.getString(R.string.description),
                ),
                notifyData = NotificationData(
                    id = reminder.id,
                    title = reminderView.taskDescription,
                    text = NotificationReceiver.notificationTextFormater(
                        text = reminder.extraText,
                        remindTime = postTime,
                        ctx = context,
                    )
                )
            )
        }.also { if (it) return }
        // No need do anything to reschedule notification that no sends yet
        // due to database is able to open during sending Notification.
    }

    override suspend fun cancel(notificationId: Int, tryRemoveDisplayed: Boolean) {
        notifyManager.cancelNotification(
            notifyId = notificationId,
            context = context,
            delShowedByNotifyId = tryRemoveDisplayed,
        )
    }

    override suspend fun cancelAll(tryRemoveDisplayed: Boolean) {
        for (data in taskReminder.getReminderViewList()) {
            notifyManager.cancelNotification(notifyId = data.reminder.id, context = context)
        }
    }

    override suspend fun isExisted(notificationId: Int): Boolean {
        taskReminder.getReminderView(notificationId) ?: return false
        val isScheduled = if (notifyManager.hasAlarmPermission(context)) {
            NotifyManager.isAlarmNotificationExist(notificationId, context)
        } else {
            NotifyManager.isWorkerNotificationExist(notificationId, context)
        }
        return isScheduled || NotifyManager.isNotificationDisplayed(notificationId, context)
    }
}