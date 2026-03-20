package com.sqz.checklist.presentation.task.modify

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sqz.checklist.MainActivity
import com.sqz.checklist.presentation.task.modify.dialog.detail.TaskModifyForDetailUI
import com.sqz.checklist.presentation.task.modify.dialog.task.TaskModifyForTaskUI
import com.sqz.checklist.ui.common.dialog.ProcessingDialog
import kotlinx.coroutines.delay
import sqz.checklist.data.database.repository.task.TaskRepository
import sqz.checklist.data.database.repository.task.TaskRepositoryFake
import sqz.checklist.data.preferences.PrimaryPreferences
import sqz.checklist.data.storage.manager.StorageManager
import sqz.checklist.data.storage.manager.StorageManagerFake
import sqz.checklist.task.api.TaskModify

/**
 * Composable function for a dialog to add or edit a task.
 *
 * This function expected to use it in an if statement, and close this via [onFinished] callback.
 * But the suggest way is to use a nullable [TaskModifyState] instead in top viewModel, then
 * by `?.let {}` method to call this function.
 *
 * Example:
 * ```
 * val mainViewModel = viewModel<MainViewModel>()
 * mainViewModel.addTask()
 * mainViewModel.modifyState?.let {
 *   TaskModifyLayout(
 *     modifyState = it, // mainViewModel.modifyState not null and provide an action
 *     onFinished = { mainViewModel.taskModifyFinished() } // set mainViewModel.modifyState to null
 *   )
 * }
 * ```
 *
 * **Critical Warning:** If [TaskModifyLayout] not in a default false method can brake `Preview`
 * method, unless define viewModel at the function which called this method.
 *
 * @param modifyState The state of the task modify, which can be [TaskModifyState.AddTask]
 *   or [TaskModifyState.EditTask] to control execute add or edit action.
 * @param onFinished A callback function to be invoked when the task modify is finished, but will
 *   not provide canceled or confirmed info.
 * @param modifier The modifier to be applied to the layout.
 * @param viewModel The [TaskModifyViewModel] instance
 */
@Composable
fun TaskModifyLayout(
    preference: PrimaryPreferences,
    view: View,
    modifyState: TaskModifyState,
    onFinished: (taskId: Long?) -> Unit,
    requestReminder: (taskId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskModifyViewModel = viewModelFactory()
) {
    val taskModify by viewModel.taskModify.collectAsState()
    val isProcessing = rememberSaveable { mutableStateOf(false) }
    val finishedId = rememberSaveable { mutableStateOf<Long?>(null) }

    // The listener is to process task modify start and finished actions
    LaunchedEffect(taskModify.state, modifyState) {
        if (taskModify.state == TaskModify.State.None) {
            if (isProcessing.value) {
                onFinished(finishedId.value)
                isProcessing.value = false
                finishedId.value = null
            } else {
                when (modifyState) {
                    is TaskModifyState.AddTask -> viewModel.newTask(view)
                    is TaskModifyState.EditTask -> viewModel.editTask(modifyState.taskId, view)
                }
                isProcessing.value = true
            }
        }
    }

    // Loading dialog
    if (taskModify.state == TaskModify.State.Loading) {
        val loadingPercentage by viewModel.loadingPercentage.collectAsState()
        val delayed = rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(delayed) {
            delay(100) // only show dialog if process need to take long time
            delayed.value = true
        }
        if (delayed.value) ProcessingDialog(loadingPercentage = loadingPercentage)
    }

    // Modify Dialog
    if (taskModify.state == TaskModify.State.Modify) {
        if (viewModel.showTaskDialog.collectAsState().value) {
            TaskModifyForTaskUI(
                onConfirm = {
                    viewModel.confirmModify(requestReminder) { finishedId.value = it }
                },
                viewModel = viewModel,
                modifier = modifier,
            )
        } else {
            TaskModifyForDetailUI(
                preference = preference,
                viewModel = viewModel,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun viewModelFactory(): TaskModifyViewModel {
    val taskRepository = TaskRepository.provider(MainActivity.taskDatabase)
    val storageManager = StorageManager.provider()
    return viewModel { TaskModifyViewModel(taskRepository, storageManager) }
}

@Preview
@Composable
private fun Preview() {
    val vmFake = viewModel { TaskModifyViewModel(TaskRepositoryFake(), StorageManagerFake()) }
    TaskModifyLayout(
        preference = PrimaryPreferences(LocalView.current.context),
        view = LocalView.current,
        modifyState = TaskModifyState.AddTask,
        onFinished = {},
        requestReminder = {},
        viewModel = vmFake
    )
}
