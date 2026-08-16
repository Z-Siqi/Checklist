package com.sqz.checklist.presentation.reminder.assign.scene.cancel

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.sqz.checklist.R
import com.sqz.checklist.ui.common.dialog.WarningAlertDialog
import sqz.checklist.data.database.model.ReminderViewData
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ReminderCancelUI(
    currentReminder: ReminderViewData?,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (currentReminder == null) {
        throw IllegalStateException("ReminderCancelUI failed to load due to data not existed!")
    }
    WarningAlertDialog(
        onDismissRequest = onDismissRequest,
        onConfirmButtonClick = onConfirm,
        onDismissButtonClick = onDismissRequest,
        text = {
            Text(
                text = stringResource(R.string.cancel_the_reminder),
                fontSize = 14.sp,
            )
            val fullDateShort = stringResource(R.string.full_date_short)
            val formatter = remember { SimpleDateFormat(fullDateShort, Locale.getDefault()) }
            Text(
                text = stringResource(
                    R.string.remind_at,
                    formatter.format(currentReminder.reminder.reminderTime),
                ),
                fontSize = 15.sp,
            )
        },
    )
}
