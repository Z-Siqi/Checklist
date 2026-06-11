package com.sqz.checklist.ui.main.history.task

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.sqz.checklist.common.AndroidEffectFeedback
import com.sqz.checklist.presentation.history.task.TaskHistoryLayout
import com.sqz.checklist.presentation.history.task.TaskHistoryRequest
import com.sqz.checklist.presentation.history.task.TaskHistoryState
import com.sqz.checklist.presentation.task.info.TaskInfoLayout
import com.sqz.checklist.presentation.task.info.TaskInfoState
import com.sqz.checklist.ui.common.ContentScaffold
import com.sqz.checklist.ui.common.unit.isLandscape
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import sqz.checklist.history.api.task.TaskHistory
import kotlin.reflect.KClass

private sealed interface TaskHistoryScreen {

    @Serializable
    data object MainRoute : TaskHistoryScreen

    @Serializable
    data class TaskInfoDialogRoute(val taskId: Long) : TaskHistoryScreen
}

fun <T : Any> NavGraphBuilder.taskHistoryScreen(
    route: KClass<T>,
    rootNavController: NavHostController,
    modifier: Modifier = Modifier,
) {
    navigation(
        route = route,
        startDestination = TaskHistoryScreen.MainRoute,
    ) {
        composable(route = TaskHistoryScreen.MainRoute::class) {
            val view = LocalView.current
            val historyState = remember { mutableStateOf<TaskHistoryState>(TaskHistoryState.None) }
            val navButtonState = rememberSaveable { mutableStateOf<Boolean?>(null) }

            val isLandscape = isLandscape()
            ContentScaffold(
                topBar = {
                    HistoryTopBar(onClick = rootNavController::popBackStack)
                },
                bottomBar = {
                    if (!isLandscape) NavigationSelector(
                        isLandscape = false,
                        selected = navButtonState.value == false,
                        deleteClick = { historyState.value = TaskHistoryState.Delete },
                        redoClick = { historyState.value = TaskHistoryState.Redo },
                        view = view,
                    )
                },
                navigationRail = {
                    if (isLandscape) NavigationSelector(
                        isLandscape = true,
                        selected = navButtonState.value == false,
                        deleteClick = { historyState.value = TaskHistoryState.Delete },
                        redoClick = { historyState.value = TaskHistoryState.Redo },
                        view = view,
                    )
                },
                contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(
                    WindowInsetsSides.Vertical + WindowInsetsSides.Start
                ).exclude(WindowInsets.navigationBars)
            ) {
                Surface(
                    modifier = modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    TaskHistoryLayout(
                        externalState = historyState.value,
                        onRequest = {
                            when (it) {
                                is TaskHistoryRequest.StateProcessed -> {
                                    historyState.value = TaskHistoryState.None
                                }

                                is TaskHistoryRequest.AllowDelOrRedo -> {
                                    navButtonState.value = it.toAll
                                }

                                is TaskHistoryRequest.TaskInfoRequest -> {
                                    rootNavController.navigate(
                                        TaskHistoryScreen.TaskInfoDialogRoute(it.taskId)
                                    )
                                }
                            }
                        },
                        config = MutableStateFlow(TaskHistory.Config()),
                        feedback = AndroidEffectFeedback(view),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        dialog(route = TaskHistoryScreen.TaskInfoDialogRoute::class) { backStackEntry ->
            val view = LocalView.current
            val taskInfoDialog: TaskHistoryScreen.TaskInfoDialogRoute = backStackEntry.toRoute()
            TaskInfoLayout(
                state = TaskInfoState(
                    taskId = taskInfoDialog.taskId,
                    config = TaskInfoState.Config.TaskAndDetail(false)
                ),
                onFinished = rootNavController::popBackStack,
                feedback = AndroidEffectFeedback(view),
                modifier = modifier,
            )
        }
    }
}
