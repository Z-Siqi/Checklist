package com.sqz.checklist.ui.main.task.layout

import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberBasicTooltipState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.sqz.checklist.R
import com.sqz.checklist.database.TaskDetailType
import com.sqz.checklist.notification.PermissionState
import com.sqz.checklist.ui.main.NavExtendedButtonData
import com.sqz.checklist.ui.main.NavMode
import com.sqz.checklist.ui.main.NavTooltipContent
import com.sqz.checklist.ui.main.OnClickType
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import com.sqz.checklist.ui.main.task.layout.function.TaskDetailData
import com.sqz.checklist.ui.main.task.layout.function.TaskDetailDialog
import com.sqz.checklist.ui.material.NonExtendedTooltip
import com.sqz.checklist.ui.material.TextTooltipBox
import com.sqz.checklist.ui.material.dialog.TaskChangeContentDialog
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
        confirm = { text, pin, reminder, detail, detailString, byteArray ->
            coroutineScope.launch {
                viewModel.insertTask(text, pin, detail, detailString, byteArray).let { taskId ->
                    if (reminder) viewModel.reminderActionCaller(
                        taskId, null, true, text
                    ).also {
                        Toast.makeText(
                            view.context,
                            view.context.getString(R.string.task_is_created),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                viewModel.taskDetailDataSaver().releaseMemory()
                taskAddCard = false
            }
        },
        reminderButton = viewModel.notificationInitState(view.context).let {
            it == PermissionState.Notification || it == PermissionState.Both
        },
        detailData = viewModel.taskDetailDataSaver(),
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
    confirm: (
        text: String, pin: Boolean, reminder: Boolean,
        detailType: TaskDetailType?, detailDataString: String?, detailDataByteArray: ByteArray?
    ) -> Unit,
    reminderButton: Boolean,
    detailData: TaskDetailData,
    view: View,
) {
    val state = rememberTextFieldState()
    val noDoNothing = stringResource(R.string.no_do_nothing)
    var pin by rememberSaveable { mutableStateOf(false) }
    var reminder by rememberSaveable { mutableStateOf(false) }
    var detail by rememberSaveable { mutableStateOf(false) }
    TaskChangeContentDialog(
        onDismissRequest = { onDismissRequest().also { detailData.releaseMemory() } },
        confirm = {
            if (state.text.toString() != "") confirm(
                state.text.toString(), pin, reminder,
                detailData.detailType(), detailData.detailString(), detailData.detailByteArray()
            ) else {
                Toast.makeText(view.context, noDoNothing, Toast.LENGTH_SHORT).show()
            }
        },
        state = state,
        extraButtonTop = {
            val onPinClick = {
                pin = !pin
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
            TextTooltipBox(textRid = R.string.create_as_pin) {
                IconButton(onClick = onPinClick, modifier = Modifier.rotate(40f)) {
                    Icon(
                        painter = painterResource(if (pin) R.drawable.pinned else R.drawable.pin),
                        contentDescription = stringResource(R.string.create_as_pin)
                    )
                }
            }
        },
        extraButtonBottom = {
            TextTooltipBox(textRid = R.string.create_task_detail) {
                IconButton(
                    onClick = { detail = !detail },
                    colors = if (detailData.detailType() != null) {
                        IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    } else IconButtonDefaults.iconButtonColors()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.attach),
                        contentDescription = stringResource(R.string.create_task_detail)
                    )
                }
            }
            if (reminderButton) {
                val onReminderClick = {
                    reminder = !reminder
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }
                TextTooltipBox(textRid = R.string.create_with_reminder) {
                    IconButton(onClick = onReminderClick) {
                        Icon(
                            painter = painterResource(if (reminder) R.drawable.timer_on else R.drawable.timer),
                            contentDescription = stringResource(R.string.create_with_reminder)
                        )
                    }
                }
            }
        },
        title = stringResource(R.string.create_task),
        confirmText = stringResource(if (!reminder) R.string.add else R.string.next),
        doneImeAction = true
    )
    if (detail) TaskDetailDialog(
        onDismissRequest = { onDismissClick ->
            if (onDismissClick != null && onDismissClick) {
                detailData.releaseMemory()
            }
            detail = false
        },
        confirm = { type, string, bitmap ->
            detailData.setter(type, string, bitmap)
            detail = false
        },
        title = stringResource(R.string.create_task_detail),
        detailData = detailData,
        view = view
    )
}
