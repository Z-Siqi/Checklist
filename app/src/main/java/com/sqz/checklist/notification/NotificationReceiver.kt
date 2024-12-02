package com.sqz.checklist.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channelId = intent.getStringExtra("channelId")
        val channelName = intent.getStringExtra("channelName")
        val channelDescription = intent.getStringExtra("channelDescription")
        val title = intent.getStringExtra("title")
        val content = intent.getStringExtra("content")
        val notifyId = intent.getIntExtra("notifyId", -1)

        if (channelId != null && channelName != null && channelDescription != null &&
            title != null && content != null && notifyId != -1
        ) {
            val notification = NotificationCreator(context)
            notification.creator(
                channelId = channelId,
                channelName = channelName,
                channelDescription = channelDescription,
                title = title,
                content = content,
                notifyId = notifyId
            )
        } else throw Exception("Notification data error!")
    }
}
