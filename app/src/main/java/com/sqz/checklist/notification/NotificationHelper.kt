package com.sqz.checklist.notification

import android.content.Context
import android.util.Log
import sqz.checklist.data.database.DatabaseProvider
import sqz.checklist.data.database.repository.reminder.TaskReminderRepository

object NotificationHelper {

    suspend fun restoreNotification(database: DatabaseProvider, context: Context) {
        Log.d("RestoreReminder", "trying to restore all reminder")
        val notificationManager = NotifyManager()
        val reminderRepo = TaskReminderRepository.provider(database)
        for (data in reminderRepo.getReminderViewList()) {
            if (!data.reminder.isReminded) try {
                notificationManager.createNotification(
                    notifyId = data.reminder.id,
                    targetTime = data.reminder.reminderTime,
                    context = context
                ).also { Log.d("RestoreReminder", "Restore NotifyId: ${data.reminder.id}") }

                //TODO: use silent way to restore the notification that should notify in past time
            } catch (e: Exception) {
                Log.w("RestoreReminder", "Exception: $e")
            }
        }
    }
}
