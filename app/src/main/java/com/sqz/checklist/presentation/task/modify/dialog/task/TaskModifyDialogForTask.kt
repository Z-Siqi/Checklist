package com.sqz.checklist.presentation.task.modify.dialog.task

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sqz.checklist.R
import com.sqz.checklist.common.AndroidEffectFeedback
import com.sqz.checklist.presentation.task.modify.TaskModifyViewModel
import com.sqz.checklist.ui.common.AdaptiveTieredFlowLayout
import com.sqz.checklist.ui.common.TextTooltipBox
import com.sqz.checklist.ui.common.TieredFlowAlignment
import com.sqz.checklist.ui.common.unit.isSmallScreenSizeForDialog
import com.sqz.checklist.ui.common.unit.pxToDp
import com.sqz.checklist.ui.common.unit.pxToDpInt
import com.sqz.checklist.ui.common.verticalColumnScrollbar
import com.sqz.checklist.ui.theme.UISizeLimit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import sqz.checklist.common.EffectFeedback
import sqz.checklist.data.database.repository.task.TaskRepositoryFake
import sqz.checklist.data.storage.manager.StorageManagerFake
import sqz.checklist.task.api.TaskModify

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun TaskModifyDialogForTask(
    onDetailRequest: () -> Unit,
    onTextChange: (String) -> Unit,
    onTypeChange: (TaskModify.Task.ModifyType) -> Unit,
    onDismissRequest: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    taskUIState: TaskModify.Task.UIState,
    isDetailSet: Boolean,
    isModified: Boolean,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
) {
    val focus = remember { FocusManager() }
    val isSmallScreenSize = isSmallScreenSizeForDialog()
    ThisDialog(onDismissRequest, isModified, isSmallScreenSize, focus, modifier) {
        ThisDialogTitle(
            taskModifyType = taskUIState.type,
            onTypeChange = { onTypeChange(it).also { feedback.onClickEffect() } },
            isSmallScreenSize = isSmallScreenSize
        )
        Spacer(modifier = Modifier.height(if (isSmallScreenSize) 8.dp else 20.dp))
        TaskDescriptionField(
            focusManager = focus,
            isSmallScreenSize = isSmallScreenSize,
            setText = taskUIState.description,
            onValueChange = onTextChange,
        )
        Spacer(modifier = Modifier.height(if (isSmallScreenSize) 10.dp else 24.dp))
        ThisDialogButtons(
            taskModifyType = taskUIState.type,
            onTypeChange = { onTypeChange(it).also { feedback.onClickEffect() } },
            onCancel = { onCancel().also { feedback.onClickEffect() } },
            onConfirm = { onConfirm().also { feedback.onClickEffect() } },
            onDetailRequest = { onDetailRequest().also { feedback.onClickEffect() } },
            taskDescription = taskUIState.description,
            isSmallScreenSize = isSmallScreenSize,
            isDetailSet = isDetailSet,
        )
    }
}

private class FocusManager {
    private val _isClear = MutableStateFlow(false)

    val onClear = _isClear.asStateFlow()

    fun clearFocus() = _isClear.update { true }

    fun reset() = _isClear.update { false }
}

@Composable
private fun TaskDescriptionField(
    focusManager: FocusManager,
    isSmallScreenSize: Boolean,
    setText: String,
    onValueChange: (String) -> Unit,
) {
    val state = rememberTextFieldState(initialText = setText)
    LaunchedEffect(Unit) { // Update change (when recomposed)
        if (state.text.toString() != setText) {
            state.clearText()
            state.edit { insert(0, setText) }
        }
    }
    LaunchedEffect(state.text, setText) { // Callback change
        onValueChange(state.text.toString())
    }
    val fieldScrollState = rememberScrollState()
    val focus = LocalFocusManager.current
    focusManager.onClear.collectAsState().value.let {
        if (it) {
            focus.clearFocus()
            focusManager.reset()
        }
    }
    TextField(
        state = state,
        placeholder = { if (!isSmallScreenSize) Text(stringResource(R.string.plan_something)) },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        onKeyboardAction = { focus.clearFocus() },
        modifier = Modifier
            .fillMaxWidth()
            .verticalColumnScrollbar(
                scrollState = fieldScrollState, scrollBarCornerRadius = 12f,
                endPadding = -8f, topBottomPadding = 6f,
                scrollBarTrackColor = MaterialTheme.colorScheme.outlineVariant,
                scrollBarColor = MaterialTheme.colorScheme.outline,
                showScrollBar = fieldScrollState.let { it.canScrollBackward || it.canScrollForward },
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        shape = MaterialTheme.shapes.large,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        lineLimits = TextFieldLineLimits.MultiLine(
            minHeightInLines = 1,
            maxHeightInLines = 4,
        ),
        scrollState = fieldScrollState
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThisDialogTitle(
    taskModifyType: TaskModify.Task.ModifyType,
    onTypeChange: (TaskModify.Task.ModifyType) -> Unit,
    isSmallScreenSize: Boolean,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
) {
    val windowSize = LocalWindowInfo.current.containerSize
    val title = when (taskModifyType) {
        is TaskModify.Task.ModifyType.NewTask -> stringResource(R.string.create_task)
        is TaskModify.Task.ModifyType.EditTask -> stringResource(R.string.edit_task)
        else -> "?"
    }
    val titleStyle = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.Bold, lineHeight = TextUnit.Unspecified
    )
    Text(
        text = title,
        style = titleStyle,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .requiredWidthIn(max = (windowSize.width.pxToDpInt() / 2.1).dp)
            .requiredHeightIn(max = (windowSize.height.pxToDp() / 5)),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 5.sp, maxFontSize = titleStyle.fontSize)
    )
    if (taskModifyType is TaskModify.Task.ModifyType.NewTask) {
        Spacer(modifier = Modifier.weight(1f))
        if (!isSmallScreenSize) Spacer(modifier = Modifier.width(8.dp))
        ToggleButton(
            checked = taskModifyType.isPin,
            onCheckedChange = { onTypeChange(taskModifyType.onPinClick()) },
            modifier = Modifier
                .requiredWidthIn(min = 40.dp)
                .requiredHeightIn(max = (windowSize.height.pxToDp() / 5))
        ) {
            val iconInt = taskModifyType.isPin.let {
                if (it) R.drawable.pinned else R.drawable.pin
            }
            val rotateModifier = Modifier.let {
                if (taskModifyType.isPin) it else it.rotate(40f)
            }
            TextTooltipBox(textRid = R.string.create_as_pin) {
                Icon(
                    painter = painterResource(iconInt),
                    contentDescription = stringResource(R.string.create_as_pin),
                    modifier = rotateModifier
                )
            }
            if (!isSmallScreenSize) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.pin_task),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 2.sp, maxFontSize = LocalTextStyle.current.fontSize
                    ),
                    modifier = Modifier.widthIn(max = 70.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThisDialogButtons(
    taskModifyType: TaskModify.Task.ModifyType,
    onTypeChange: (TaskModify.Task.ModifyType) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onDetailRequest: () -> Unit,
    taskDescription: String,
    isSmallScreenSize: Boolean,
    isDetailSet: Boolean,
) = AdaptiveTieredFlowLayout(
    modifier = Modifier.fillMaxWidth(),
    topAlignment = TieredFlowAlignment.Start,
    bottomAlignment = TieredFlowAlignment.End,
    insertBottomBeforeTopWrap = true,
    topContent = {
        val detailText = when (taskModifyType) {
            is TaskModify.Task.ModifyType.NewTask -> stringResource(R.string.create_task_detail)
            is TaskModify.Task.ModifyType.EditTask -> stringResource(R.string.edit_task_detail)
            else -> "?"
        }
        TextTooltipBox(text = detailText) {
            OutlinedButton(
                onClick = onDetailRequest,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors().let {
                    if (!isDetailSet) it else it.copy(
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                }
            ) {
                val iconModifier = Modifier.let {
                    if (isSmallScreenSize) it else it.padding(vertical = 3.5.dp)
                }
                Icon(
                    painter = painterResource(R.drawable.attach),
                    contentDescription = detailText,
                    modifier = iconModifier.let { if (isSmallScreenSize) it else it.size(20.dp) }
                )
                if (!isSmallScreenSize) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.detail))
                }
            }
        }
        val reminderTooltipRid = R.string.create_with_reminder
        if (taskModifyType is TaskModify.Task.ModifyType.NewTask) TextTooltipBox(reminderTooltipRid) {
            val reminderIconId = taskModifyType.withReminder.let {
                if (it) R.drawable.timer_on else R.drawable.timer
            }
            if (isSmallScreenSize) {
                FilledIconToggleButton(
                    checked = taskModifyType.withReminder,
                    onCheckedChange = { onTypeChange(taskModifyType.onReminderClick()) },
                ) {
                    Icon(
                        painter = painterResource(reminderIconId),
                        contentDescription = stringResource(R.string.create_with_reminder)
                    )
                }
            } else Row(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(reminderIconId),
                    contentDescription = stringResource(R.string.reminder),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = taskModifyType.withReminder,
                    onCheckedChange = { onTypeChange(taskModifyType.onReminderClick()) },
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    },
    bottomContent = {
        TextButton(onClick = onCancel) {
            Text(
                text = stringResource(R.string.cancel),
                fontWeight = FontWeight.SemiBold
            )
        }
        val confirmText = when (taskModifyType) {
            is TaskModify.Task.ModifyType.NewTask -> stringResource(R.string.add)
            is TaskModify.Task.ModifyType.EditTask -> stringResource(R.string.edit)
            else -> "?"
        }
        val confirmEnabled = taskDescription.isNotBlank()
        val view = LocalView.current
        TextButton(
            onClick = onConfirm,
            enabled = confirmEnabled
        ) {
            Text(
                text = confirmText,
                modifier = if (confirmEnabled) Modifier else Modifier.pointerInput(Unit) {
                    detectTapGestures {
                        Toast.makeText(view.context, R.string.no_do_nothing, Toast.LENGTH_SHORT).show()
                    }
                },
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
)

@Composable
private fun ThisDialog(
    onDismissRequest: () -> Unit,
    isModified: Boolean,
    isSmallScreenSize: Boolean,
    focus: FocusManager,
    modifier: Modifier = Modifier,
    content: @Composable (ColumnScope.() -> Unit),
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = !isModified
        ),
    ) {
        val surfaceModifier = Modifier.let { modifier ->
            if (!isSmallScreenSize) {
                modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
            } else {
                modifier.fillMaxSize()
            }
        }
        val surfaceShape = MaterialTheme.shapes.let {
            if (isSmallScreenSize) it.extraSmall else it.extraLarge
        }
        UISizeLimit {
            val contentModifier = Modifier.let {
                val scrollState = rememberScrollState()
                it.verticalColumnScrollbar(
                    scrollState = scrollState, scrollBarCornerRadius = 12f,
                    scrollBarTrackColor = MaterialTheme.colorScheme.outlineVariant,
                    scrollBarColor = MaterialTheme.colorScheme.outline,
                    showScrollBar = scrollState.canScrollBackward || scrollState.canScrollForward
                ) then it.verticalScroll(scrollState)
            }.let {
                if (isSmallScreenSize) it.padding(1.dp) else it.padding(24.dp)
            }
            Surface(
                modifier = surfaceModifier then modifier,
                shape = surfaceShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = contentModifier.pointerInput(Unit) {
                        detectTapGestures { focus.clearFocus() }
                    },
                    content = content,
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    val vmFake = viewModel { TaskModifyViewModel(TaskRepositoryFake(), StorageManagerFake()) }
    val v = LocalView.current
    vmFake.newTask(v)
    TaskModifyDialogForTask(
        taskUIState = vmFake.taskModify.collectAsState().value.taskState!!,
        onDetailRequest = {}, onTextChange = {}, onTypeChange = {},
        onDismissRequest = {}, onCancel = {}, onConfirm = {}, isDetailSet = false,
        isModified = false, feedback = AndroidEffectFeedback(v)
    )
}
