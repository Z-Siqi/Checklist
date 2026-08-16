package com.sqz.checklist.presentation.reminder.assign.scene.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import com.sqz.checklist.R
import com.sqz.checklist.notification.NotifyManager
import com.sqz.checklist.notification.PermissionState
import com.sqz.checklist.ui.common.dialog.WarningAlertDialog
import com.sqz.checklist.ui.common.unit.isApi31AndAbove
import com.sqz.checklist.ui.common.unit.isApi33AndAbove
import kotlinx.coroutines.delay


/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun RequestNotificationPermission(
    notifyManager: NotifyManager,
    context: Context,
    onTried: () -> Unit,
) {
    InitCheck(context, notifyManager) {
        Log.w(
            "RequestNotificationPermission",
            "No need to execute this due to already got permission."
        )
        onTried()
    }
    val localWindow = LocalWindowInfo.current
    var launchAlarmPermission by rememberSaveable { mutableStateOf(false) }
    var notificationPermissionDialog by rememberSaveable { mutableStateOf(!isApi33AndAbove) }
    if (isApi33AndAbove) {
        NewNotificationPermissionRequester(
            context = context,
            openSettingsIfNotGranted = false
        ) { granted ->
            if (granted) {
                if (!context.needAlarmPermission(notifyManager)) {
                    onTried()
                    return@NewNotificationPermissionRequester
                }
                launchAlarmPermission = true
            } else {
                notificationPermissionDialog = true
            }
        }
    }
    var launchNotificationPermission by rememberSaveable { mutableStateOf(false) }
    if (notificationPermissionDialog) {
        NotificationPermissionRequestDialog(
            context = context,
            onDismissRequest = onTried,
            onRequestPermission = { launchNotificationPermission = true },
        )
    }
    if (launchNotificationPermission) {
        if (isApi33AndAbove) {
            NewNotificationPermissionRequester(
                context = context,
                openSettingsIfNotGranted = true,
                onLaunched = { granted ->
                    if (granted && context.needAlarmPermission(notifyManager)) {
                        launchAlarmPermission = true
                        return@NewNotificationPermissionRequester
                    }
                    onTried()
                },
            )
        } else {
            /*
            / openNotificationSettings(context) is already execute in
            / NotificationPermissionRequestDialog when isApi33AndAbove == false
            */
            LaunchedEffect(Unit) {
                while (true) {
                    delay(168)
                    when (notifyManager.checkPermissions(context)) {
                        PermissionState.Both -> {
                            onTried()
                            break
                        }

                        PermissionState.Notification -> {
                            if (!isApi31AndAbove) onTried() else launchAlarmPermission = true
                            break
                        }

                        PermissionState.Null -> Unit
                        PermissionState.Alarm -> Unit
                    }
                    if (!localWindow.isWindowFocused) {
                        delay(168)
                        continue
                    }
                    return@LaunchedEffect
                }
            }
        }
    }
    if (launchAlarmPermission) {
        if (!isApi31AndAbove) throw IllegalStateException(
            "RequestAlarmPermissionDialog is unnecessary for current Android version!"
        )
        RequestAlarmPermissionDialog(onDismissRequest = onTried, context = context)
    }
}

@Composable
private fun InitCheck(context: Context, notifyManager: NotifyManager, noNeed: () -> Unit) {
    val initCheck = rememberSaveable { mutableStateOf(false) }
    initCheck.value.let {
        if (!it) when (notifyManager.checkPermissions(context)) {
            PermissionState.Both -> noNeed()
            PermissionState.Notification -> noNeed()
            PermissionState.Null -> Unit
            PermissionState.Alarm -> Unit
        }
    }
    initCheck.value = true
}

private fun Context.needAlarmPermission(notifyManager: NotifyManager): Boolean {
    if (!isApi31AndAbove) {
        return false
    }
    return notifyManager.checkPermissions(this) == PermissionState.Notification
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun NewNotificationPermissionRequester(
    context: Context,
    openSettingsIfNotGranted: Boolean = true,
    onLaunched: (granted: Boolean) -> Unit,
) {
    val rememberLaunch = rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted && openSettingsIfNotGranted) {
            Toast.makeText(
                context,
                context.getString(R.string.no_set_reminder_permission) + " " +
                        context.getString(R.string.please_grant_permission),
                Toast.LENGTH_SHORT,
            ).show()
            openNotificationSettings(context)
        }
        onLaunched(granted)
    }

    if (!rememberLaunch.value) LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        rememberLaunch.value = true
    }
}

@Composable
private fun NotificationPermissionRequestDialog(
    context: Context,
    onDismissRequest: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    WarningAlertDialog(
        textString = stringResource(R.string.unable_sent_cause_no_permission) +
                "\n" + stringResource(R.string.please_grant_permission),
        onDismissRequest = onDismissRequest,
        onConfirmButtonClick = {
            if (isApi33AndAbove) {
                onRequestPermission()
            } else {
                openNotificationSettings(context)
                onRequestPermission()
            }
        },
    )
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        addFlags(FLAG_ACTIVITY_NEW_TASK)
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    context.startActivity(intent)
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun RequestAlarmPermissionDialog(
    onDismissRequest: () -> Unit,
    context: Context,
) {
    WarningAlertDialog(
        onDismissRequest = onDismissRequest,
        onConfirmButtonClick = {
            val intents = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                flags = FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intents)
            onDismissRequest()
        },
        textString = stringResource(R.string.request_SCHEDULE_EXACT_ALARM_content)
    )
}
