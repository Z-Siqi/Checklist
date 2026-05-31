package com.sqz.checklist.presentation.history.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.common.AndroidEffectFeedback
import com.sqz.checklist.presentation.history.task.dialog.DoAllWarnDialogUI
import com.sqz.checklist.presentation.history.task.dialog.WarnType
import com.sqz.checklist.presentation.history.task.list.TaskHistoryListUI
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import sqz.checklist.common.EffectFeedback
import sqz.checklist.data.database.repository.history.TaskHistoryRepository
import sqz.checklist.data.database.repository.history.TaskHistoryRepositoryFake
import sqz.checklist.history.api.task.TaskHistory

@Composable
fun TaskHistoryLayout(
    externalState: TaskHistoryState,
    onRequest: (TaskHistoryRequest) -> Unit,
    config: StateFlow<TaskHistory.Config>, //TODO: impl this feature
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
    viewModel: TaskHistoryViewModel = viewModelFactory(config),
) {
    val historyInventory by viewModel.historyInventory.collectAsState()
    val secondConfirmationState by viewModel.secondConfirmationState.collectAsState()
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        if (historyInventory is TaskHistory.Inventory.Loading) {
            var showLoading by rememberSaveable { mutableStateOf(false) }
            showLoading.let {
                if (it) LoadingState(modifier = Modifier.fillMaxSize())
            }
            if (!showLoading) LaunchedEffect(Unit) {
                delay(168)
                showLoading = true
            }
        } else {
            viewModel.isInventoryListEmpty().collectAsState(false).value.let {
                if (it) EmptyListText(modifier = Modifier.fillMaxSize())
            }
            TaskHistoryListUI(
                viewModel = viewModel,
                feedback = feedback,
            ) { onRequest(TaskHistoryRequest.TaskInfoRequest(it)) }
        }
    }
    when (secondConfirmationState) {
        TaskHistoryViewModel.SecondConfirmationState.DeleteAll -> DoAllWarnDialogUI(
            warnType = WarnType.DeleteAll,
            onDismissRequest = { viewModel.onSecondConfirmation(null) },
            onConfirmRequest = {
                val delAll = TaskHistoryViewModel.SecondConfirmationState.DeleteAll
                viewModel.onSecondConfirmation(delAll)
            },
            feedback = feedback,
        )

        TaskHistoryViewModel.SecondConfirmationState.RedoAll -> DoAllWarnDialogUI(
            warnType = WarnType.DeleteAll,
            onDismissRequest = { viewModel.onSecondConfirmation(null) },
            onConfirmRequest = {
                val redoAll = TaskHistoryViewModel.SecondConfirmationState.RedoAll
                viewModel.onSecondConfirmation(redoAll)
            },
            feedback = feedback,
        )

        null -> {}
    }
    LaunchedEffect(historyInventory) {
        // Process delete or undo all state
        if (historyInventory is TaskHistory.Inventory.Loading) {
            onRequest(TaskHistoryRequest.AllowDelOrRedo(null))
            return@LaunchedEffect
        }
        (historyInventory as? TaskHistory.Inventory.Default).let { inventory ->
            if (inventory == null) {
                return@LaunchedEffect
            }
            val allowedOrAll: Boolean? = inventory.historyList.first().isEmpty().let { noItem ->
                if (noItem) { // history list is empty
                    return@let null
                }
                // true if it is null which means no task is selected
                return@let inventory.selectedTaskId == null
            }
            onRequest(TaskHistoryRequest.AllowDelOrRedo(allowedOrAll))
        }
    }
    LaunchedEffect(externalState) {
        // Process external state
        viewModel.onExternalState(
            state = externalState,
            onFailed = { /*TODO: process accidentally failed case*/ }
        )
        onRequest(TaskHistoryRequest.StateProcessed)
    }
}

@Composable
private fun EmptyListText(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.nothing_here),
            fontWeight = FontWeight.Medium,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
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
private fun viewModelFactory(config: StateFlow<TaskHistory.Config>): TaskHistoryViewModel {
    val taskHistoryRepository = TaskHistoryRepository.provider(MainActivity.taskDatabase)
    return viewModel {
        TaskHistoryViewModel(config, taskHistoryRepository)
    }
}

@Preview
@Composable
private fun TaskHistoryLayoutPreview() {
    val config = MutableStateFlow(TaskHistory.Config())
    val fkTaskHistory = TaskHistoryRepositoryFake()
    TaskHistoryLayout(
        externalState = TaskHistoryState.None,
        onRequest = {},
        config = config,
        feedback = AndroidEffectFeedback(androidx.compose.ui.platform.LocalView.current),
        viewModel = viewModel { TaskHistoryViewModel(config, fkTaskHistory) }
    )
}
