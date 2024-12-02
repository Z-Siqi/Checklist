package com.sqz.checklist.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.room.Room
import com.sqz.checklist.R
import com.sqz.checklist.database.TaskDatabase
import com.sqz.checklist.database.taskDatabaseName
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
                    val db = Room.databaseBuilder(
                        context, TaskDatabase::class.java, taskDatabaseName
                    ).build()
                    val taskDao = db.taskDao()
                    val list = taskDao.getIsRemindedList()
                    val notification = NotificationCreator(context)
                    for (data in list) {
                        val parts = data.reminder?.split(":")
                        val queryCharacter = if (parts?.size!! >= 2) parts[0] else null
                        if (queryCharacter != null) try {
                            if (!notification.getAlarmNotificationState(queryCharacter.toInt()) &&
                                parts[1].toLong() >= System.currentTimeMillis()
                            ) {
                                notificationManager.value.createNotification(
                                    channelId = context.getString(R.string.tasks),
                                    channelName = context.getString(R.string.task_reminder),
                                    channelDescription = context.getString(R.string.description),
                                    description = data.description, notifyId = data.id,
                                    delayDuration = parts[1].toLong(),
                                    timeUnit = TimeUnit.MILLISECONDS, context = context
                                ).also {
                                    Log.d("RestoreReminder", "Restore NotifyId: $queryCharacter")
                                }
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
