package com.sqz.checklist.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sqz.checklist.R
import com.sqz.checklist.database.DatabaseRepository
import com.sqz.checklist.database.ReminderModeType
import com.sqz.checklist.database.buildDatabase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Restore delayed notification (reminder) when boot or restart app
 */
class BootReceiver : BroadcastReceiver() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val notificationManager = MutableStateFlow(NotifyManager())
            GlobalScope.launch {
                notificationManager.value.requestPermission(context)
                if (notificationManager.value.getAlarmPermission()) {
                    val db = buildDatabase(context)
                    val dao = db.taskReminderDao()
                    val list = dao.getAll()
                    val databaseRepository = DatabaseRepository(db)
                    val notification = NotificationCreator(context)
                    for (data in list) {
                        try {
                            if (!notification.getAlarmNotificationState(data.id) &&
                                data.mode == ReminderModeType.AlarmManager && !data.isReminded
                            ) {
                                notificationManager.value.createNotification(
                                    channelId = context.getString(R.string.tasks),
                                    channelName = context.getString(R.string.task_reminder),
                                    channelDescription = context.getString(R.string.description),
                                    description = data.description, notifyId = data.id,
                                    delayDuration = data.reminderTime,
                                    timeUnit = TimeUnit.MILLISECONDS, context = context
                                ).also {
                                    Log.d("RestoreReminder", "Restore NotifyId: ${data.id}")
                                }
                                if (data.reminderTime < System.currentTimeMillis()) {
                                    databaseRepository.setIsReminded(data.id, true)
                                }
                            }
                            if (data.mode == ReminderModeType.Worker) {
                                Log.d("RestoreReminder", "Ignored: work manager no need this!")
                            }
                        } catch (e: NumberFormatException) {
                            Log.d("RestoreReminder", "Ignored: work manager no need this!")
                        } catch (e: Exception) {
                            Log.e("RestoreReminder", "Failed: ${e.message}")
                        }
                    }
                    Log.d("BootReceiver", "Delayed notification is restored!")
                    db.close()
                }
            }
        }
    }
}
