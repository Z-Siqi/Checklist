package com.sqz.checklist.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import sqz.checklist.data.database.repository.DatabaseRepository
import sqz.checklist.data.database.ReminderModeType
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import sqz.checklist.data.database.DatabaseProvider
import sqz.checklist.data.database.getDatabaseBuilder
import java.util.concurrent.TimeUnit

/**
 * Restore delayed notification (reminder) when boot or restart app
 */
class BootReceiver : BroadcastReceiver() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            Log.d("BootReceiver", "intent.action: ${intent.action}")
            val notificationManager = NotifyManager()
            GlobalScope.launch {
                if (notificationManager.hasAlarmPermission(context)) {
                    this@BootReceiver.restoreReminder(
                        notificationManager = notificationManager,
                        context = context
                    )
                }
            }
        }
    }

    private suspend fun restoreReminder(
        notificationManager: NotifyManager,
        context: Context
    ) {
        val db = DatabaseProvider(getDatabaseBuilder(context)).getDatabase()
        val dao = db.taskReminderDao()
        val list = dao.getAll()
        val databaseRepository = DatabaseRepository(db)
        val notification = NotificationCreator(context)
        for (data in list) {
            try {
                if (!notification.getAlarmNotificationState(data.reminder.id) &&
                    data.reminder.mode == ReminderModeType.AlarmManager && !data.reminder.isReminded
                ) {
                    notificationManager.createNotification(
                        notifyId = data.reminder.id,
                        targetTime = data.reminder.reminderTime,
                        context = context
                    ).also {
                        Log.d("RestoreReminder", "Restore NotifyId: ${data.reminder.id}")
                    }
                    if (data.reminder.reminderTime < System.currentTimeMillis()) {
                        databaseRepository.setIsReminded(data.reminder.id, true)
                    }
                }
                if (data.reminder.mode == ReminderModeType.Worker) {
                    Log.d("RestoreReminder", "Ignored: work manager no need this!")
                }
            } catch (_: NumberFormatException) {
                Log.d("RestoreReminder", "Ignored: work manager no need this!")
            } catch (e: Exception) {
                Log.e("RestoreReminder", "Failed: ${e.message}")
            }
        }
        Log.d("BootReceiver", "Delayed notification is restored!")
    }
}
