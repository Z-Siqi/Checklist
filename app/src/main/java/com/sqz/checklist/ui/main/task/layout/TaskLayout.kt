package com.sqz.checklist.ui.main.task.layout

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sqz.checklist.R
import com.sqz.checklist.database.Task
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import com.sqz.checklist.ui.main.task.layout.check.CheckTaskAction
import com.sqz.checklist.ui.main.task.layout.item.EditState
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
    scrollBehavior: TopAppBarScrollBehavior,
    context: Context, view: View,
    modifier: Modifier = Modifier,
    taskState: TaskLayoutViewModel = viewModel(),
    listState: ListData = taskState.listState.collectAsState().value,
) {
    val lazyState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var undoTask by rememberSaveable { mutableStateOf(false) }
    val navConnector = taskState.navExtendedConnector.collectAsState().value
    NavBarConnectorAction(
        navConnector = navConnector,
        lazyState = lazyState,
        scrollBehavior = scrollBehavior,
        updateNavConnector = taskState::updateNavConnector
    )
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        LazyList( // LazyColumn lists
            listState = listState,
            lazyState = lazyState,
            undoTask = { state ->
                if (undoTask) coroutineScope.launch {
                    state.reset()
                    undoTask = false
                }
            },
            isInSearch = navConnector.searchState,
            context = context,
            taskState = taskState
        )
        TaskSearchBar( // Search function
            searchState = navConnector.searchState,
            taskState = taskState
        )
        if (listState.item.isEmpty()) { // Show text if not any task
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
        CheckTaskAction( // processing check & undo
            whenUndo = { undoTask = true },
            taskState = taskState,
            lazyState = lazyState,
            context = context
        )
    }
    EditTask(
        editState = taskState.taskData.collectAsState().value.editState,
        editTask = taskState::editTask,
        resetState = { taskState.resetTaskData() },
        context = context
    )
    ReminderAction(
        reminder = taskState.taskData.collectAsState().value.reminder,
        context = context,
        view = view,
        taskState = taskState,
        coroutineScope = coroutineScope
    )
}

@Composable
private fun EditTask(
    editState: EditState,
    editTask: (id: Int, edit: String) -> Unit,
    resetState: () -> Unit,
    context: Context
) {
    if (editState.state) {
        val textState = rememberTextFieldState()
        LaunchedEffect(true) {
            textState.clearText()
            textState.edit { insert(0, editState.description) }
        }
        val noChangeDoNothing = stringResource(R.string.no_change_do_nothing)
        TaskChangeContentCard(
            onDismissRequest = { resetState() },
            confirm = {
                if (textState.text.toString() != "") {
                    editTask(editState.id, textState.text.toString())
                    resetState()
                } else Toast.makeText(context, noChangeDoNothing, Toast.LENGTH_SHORT).show()
            },
            state = textState,
            title = stringResource(R.string.edit_task),
            confirmText = stringResource(R.string.edit),
            doneImeAction = true
        )
    }
}

@Composable
private fun TaskSearchBar(
    searchState: Boolean,
    taskState: TaskLayoutViewModel,
    modifier: Modifier = Modifier
) {
    val undo = taskState.undo.collectAsState().value
    if (searchState) Column(modifier = modifier.fillMaxSize()) {
        val textFieldState = rememberTextFieldState()
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
                    .padding(start = 9.dp, end = 9.dp, top = 10.dp, bottom = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                state = textFieldState,
                lineLimits = TextFieldLineLimits.SingleLine,
                textStyle = TextStyle(
                    fontSize = 24.sp,
                    textAlign = TextAlign.Start
                )
            )
            var oldText by remember { mutableStateOf("") }
            if (textFieldState.text.toString() != oldText || undo.checkTaskAction) {
                LaunchedEffect(key1 = true) {
                    taskState.searchingText = textFieldState.text.toString()
                    taskState.updateInSearch(taskState.searchingText)
                    oldText = textFieldState.text.toString()
                }
            } else if (textFieldState.text.toString().isEmpty()) LaunchedEffect(key1 = true) {
                taskState.updateInSearch(initWithAll = true)
            }
        }
    }
    if (searchState) BackHandler {
        taskState.updateNavConnector(
            NavExtendedConnectData(searchState = false),
            NavExtendedConnectData(searchState = true)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavBarConnectorAction(
    navConnector: NavExtendedConnectData,
    lazyState: LazyListState,
    scrollBehavior: TopAppBarScrollBehavior,
    updateNavConnector: (data: NavExtendedConnectData, updateSet: NavExtendedConnectData) -> Unit,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp
    LaunchedEffect(lazyState) {
        snapshotFlow { lazyState.layoutInfo.totalItemsCount * 120 > screenHeight }.collect {
            updateNavConnector(
                NavExtendedConnectData(canScroll = it),
                NavExtendedConnectData(canScroll = true)
            )
        }
    }
    LaunchedEffect(lazyState) {
        snapshotFlow { lazyState.canScrollForward }.collect {
            updateNavConnector(
                NavExtendedConnectData(canScrollForward = it),
                NavExtendedConnectData(canScrollForward = true)
            )
        }
    }
    if (navConnector.scrollToFirst) LaunchedEffect(true) {
        lazyState.animateScrollToItem(0)
        scrollBehavior.state.heightOffset = 0f
        updateNavConnector(
            NavExtendedConnectData(scrollToFirst = false),
            NavExtendedConnectData(scrollToFirst = true)
        )
    }
    if (navConnector.scrollToBottom) LaunchedEffect(true) {
        lazyState.animateScrollToItem(lazyState.layoutInfo.totalItemsCount)
        scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
        updateNavConnector(
            NavExtendedConnectData(scrollToBottom = false),
            NavExtendedConnectData(scrollToBottom = true)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview() {
    val item = listOf(Task(0, "The quick brown fox jumps over the lazy dog.", LocalDate.now()))
    TaskLayout(
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
        LocalContext.current, LocalView.current, listState = ListData(item, item, item)
    )
}
