package com.sqz.checklist.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import sqz.checklist.data.database.DatabaseProvider
import sqz.checklist.data.database.getDatabaseBuilder

import com.sqz.checklist.common.device.isResourceReadyForHighPerformance
import android.app.AlarmManager
import sqz.checklist.data.database.repository.reminder.TaskReminderRepository

/**
 * Restore delayed notification (reminder) when boot or restart app
 */
class BootReceiver : BroadcastReceiver() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        ) {
            Log.d("BootReceiver", "intent.action: ${intent.action}")
            val notificationManager = NotifyManager()
            GlobalScope.launch {
                // If it's just a permission change, verify performance before heavy sync
                if (intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
                    // Only defer the upgrade (Worker -> Alarm) due to performance.
                    // If we lost permission (Alarm -> Worker), we must fall back immediately to ensure delivery.
                    if (notificationManager.hasAlarmPermission(context) && !isResourceReadyForHighPerformance(context)) {
                        Log.d("BootReceiver", "Ignored sync due to device performance state")
                        return@launch
                    }
                }

                this@BootReceiver.syncReminders(
                    notificationManager = notificationManager,
                    context = context
                )
            }
        }
    }

    private suspend fun syncReminders(
        notificationManager: NotifyManager,
        context: Context
    ) {
        val db = DatabaseProvider(getDatabaseBuilder(context))
        val reminderRepo = TaskReminderRepository.provider(db)
        val list = reminderRepo.getReminderViewList()
        val hasAlarmPermission = notificationManager.hasAlarmPermission(context)
        
        for (data in list) {
            try {
                if (data.reminder.isReminded) continue
                
                // Determine if we need to reschedule
                val needsReschedule = if (hasAlarmPermission) {
                    !NotifyManager.isAlarmNotificationExist(data.reminder.id, context)
                } else {
                    // Always re-trigger createNotification since WorkManager uses REPLACE,
                    // handling downgrade cleanly.
                    true
                }
                
                if (needsReschedule) {
                    // Cancel old notifications first to avoid duplicates (e.g. leftover workers)
                    notificationManager.cancelNotification(
                        notifyId = data.reminder.id, 
                        context = context, 
                        delShowedByNotifyId = false
                    )

                    // This creates Alarm if permission exists, Worker otherwise.
                    // Handles both downgrading to Worker and upgrading to Alarm.
                    notificationManager.createNotification(
                        notifyId = data.reminder.id,
                        targetTime = data.reminder.reminderTime,
                        context = context
                    )
                    Log.d("BootReceiver", "Synced NotifyId: ${data.reminder.id}")
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed: ${e.message}")
            }
        }
        Log.d("BootReceiver", "Delayed notification is restored/synced!")
    }
}
