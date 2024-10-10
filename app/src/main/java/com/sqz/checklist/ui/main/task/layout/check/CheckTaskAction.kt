package com.sqz.checklist.ui.main.task.layout.check

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqz.checklist.R
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import kotlinx.coroutines.delay

@Composable
fun CheckTaskAction(
    whenUndo: () -> Unit,
    taskState: TaskLayoutViewModel,
    lazyState: LazyListState,
    context: Context,
) {
    val undo = taskState.undo.collectAsState().value
    val isWindowFocused = LocalWindowInfo.current.isWindowFocused
    if (taskState.undoTimeout(lazyState, context)) UndoButton(
        onClick = {
            taskState.changeTaskVisibility(
                undo.undoActionId,
                undoToHistory = true,
                context = context
            )
            whenUndo()
        }) else LaunchedEffect(true) { // processing after checked
        delay(100)
        taskState.autoDeleteHistoryTask(5)
        taskState.remindedState(autoDel = true) // delete reminder info which 12h ago
        Log.d("TaskLayout", "Auto del history tasks & del reminder info that 12h ago")
    }
    if (!isWindowFocused && undo.undoButtonState) {
        taskState.cancelReminder(reminder = null, context = context, cancelHistory = true)
    }
}

@Composable
private fun UndoButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
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
