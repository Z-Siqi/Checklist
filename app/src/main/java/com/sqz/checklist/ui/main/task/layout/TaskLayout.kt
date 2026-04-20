package com.sqz.checklist.ui.main.task.layout

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sqz.checklist.R
import com.sqz.checklist.common.AndroidEffectFeedback
import com.sqz.checklist.presentation.task.info.TaskInfoLayout
import com.sqz.checklist.presentation.task.info.TaskInfoState
import com.sqz.checklist.presentation.task.list.TaskListLayout
import com.sqz.checklist.presentation.task.list.TaskListState
import com.sqz.checklist.presentation.task.modify.TaskModifyLayout
import com.sqz.checklist.presentation.task.modify.TaskModifyState
import com.sqz.checklist.ui.common.unit.screenIsWidthAndAPI34Above
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import com.sqz.checklist.ui.main.task.TaskLayoutViewModelPreview
import com.sqz.checklist.ui.main.task.layout.function.ReminderHandlerListener
import com.sqz.checklist.ui.theme.Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sqz.checklist.data.preferences.PrimaryPreferences
import kotlin.time.ExperimentalTime

/**
 * Top layout of TaskLayout.kt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskLayout(
    scrollBehavior: TopAppBarScrollBehavior,
    context: Context,
    view: View,
    refreshListRequest: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    taskState: TaskLayoutViewModel = viewModel(),
) {
    val colors = Theme.color
    val lazyState = rememberLazyListState(
        navConnector = taskState.navExtendedConnector.collectAsState().value,
        scrollBehavior = scrollBehavior,
        updateNavConnector = taskState::updateNavConnector
    )
    val coroutineScope = rememberCoroutineScope()
    Surface(
        modifier = modifier,
        color = colors.backgroundColor
    ) {
        val left = WindowInsets.displayCutout.asPaddingValues()
            .calculateLeftPadding(LocalLayoutDirection.current)
        val safePaddingForFullscreen = if (screenIsWidthAndAPI34Above()) modifier.padding(
            start = left, end = if (left / 3 > 15.dp) 15.dp else left / 3
        ) else modifier

        TaskListLayout(
            refreshListRequest = refreshListRequest,
            requestSearch = taskState.onSearchRequest.collectAsState().value,
            lazyListState = lazyState,
            config = taskState.listConfig,
            view = view,
            externalRequest = {
                //TODO move to viewModel
                when (it) {
                    is TaskListState.SearchProcessed -> {
                        taskState.onResetSearchRequest()
                    }

                    is TaskListState.Edit -> {
                        taskState.requestModify(TaskModifyState.EditTask(it.taskId))
                    }

                    is TaskListState.Reminder -> {
                        taskState.reminderHandler.requestReminder(it.taskId)
                    }

                    is TaskListState.RemoveReminded -> {
                        taskState.onCloseNotification(it.taskId, view.context)
                    }

                    is TaskListState.Detail -> {
                        val state = TaskInfoState(
                            taskId = it.taskId,
                            config = TaskInfoState.Config.DetailOnly
                        )
                        taskState.requestTaskInfo(state)
                    }

                    is TaskListState.Info -> {
                        val state = TaskInfoState(
                            taskId = it.taskId,
                            config = TaskInfoState.Config.TaskOnly(
                                pinChangeAllowed = false
                            )
                        )
                        taskState.requestTaskInfo(state)
                    }
                }
            },
            modifier = safePaddingForFullscreen.fillMaxSize()
        )
        taskState.isTaskInfo.collectAsState().value?.let {
            TaskInfoLayout(
                state = it,
                onFinished = { taskState.requestTaskInfo(null) },
                feedback = AndroidEffectFeedback(view),
                modifier = modifier
            )
        }
    }
    val taskCreatedToast = remember { mutableStateOf(false) }
    taskState.isModify.collectAsState().value?.let {
        TaskModifyLayout(
            preference = PrimaryPreferences(context),
            view = view,
            modifyState = it,
            requestReminder = { taskId ->
                taskCreatedToast.value = true
                taskState.requestReminder(taskId)
            },
            onFinished = { taskId ->
                taskState.requestModify(null)
                taskId?.let { let -> taskState.updateNotification(let, context) }
            },
        )
    }
    if (taskCreatedToast.value) LaunchedEffect(Unit) {
        Toast.makeText(
            view.context, R.string.task_is_created, Toast.LENGTH_LONG
        ).show()
        taskCreatedToast.value = false
    }
    ReminderHandlerListener(
        reminderHandler = taskState.reminderHandler,
        context = context,
        view = view,
        coroutineScope = coroutineScope
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberLazyListState(
    navConnector: NavConnectData,
    scrollBehavior: TopAppBarScrollBehavior,
    updateNavConnector: (data: NavConnectData, updateSet: NavConnectData) -> Unit,
): LazyListState {
    val coroutineScope = rememberCoroutineScope()
    val lazyState = rememberLazyListState()
    LaunchedEffect(lazyState) {
        snapshotFlow { lazyState.canScrollForward || lazyState.canScrollBackward }.collect {
            updateNavConnector(
                NavConnectData(canScroll = it), NavConnectData(canScroll = true)
            )
        }
    }
    LaunchedEffect(lazyState) {
        snapshotFlow { lazyState.canScrollForward }.collect {
            updateNavConnector(
                NavConnectData(canScrollForward = it), NavConnectData(canScrollForward = true)
            )
        }
    }
    if (navConnector.scrollToFirst) LaunchedEffect(Unit) {
        lazyState.animateScrollToItem(0)
        scrollBehavior.state.heightOffset = 0f
        updateNavConnector(
            NavConnectData(scrollToFirst = false), NavConnectData(scrollToFirst = true)
        )
    }
    if (navConnector.scrollToBottom) LaunchedEffect(Unit) {
        lazyState.animateScrollToItem(lazyState.layoutInfo.totalItemsCount)
        try { // try to fix a crash when rotate screen in auto scroll
            scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
        } catch (_: Exception) {
            coroutineScope.launch {
                delay(220)
                scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
            }
        }
        updateNavConnector(
            NavConnectData(scrollToBottom = false), NavConnectData(scrollToBottom = true)
        )
    }
    LaunchedEffect(navConnector.scrollToFirst || navConnector.scrollToBottom) {
        delay(850) // Timeout for scroll state change
        if (navConnector.scrollToFirst || navConnector.scrollToBottom) updateNavConnector(
            NavConnectData(scrollToFirst = false, scrollToBottom = false),
            NavConnectData(scrollToFirst = true, scrollToBottom = true)
        )
    }
    return lazyState
}

//TODO: Fix preview
@SuppressLint("ViewModelConstructorInComposable")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Preview
@Composable
private fun Preview() {
    TaskLayout(
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
        LocalContext.current, LocalView.current,
        remember { mutableStateOf(false) },
        taskState = TaskLayoutViewModelPreview()
    )
}
