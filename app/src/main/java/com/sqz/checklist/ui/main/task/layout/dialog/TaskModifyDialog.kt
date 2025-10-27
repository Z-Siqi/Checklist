package com.sqz.checklist.ui.main.task.layout.dialog

import android.content.Context
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sqz.checklist.R
import com.sqz.checklist.database.TaskData
import com.sqz.checklist.database.TaskDetailData
import com.sqz.checklist.ui.common.TextTooltipBox
import com.sqz.checklist.ui.common.dialog.EditableContentDialog

/**
 * Composable function for a dialog to add a new task.
 *
 * This dialog allows users to input a task description, optionally pin the task,
 * add detailed sub-items, and set a reminder. It uses a ViewModel to manage the state
 * of the new task being created.
 *
 * **Remember:** clean inProcessFilesPath when finished.
 *
 * @param reminderButton Whether to show a reminder button in the dialog.
 * @param confirm A callback function that is invoked when the user confirms the new task.
 * It passes the created task and the current cache in preferences.
 * @param onDismissRequest A callback function to be invoked when the dialog is dismissed.
 * @param view The current [View] instance.
 */
@Composable
fun TaskModifyDialog(
    reminderButton: Boolean,
    confirm: (CreateTask) -> Unit,
    onDismissRequest: () -> Unit,
    view: View,
) {
    val viewModel: TaskModifyDialogViewModel = viewModel()
    rememberSaveable {
        mutableStateOf(true.also {
            viewModel.onCleared()
            viewModel.init()
        })
    }
    val taskData = viewModel.taskData.collectAsState().value!!
    val detailList by viewModel.taskDetailList.collectAsState()
    val requestReminder by viewModel.requestReminder.collectAsState()

    val extraButtonTop: @Composable () -> Unit = {
        val onPinClick = {
            viewModel.updateTask { it.copy(isPin = !taskData.isPin!!) }
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
        TextTooltipBox(textRid = R.string.create_as_pin) {
            IconButton(onClick = onPinClick, modifier = Modifier.rotate(40f)) {
                Icon(
                    painter = painterResource(if (taskData.isPin!!) R.drawable.pinned else R.drawable.pin),
                    contentDescription = stringResource(R.string.create_as_pin)
                )
            }
        }
    }
    val extraButtonBottom: @Composable (run: () -> Unit) -> Unit = {
        TaskDetailIcon(
            textRid = R.string.create_task_detail,
            onClick = { run(it) },
            selected = detailList?.isNotEmpty() == true
        )
        if (reminderButton) {
            val onReminderClick = {
                viewModel.requestReminder()
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
            TextTooltipBox(textRid = R.string.create_with_reminder) {
                IconButton(onClick = onReminderClick) {
                    Icon(
                        painter = painterResource(if (requestReminder) R.drawable.timer_on else R.drawable.timer),
                        contentDescription = stringResource(R.string.create_with_reminder)
                    )
                }
            }
        }
    }
    TaskModifyDialog(
        onDismissRequest = { onDismissRequest().also { viewModel.onCleared() } },
        confirm = {
            val onConfirm = CreateTask(
                task = taskData, detail = detailList, requestReminder = requestReminder
            )
            confirm(onConfirm)
            viewModel.onCleared()
        },
        viewModel = viewModel,
        parameter = TaskModifyDialog(
            title = stringResource(R.string.create_task),
            confirmText = stringResource(if (!requestReminder) R.string.add else R.string.next),
            detailTitle = stringResource(R.string.create_task_detail),
            extraButtonTop = extraButtonTop,
            extraButtonBottom = extraButtonBottom
        ),
        view = view
    )
}

/**
 * Composable function for a dialog to edit an existing task.
 *
 * This dialog allows users to modify the description of a task and its associated details.
 * It pre-populates the dialog with the existing task data. It utilizes a ViewModel to manage
 * the state of the task being edited.
 *
 * **Remember:** clean inProcessFilesPath when finished.
 *
 * @param editTask The existing task data to be edited. See [EditTask] for details.
 * @param confirm A callback function that is invoked when the user confirms the edits.
 * It passes the updated task and the current preferences cache.
 * @param onDismissRequest A callback function to be invoked when the dialog is dismissed.
 * @param view The current [View] instance.
 */
@Composable
fun TaskModifyDialog(
    editTask: EditTask,
    confirm: (EditTask, Context) -> Unit,
    onDismissRequest: () -> Unit,
    view: View,
) {
    val viewModel: TaskModifyDialogViewModel = viewModel()
    val init = rememberSaveable { mutableStateOf(false) }
    if (!init.value) {
        viewModel.onCleared()
        viewModel.init(task = editTask.task, taskDetail = editTask.detailGetter)
        init.value = true
    }

    val detailList by viewModel.taskDetailList.collectAsState()
    val extraButtonBottom: @Composable (run: () -> Unit) -> Unit = {
        TaskDetailIcon(
            textRid = R.string.edit_task_detail,
            onClick = { run(it) },
            selected = detailList?.isNotEmpty() == true
        )
    }
    viewModel.taskData.collectAsState().value?.let {
        TaskModifyDialog(
            onDismissRequest = onDismissRequest,
            confirm = {
                val onConfirm = EditTask(
                    taskId = editTask.taskId,
                    task = it,
                    detailGetter = editTask.detailGetter,
                    detailSetter = viewModel.taskDetailList.value
                )
                confirm(onConfirm, view.context)
                viewModel.onCleared()
            },
            viewModel = viewModel,
            parameter = TaskModifyDialog(
                title = stringResource(R.string.edit_task),
                confirmText = stringResource(R.string.edit),
                detailTitle = stringResource(R.string.edit_task_detail),
                extraButtonBottom = extraButtonBottom
            ),
            view = view
        )
    }
}

/**
 * Data class representing a task to be created or edited.
 *
 * @property task The task data to be created.
 * @property detail A list of task detail data to be associated with the task.
 * @property requestReminder A flag indicating whether a reminder should be requested for the task.
 */
data class CreateTask(
    val task: TaskData,
    val detail: List<TaskDetailData>?,
    val requestReminder: Boolean,
)

/**
 * Data class representing a task to be edited.
 *
 * @property taskId The ID of the task to be edited.
 * @property task The updated task data.
 * @property detailGetter A list of task detail data to be associated with the task, if any. Used
 * to retrieve the current data.
 * @property detailSetter A list of task detail data to be associated with the task. Used to set
 * the new data.
 */
data class EditTask(
    val taskId: Long,
    val task: TaskData,
    val detailGetter: List<TaskDetailData>? = null,
    val detailSetter: List<TaskDetailData>? = null,
)

/**
 * Private composable function that provides the core UI and logic for both adding and editing tasks.
 *
 * This function is the underlying implementation for the public `TaskModifyDialog` composable.
 * It constructs an `EditableContentDialog` and wires it up with the provided ViewModel and parameters.
 * It handles user input for the task description, displays extra buttons (like pin, details),
 * and manages the display of the `TaskDetailDialog` when requested.
 *
 * @param onDismissRequest A callback function invoked when the dialog is dismissed.
 * @param confirm A callback function invoked when the user presses the confirm button.
 * @param viewModel The [TaskModifyDialogViewModel] instance that holds the state for the dialog.
 * @param parameter A [TaskModifyDialog] data class instance containing UI-specific parameters like titles and extra buttons.
 * @param view The current [View] instance, used for showing toasts.
 */
@Composable
private fun TaskModifyDialog(
    onDismissRequest: () -> Unit,
    confirm: () -> Unit,
    viewModel: TaskModifyDialogViewModel,
    parameter: TaskModifyDialog,
    view: View,
) {
    val taskData = viewModel.taskData.collectAsState()
    val state = parameter.textFieldState(rememberTextFieldState(
        initialText = taskData.value?.description ?: ""
    ))
    var detailDialog by rememberSaveable { mutableStateOf(false) }
    val noDoNothingStr = stringResource(R.string.no_do_nothing)
    LaunchedEffect(state.text, taskData) {
        viewModel.setTaskDescription(state.text.toString())
    }
    EditableContentDialog(
        onDismissRequest = {
            if (!viewModel.isChanged.value) onDismissRequest()
        },
        confirm = {
            if (!state.text.toString().isBlank()) {
                confirm()
            } else {
                Toast.makeText(view.context, noDoNothingStr, Toast.LENGTH_SHORT).show()
            }
        },
        title = parameter.title,
        confirmText = parameter.confirmText,
        state = state,
        contentProperties = EditableContentDialog(
            extraButtonBottom = { parameter.extraButtonBottom { detailDialog = true } },
            extraButtonTop = parameter.extraButtonTop,
        ),
        doneImeAction = true,
        onDismissClick = onDismissRequest
    )
    if (detailDialog) TaskDetailDialog(
        onDismissRequest = { detailDialog = false },
        confirm = { list, isChanged ->
            viewModel.setTaskDetailList(list, isChanged)
            detailDialog = false
        },
        title = parameter.detailTitle,
        taskDetail = viewModel.taskDetailList.collectAsState().value
    )
}

/** A data class representing the parameters for the task modify dialog **/
private data class TaskModifyDialog(
    val title: String,
    val confirmText: String,
    val detailTitle: String,
    val extraButtonTop: @Composable () -> Unit = {},
    val extraButtonBottom: @Composable (run: () -> Unit) -> Unit = {},
    val textFieldState: (TextFieldState) -> TextFieldState = { it },
)

/** Task detail icon **/
@Composable
private fun TaskDetailIcon(textRid: Int, onClick: () -> Unit, selected: Boolean) {
    TextTooltipBox(textRid = textRid) {
        IconButton(
            onClick = onClick,
            colors = if (selected) {
                IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            } else IconButtonDefaults.iconButtonColors()
        ) {
            Icon(
                painter = painterResource(R.drawable.attach),
                contentDescription = stringResource(textRid)
            )
        }
    }
}

@Preview
@Composable
private fun TaskModifyDialogPreview() {
    TaskModifyDialog(
        reminderButton = true,
        confirm = {},
        onDismissRequest = {},
        view = View(LocalContext.current)
    )
}
