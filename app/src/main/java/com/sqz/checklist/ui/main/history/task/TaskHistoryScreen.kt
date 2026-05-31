package com.sqz.checklist.ui.main.history.task

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sqz.checklist.common.AndroidEffectFeedback
import com.sqz.checklist.presentation.history.task.TaskHistoryLayout
import com.sqz.checklist.presentation.history.task.TaskHistoryRequest
import com.sqz.checklist.presentation.history.task.TaskHistoryState
import com.sqz.checklist.presentation.task.info.TaskInfoLayout
import com.sqz.checklist.presentation.task.info.TaskInfoState
import com.sqz.checklist.ui.main.NavMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import sqz.checklist.history.api.task.TaskHistory

@Composable
fun TaskHistoryScreen( //TODO: refactor nav feature
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val viewModel: TaskHistoryScreenViewModel = viewModel { TaskHistoryScreenViewModel() }
    val view = LocalView.current
    val historyState = remember { mutableStateOf<TaskHistoryState>(TaskHistoryState.None) }
    val navButtonState = rememberSaveable { mutableStateOf<Boolean?>(null) }
    Scaffold(
        topBar = {
            HistoryTopBar(onClick = { navController.popBackStack() })
        },
        bottomBar = { //TODO: Nav rail
            NavigationSelector(
                mode = NavMode.NavBar,
                selected = navButtonState.value == false,
                deleteClick = { historyState.value = TaskHistoryState.Delete },
                redoClick = { historyState.value = TaskHistoryState.Redo },
                view = view,
            )
        },
        //contentWindowInsets = WindowInsets(),
    ) { paddingValues ->
        Surface(
            modifier = modifier.padding(paddingValues),
            color = MaterialTheme.colorScheme.surface
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
                            val state = TaskInfoState(
                                taskId = it.taskId,
                                config = TaskInfoState.Config.TaskAndDetail(false)
                            )
                            viewModel.requestTaskInfo(state)
                        }
                    }
                },
                config = MutableStateFlow(TaskHistory.Config()),
                feedback = AndroidEffectFeedback(view),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
    viewModel.isTaskInfo.collectAsState().value?.let {
        TaskInfoLayout(
            state = it,
            onFinished = { viewModel.requestTaskInfo(null) },
            feedback = AndroidEffectFeedback(view),
            modifier = modifier
        )
    }
}

private class TaskHistoryScreenViewModel: ViewModel() {

    private val _isTaskInfo: MutableStateFlow<TaskInfoState?> = MutableStateFlow(null)

    fun requestTaskInfo(state: TaskInfoState?) {
        _isTaskInfo.update { state }
    }

    val isTaskInfo: StateFlow<TaskInfoState?> = _isTaskInfo.asStateFlow()
}
