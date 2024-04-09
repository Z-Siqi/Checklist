package com.sqz.checklist.ui.mainLayout.taskLayout

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.database.Task
import com.sqz.checklist.ui.NavBar
import com.sqz.checklist.ui.TopBar
import com.sqz.checklist.ui.mainLayout.TaskChangeContentCard
import com.sqz.checklist.ui.mainLayout.WarningAlertDialog
import com.sqz.checklist.ui.material.TimeSelectDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaskLayout(
    toTaskHistory: () -> Unit,
    modifier: Modifier = Modifier,
    taskState: TaskLayoutViewModel = viewModel(),
    item: List<Task> = taskState.loadTaskData(MainActivity.taskDatabase.taskDao())
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topBarState)

    var taskAddCard by rememberSaveable { mutableStateOf(false) }
    var menu by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp, end = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Menu(expanded = menu, onDismissRequest = { menu = false }, onClickToTaskHistory = {
                    taskState.checkTaskAction = false
                    menu = false
                    toTaskHistory()
                })
            }
            TopBar(scrollBehavior, topBarState, onClick = { menu = true })
        },
        bottomBar = {
            val add = stringResource(R.string.add)
            NavBar(
                icon = { Icon(Icons.Filled.AddCircle, contentDescription = add) },
                label = { Text(add) },
                onClick = { taskAddCard = true }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = modifier.padding(paddingValues),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            var undoTask by rememberSaveable { mutableStateOf(false) }
            val lazyState = rememberLazyListState()
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                state = lazyState
            ) {
                item {
                    Spacer(modifier = modifier.height(20.dp))
                }
                items(item, key = { it.id }) {
                    val screenWidthPx =
                        LocalConfiguration.current.screenWidthDp * LocalDensity.current.density
                    val state = rememberSwipeToDismissBoxState(
                        positionalThreshold = {
                            screenWidthPx * 0.38f
                        },
                    )
                    TaskItem(
                        id = it.id,
                        description = it.description,
                        createDate = it.createDate,
                        context = context,
                        itemState = state
                    )
                    if (undoTask) {
                        LaunchedEffect(true) {
                            coroutineScope.launch {
                                state.reset()
                                undoTask = false
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = modifier.height(10.dp))
                }
            }
            if (taskState.checkTaskAction) { // ture if task is checked
                CheckUndoAction(lazyState, taskState)
            }
            if (!taskState.checkTaskAction) { // processing after checked
                LaunchedEffect(true) {
                    taskState.autoDeleteHistoryTask(5)
                }
            }
            if (taskState.reminderCard) { // processing cancel reminder
                fun dismiss() {
                    taskState.reminderCard = false
                    taskState.setReminderId = -0
                }
                WarningAlertDialog(
                    onDismissRequest = { dismiss() },
                    onConfirmButtonClick = {
                        taskState.cancelReminder(taskState.setReminderId, context)
                        dismiss()
                    },
                    onDismissButtonClick = { dismiss() },
                    text = {
                        var remindTime by rememberSaveable { mutableLongStateOf(0) }
                        LaunchedEffect(true) {
                            val uuidAndTime = MainActivity.taskDatabase.taskDao()
                                .getReminderInfo(taskState.setReminderId)
                            uuidAndTime?.let {
                                val parts = it.split(":")
                                if (parts.size >= 2) {
                                    parts[0]
                                    val time = parts[1].toLong()
                                    remindTime = time
                                }
                            }
                        }
                        Text(text = stringResource(R.string.cancel_the_reminder))
                        val fullDateShort = stringResource(R.string.full_date_short)
                        val formatter = remember {
                            SimpleDateFormat(fullDateShort, Locale.getDefault())
                        }
                        Text(
                            text = stringResource(
                                R.string.remind_at,
                                formatter.format(remindTime)
                            )
                        )
                    }
                )
            }
            if (taskState.undoTaskAction) { // processing undo
                taskState.undoTaskToHistory(taskState.undoActionId)
                undoTask = true
                taskState.undoTaskAction = false
            }
        }
    }
    val state = rememberTextFieldState() // to add task
    val noDoNothing = stringResource(R.string.no_do_nothing)
    if (taskAddCard) TaskChangeContentCard(
        onDismissRequest = { taskAddCard = false },
        confirm = {
            if (state.text.toString() != "") {
                taskState.insertTask(state.text.toString())
                taskAddCard = false
            } else {
                Toast.makeText(context, noDoNothing, Toast.LENGTH_SHORT).show()
            }
        },
        state = state,
        title = stringResource(R.string.create_task),
        confirmText = stringResource(R.string.add),
        doneImeAction = true
    ) else LaunchedEffect(true) {
        state.clearText()
    }
    if (taskState.setReminderState) { // to set reminder
        TimeSelectDialog(
            onDismissRequest = {
                taskState.setReminderId = -1
                taskState.setReminderState = false
            },
            onConfirmClick = { timeInMilli ->
                coroutineScope.launch {
                    taskState.setReminder(
                        timeInMilli,
                        TimeUnit.MILLISECONDS,
                        taskState.setReminderId,
                        context
                    )
                    taskState.setReminderId = -1
                    taskState.setReminderState = false
                }
            },
            onFailed = {
                taskState.setReminderId = -1
                taskState.setReminderState = false
            },
            context = context
        )
    }
}

@Composable
private fun CheckUndoAction(
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

@Composable
private fun Menu(
    expanded: Boolean = false,
    onDismissRequest: () -> Unit,
    onClickToTaskHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(verticalArrangement = Arrangement.Top) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier
        ) {
            DropdownMenuItem(
                onClick = onClickToTaskHistory,
                text = {
                    Text(text = stringResource(R.string.task_history))
                }
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    val item = listOf(Task(0, "The quick brown fox jumps over the lazy dog.", LocalDate.now()))
    TaskLayout({}, item = item)
}
