package com.sqz.checklist.ui.main.task

import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
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
import com.sqz.checklist.notification.NotifyManager
import com.sqz.checklist.presentation.reminder.assign.ReminderLayout
import com.sqz.checklist.presentation.reminder.runtime.ReminderPermissionWarningEffect
import com.sqz.checklist.presentation.reminder.runtime.rememberTaskReminderNotificationController
import com.sqz.checklist.presentation.task.info.TaskInfoLayout
import com.sqz.checklist.presentation.task.info.TaskInfoState
import com.sqz.checklist.presentation.task.list.TaskListLayout
import com.sqz.checklist.presentation.task.list.TaskListState
import com.sqz.checklist.presentation.task.modify.TaskModifyLayout
import com.sqz.checklist.presentation.task.modify.TaskModifyState
import com.sqz.checklist.ui.common.ContentScaffold
import com.sqz.checklist.ui.nav.group.home.HomeNavGroup
import com.sqz.checklist.ui.nav.group.home.HomeNavGroupInterface
import com.sqz.checklist.ui.nav.group.home.button.TaskExtendedButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import sqz.checklist.data.preferences.PrimaryPreferences
import sqz.checklist.task.api.list.TaskList
import kotlin.time.Duration.Companion.milliseconds

internal sealed interface TaskScreen {

    @Serializable
    data object MainRoute : TaskScreen

    @Serializable
    data class ModifyDialogRoute(
        val taskId: Long?
    ) : TaskScreen

    @Serializable
    data class ReminderDialogRoute(
        val taskId: Long,
        val allowDismiss: Boolean = true,
    ) : TaskScreen

    @androidx.annotation.Keep
    @Serializable
    enum class InfoType {
        ViewTaskDetail,
        DefaultTask,
        RemindedTask,
    }

    @Serializable
    data class InfoDialogRoute(val taskId: Long, val type: InfoType) : TaskScreen
}

fun NavGraphBuilder.taskScreen(
    mainCoroutineScope: CoroutineScope,
    homeViewModel: HomeNavGroupInterface,
    homeNavController: NavHostController,
    rootNavController: NavHostController,
    view: View,
    refreshListRequest: androidx.compose.runtime.MutableState<Boolean>,
    modifier: Modifier = Modifier,
) {
    val coordinator = TaskScreenCoordinator(
        homeNavController = homeNavController,
        homeViewModel = homeViewModel,
        refreshListRequest = refreshListRequest,
        preference = PrimaryPreferences(view.context)
    )
    navigation(
        route = HomeNavGroup.TaskNavRoute::class,
        startDestination = TaskScreen.MainRoute,
    ) {
        composable(route = TaskScreen.MainRoute::class) {
            val lifecycleOwner = LocalLifecycleOwner.current
            val notifyManager = remember { NotifyManager() }
            val reminderController = rememberTaskReminderNotificationController(
                notifyManager = notifyManager,
            )
            val preference = remember(view.context) { PrimaryPreferences(view.context) }
            val rememberListCfg = remember { MutableStateFlow(TaskList.Config()) }
            fun updateListConfig(prefs: PrimaryPreferences) {
                fun Int.prefsLimit(): Int? = this.let {
                    if (it >= 21) null else it
                }

                val config = TaskList.Config(
                    enableUndo = !prefs.disableUndoButton(),
                    autoDelIsHistoryTaskNumber = prefs.allowedNumberOfHistory().prefsLimit(),
                    recentlyRemindedKeepTime = prefs.recentlyRemindedKeepTime(),
                )
                rememberListCfg.update { config }
            }
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        updateListConfig(preference)
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
                        delay(100.milliseconds)
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

            ReminderPermissionWarningEffect(
                view = view,
                controller = reminderController,
                notifyManager = notifyManager,
            )
            ContentScaffold(
                topBar = {
                    val onMenuClick: @Composable (androidx.compose.runtime.MutableState<Boolean>) -> Unit =
                        { state ->
                            TopBarExtendedMenu(
                                state = state,
                                navController = rootNavController,
                                onClickType = { type ->
                                    if (type == TopBarMenuClickType.Search) {
                                        taskListState.value = TaskListState.IsSearchRequest
                                        return@TopBarExtendedMenu
                                    }
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
                    val coroutineScope = rememberCoroutineScope()
                    TaskListLayout(
                        listState = taskListState.value,
                        config = rememberListCfg,
                        view = view,
                        externalRequest = {
                            coordinator.onTaskListRequest(
                                state = it,
                                taskListState = taskListState,
                                isSearching = isSearching,
                                reminderController = reminderController,
                                coroutineScope = coroutineScope
                            )
                        },
                        lazyListState = lazyListState,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        dialog(route = TaskScreen.ModifyDialogRoute::class) { backStackEntry ->
            val modifyDialog: TaskScreen.ModifyDialogRoute = backStackEntry.toRoute()
            val modifyState: TaskModifyState = modifyDialog.taskId.let {
                if (it == null) {
                    return@let TaskModifyState.AddTask
                }
                return@let TaskModifyState.EditTask(it)
            }
            var navigatedToReminder by remember { mutableStateOf<Long?>(null) }
            val reminderController = rememberTaskReminderNotificationController()
            TaskModifyLayout(
                preference = PrimaryPreferences(view.context),
                view = view,
                modifyState = modifyState,
                requestReminder = { taskId -> navigatedToReminder = taskId },
                onFinished = { taskId ->
                    taskId?.let { updatedTaskId ->
                        mainCoroutineScope.launch {
                            reminderController.refreshDisplayedReminder(updatedTaskId)
                        }
                    }
                    if (navigatedToReminder != null) {
                        Toast.makeText(
                            view.context, R.string.task_is_created, Toast.LENGTH_LONG
                        ).show()
                        homeNavController.navigate(
                            TaskScreen.ReminderDialogRoute(
                                taskId = navigatedToReminder!!, allowDismiss = false
                            )
                        ) {
                            popUpTo(TaskScreen.ModifyDialogRoute::class) { inclusive = true }
                        }
                    } else {
                        homeNavController.popBackStack()
                    }
                },
                feedback = AndroidEffectFeedback(view)
            )
        }

        dialog(route = TaskScreen.ReminderDialogRoute::class) { backStackEntry ->
            val reminderDialog: TaskScreen.ReminderDialogRoute = backStackEntry.toRoute()
            ReminderLayout(
                taskId = reminderDialog.taskId,
                allowDismiss = reminderDialog.allowDismiss,
                context = view.context,
                onFinished = { homeNavController.popBackStack() },
            )
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
    LaunchedEffect(lazyState) {
        snapshotFlow { lazyState.canScrollForward || lazyState.canScrollBackward }.collect {
            if (!it && enableMenuSwitcher) {
                val state = TaskExtendedButton.State.LongClickMenuDisabled
                nextState = state
            }
            enableLongClickMenu = it
        }
    }
    LaunchedEffect(lazyState) {
        snapshotFlow { lazyState.canScrollForward }.collect {
            if (enableLongClickMenu && enableMenuSwitcher) {
                val state = TaskExtendedButton.State.LongClickMenuEnabled(!it)
                nextState = state
            }
        }
    }
    LaunchedEffect(nextState) {
        nextState?.let {
            if (!enableMenuSwitcher) {
                return@let
            }
            homeViewModel.updateState(it)
        }
    }
    return lazyState
}
