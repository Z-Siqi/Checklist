package com.sqz.checklist.ui.main.task.layout

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.database.Task
import com.sqz.checklist.ui.NavBar
import com.sqz.checklist.ui.TopBar
import com.sqz.checklist.ui.main.NavTooltipContent
import com.sqz.checklist.ui.main.OnClickType
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import com.sqz.checklist.ui.material.TaskChangeContentCard
import com.sqz.checklist.ui.reminder.ReminderAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

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
    isRemindedItem: List<Task> = taskState.remindedState(load = true),
    inSearchItem: List<Task> = taskState.updateInSearch()
) {
    val lazyState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topBarState)
    val tooltipState = rememberTooltipState(isPersistent = true)

    var taskAddCard by rememberSaveable { mutableStateOf(false) }
    var menu by rememberSaveable { mutableStateOf(false) }
    var searchState by rememberSaveable { mutableStateOf(false) }

    Scaffold(modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {
        NavTooltipContent(textRid = R.string.task_history, onClickToTaskHistory = {
            taskState.checkTaskAction = false
            menu = false
            toTaskHistory()
        }, onDismissRequest = { menu = false }, expanded = menu, view = view)
        TopBar(scrollBehavior, topBarState, onClick = { menu = true }, view)
    }, bottomBar = {
        val buttonInfo = stringResource(if (!searchState) R.string.add else R.string.cancel)
        val screenHeight = LocalConfiguration.current.screenHeightDp
        val canScroll = remember {
            derivedStateOf { lazyState.layoutInfo.totalItemsCount * 120 > screenHeight }
        }
        NavBar(icon = {
            val icons = if (!searchState) Icons.Filled.AddCircle else Icons.Filled.Close
            Icon(icons, contentDescription = buttonInfo)
        }, label = { Text(buttonInfo) }, tooltipContent = {
            if (canScroll.value && !searchState) NavTooltipContent(
                onClickType = { onClickType ->
                    when (onClickType) {
                        OnClickType.Search -> {
                            tooltipState.dismiss()
                            searchState = true
                        }

                        OnClickType.ScrollUp -> coroutineScope.launch {
                            tooltipState.dismiss()
                            lazyState.animateScrollToItem(0)
                        }

                        OnClickType.ScrollDown -> coroutineScope.launch {
                            tooltipState.dismiss()
                            lazyState.animateScrollToItem(lazyState.layoutInfo.totalItemsCount)
                        }
                    }
                },
                view = view,
                scrollUp = !lazyState.canScrollForward
            ) else PlainTooltip {
                Text(text = buttonInfo)
                LaunchedEffect(true) { // click feedback
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ContextCompat.getSystemService(
                        context, Vibrator::class.java
                    )?.vibrate(
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    ) else view.playSoundEffect(SoundEffectConstants.CLICK)
                }
            }
        }, tooltipState = tooltipState, onClick = {
            if (!searchState) taskAddCard = true else {
                searchState = false
                taskState.updateInSearch(reset = true)
            }
            view.playSoundEffect(SoundEffectConstants.CLICK)
        })
    }) { paddingValues ->
        var undoTask by rememberSaveable { mutableStateOf(false) }
        var reminderCard by rememberSaveable { mutableIntStateOf(-1) }
        var setReminder by rememberSaveable { mutableIntStateOf(-1) }
        Surface(
            modifier = modifier.padding(paddingValues),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            LazyList( // LazyColumn lists
                item = item, pinnedItem = pinnedItem,
                isRemindedItem = isRemindedItem, inSearchItem = inSearchItem,
                lazyState = lazyState,
                reminderCard = { reminderCard = it },
                setReminder = { setReminder = it },
                undoTask = { state ->
                    if (undoTask) coroutineScope.launch {
                        state.reset()
                        undoTask = false
                    }
                },
                isInSearch = searchState,
                context = context
            )
            if (searchState) { // Search function
                val textFieldState = rememberTextFieldState()
                Column(modifier = modifier.fillMaxSize()) {
                    OutlinedCard(
                        modifier = modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 18.dp, top = 12.dp)
                            .height(50.dp),
                        shape = ShapeDefaults.ExtraLarge
                    ) {
                        BasicTextField(
                            modifier = modifier
                                .fillMaxSize()
                                .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            state = textFieldState,
                            lineLimits = TextFieldLineLimits.SingleLine,
                            textStyle = TextStyle(
                                fontSize = 24.sp,
                                textAlign = TextAlign.Start
                            )
                        )
                        var oldText by remember { mutableStateOf("") }
                        if (textFieldState.text.toString() != oldText || taskState.undoTaskAction) {
                            LaunchedEffect(key1 = true) {
                                taskState.updateInSearch(textFieldState.text.toString())
                                oldText = textFieldState.text.toString()
                            }
                        }
                    }
                }
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
                            fontWeight = FontWeight.Medium, fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.outline,
                            lineHeight = 30.sp, textAlign = TextAlign.Center
                        )
                    }
                } else LaunchedEffect(true) {
                    delay(800)
                    delayed = true
                }
            }
            if (taskState.checkTaskAction) { // ture if task is checked
                CheckUndoAction(lazyState, taskState)
                taskState.updateInSearch(reset = true)
            } else LaunchedEffect(true) { // processing after checked
                delay(100)
                taskState.autoDeleteHistoryTask(5)
                taskState.remindedState(autoDel = true) // delete reminder info which 12h ago
                Log.d("TaskLayout", "Auto del history tasks & del reminder info that 12h ago")
            }
            if (taskState.undoTaskAction) { // processing undo
                taskState.changeTaskVisibility(taskState.undoActionId, undoToHistory = true)
                undoTask = true
                taskState.undoTaskAction = false
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
        ReminderAction(
            reminderCard = reminderCard,
            reminderCardClose = { reminderCard = it },
            setReminder = setReminder,
            setReminderDone = { setReminder = it },
            context = context,
            view = view,
            taskState = taskState,
            coroutineScope = coroutineScope
        )
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
