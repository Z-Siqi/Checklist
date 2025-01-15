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
import java.util.UUID

/**
 * This is the Main notification manager.
 * All notification related functions should be called from here.
 * This class is use to create delayed notification for Checklist.
 */
class NotifyManager : Exception() {
    private var permissionChecker: Boolean
    private var notificationPermission: Boolean
    private var alarmPermission: Boolean
    private var repeatTime: Int

    init {
        this.permissionChecker = false
        this.notificationPermission = false
        this.alarmPermission = false
        this.repeatTime = 0
    }

    fun requestPermission(context: Context): PermissionState {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) this.notificationPermission = true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) this.alarmPermission = true
        } else if (this.notificationPermission) this.alarmPermission = true
        this.permissionChecker = true
        return when {
            this.notificationPermission && this.alarmPermission -> PermissionState.Both
            this.alarmPermission -> PermissionState.Alarm
            this.notificationPermission -> PermissionState.Notification
            else -> PermissionState.Null
        }
    }

    fun createNotification(
        channelId: String, channelName: String, channelDescription: String,
        description: String, content: String = "",
        notifyId: Int, delayDuration: Long, timeUnit: java.util.concurrent.TimeUnit,
        context: Context
    ): String {
        if (!this.permissionChecker) throw Exception("Permission not check! run requestPermission() first!")
        if (!this.notificationPermission) throw Exception("Notification permission not granted!")
        if (!this.alarmPermission) Log.w(
            "ChecklistNotification", "Alarm permission not granted! Notification may arrive late!"
        )
        val notificationCreator = NotificationCreator(context)
        if (this.alarmPermission) {
            notificationCreator.createAlarmed(
                channelId, channelName, channelDescription, description, content,
                notifyId, delayDuration
            )
            return notifyId.toString()
        } else {
            return notificationCreator.createWorker(
                channelId, channelName, channelDescription, description, content,
                notifyId, delayDuration, timeUnit
            ).toString()
        }
    }

    fun cancelNotification(
        string: String, context: Context,
        delShowedByNotifyId: Int = -1, forceAsWorkManager: Boolean = false
    ) {
        if (!this.permissionChecker) this.requestPermission(context) // init if not init
        try {
            if (this.alarmPermission && !forceAsWorkManager) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, NotificationReceiver::class.java).apply {
                    putExtra("notifyId", string.toInt())
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, string.toInt(), intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
            } else {
                val workManager = WorkManager.getInstance(context)
                workManager.cancelWorkById(UUID.fromString(string))
                this.repeatTime = 0
            }
        } catch (e: NumberFormatException) {
            Log.w("ChecklistNotification", "NumberFormatException: ${e.message}")
            if (this.alarmPermission && this.repeatTime < 3) {
                this.repeatTime++
                val text = "Detected the reminder not alarmed, trying to cancel as worker manager."
                Log.i("ChecklistNotification", text)
                this.cancelNotification(string, context, delShowedByNotifyId, true)
            }
            if (this.repeatTime >= 3) Log.e("ChecklistNotification", "ERROR: $e")
        } catch (e: Exception) {
            Log.e("ChecklistNotification", "Exception: ${e.message}")
        }
        if (delShowedByNotifyId != -1) { // Delete notification if showed
            this.removeShowedNotification(delShowedByNotifyId, context)
        }
    }

    fun removeShowedNotification(notifyId: Int, context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notifyId)
    }

    fun getAlarmPermission(): Boolean {
        if (!this.permissionChecker) throw Exception("Permission not check! run requestPermission() first!")
        return this.alarmPermission
    }
}
