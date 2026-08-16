package com.sqz.checklist.presentation.reminder.assign

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.common.AndroidNotificationScheduler
import com.sqz.checklist.notification.NotifyManager
import com.sqz.checklist.presentation.reminder.assign.scene.cancel.ReminderCancelUI
import com.sqz.checklist.presentation.reminder.assign.scene.permission.NotificationGrant
import com.sqz.checklist.presentation.reminder.assign.scene.permission.NotificationGrantUI
import com.sqz.checklist.presentation.reminder.assign.scene.set.ReminderSetUI
import com.sqz.checklist.ui.common.dialog.ProcessingDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sqz.checklist.data.database.repository.reminder.TaskReminderRepository
import sqz.checklist.data.preferences.PrimaryPreferences
import sqz.checklist.reminder.api.assign.ReminderAssign

@Composable
fun ReminderLayout(
    taskId: Long,
    allowDismiss: Boolean,
    context: Context,
    viewModel: ReminderAssignViewModel = viewModelFactory(context),
    onFinished: () -> Unit,
) {
    val preferences = PrimaryPreferences(context)
    val view = androidx.compose.ui.platform.LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val notifyManager = remember { NotifyManager() }

    val reminderAction by viewModel.currentAction.collectAsState()
    val isSubmitting = rememberSaveable { mutableStateOf(false) }

    when (reminderAction) {
        null -> {
            LaunchedEffect(Unit) {
                viewModel.setAction(
                    taskId = taskId,
                    currentPermissionState = notifyManager.checkPermissions(context),
                )
            }
        }

        ReminderAssign.Action.Error -> {
            NotificationGrantUI(
                mode = NotificationGrant.Diagnosis,
                onFailed = { onFinished() },
                onGrant = { viewModel.resetAction() },
                notifyManager = notifyManager,
                context = context,
            )
        }

        ReminderAssign.Action.Set -> {
            ReminderSetUI(
                allowDialogDismiss = allowDismiss,
                view = view,
                notifyManager = notifyManager,
                onDismissRequest = onFinished,
                onConfirm = { remindAtMillis ->
                    isSubmitting.value = true
                    coroutineScope.launch {
                        viewModel.setReminder(remindAtMillis)
                        isSubmitting.value = false
                        onFinished()
                    }
                },
            )
        }

        ReminderAssign.Action.Cancel -> {
            ReminderCancelUI(
                currentReminder = viewModel.currentReminder.collectAsState().value,
                onDismissRequest = onFinished,
                onConfirm = {
                    isSubmitting.value = true
                    coroutineScope.launch {
                        viewModel.cancelReminder()
                        isSubmitting.value = false
                        onFinished()
                    }
                },
            )
        }

        ReminderAssign.Action.NoPermission -> {
            NotificationGrantUI(
                mode = NotificationGrant.Request,
                onFailed = { onFinished() },
                onGrant = { viewModel.resetAction() },
                notifyManager = notifyManager,
                context = context,
            )
        }
    }

    val noFullPermissionShowed = rememberSaveable { mutableStateOf(false) }
    if (reminderAction == ReminderAssign.Action.Set && !noFullPermissionShowed.value) {
        val noFullPermission by viewModel.noFullPermission.collectAsState()
        LaunchedEffect(Unit) {
            if (!noFullPermission || preferences.disableNoScheduleExactAlarmNotice()) {
                return@LaunchedEffect
            }
            Toast.makeText(
                context,
                R.string.no_SCHEDULE_EXACT_ALARM_permission_explain,
                Toast.LENGTH_SHORT
            ).show()
            noFullPermissionShowed.value = true
        }
    }

    if (isSubmitting.value || reminderAction == null) {
        var delayedShow by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!delayedShow) {
                delay(168)
                delayedShow = true
            }
        }
        if (delayedShow) ProcessingDialog(processText = null)
    }
}

@Composable
private fun viewModelFactory(context: Context): ReminderAssignViewModel {
    val taskReminderRepository = TaskReminderRepository.provider(MainActivity.taskDatabase)
    val scheduler = AndroidNotificationScheduler(
        context = context,
        taskReminder = TaskReminderRepository.provider(MainActivity.taskDatabase)
    )
    return viewModel {
        ReminderAssignViewModel(taskReminderRepository, scheduler)
    }
}
