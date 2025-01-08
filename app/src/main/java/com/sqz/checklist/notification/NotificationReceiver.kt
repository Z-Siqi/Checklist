package com.sqz.checklist.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sqz.checklist.database.DatabaseRepository
import com.sqz.checklist.database.buildDatabase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {
    @OptIn(DelicateCoroutinesApi::class)
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
            GlobalScope.launch {
                val db = buildDatabase(context)
                val databaseRepository = DatabaseRepository(db)
                databaseRepository.setIsReminded(notifyId, true)
                db.close()
            }
        } else throw Exception("Notification data error!")
    }
}
