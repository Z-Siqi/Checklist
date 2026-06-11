package com.sqz.checklist.ui.main.task

import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.sqz.checklist.R
import com.sqz.checklist.common.AndroidEffectFeedback
import com.sqz.checklist.presentation.task.info.TaskInfoLayout
import com.sqz.checklist.presentation.task.info.TaskInfoState
import com.sqz.checklist.presentation.task.list.TaskListLayout
import com.sqz.checklist.presentation.task.list.TaskListState
import com.sqz.checklist.presentation.task.modify.TaskModifyLayout
import com.sqz.checklist.presentation.task.modify.TaskModifyState
import com.sqz.checklist.ui.common.ContentScaffold
import com.sqz.checklist.ui.main.task.layout.TaskLayoutTopBar
import com.sqz.checklist.ui.main.task.layout.TopBarExtendedMenu
import com.sqz.checklist.ui.main.task.layout.TopBarMenuClickType
import com.sqz.checklist.ui.main.task.layout.function.ReminderHandlerListener
import com.sqz.checklist.ui.nav.group.home.HomeNavGroup
import com.sqz.checklist.ui.nav.group.home.HomeNavGroupInterface
import com.sqz.checklist.ui.nav.group.home.button.TaskExtendedButton
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import sqz.checklist.data.preferences.PrimaryPreferences

internal sealed interface TaskScreen {

    @Serializable
    data object MainRoute : TaskScreen

    @Serializable
    data class ModifyDialogRoute(
        val taskId: Long? // null to add, otherwise edit
    ) : TaskScreen

    //data class ReminderDialogRoute() : TaskScreen

    enum class InfoType {
        ViewTaskDetail,
        DefaultTask,
        RemindedTask,
    }

    @Serializable
    data class InfoDialogRoute(val taskId: Long, val type: InfoType) : TaskScreen
}

fun NavGraphBuilder.taskScreen(
    homeViewModel: HomeNavGroupInterface,
    homeNavController: NavHostController,
    rootNavController: NavHostController,
    taskState: TaskLayoutViewModel,
    view: View,
    refreshListRequest: androidx.compose.runtime.MutableState<Boolean>,
    modifier: Modifier = Modifier,
) {
    val coordinator = TaskScreenCoordinator(
        view = view,
        homeNavController = homeNavController,
        homeViewModel = homeViewModel,
        taskState = taskState,
        refreshListRequest = refreshListRequest,
    )
    navigation(
        route = HomeNavGroup.TaskNavRoute::class,
        startDestination = TaskScreen.MainRoute,
    ) {
        composable(route = TaskScreen.MainRoute::class) {
            val coroutineScope = rememberCoroutineScope()

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        taskState.updateListConfig(PrimaryPreferences(view.context))
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            val topBarState = rememberTopAppBarState()
            var canScroll by rememberSaveable { mutableStateOf(true) }
            val scrollBehavior = if (canScroll) {
                TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topBarState)
            } else {
                TopAppBarDefaults.pinnedScrollBehavior().also {
                    if (it.state.heightOffset != 0f) {
                        it.state.heightOffset = 0f
                    }
                }
            }

            val taskListState = remember { mutableStateOf<TaskListState>(TaskListState.None) }
            val isSearching = rememberSaveable { mutableStateOf(false) }
            val lazyListState = rememberLazyListState(homeViewModel, !isSearching.value)

            if (refreshListRequest.value) {
                LaunchedEffect(Unit) {
                    while (taskListState.value !is TaskListState.None) {
                        delay(100)
                        Log.d("TaskLayout", "refreshListRequest delayed")
                    }
                    taskListState.value = TaskListState.IsRefreshListRequest
                }
            }

            val homeRequest by homeViewModel.getTaskTypeRequest().collectAsState()
            LaunchedEffect(homeRequest) {
                coordinator.onHomeRequest(
                    state = homeRequest ?: return@LaunchedEffect,
                    scrollBehavior = scrollBehavior,
                    lazyListState = lazyListState,
                    taskListState = taskListState,
                )
            }
            ContentScaffold(
                topBar = {
                    val onMenuClick: @Composable (androidx.compose.runtime.MutableState<Boolean>) -> Unit =
                        { state ->
                            TopBarExtendedMenu(
                                state = state,
                                navController = rootNavController,
                                onClickType = { type, ctx ->
                                    if (type == TopBarMenuClickType.Search) {
                                        taskListState.value = TaskListState.IsSearchRequest
                                        return@TopBarExtendedMenu
                                    }
                                    taskState.resetUndo(ctx)
                                },
                                view = view,
                            )
                        }
                    canScroll = TaskLayoutTopBar(
                        scrollBehavior = scrollBehavior,
                        topBarState = topBarState,
                        onMenuClick = onMenuClick,
                        view = view,
                    )
                },
                contentWindowInsets = WindowInsets.displayCutout,
                modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface
                ) {
                    TaskListLayout(
                        listState = taskListState.value,
                        config = taskState.listConfig,
                        view = view,
                        externalRequest = {
                            coordinator.onTaskListRequest(
                                state = it,
                                taskListState = taskListState,
                                isSearching = isSearching,
                            )
                        },
                        lazyListState = lazyListState,
                    )
                }
            }
            ReminderHandlerListener(
                reminderHandler = taskState.reminderHandler,
                context = view.context,
                view = view,
                coroutineScope = coroutineScope
            )
        }

        dialog(route = TaskScreen.ModifyDialogRoute::class) { backStackEntry ->
            val modifyDialog: TaskScreen.ModifyDialogRoute = backStackEntry.toRoute()
            val modifyState: TaskModifyState = modifyDialog.taskId.let {
                if (it == null) {
                    return@let TaskModifyState.AddTask
                }
                return@let TaskModifyState.EditTask(it)
            }
            val taskCreatedToast = remember { mutableStateOf(false) }
            TaskModifyLayout(
                preference = PrimaryPreferences(view.context),
                view = view,
                modifyState = modifyState,
                requestReminder = { taskId ->
                    taskCreatedToast.value = true
                    taskState.requestReminder(taskId)
                },
                onFinished = { taskId ->
                    taskId?.let { let -> taskState.updateNotification(let, view.context) }
                    homeNavController.popBackStack()
                },
                feedback = AndroidEffectFeedback(view)
            )
            if (taskCreatedToast.value) LaunchedEffect(Unit) {
                Toast.makeText(
                    view.context, R.string.task_is_created, Toast.LENGTH_LONG
                ).show()
                taskCreatedToast.value = false
            }
        }

        dialog(route = TaskScreen.InfoDialogRoute::class) { backStackEntry ->
            val infoDialog: TaskScreen.InfoDialogRoute = backStackEntry.toRoute()
            val toInfoState: TaskInfoState = infoDialog.let {
                when (it.type) {
                    TaskScreen.InfoType.ViewTaskDetail -> TaskInfoState(
                        taskId = it.taskId,
                        config = TaskInfoState.Config.DetailOnly
                    )

                    TaskScreen.InfoType.DefaultTask -> TaskInfoState(
                        taskId = it.taskId,
                        config = TaskInfoState.Config.TaskOnly(pinChangeAllowed = false)
                    )

                    TaskScreen.InfoType.RemindedTask -> TaskInfoState(
                        taskId = it.taskId,
                        config = TaskInfoState.Config.TaskOnly(pinChangeAllowed = true)
                    )
                }
            }
            TaskInfoLayout(
                state = toInfoState,
                onFinished = { homeNavController.popBackStack() },
                feedback = AndroidEffectFeedback(view),
                modifier = modifier
            )
        }
    }
}

@Composable
private fun rememberLazyListState(
    homeViewModel: HomeNavGroupInterface,
    enableMenuSwitcher: Boolean,
): LazyListState {
    val lazyState = rememberLazyListState()
    var enableLongClickMenu by remember { mutableStateOf(false) }

    var nextState by remember { mutableStateOf<TaskExtendedButton.State?>(null) }
    LaunchedEffect(lazyState) { // control whether enable long click menu
        snapshotFlow { lazyState.canScrollForward || lazyState.canScrollBackward }.collect {
            if (!it && enableMenuSwitcher) { // list cannot scroll
                val state = TaskExtendedButton.State.LongClickMenuDisabled
                nextState = state
            }
            enableLongClickMenu = it
        }
    }
    LaunchedEffect(lazyState) { // set LongClickMenuEnabled if enableLongClickMenu = true
        snapshotFlow { lazyState.canScrollForward }.collect {
            if (enableLongClickMenu && enableMenuSwitcher) {
                val state = TaskExtendedButton.State.LongClickMenuEnabled(!it)
                nextState = state
            }
        }
    }
    LaunchedEffect(nextState) { // lazy update state
        nextState?.let {
            if (!enableMenuSwitcher) {
                return@let
            }
            homeViewModel.updateState(it)
        }
    }
    return lazyState
}
