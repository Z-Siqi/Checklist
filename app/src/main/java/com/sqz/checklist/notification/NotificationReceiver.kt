package com.sqz.checklist.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.database.DatabaseRepository
import com.sqz.checklist.database.TaskDatabase
import com.sqz.checklist.database.buildDatabase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationReceiver : BroadcastReceiver() {
    companion object {
        fun notificationTextFormater(text: String?, remindTime: Long, ctx: Context): String {
            val dateFormat = ctx.getString(R.string.full_date_short)
            val format = SimpleDateFormat(dateFormat, Locale.getDefault())
            val date = format.format(remindTime)
            if (text == null) {
                return ctx.getString(R.string.task_reminded_time, date)
            }
            return text + "\n\n" + ctx.getString(R.string.task_reminded_time, date)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("NotificationId", Int.MAX_VALUE)
        GlobalScope.launch {
            val db: TaskDatabase = try {
                MainActivity.taskDatabase
            } catch (_: Exception) {
                buildDatabase(context)
            }
            try {
                val databaseRepository = DatabaseRepository(db)
                val reminderData = databaseRepository.getReminderData(reminderId = notificationId)!!
                val notificationData = NotificationData(
                    id = reminderData.reminder.id,
                    title = reminderData.taskDescription,
                    text = notificationTextFormater(
                        text = reminderData.reminder.extraText,
                        remindTime = reminderData.reminder.reminderTime,
                        ctx = context
                    ),
                )
                this@NotificationReceiver.createNotification(context, notificationData)
                databaseRepository.setIsReminded(notificationId, true)
                try {
                    MainActivity.taskDatabase
                } catch (_: Exception) {
                    db.close()
                }
            } catch (e: Exception) {
                if (e is NullPointerException) {
                    Log.i("NotificationReceiver", "Reminder not found")
                    return@launch
                }
                throw e
            }
        }
    }

    private fun createNotification(context: Context, notifyData: NotificationData) {
        val notification = NotificationCreator(context)
        notification.pushedNotificationCreator(
            channel = NotificationChannelData(
                id = context.getString(R.string.tasks),
                name = context.getString(R.string.task_reminder),
                description = context.getString(R.string.description),
            ),
            notifyData = NotificationData(
                id = notifyData.id,
                title = notifyData.title,
                text = notifyData.text
            )
        )
    }
}
