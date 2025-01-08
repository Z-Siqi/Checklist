package com.sqz.checklist.notification

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.sqz.checklist.database.DatabaseRepository
import com.sqz.checklist.database.buildDatabase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DelayedNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {
    @OptIn(DelicateCoroutinesApi::class)
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
            val notification = NotificationCreator(applicationContext)
            notification.creator(
                channelId = channelId,
                channelName = channelName,
                channelDescription = channelDescription,
                title = title,
                content = content,
                notifyId = notifyId
            )
            GlobalScope.launch {
                val db = buildDatabase(applicationContext)
                val databaseRepository = DatabaseRepository(db)
                databaseRepository.setIsReminded(notifyId, true)
                db.close()
            }
        } else {
            return Result.failure()
        }
        return Result.success()
    }
}
