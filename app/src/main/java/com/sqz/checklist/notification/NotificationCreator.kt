package com.sqz.checklist.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import java.util.UUID

class NotificationCreator(private val context: Context) {
    /**
     * Direct push notification.
     *
     * The notification will send immediately if call this function; NOT a delayed notification
     */
    fun pushedNotificationCreator(
        channel: NotificationChannelData,
        notifyData: NotificationData,
    ) {
        // Notification channel
        this.channelHandler(
            channel = channel,
            notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        )
        // Create intent to open app when notification clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_MUTABLE
        )
        // Build and show notification
        val builder = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.task_icon)
            .setContentTitle(notifyData.title)
            .setContentText(notifyData.text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("ERROR", "Failed: Permission denied!")
            }
            this.notify(notifyData.id, builder.build())
        }
    }

    /** Set or get notification channel **/
    private fun channelHandler(
        channel: NotificationChannelData,
        notificationManager: NotificationManager
    ): NotificationChannel {
        val getChannel = notificationManager.getNotificationChannel(channel.id)
        if (getChannel != null) {
            return getChannel
        }
        val importance = channel.importance ?: NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channel.id, channel.name, importance).apply {
            description = channel.description
        }
        notificationManager.createNotificationChannel(channel)
        return channel
    }

    /** Create delayed notification **/
    fun createWorker(
        notifyId: Int,
        delayDuration: Long,
        timeUnit: java.util.concurrent.TimeUnit
    ): UUID {
        val workRequest = OneTimeWorkRequestBuilder<DelayedNotificationWorker>()
            .setInputData(
                Data.Builder()
                    .putInt("NotificationId", notifyId)
                    .build()
            )
            .setInitialDelay(delayDuration, timeUnit)
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
        return workRequest.id
    }

    /** Create Alarmed notification **/
    fun createAlarmed(
        notifyId: Int,
        delayDuration: Long
    ): Int {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("NotificationId", notifyId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, notifyId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, delayDuration, pendingIntent
                )
                //alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(delayDuration, pendingIntent), pendingIntent)
            } catch (_: SecurityException) {
                Log.e("ERROR", "Failed: AlarmManager Permission denied! Notification cannot sent!")
            }
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, delayDuration, pendingIntent)
        }
        return notifyId
    }

    fun getAlarmNotificationState(notifyId: Int): Boolean {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("NotificationId", notifyId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, notifyId, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return pendingIntent != null
    }
}
