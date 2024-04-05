package com.sqz.checklist.notification

import android.content.Context
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
            NotificationObject.create(
                applicationContext,
                channelId = channelId,
                channelName = channelName,
                channelDescription = channelDescription,
                title = title,
                content = content,
                notifyId = notifyId
            )
        } else {
            return Result.failure()
        }
        return Result.success()
    }
}