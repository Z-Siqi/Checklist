package com.sqz.checklist.presentation.task.list.scene.item

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sqz.checklist.R
import com.sqz.checklist.presentation.task.list.scene.TaskCardScaffold
import com.sqz.checklist.ui.common.TextTooltipBox
import sqz.checklist.common.EffectFeedback
import sqz.checklist.task.api.list.TaskList
import sqz.checklist.task.api.list.model.TaskItemModel

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun DefaultTaskCard(
    task: TaskItemModel,
    taskCardDefaultState: TaskCardDefaultState,
    feedback: EffectFeedback,
    modifier: Modifier,
) {
    val taskViewData = task.taskViewData
    val borderColor = MaterialTheme.colorScheme.let {
        if (isSystemInDarkTheme()) it.surfaceContainerHighest
        else it.surfaceDim
    }
    val titleState = TaskCardScaffold.TitleState(
        text = taskViewData.task.description,
        onOverflowedClick = {
            task.onInfoRequest(TaskList.ListType.Primary)
            feedback.onClickEffect()
        },
        onLongClick = {
            task.onInfoRequest(TaskList.ListType.Primary)
            feedback.onTapEffect()
        },
    ) {
        PinButton(
            onClick = task::onPinRequest,
            isPinned = task.taskViewData.task.isPin,
            feedback = feedback
        )
    }
    val subTitleState = TaskCardScaffold.SubTitleState(
        text = taskCardDefaultState.createDateText,
        subTitleEndRowButtons = taskCardDefaultState.subTitleEndRowButtons
    )
    TaskCardScaffold(
        titleState = titleState,
        subTitleState = subTitleState,
        backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
        backgroundBorder = BorderStroke(1.dp, borderColor),
        backgroundShape = TaskCardScaffold.BgShape.Whole,
        modifier = modifier,
    )
}

@Composable
private fun PinButton(
    onClick: () -> Unit,
    isPinned: Boolean,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
) = TextTooltipBox(R.string.pin, modifier = modifier.requiredSizeIn(minWidth = 30.dp)) {
    IconButton(
        modifier = Modifier.rotate(40f),
        onClick = {
            onClick()
            feedback.onClickEffect()
        }
    ) {
        val iconId = if (isPinned) R.drawable.pinned else R.drawable.pin
        Icon(
            painter = painterResource(iconId),
            contentDescription = stringResource(R.string.pin)
        )
    }
}
