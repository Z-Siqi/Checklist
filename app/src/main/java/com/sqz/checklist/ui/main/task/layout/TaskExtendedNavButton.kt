package com.sqz.checklist.ui.main.task.layout

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberBasicTooltipState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.core.content.ContextCompat
import com.sqz.checklist.R
import com.sqz.checklist.database.TaskDetailType
import com.sqz.checklist.notification.PermissionState
import com.sqz.checklist.ui.main.NavExtendedButtonData
import com.sqz.checklist.ui.main.NavMode
import com.sqz.checklist.ui.main.NavTooltipContent
import com.sqz.checklist.ui.main.OnClickType
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import com.sqz.checklist.ui.main.task.layout.action.TaskDetailDialog
import com.sqz.checklist.ui.material.dialog.TaskChangeContentDialog
import com.sqz.checklist.ui.material.TextTooltipBox
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
        confirm = { text, pin, reminder, detail, detailString ->
            coroutineScope.launch {
                viewModel.insertTask(text, pin, detail, detailString).let { taskId ->
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
                taskAddCard = false
            }
        },
        reminderButton = viewModel.notificationInitState(view.context).let {
            it == PermissionState.Notification || it == PermissionState.Both
        },
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
        text: String, pin: Boolean, reminder: Boolean, detailType: TaskDetailType?, detailDataString: String?
    ) -> Unit,
    reminderButton: Boolean,
    view: View,
) {
    val state = rememberTextFieldState()
    val noDoNothing = stringResource(R.string.no_do_nothing)
    var pin by rememberSaveable { mutableStateOf(false) }
    var reminder by rememberSaveable { mutableStateOf(false) }
    var detail by rememberSaveable { mutableStateOf(false) }
    var detailType by rememberSaveable { mutableStateOf<TaskDetailType?>(null) }
    var detailString by rememberSaveable { mutableStateOf<String?>(null) }
    TaskChangeContentDialog(
        onDismissRequest = onDismissRequest,
        confirm = {
            if (state.text.toString() != "") confirm(
                state.text.toString(), pin, reminder, detailType, detailString
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
                    colors = if (detailType != null) {
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
                detailType = null
                detailString = null
            }
            detail = false
        },
        confirm = { type, string ->
            detailType = type
            detailString = string
            detail = false
        },
        title = stringResource(R.string.create_task_detail),
        getType = detailType,
        getString = detailString,
        view = view
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NonExtendedTooltip(text: String, view: View) {
    var rememberPosition by remember { mutableStateOf(IntOffset.Zero) }
    Spacer(modifier = Modifier
        .size(40.dp, 30.dp)
        .onGloballyPositioned { layoutCoordinates ->
            val position = layoutCoordinates.positionOnScreen()
            if (position.x < 2147483647L || position.y < 2147483647L) {
                rememberPosition = IntOffset(position.x.toInt(), position.y.toInt())
            }
        })
    TooltipBox(
        positionProvider = object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset = IntOffset(rememberPosition.x, rememberPosition.y)
        },
        tooltip = {
            PlainTooltip { Text(text = text) }
            LaunchedEffect(true) { // click feedback
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ContextCompat.getSystemService(
                    view.context, Vibrator::class.java
                )?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                ) else view.playSoundEffect(SoundEffectConstants.CLICK)
            }
        },
        state = rememberTooltipState(initialIsVisible = true, isPersistent = true)
    ) {}
}
