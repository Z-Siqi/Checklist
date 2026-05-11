package com.sqz.checklist.presentation.task.modify.dialog.task

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.sqz.checklist.presentation.task.modify.TaskModifyViewModel
import sqz.checklist.common.EffectFeedback
import sqz.checklist.task.api.TaskModify

@Composable
fun TaskModifyForTaskUI(
    onConfirm: () -> Unit,
    viewModel: TaskModifyViewModel,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
) {
    val task = viewModel.taskDialogHandler() ?: return
    val taskModify by viewModel.taskModify.collectAsState()
    val isModified by viewModel.isModified.collectAsState()
    TaskModifyDialogForTask(
        taskUIState = taskModify.taskState!!,
        onDetailRequest = viewModel::switchDialog,
        onTextChange = task::updateDescription,
        onTypeChange = { change ->
            if (change is TaskModify.Task.ModifyType.NewTask) {
                task.onTypeValueChange { change }
            }
        },
        onDismissRequest = viewModel::onDismissRequest,
        onCancel = viewModel::cancelModify,
        onConfirm = onConfirm,
        isDetailSet = !taskModify.detailState.isNullOrEmpty(),
        isModified = isModified,
        feedback = feedback,
        modifier = modifier
    )
}
