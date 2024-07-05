package com.sqz.checklist.ui.main.task.layout

import android.content.Context
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.database.Task
import com.sqz.checklist.ui.NavBar
import com.sqz.checklist.ui.TopBar
import com.sqz.checklist.ui.main.NavTooltipContent
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import com.sqz.checklist.ui.main.task.layout.item.TaskData
import com.sqz.checklist.ui.main.task.layout.item.TaskItem
import com.sqz.checklist.ui.material.TaskChangeContentCard
import com.sqz.checklist.ui.material.TimeSelectDialog
import com.sqz.checklist.ui.material.WarningAlertDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Top layout of TaskLayout.kt
 **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskLayout(
    toTaskHistory: () -> Unit,
    context: Context, view: View,
    modifier: Modifier = Modifier,
    taskState: TaskLayoutViewModel = viewModel(),
    item: List<Task> = taskState.loadTaskData(MainActivity.taskDatabase.taskDao()),
    pinnedItem: List<Task> = taskState.pinState(load = true),
    isRemindedItem: List<Task> = taskState.remindedState(load = true)
) {
    val lazyState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topBarState)
    val tooltipState = rememberTooltipState(isPersistent = true)

    var taskAddCard by rememberSaveable { mutableStateOf(false) }
    var menu by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NavTooltipContent(textRid = R.string.task_history, onClickToTaskHistory = {
                taskState.checkTaskAction = false
                menu = false
                toTaskHistory()
            }, onDismissRequest = { menu = false }, expanded = menu, view = view)
            TopBar(scrollBehavior, topBarState, onClick = { menu = true }, view)
        },
        bottomBar = {
            val add = stringResource(R.string.add)
            NavBar(
                icon = { Icon(Icons.Filled.AddCircle, contentDescription = add) },
                label = { Text(add) },
                tooltipContent = {
                    NavTooltipContent(
                        onScrollClick = {
                            coroutineScope.launch {
                                tooltipState.dismiss()
                                lazyState.animateScrollToItem(lazyState.layoutInfo.totalItemsCount)
                            }
                        },
                        view = view,
                    )
                },
                tooltipState = tooltipState,
                onClick = {
                    taskAddCard = true
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = modifier.padding(paddingValues),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            var undoTask by rememberSaveable { mutableStateOf(false) }
            var reminderCard by rememberSaveable { mutableIntStateOf(-1) }
            var setReminder by rememberSaveable { mutableIntStateOf(-1) }

            LazyList(
                item = item,
                pinnedItem = pinnedItem,
                isRemindedItem = isRemindedItem,
                lazyState = lazyState,
                reminderCard = { reminderCard = it },
                setReminder = { setReminder = it },
                undoTask = { state ->
                    if (undoTask) coroutineScope.launch {
                        state.reset()
                        undoTask = false
                    }
                },
                context = context
            )

            if (taskState.checkTaskAction) { // ture if task is checked
                CheckUndoAction(lazyState, taskState)
            } else LaunchedEffect(true) { // processing after checked
                delay(100)
                taskState.autoDeleteHistoryTask(5)
                taskState.remindedState() // delete reminder info which 12h ago
            }
            // cancel set reminder when checked
            if (taskState.cancelReminderAction) {
                taskState.cancelHistoryReminder(context)
                taskState.cancelReminderAction = false
            }
            if (setReminder > 0) { // to set reminder
                TimeSelectDialog(
                    onDismissRequest = {
                        setReminder = -1
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                    },
                    onConfirmClick = { timeInMilli ->
                        coroutineScope.launch {
                            taskState.setReminder(
                                timeInMilli,
                                TimeUnit.MILLISECONDS,
                                setReminder,
                                context
                            )
                            setReminder = -1
                        }
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                    },
                    onFailed = { setReminder = -1 },
                    context = context
                )
            }
            if (reminderCard > 0) { // processing cancel reminder
                WarningAlertDialog(
                    onDismissRequest = { reminderCard = -1 },
                    onConfirmButtonClick = {
                        taskState.cancelReminder(reminderCard, context)
                        reminderCard = -1
                    },
                    onDismissButtonClick = { reminderCard = -1 },
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
                        Text(text = stringResource(R.string.cancel_the_reminder))
                        val fullDateShort = stringResource(R.string.full_date_short)
                        val formatter = remember {
                            SimpleDateFormat(fullDateShort, Locale.getDefault())
                        }
                        Text(stringResource(R.string.remind_at, formatter.format(remindTime)))
                    }
                )
            }
            if (taskState.undoTaskAction) { // processing undo
                taskState.undoTaskToHistory(taskState.undoActionId)
                undoTask = true
                taskState.undoTaskAction = false
            }
            if (item.isEmpty()) { // Show text if not any task
                var delayed by rememberSaveable { mutableStateOf(false) }
                if (delayed) {
                    Column(
                        modifier = modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.nothing_need_do),
                            fontWeight = FontWeight.Medium,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.outline,
                            lineHeight = 30.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else LaunchedEffect(true) {
                    delay(800)
                    delayed = true
                }
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
}

/**
 * List of task
 **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LazyList(
    item: List<Task>,
    pinnedItem: List<Task>,
    isRemindedItem: List<Task>,
    lazyState: LazyListState,
    reminderCard: (Int) -> Unit,
    setReminder: (Int) -> Unit,
    undoTask: (state: SwipeToDismissBoxState) -> Unit,
    context: Context,
    modifier: Modifier = Modifier,
) {
    val screenWidthPx = LocalConfiguration.current.screenWidthDp * LocalDensity.current.density
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = lazyState
    ) {
        if (isRemindedItem.isNotEmpty()) {
            item {
                val remindedHeight = (35 + (120 * isRemindedItem.size)).dp
                val animatedRemindedHeight by animateDpAsState(
                    targetValue = remindedHeight,
                    label = "Reminded Height"
                )
                Spacer(modifier = modifier.height(10.dp))
                OutlinedCard(
                    modifier = modifier
                        .height(animatedRemindedHeight)
                        .padding(start = 8.dp, end = 8.dp),
                    shape = ShapeDefaults.Large,
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Text(
                        text = "The task just reminded",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = modifier.padding(start = 9.dp, top = 5.dp, bottom = 5.dp)
                    )
                    LazyColumn {
                        items(isRemindedItem, key = { it.id }) {
                            val state = rememberSwipeToDismissBoxState(
                                positionalThreshold = {
                                    screenWidthPx * 0.35f
                                },
                            )
                            TaskItem(
                                taskData = TaskData(
                                    it.id, it.description, it.createDate, it.reminder
                                ),
                                reminderCardClick = { id -> reminderCard(id) },
                                setReminderClick = { id -> setReminder(id) },
                                isPin = it.isPin, context = context, itemState = state,
                                pinnedTask = true
                            )
                        }
                    }
                }
            }
        }
        if (pinnedItem.isNotEmpty()) {
            item {
                val pinnedHeight = (35 + (120 * pinnedItem.size)).dp
                val animatedPinnedHeight by animateDpAsState(
                    targetValue = pinnedHeight,
                    label = "Pinned Height"
                )
                Spacer(modifier = modifier.height(10.dp))
                OutlinedCard(
                    modifier = modifier
                        .height(animatedPinnedHeight)
                        .padding(start = 8.dp, end = 8.dp),
                    shape = ShapeDefaults.Large,
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Text(
                        text = stringResource(R.string.pinned_task),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = modifier.padding(start = 9.dp, top = 5.dp, bottom = 5.dp)
                    )
                    LazyColumn {
                        items(pinnedItem, key = { it.id }) {
                            val state = rememberSwipeToDismissBoxState(
                                positionalThreshold = {
                                    screenWidthPx * 0.35f
                                },
                            )
                            TaskItem(
                                taskData = TaskData(
                                    it.id, it.description, it.createDate, it.reminder
                                ),
                                reminderCardClick = { id -> reminderCard(id) },
                                setReminderClick = { id -> setReminder(id) },
                                isPin = it.isPin, context = context, itemState = state,
                                pinnedTask = true
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = modifier.height(20.dp)) }
        items(item, key = { it.id }) {
            val state = rememberSwipeToDismissBoxState(
                positionalThreshold = {
                    screenWidthPx * 0.35f
                },
            )
            TaskItem(
                taskData = TaskData(it.id, it.description, it.createDate, it.reminder),
                reminderCardClick = { id -> reminderCard(id) },
                setReminderClick = { id -> setReminder(id) },
                isPin = it.isPin, context = context, itemState = state
            )
            undoTask(state)
        }
        item { Spacer(modifier = modifier.height(10.dp)) }
    }
}

@Preview
@Composable
private fun Preview() {
    val item = listOf(Task(0, "The quick brown fox jumps over the lazy dog.", LocalDate.now()))
    TaskLayout(
        {}, LocalContext.current, LocalView.current,
        item = item, pinnedItem = item, isRemindedItem = item
    )
}
