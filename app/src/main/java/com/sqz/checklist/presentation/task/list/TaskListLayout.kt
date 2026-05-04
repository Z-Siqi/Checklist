package com.sqz.checklist.presentation.task.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.common.AndroidEffectFeedback
import com.sqz.checklist.presentation.task.list.scene.TaskListSceneUI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import sqz.checklist.common.EffectFeedback
import sqz.checklist.data.database.repository.history.TaskHistoryRepository
import sqz.checklist.data.database.repository.history.TaskHistoryRepositoryFake
import sqz.checklist.data.database.repository.reminder.TaskReminderRepository
import sqz.checklist.data.database.repository.reminder.TaskReminderRepositoryFake
import sqz.checklist.data.database.repository.task.TaskRepository
import sqz.checklist.data.database.repository.task.TaskRepositoryFake
import sqz.checklist.task.api.list.TaskList
import sqz.checklist.task.api.list.model.TaskItemModel

/**
 * Top layout of TaskList.kt
 */
@Composable
fun TaskListLayout(
    listState: TaskListState,
    view: android.view.View,
    config: StateFlow<TaskList.Config>,
    externalRequest: (TaskListRequest) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskListViewModel = viewModelFactory(config),
    lazyListState: LazyListState = rememberLazyListState(),
    feedback: EffectFeedback = AndroidEffectFeedback(view),
) {
    val listInventory by viewModel.listInventory.collectAsState()
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (listInventory is TaskList.Inventory.Loading) {
            val loadingModifier = Modifier
                .fillMaxWidth()
                .padding(top = 38.dp)
            LoadingState(modifier = loadingModifier)
        } else {
            viewModel.isListEmpty().collectAsState(false).value.let {
                if (it) EmptyListText()
            }
            TaskListSceneUI(
                viewModel = viewModel,
                view = view,
                feedback = feedback,
                lazyListState = lazyListState
            )
        }
    }
    if (listState is TaskListState.IsRefreshListRequest) LaunchedEffect(Unit) {
        viewModel.updateList()
        externalRequest(TaskListRequest.RefreshListProcessed)
    }
    if (listState is TaskListState.IsSearchRequest) LaunchedEffect(Unit) {
        //TODO: Finish refactoring this
        viewModel.setSearchState(listState.searchState)
        externalRequest(TaskListRequest.SearchProcessed)
    }
    val request by viewModel.externalRequest.collectAsState()
    LaunchedEffect(request) {
        request.let { request ->
            when (request) {
                is TaskItemModel.ExternalRequest.Info -> externalRequest(
                    TaskListRequest.Info(
                        request.taskId, request.pinChangeAllowed
                    )
                )

                is TaskItemModel.ExternalRequest.Detail -> externalRequest(
                    TaskListRequest.Detail(request.taskId)
                )

                is TaskItemModel.ExternalRequest.Edit -> externalRequest(
                    TaskListRequest.Edit(request.taskId)
                )

                is TaskItemModel.ExternalRequest.Reminder -> externalRequest(
                    TaskListRequest.Reminder(request.taskId)
                )

                is TaskItemModel.ExternalRequest.RemoveReminded -> externalRequest(
                    TaskListRequest.RemoveReminded(request.taskId)
                )

                is TaskItemModel.ExternalRequest.None -> {}
            }
        }
        viewModel.resetExternalRequest()
    }
}

@Composable
private fun EmptyListText(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.nothing_need_do),
        modifier = modifier,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        color = MaterialTheme.colorScheme.outline,
        lineHeight = 30.sp, textAlign = TextAlign.Center
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LoadingIndicator(modifier = Modifier.padding(end = 5.dp))
        Spacer(modifier = Modifier.height(8.dp))
        val titleStyle = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold, lineHeight = TextUnit.Unspecified
        )
        Text(
            text = stringResource(R.string.loading),
            autoSize = TextAutoSize.StepBased(
                minFontSize = 5.sp, maxFontSize = titleStyle.fontSize
            ),
            maxLines = 1,
            color = MaterialTheme.colorScheme.outline,
            style = titleStyle,
        )
    }
}

@Composable
private fun viewModelFactory(config: StateFlow<TaskList.Config>): TaskListViewModel {
    val taskRepository = TaskRepository.provider(MainActivity.taskDatabase)
    val taskHistoryRepository = TaskHistoryRepository.provider(MainActivity.taskDatabase)
    val taskReminderRepo = TaskReminderRepository.provider(MainActivity.taskDatabase)
    return viewModel {
        TaskListViewModel(config, taskHistoryRepository, taskReminderRepo, taskRepository)
    }
}

@Preview
@Composable
private fun TaskListLayoutPreview() {
    val config = MutableStateFlow(TaskList.Config(enableUndo = true))
    val vmFake = viewModel {
        TaskListViewModel(
            config, TaskHistoryRepositoryFake(), TaskReminderRepositoryFake(), TaskRepositoryFake()
        )
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        TaskListLayout(
            listState = TaskListState.None,
            lazyListState = rememberLazyListState(),
            view = androidx.compose.ui.platform.LocalView.current,
            config = config,
            externalRequest = {},
            modifier = Modifier,
            viewModel = vmFake
        )
    }
}
