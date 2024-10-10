package com.sqz.checklist.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import java.util.UUID

class NotificationCreator {
    /** Config delayed notification worker. NOT using to create notification **/
    fun creator(
        context: Context,
        channelId: String,
        channelName: String,
        channelDescription: String,
        title: String,
        content: String,
        notifyId: Int
    ) {
        // Notification channel
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = channelDescription
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        val fullScreenIntent = Intent(context, MainActivity::class.java)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 0,
            fullScreenIntent, PendingIntent.FLAG_MUTABLE
        )
        // Build and show notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.task_icon)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setFullScreenIntent(fullScreenPendingIntent, true)
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                val packageManager = context.packageManager
                val checkApp1 = try {
                    packageManager.getPackageInfo("me.piebridge.brevent", 0)
                    true
                } catch (e: PackageManager.NameNotFoundException) {
                    false
                }
                val checkApp2 = try {
                    packageManager.getPackageInfo("github.tornaco.android.thanos.pro", 0)
                    true
                } catch (e: PackageManager.NameNotFoundException) {
                    false
                }
                if (checkApp1) Toast.makeText(
                    context, context.getString(R.string.note_brevent), Toast.LENGTH_SHORT
                ).show()
                if (checkApp2) Toast.makeText(
                    context, context.getString(R.string.note_thanox), Toast.LENGTH_SHORT
                ).show()

                Log.e("ERROR", "Failed: Permission denied!")
            }
            this.notify(notifyId, builder.build())
        }
    }

    /** Create delayed notification **/
    fun create(
        channelId: String,
        channelName: String,
        channelDescription: String,
        description: String,
        content: String = "",
        notifyId: Int,
        delayDuration: Long,
        timeUnit: java.util.concurrent.TimeUnit,
        context: Context
    ) : UUID {
        val workRequest = OneTimeWorkRequestBuilder<DelayedNotificationWorker>()
            .setInputData(
                Data.Builder()
                    .putString("channelId", channelId)
                    .putString("channelName", channelName)
                    .putString("channelDescription", channelDescription)
                    .putString("title", description)
                    .putString("content", content)
                    .putInt("notifyId", notifyId)
                    .build()
            )
            .setInitialDelay(delayDuration, timeUnit)
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
        return workRequest.id
    }
}
