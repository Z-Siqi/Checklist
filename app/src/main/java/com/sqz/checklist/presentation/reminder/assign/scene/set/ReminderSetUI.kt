package com.sqz.checklist.presentation.reminder.assign.scene.set

import android.content.Context
import android.content.pm.PackageManager
import android.view.View
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.sqz.checklist.R
import com.sqz.checklist.notification.NotifyManager
import com.sqz.checklist.ui.common.dialog.TimeSelectDialog
import sqz.checklist.data.preferences.PreferencesInCache
import sqz.checklist.data.preferences.PrimaryPreferences

@Composable
fun ReminderSetUI(
    allowDialogDismiss: Boolean,
    view: View,
    notifyManager: NotifyManager,
    onDismissRequest: () -> Unit,
    onConfirm: (remindAtMillis: Long) -> Unit,
) {
    val cachePreferences = PreferencesInCache(view.context)
    val primaryPreferences = remember(view.context) { PrimaryPreferences(view.context) }
    LaunchedEffect(Unit) {
        if (!cachePreferences.checkBackgroundManageApp()) {
            if (!checkInstalledApp(view.context)) {
                return@LaunchedEffect
            }
            cachePreferences.checkBackgroundManageApp(true)
        }
    }
    TimeSelectDialog(
        onDismissRequest = onDismissRequest,
        onConfirmClick = { delayMillis ->
            val remindAtMillis = System.currentTimeMillis() + delayMillis
            if (!notifyManager.hasAlarmPermission(view.context) &&
                !primaryPreferences.disableNoScheduleExactAlarmNotice()
            ) {
                Toast.makeText(
                    view.context,
                    R.string.no_SCHEDULE_EXACT_ALARM_permission_explain,
                    Toast.LENGTH_SHORT,
                ).show()
            }
            onConfirm(remindAtMillis)
        },
        onFailed = onDismissRequest,
        view = view,
        allowDismissRequest = allowDialogDismiss,
    )
}

/** @return A Boolean value. `true` for detected, otherwise not. **/
@Suppress("SpellCheckingInspection")
private fun checkInstalledApp(context: Context): Boolean {
    var found = false
    val packageManager = context.packageManager
    try {
        packageManager.getPackageInfo("me.piebridge.brevent", 0)
        Toast.makeText(
            context,
            context.getString(R.string.note_brevent),
            Toast.LENGTH_LONG,
        ).show()
        found = true
    } catch (_: PackageManager.NameNotFoundException) {
    }
    try {
        packageManager.getPackageInfo("github.tornaco.android.thanos.pro", 0)
        Toast.makeText(
            context,
            context.getString(R.string.note_thanox),
            Toast.LENGTH_LONG,
        ).show()
        found = true
    } catch (_: PackageManager.NameNotFoundException) {
    }
    return found
}
