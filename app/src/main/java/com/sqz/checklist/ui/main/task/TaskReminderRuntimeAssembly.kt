package com.sqz.checklist.ui.main.task

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.sqz.checklist.MainActivity
import com.sqz.checklist.common.AndroidNotificationScheduler
import com.sqz.checklist.notification.NotifyManager
import com.sqz.checklist.presentation.reminder.runtime.ReminderNotificationController
import sqz.checklist.data.database.repository.reminder.TaskReminderRepository
import sqz.checklist.reminder.api.reminderRuntimeProvider

@Composable
internal fun rememberTaskReminderNotificationController(
    notifyManager: NotifyManager = remember { NotifyManager() },
): ReminderNotificationController {
    val context = LocalView.current.context
    return remember(context, notifyManager) {
        ReminderNotificationController(
            context = context,
            notifyManager = notifyManager,
            runtime = reminderRuntimeProvider(
                TaskReminderRepository.provider(MainActivity.taskDatabase),
                AndroidNotificationScheduler(
                    context, TaskReminderRepository.provider(MainActivity.taskDatabase)
                )
            ),
        )
    }
}
