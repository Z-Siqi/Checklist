package com.sqz.checklist.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sqz.checklist.R
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import sqz.checklist.data.database.DatabaseProvider
import sqz.checklist.data.database.getDatabaseBuilder
import sqz.checklist.data.database.repository.reminder.TaskReminderRepository
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
        val notifyIdDeprecated = intent.getIntExtra("notifyId", Int.MAX_VALUE)
        val notificationId = if (notifyIdDeprecated != Int.MAX_VALUE) {
            Log.w("NotificationReceiver", "Deprecated notifyId!")
            notifyIdDeprecated
        } else {
            intent.getIntExtra("NotificationId", Int.MAX_VALUE)
        }
        GlobalScope.launch {
            val db = DatabaseProvider(getDatabaseBuilder(context))
            try {
                val reminderRepository = TaskReminderRepository.provider(db)
                val reminderData = reminderRepository.getReminderView(notificationId)!!
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
                reminderRepository.updateRemindedState(notificationId, true)
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
