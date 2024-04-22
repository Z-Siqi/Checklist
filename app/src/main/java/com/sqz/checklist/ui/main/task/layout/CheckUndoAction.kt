package com.sqz.checklist.ui.main.task.layout

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqz.checklist.R
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import kotlinx.coroutines.delay

@Composable
internal fun CheckUndoAction(
    lazyState: LazyListState,
    taskState: TaskLayoutViewModel
) {
    var undoButton by rememberSaveable { mutableStateOf(false) }
    var rememberScroll by rememberSaveable { mutableIntStateOf(0) }
    var rememberTime by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(true) {
        delay(50)
        rememberScroll = lazyState.firstVisibleItemIndex
        undoButton = true
        delay(2000)
        if (rememberScroll != lazyState.firstVisibleItemIndex) {
            undoButton = false
            taskState.checkTaskAction = false
        } else {
            while (rememberTime < 7) {
                delay(500)
                if (rememberScroll != lazyState.firstVisibleItemIndex) {
                    undoButton = false
                    taskState.checkTaskAction = false
                }
                rememberTime++
            }
            undoButton = false
            taskState.checkTaskAction = false
        }
    }
    if (undoButton) UndoButton(onClick = {
        taskState.undoTaskAction = true
        rememberScroll = 0
        undoButton = false
        taskState.checkTaskAction = false
    })
}


@Composable
private fun UndoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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