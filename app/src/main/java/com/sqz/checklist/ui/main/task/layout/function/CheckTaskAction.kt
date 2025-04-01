package com.sqz.checklist.ui.main.task.layout.function

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqz.checklist.R
import com.sqz.checklist.preferences.PrimaryPreferences
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import kotlinx.coroutines.delay

@Composable
fun CheckTaskAction(
    whenUndo: () -> Unit,
    taskState: TaskLayoutViewModel,
    lazyState: LazyListState,
    context: Context,
) {
    val preferences = PrimaryPreferences(context).allowedNumberOfHistory()
    val undo = taskState.undo.collectAsState().value
    val isWindowFocused = LocalWindowInfo.current.isWindowFocused
    if (taskState.undoButtonProcess(lazyState, context)) UndoButton(
        onClick = {
            taskState.modifyHandler.onTaskUndoChecked(undo.undoActionId).let {
                if (it.isActive || it.isCompleted || it.isCancelled) {
                    taskState.resetUndo(context)
                }
            }
            whenUndo()
        }) else LaunchedEffect(true) { // processing after checked
        delay(100)
        if (preferences != 21) taskState.autoDeleteHistoryTask(preferences)
        taskState.autoDeleteRemindedTaskInfo(context) // delete reminder info
        Log.d("TaskLayout", "Auto del history tasks & del reminder info")
    }
    if (!isWindowFocused && undo.undoButtonState) {
        taskState.reminderHandler.cancelHistoryReminder(context = context)
    }
}

@Composable
private fun UndoButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val localConfig = LocalConfiguration.current
    val screenIsWidth = localConfig.screenWidthDp > localConfig.screenHeightDp * 1.2
    val safeBottomForFullscreen =
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE && screenIsWidth
        ) modifier.windowInsetsPadding(WindowInsets.navigationBars) else modifier
    Box(modifier = modifier.fillMaxSize() then safeBottomForFullscreen) {
        FloatingActionButton(
            modifier = modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.secondary
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(R.drawable.undo),
                    contentDescription = stringResource(R.string.undo),
                    tint = MaterialTheme.colorScheme.onSecondary
                )
                Text(
                    text = stringResource(R.string.undo),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondaryContainer
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    UndoButton({})
}
