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
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R

object NotificationObject {
    fun create(
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setFullScreenIntent(fullScreenPendingIntent, true)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.

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
}