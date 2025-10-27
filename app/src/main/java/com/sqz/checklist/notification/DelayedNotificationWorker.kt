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
        val notifyId = inputData.getInt("NotificationId", Int.MAX_VALUE)
        if (notifyId != 0) {
            val intent = Intent(applicationContext, NotificationReceiver::class.java).apply {
                putExtra("NotificationId", notifyId)
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
