package com.sqz.checklist.ui.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.work.WorkManager
import com.sqz.checklist.notification.NotificationReceiver
import java.util.UUID

/**
 * Check the notification is set or not by task reminder data.
 */
@Composable
fun notificationState(string: String, context: Context, isAlarm: Boolean): Boolean {
    var find by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(string) { // the LaunchedEffect is to fix delay after set reminder (string listener)
        try {
            if (isAlarm) {
                val intent = Intent(context, NotificationReceiver::class.java).apply {
                    putExtra("notifyId", string.toInt())
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, string.toInt(), intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                find = pendingIntent != null
            } else {
                val workManager = WorkManager.getInstance(context)
                workManager.getWorkInfoByIdLiveData(UUID.fromString(string)).observeForever {
                    find = !(it != null && it.state.isFinished)
                }
            }
        } catch (e: Exception) {
            Log.d("ChecklistNotification", "Notification state check: $e")
        }
    }
    return find
}
