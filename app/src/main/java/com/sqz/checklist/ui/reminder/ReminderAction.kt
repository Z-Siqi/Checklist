package com.sqz.checklist.ui.reminder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.sqz.checklist.R
import com.sqz.checklist.notification.PermissionState
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import com.sqz.checklist.ui.material.WarningAlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class ReminderActionType { Set, Cancel, None }

data class ReminderData(
    val id: Int = 0,
    val reminderInfo: String? = null,
    val set: ReminderActionType = ReminderActionType.None,
    val description: String = "",
)

/** Processing set and cancel reminder **/
@Composable
fun ReminderAction(
    reminder: ReminderData,
    context: Context,
    view: View,
    taskState: TaskLayoutViewModel,
    coroutineScope: CoroutineScope
) {
    val resetState = {
        taskState.resetTaskData()
        view.playSoundEffect(SoundEffectConstants.CLICK)
    }
    var requestPermission by rememberSaveable { mutableStateOf(false) }
    when (reminder.set) {
        ReminderActionType.Set -> {
            when (taskState.notificationInitState(context)) {
                PermissionState.Null -> if (!requestPermission) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (requestNotificationPermission(context) {
                                requestPermission = true
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                            } && taskState.notificationInitState(context) == PermissionState.Null
                        ) NoPermissionDialog(onDismissRequest = {
                            resetState()
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                        }, context = context, onConfirmButtonClick = {
                            requestPermission = true
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                        })
                    } else NoPermissionDialog(
                        onDismissRequest = { resetState() },
                        onConfirmButtonClick = {
                            requestPermission = true
                            resetState()
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                        },
                        context = context
                    )
                } else {
                    if (ActivityCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) requestPermission = false
                }

                PermissionState.Alarm -> {
                    NoPermissionDialog(onDismissRequest = { resetState() }, context = context)
                }

                else -> {
                    if (requestPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) NeedAlarmPermissionDialog(
                        onDismissRequest = {
                            resetState()
                            requestPermission = false
                        }, context = context
                    ) else TimeSelectDialog( // to set reminder
                        onDismissRequest = {
                            resetState()
                        },
                        onConfirmClick = { timeInMilli ->
                            if (!taskState.isAlarmPermission()) Toast.makeText(
                                context, context.getString(
                                    R.string.no_SCHEDULE_EXACT_ALARM_permission_explain
                                ), Toast.LENGTH_SHORT
                            ).show()
                            coroutineScope.launch {
                                taskState.setReminder(
                                    timeInMilli,
                                    TimeUnit.MILLISECONDS,
                                    reminder.id,
                                    reminder.description,
                                    context
                                )
                                resetState()
                            }
                        },
                        onFailed = { resetState() },
                        context = context
                    )
                }
            }
        }

        ReminderActionType.Cancel -> WarningAlertDialog(
            onDismissRequest = { resetState() },
            onConfirmButtonClick = {
                taskState.cancelReminder(reminder.id, reminder.reminderInfo, context)
                resetState()
            },
            onDismissButtonClick = { resetState() },
            text = {
                val parts = reminder.reminderInfo?.split(":")
                val getTimeInLong = if (parts != null && parts.size >= 2) parts[1].toLong() else 0L
                Text(
                    text = stringResource(R.string.cancel_the_reminder),
                    fontSize = (15 - 1).sp
                )
                val fullDateShort = stringResource(R.string.full_date_short)
                val formatter = remember { SimpleDateFormat(fullDateShort, Locale.getDefault()) }
                Text(
                    text = stringResource(
                        R.string.remind_at,
                        formatter.format(getTimeInLong)
                    ),
                    fontSize = 15.sp
                )
            }
        )

        ReminderActionType.None -> {}
    }
}

@Composable
private fun NoPermissionDialog(
    onDismissRequest: () -> Unit,
    context: Context,
    onConfirmButtonClick: () -> Unit = onDismissRequest
) {
    var requestPermission by rememberSaveable { mutableStateOf(false) }
    WarningAlertDialog(
        textString = stringResource(R.string.unable_sent_cause_no_permission) +
                "\n" + stringResource(R.string.please_grant_permission),
        onDismissRequest = { onDismissRequest() },
        onConfirmButtonClick = { requestPermission = true },
    )
    if (requestPermission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (requestNotificationPermission(context, true) {
                    requestPermission = !it
                    if (it) onConfirmButtonClick()
                }) requestPermission = false
        } else {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                addFlags(FLAG_ACTIVITY_NEW_TASK)
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                onConfirmButtonClick()
            } else onDismissRequest()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun NeedAlarmPermissionDialog(
    onDismissRequest: () -> Unit,
    context: Context
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

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun requestNotificationPermission(
    context: Context, tryForce: Boolean = false, ifGranted: (isGranted: Boolean) -> Unit = {}
): Boolean {
    var actionFinished by remember { mutableStateOf(false) }
    val openSetting = {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            addFlags(FLAG_ACTIVITY_NEW_TASK)
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        context.startActivity(intent)
        actionFinished = true
    }
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            if (tryForce) openSetting() else {
                Toast.makeText(
                    context, context.getString(R.string.no_set_reminder_permission) + " " +
                            context.getString(R.string.please_grant_permission), Toast.LENGTH_SHORT
                ).show()
                actionFinished = true
            }
        } else {
            ifGranted(true)
            actionFinished = true
        }
    }
    if (tryForce) {
        val state = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        ifGranted(state)
    }
    LaunchedEffect(Unit) {
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    return actionFinished
}
