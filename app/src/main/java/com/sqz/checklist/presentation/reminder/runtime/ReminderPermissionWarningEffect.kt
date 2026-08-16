package com.sqz.checklist.presentation.reminder.runtime

import android.view.View
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.sqz.checklist.R
import com.sqz.checklist.notification.NotifyManager
import com.sqz.checklist.notification.PermissionState

@Composable
fun ReminderPermissionWarningEffect(
    view: View,
    controller: ReminderNotificationController,
    notifyManager: NotifyManager,
) {
    val context = view.context
    var toastShown by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (toastShown) {
            return@LaunchedEffect
        }
        val permissionState = notifyManager.checkPermissions(context)
        if (permissionState == PermissionState.Both || permissionState == PermissionState.Notification) {
            return@LaunchedEffect
        }
        if (!controller.hasPendingReminder()) {
            return@LaunchedEffect
        }
        Toast.makeText(
            context,
            context.getString(R.string.permission_lost_toast),
            Toast.LENGTH_LONG,
        ).show()
        toastShown = true
    }
}
