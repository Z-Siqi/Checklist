package com.sqz.checklist.ui.common.unit

import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sqz.checklist.database.DatabaseRepository

/** Android 15 and above return true **/
val isApi35AndAbove = Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE

/** Android 10 and above return true **/
val isApi29AndAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

/** check the reminder is set or not **/
@Composable
fun DatabaseRepository.reminderState(reminder: Int?): Boolean {
    var state by remember { mutableStateOf(false) }
    LaunchedEffect(this.getIsRemindedNum(true)) {
        if (reminder != 0 && reminder != null) {
            try {
                state = !(this@reminderState.getReminderData(reminder)?.isReminded ?: true)
            } catch (e: Exception) {
                if (e.message != "The coroutine scope left the composition") Log.w(
                    "Exception: TaskItem", "$reminder, err: $e"
                )
            }
        } else state = false
    }
    return state
}
