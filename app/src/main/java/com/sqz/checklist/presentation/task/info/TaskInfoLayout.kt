package com.sqz.checklist.presentation.task.info

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sqz.checklist.MainActivity
import com.sqz.checklist.presentation.task.info.type.detail.InfoDetailDialogUI
import com.sqz.checklist.presentation.task.info.type.task.InfoTaskDialogUI
import sqz.checklist.common.EffectFeedback
import sqz.checklist.data.database.repository.task.TaskRepository
import sqz.checklist.task.api.info.TaskInfo

/**
 * Top layout of TaskInfo.kt
 *
 * @param state The state of the task info.
 */
@Composable
fun TaskInfoLayout(
    state: TaskInfoState,
    onFinished: () -> Unit,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
    viewModel: TaskInfoViewModel = viewModelFactory()
) {
    val taskInfo by viewModel.taskInfo.collectAsState()

    LaunchedEffect(state, taskInfo) {
        viewModel.onStateChanged(state, onFinished)
    }

    (taskInfo as? TaskInfo.TaskInfoData.TaskOnly)?.let {
        if (it.pinChangeAllowed) {
            InfoTaskDialogUI(
                pinned = it.task.isPin,
                onPinChange = { viewModel.onPinChange() },
                description = it.task.description,
                onDismissRequest = viewModel::clearTaskInfo,
                feedback = feedback,
                modifier = modifier
            )
        } else {
            InfoTaskDialogUI(
                description = it.task.description,
                onDismissRequest = viewModel::clearTaskInfo,
                feedback = feedback,
                modifier = modifier
            )
        }
    }

    (taskInfo as? TaskInfo.TaskInfoData.DetailOnly)?.let {
        InfoDetailDialogUI(
            details = it.detail,
            onDismissRequest = viewModel::clearTaskInfo,
            feedback = feedback,
            modifier = modifier
        )
    }

    (taskInfo as? TaskInfo.TaskInfoData.TaskAndDetail)?.let {
        var isDialogSwitched by rememberSaveable { mutableStateOf(false) }
        if (it.pinChangeAllowed) {
            InfoTaskDialogUI(
                onDetailClick = { isDialogSwitched = true },
                pinned = it.task.isPin,
                onPinChange = { viewModel.onPinChange() },
                description = it.task.description,
                onDismissRequest = viewModel::clearTaskInfo,
                feedback = feedback,
                modifier = modifier
            )
        } else {
            InfoTaskDialogUI(
                onDetailClick = { isDialogSwitched = true },
                description = it.task.description,
                onDismissRequest = viewModel::clearTaskInfo,
                feedback = feedback,
                modifier = modifier
            )
        }
        if (isDialogSwitched) {
            InfoDetailDialogUI(
                details = it.detail,
                onDismissRequest = viewModel::clearTaskInfo,
                feedback = feedback,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun viewModelFactory(): TaskInfoViewModel {
    val taskRepository = TaskRepository.provider(MainActivity.taskDatabase)
    return viewModel { TaskInfoViewModel(taskRepository) }
}
