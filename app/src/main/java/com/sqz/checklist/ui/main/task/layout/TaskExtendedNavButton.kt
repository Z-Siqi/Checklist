package com.sqz.checklist.ui.main.task.layout

import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberBasicTooltipState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.sqz.checklist.R
import com.sqz.checklist.notification.PermissionState
import com.sqz.checklist.ui.main.NavExtendedButtonData
import com.sqz.checklist.ui.main.NavMode
import com.sqz.checklist.ui.main.NavTooltipContent
import com.sqz.checklist.ui.main.OnClickType
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import com.sqz.checklist.ui.main.task.layout.function.CreateTask
import com.sqz.checklist.ui.main.task.layout.function.TaskDetailData
import com.sqz.checklist.ui.main.task.layout.function.TaskModifyDialog
import com.sqz.checklist.ui.material.NonExtendedTooltip
import kotlinx.coroutines.launch

/** Nav Extended Button Connect Data **/
data class NavConnectData(
    val canScroll: Boolean = false,
    val scrollToFirst: Boolean = false,
    val scrollToBottom: Boolean = false,
    val searchState: Boolean = false,
    val canScrollForward: Boolean = false,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun taskExtendedNavButton(
    mode: NavMode, view: View, viewModel: TaskLayoutViewModel
): NavExtendedButtonData {
    val connector = viewModel.navExtendedConnector.collectAsState().value
    val coroutineScope = rememberCoroutineScope()
    var taskAddCard by rememberSaveable { mutableStateOf(false) }
    val buttonInfo = stringResource(if (!connector.searchState) R.string.add else R.string.cancel)
    val icon = @Composable {
        val icons = if (!connector.searchState) Icons.Filled.AddCircle else Icons.Filled.Close
        Icon(icons, contentDescription = buttonInfo)
    }
    val label = @Composable { Text(buttonInfo) }
    val extendedTooltipState = connector.canScroll && !connector.searchState
    val tooltipState = rememberBasicTooltipState(isPersistent = extendedTooltipState)
    val tooltipContent = @Composable {
        if (extendedTooltipState) NavTooltipContent(
            mode = mode, onClickType = { onClickType ->
                when (onClickType) {
                    OnClickType.Search -> {
                        tooltipState.dismiss()
                        val it = NavConnectData(searchState = true)
                        viewModel.updateNavConnector(it, it)
                    }

                    OnClickType.ScrollUp -> coroutineScope.launch {
                        tooltipState.dismiss()
                        val it = NavConnectData(scrollToFirst = true)
                        viewModel.updateNavConnector(it, it)
                    }

                    OnClickType.ScrollDown -> coroutineScope.launch {
                        tooltipState.dismiss()
                        val it = NavConnectData(scrollToBottom = true)
                        viewModel.updateNavConnector(it, it)
                    }
                }
            }, view = view, scrollUp = !connector.canScrollForward
        ) else NonExtendedTooltip(
            text = buttonInfo, view = view
        )
    }
    val onClick = {
        if (!connector.searchState) taskAddCard = true else {
            val it = NavConnectData(searchState = false)
            viewModel.updateNavConnector(it, NavConnectData(searchState = true))
            viewModel.updateInSearch(reset = true)
        }
        view.playSoundEffect(SoundEffectConstants.CLICK)
    }

    // to add task
    if (taskAddCard) TaskAddCard(
        onDismissRequest = { taskAddCard = false },
        confirm = {
            coroutineScope.launch {
                viewModel.modifyHandler.insertTask(
                    it.description, it.pin,
                    it.detail?.type, it.detail?.dataString, it.detail?.dataByte
                ).let { taskId ->
                    if (it.reminder) viewModel.reminderHandler.requestReminder(taskId).also {
                        Toast.makeText(
                            view.context,
                            view.context.getString(R.string.task_is_created),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                viewModel.modifyHandler.taskDetailDataSaver().releaseMemory()
                taskAddCard = false
            }
        },
        permissionState = viewModel.reminderHandler.notificationInitState(view.context),
        view = view,
    )

    return NavExtendedButtonData(
        icon = icon,
        label = label,
        tooltipContent = tooltipContent,
        tooltipState = tooltipState,
        onClick = onClick
    )
}

@Composable
private fun TaskAddCard(
    onDismissRequest: () -> Unit,
    confirm: (CreateTask) -> Unit,
    permissionState: PermissionState,
    view: View,
) {
    TaskModifyDialog(
        reminderButton = permissionState.let {
            it == PermissionState.Notification || it == PermissionState.Both
        },
        confirm = {
            confirm(it)
            TaskDetailData.instance().releaseMemory()
        },
        onDismissRequest = onDismissRequest,
        view = view
    )
}
