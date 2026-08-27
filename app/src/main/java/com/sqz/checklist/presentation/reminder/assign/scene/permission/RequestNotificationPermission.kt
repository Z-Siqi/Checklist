package com.sqz.checklist.presentation.reminder.assign.scene.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.sqz.checklist.R
import com.sqz.checklist.notification.NotifyManager
import com.sqz.checklist.notification.PermissionState
import com.sqz.checklist.ui.common.dialog.WarningAlertDialog
import com.sqz.checklist.ui.common.unit.isApi31AndAbove
import com.sqz.checklist.ui.common.unit.isApi33AndAbove
import androidx.core.net.toUri

/** This method is expected to be called only within this package and its sub-packages. */
@Composable
internal fun RequestNotificationPermission(
    notifyManager: NotifyManager,
    context: Context,
    onTried: () -> Unit,
) {
    val initialPermissionState = remember { notifyManager.checkPermissions(context) }
    var completed by rememberSaveable { mutableStateOf(false) }
    var showNotificationDialog by rememberSaveable {
        mutableStateOf(!isApi33AndAbove && !initialPermissionState.hasNotificationPermission())
    }
    var retryNotificationPermission by rememberSaveable { mutableStateOf(false) }
    var showAlarmDialog by rememberSaveable {
        mutableStateOf(isApi31AndAbove && initialPermissionState == PermissionState.Notification)
    }

    fun finish() {
        if (!completed) {
            completed = true
            onTried()
        }
    }

    fun handlePermissionState() {
        when (notifyManager.checkPermissions(context)) {
            PermissionState.Both -> finish()
            PermissionState.Notification -> {
                if (isApi31AndAbove) showAlarmDialog = true else finish()
            }
            PermissionState.Alarm,
            PermissionState.Null -> finish()
        }
    }

    val notificationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        handlePermissionState()
    }
    val alarmSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        // Exact alarms are optional because NotificationScheduler can fall back to WorkManager.
        finish()
    }

    val alreadyReady = initialPermissionState == PermissionState.Both ||
            (!isApi31AndAbove && initialPermissionState == PermissionState.Notification)
    if (alreadyReady) {
        LaunchedEffect(Unit) { finish() }
        return
    }

    if (isApi33AndAbove && !initialPermissionState.hasNotificationPermission()) {
        NewNotificationPermissionRequester { granted ->
            if (granted) handlePermissionState() else showNotificationDialog = true
        }
    }

    if (showNotificationDialog) {
        NotificationPermissionRequestDialog(
            onDismissRequest = ::finish,
            onRequestPermission = {
                showNotificationDialog = false
                if (isApi33AndAbove) {
                    retryNotificationPermission = true
                } else {
                    notificationSettingsLauncher.launch(notificationSettingsIntent(context))
                }
            },
        )
    }

    if (retryNotificationPermission) {
        NewNotificationPermissionRequester { granted ->
            if (granted) {
                handlePermissionState()
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.no_set_reminder_permission) + " " +
                            context.getString(R.string.please_grant_permission),
                    Toast.LENGTH_SHORT,
                ).show()
                notificationSettingsLauncher.launch(notificationSettingsIntent(context))
            }
        }
    }

    if (showAlarmDialog) {
        RequestAlarmPermissionDialog(
            onDismissRequest = ::finish,
            onRequestPermission = {
                showAlarmDialog = false
                alarmSettingsLauncher.launch(exactAlarmSettingsIntent(context))
            },
        )
    }
}

private fun PermissionState.hasNotificationPermission(): Boolean =
    this == PermissionState.Notification || this == PermissionState.Both

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun NewNotificationPermissionRequester(
    onLaunched: (granted: Boolean) -> Unit,
) {
    var launched by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onLaunched,
    )

    LaunchedEffect(Unit) {
        if (!launched) {
            launched = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun NotificationPermissionRequestDialog(
    onDismissRequest: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    WarningAlertDialog(
        textString = stringResource(R.string.unable_sent_cause_no_permission) +
                "\n" + stringResource(R.string.please_grant_permission),
        onDismissRequest = onDismissRequest,
        onConfirmButtonClick = onRequestPermission,
    )
}

private fun notificationSettingsIntent(context: Context) =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }

@RequiresApi(Build.VERSION_CODES.S)
private fun exactAlarmSettingsIntent(context: Context) = Intent(
    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
    "package:${context.packageName}".toUri(),
)

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun RequestAlarmPermissionDialog(
    onDismissRequest: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    WarningAlertDialog(
        onDismissRequest = onDismissRequest,
        onConfirmButtonClick = onRequestPermission,
        textString = stringResource(R.string.request_SCHEDULE_EXACT_ALARM_content),
    )
}
