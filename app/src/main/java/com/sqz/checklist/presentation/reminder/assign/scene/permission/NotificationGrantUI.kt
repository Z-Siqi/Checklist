package com.sqz.checklist.presentation.reminder.assign.scene.permission

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.notification.NotifyManager
import com.sqz.checklist.notification.PermissionState
import com.sqz.checklist.ui.common.dialog.WarningAlertDialog
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

enum class NotificationGrant {
    Request, Diagnosis
}

@Composable
fun NotificationGrantUI(
    mode: NotificationGrant,
    onFailed: () -> Unit,
    onGrant: () -> Unit,
    notifyManager: NotifyManager,
    context: Context,
) = when (mode) {
    NotificationGrant.Request -> {
        RequestNotificationPermission(
            notifyManager = notifyManager,
            context = context
        ) { // onTried
            val isPermissionExisted = notifyManager.checkPermissions(context).let {
                it == PermissionState.Notification || it == PermissionState.Both
            }
            if (!isPermissionExisted) {
                onFailed()
                return@RequestNotificationPermission
            }
            onGrant()
        }
    }

    NotificationGrant.Diagnosis -> {
        val coroutineScope = rememberCoroutineScope()
        val isPermissionExisted = notifyManager.checkPermissions(context).let {
            it == PermissionState.Notification || it == PermissionState.Both
        }
        val requestPermission = rememberSaveable { mutableStateOf(false) }
        if (!isPermissionExisted) WarningAlertDialog(
            textString = stringResource(R.string.permission_lost_toast) +
                    "\n" + stringResource(R.string.please_grant_permission),
            onDismissRequest = onFailed,
            onConfirmButtonClick = { requestPermission.value = true }
        ) else WarningAlertDialog(
            textString = stringResource(R.string.no_reminder_set_err),
            onDismissRequest = onFailed,
            onConfirmButtonClick = { exitProcess(0) }
        )
        requestPermission.let {
            if (it.value) RequestNotificationPermission(
                notifyManager = notifyManager,
                context = context
            ) {
                val isPermissionExisted = notifyManager.checkPermissions(context).let { check ->
                    check == PermissionState.Notification || check == PermissionState.Both
                }
                if (!isPermissionExisted) {
                    onFailed()
                    return@RequestNotificationPermission
                }
                coroutineScope.launch {
                    MainActivity.autoCheckRestoreReminder(context)
                    onGrant()
                }
            }
        }
    }
}
