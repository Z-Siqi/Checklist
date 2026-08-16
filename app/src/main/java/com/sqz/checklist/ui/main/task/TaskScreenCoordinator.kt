package com.sqz.checklist.ui.main.task

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.MutableState
import androidx.navigation.NavHostController
import com.sqz.checklist.presentation.reminder.runtime.ReminderNotificationController
import com.sqz.checklist.presentation.task.list.TaskListRequest
import com.sqz.checklist.presentation.task.list.TaskListState
import com.sqz.checklist.ui.nav.group.home.HomeNavGroupInterface
import com.sqz.checklist.ui.nav.group.home.button.TaskExtendedButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sqz.checklist.data.preferences.PrimaryPreferences

internal class TaskScreenCoordinator(
    private val homeNavController: NavHostController,
    private val homeViewModel: HomeNavGroupInterface,
    private val refreshListRequest: MutableState<Boolean>,
    private val preference: PrimaryPreferences,
) {
    fun onTaskListRequest(
        state: TaskListRequest,
        taskListState: MutableState<TaskListState>,
        isSearching: MutableState<Boolean>,
        reminderController: ReminderNotificationController,
        coroutineScope: CoroutineScope,
    ) = when (state) {
        is TaskListRequest.SearchProcessed -> {
            if (state.currentState) {
                homeViewModel.updateState(TaskExtendedButton.State.CancelClickStyle)
            }
            isSearching.value = state.currentState
            taskListState.value = TaskListState.None
        }

        is TaskListRequest.RefreshListProcessed -> {
            refreshListRequest.value = false
            taskListState.value = TaskListState.None
        }

        is TaskListRequest.Edit -> {
            val editRoute = TaskScreen.ModifyDialogRoute(state.taskId)
            homeNavController.navigate(editRoute)
        }

        is TaskListRequest.Reminder -> {
            homeNavController.navigate(
                TaskScreen.ReminderDialogRoute(
                    taskId = state.taskId,
                    allowDismiss = true,
                )
            )
        }

        is TaskListRequest.Detail -> {
            val infoRoute = TaskScreen.InfoDialogRoute(
                taskId = state.taskId, type = TaskScreen.InfoType.ViewTaskDetail
            )
            homeNavController.navigate(infoRoute)
        }

        is TaskListRequest.RemoveReminded -> coroutineScope.launch {
            reminderController.removeReminder(
                taskId = state.taskId,
                removeDisplayedNotification = !preference.disableRemoveNotifyInReminded(),
            )
        }

        is TaskListRequest.Info -> {
            if (state.pinChangeAllowed) {
                val infoRoute = TaskScreen.InfoDialogRoute(
                    taskId = state.taskId, type = TaskScreen.InfoType.RemindedTask
                )
                homeNavController.navigate(infoRoute)
            } else {
                val infoRoute = TaskScreen.InfoDialogRoute(
                    taskId = state.taskId, type = TaskScreen.InfoType.DefaultTask
                )
                homeNavController.navigate(infoRoute)
            }
        }
    }

    suspend fun onHomeRequest(
        state: TaskExtendedButton.ClickRequest,
        scrollBehavior: TopAppBarScrollBehavior,
        lazyListState: LazyListState,
        taskListState: MutableState<TaskListState>,
    ) = when (state) {
        TaskExtendedButton.ClickRequest.Add -> {
            homeNavController.navigate(TaskScreen.ModifyDialogRoute(null))
        }

        TaskExtendedButton.ClickRequest.ScrollUp -> {
            scrollBehavior.state.heightOffset = 0f
            lazyListState.animateScrollToItem(0)
        }

        TaskExtendedButton.ClickRequest.ScrollDown -> {
            val totalItemsCount = lazyListState.layoutInfo.totalItemsCount
            lazyListState.animateScrollToItem(totalItemsCount)
            scrollBehavior.state.let { let ->
                try {
                    let.heightOffset = let.heightOffsetLimit
                } catch (_: Exception) {
                    delay(220)
                    let.heightOffset = let.heightOffsetLimit
                }
            }
        }

        TaskExtendedButton.ClickRequest.Search -> {
            taskListState.value = TaskListState.IsSearchRequest
        }

        TaskExtendedButton.ClickRequest.Cancel -> {
            taskListState.value = TaskListState.IsSearchRequest
        }
    }.also { homeViewModel.resetRequest() }
}
