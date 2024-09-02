package com.sqz.checklist.ui.reminder

import android.content.Context
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import com.sqz.checklist.ui.material.WarningAlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Processing set and cancel reminder **/
@Composable
fun ReminderAction(
    reminderCard: Int, reminderCardClose: (set: Int) -> Unit,
    setReminder: Int, setReminderDone: (set: Int) -> Unit,
    context: Context, view: View,
    taskState: TaskLayoutViewModel,
    coroutineScope: CoroutineScope
) {
    // cancel set reminder when checked
    if (taskState.cancelReminderAction) {
        taskState.cancelReminder(context = context, cancelHistory = true)
        taskState.cancelReminderAction = false
    }
    if (setReminder > 0) { // to set reminder
        TimeSelectDialog(
            onDismissRequest = {
                setReminderDone(-1)
                view.playSoundEffect(SoundEffectConstants.CLICK)
            },
            onConfirmClick = { timeInMilli ->
                coroutineScope.launch {
                    taskState.setReminder(timeInMilli, TimeUnit.MILLISECONDS, setReminder, context)
                    setReminderDone(-1)
                }
                view.playSoundEffect(SoundEffectConstants.CLICK)
            },
            onFailed = { setReminderDone(-1) },
            context = context
        )
    }
    if (reminderCard > 0) { // processing cancel reminder
        WarningAlertDialog(
            onDismissRequest = { reminderCardClose(-1) },
            onConfirmButtonClick = {
                taskState.cancelReminder(reminderCard, context)
                reminderCardClose(-1)
            },
            onDismissButtonClick = { reminderCardClose(-1) },
            text = {
                var remindTime by rememberSaveable { mutableLongStateOf(0) }
                LaunchedEffect(true) {
                    val uuidAndTime = MainActivity.taskDatabase.taskDao()
                        .getReminderInfo(reminderCard)
                    uuidAndTime?.let {
                        val parts = it.split(":")
                        if (parts.size >= 2) {
                            parts[0]
                            val time = parts[1].toLong()
                            remindTime = time
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.cancel_the_reminder),
                    fontSize = (15 - 1).sp
                )
                val fullDateShort = stringResource(R.string.full_date_short)
                val formatter = remember { SimpleDateFormat(fullDateShort, Locale.getDefault()) }
                Text(
                    text = stringResource(
                        R.string.remind_at,
                        formatter.format(remindTime)
                    ),
                    fontSize = 15.sp
                )
            }
        )
    }
}
