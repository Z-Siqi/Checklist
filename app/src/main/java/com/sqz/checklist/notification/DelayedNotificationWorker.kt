package com.sqz.checklist.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters

class DelayedNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val channelId = inputData.getString("channelId")
        val channelName = inputData.getString("channelName")
        val channelDescription = inputData.getString("channelDescription")
        val title = inputData.getString("title")
        val content = inputData.getString("content")
        val notifyId = inputData.getInt("notifyId", -1)
        if (channelId != null &&
            channelName != null &&
            channelDescription != null &&
            title != null &&
            content != null &&
            notifyId != -1
        ) {
            val intent = Intent(applicationContext, NotificationReceiver::class.java).apply {
                putExtra("channelId", channelId)
                putExtra("channelName", channelName)
                putExtra("channelDescription", channelDescription)
                putExtra("title", title)
                putExtra("content", content)
                putExtra("notifyId", notifyId)
            }
            PendingIntent.getBroadcast( // Use NotificationReceiver to send notification
                applicationContext, notifyId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ).send()
        } else {
            return Result.failure()
        }
        return Result.success()
    }
}
