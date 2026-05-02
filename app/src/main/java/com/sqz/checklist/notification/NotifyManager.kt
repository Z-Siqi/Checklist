package com.sqz.checklist.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * This is the Main notification manager.
 * All notification related functions should be called from here.
 * This class is use to create delayed notification for Checklist.
 */
class NotifyManager {
    companion object {
        fun isNotificationExist(
            notifyId: Int,
            context: Context,
            getNotification: (channelId: String, postTime: Long) -> Unit = { _, _ -> }
        ): Boolean {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            for (sbn in nm.activeNotifications) {
                if (sbn.id == notifyId) {
                    val old = sbn.notification
                    getNotification(old.channelId, sbn.postTime)
                    return true
                }
            }
            return false
        }
    }

    fun checkPermissions(context: Context): PermissionState {
        val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        var alarmPermission = false
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) alarmPermission = true
        } else {
            alarmPermission = true
        }

        return when {
            notificationPermission && alarmPermission -> PermissionState.Both
            alarmPermission -> PermissionState.Alarm
            notificationPermission -> PermissionState.Notification
            else -> PermissionState.Null
        }
    }

    fun hasAlarmPermission(context: Context): Boolean {
        return checkPermissions(context).let { it == PermissionState.Both || it == PermissionState.Alarm }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return checkPermissions(context).let { it == PermissionState.Both || it == PermissionState.Notification }
    }

    /**
     * @param targetTime The absolute target time (Epoch milliseconds) to trigger the notification.
     */
    fun createNotification(
        notifyId: Int,
        targetTime: Long,
        context: Context
    ) {
        val hasNotification = hasNotificationPermission(context)
        val hasAlarm = hasAlarmPermission(context)
        
        if (!hasNotification) throw Exception("Notification permission not granted!")
        if (!hasAlarm) Log.w(
            "ChecklistNotification", "Alarm permission not granted! Notification may arrive late!"
        )
        val notificationCreator = NotificationCreator(context)
        if (hasAlarm) {
            notificationCreator.createAlarmed(
                notifyId = notifyId,
                delayDuration = targetTime
            )
        } else {
            val delayDuration = targetTime - System.currentTimeMillis()
            notificationCreator.createWorker(
                notifyId = notifyId,
                delayDuration = if (delayDuration > 0) delayDuration else 0L,
                timeUnit = TimeUnit.MILLISECONDS
            )
        }
    }

    fun cancelNotification(
        notifyId: Int,
        context: Context,
        delShowedByNotifyId: Boolean = true
    ) {
        try {
            // Cancel Alarm
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("NotificationId", notifyId)
                putExtra("notifyId", notifyId) // For backwards compatibility
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, notifyId, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }

            // Cancel Worker
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag(notifyId.toString())
            
        } catch (e: Exception) {
            Log.e("ChecklistNotification", "Exception: ${e.message}")
        }
        if (delShowedByNotifyId) { // Delete notification if showed
            this.removeShowedNotification(notifyId, context)
        }
    }

    fun removeShowedNotification(notifyId: Int, context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notifyId)
    }
}