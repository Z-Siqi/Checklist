package com.sqz.checklist.ui.reminder

import android.content.Context
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.sqz.checklist.R
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
    val state: Boolean = false,
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
    val resetState = { taskState.resetTaskData() }
    when (reminder.set) {
        ReminderActionType.Set -> TimeSelectDialog( // to set reminder
            onDismissRequest = {
                resetState()
                view.playSoundEffect(SoundEffectConstants.CLICK)
            },
            onConfirmClick = { timeInMilli ->
                coroutineScope.launch {
                    taskState.setReminder(timeInMilli, TimeUnit.MILLISECONDS, reminder.id, context)
                    resetState()
                }
                view.playSoundEffect(SoundEffectConstants.CLICK)
            },
            onFailed = { resetState() },
            context = context
        )

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
