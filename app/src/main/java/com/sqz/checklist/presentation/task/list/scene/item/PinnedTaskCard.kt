package com.sqz.checklist.presentation.task.list.scene.item

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.lerp
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
internal fun PinnedTaskCard(
    task: TaskItemModel,
    taskCardDefaultState: TaskCardDefaultState,
    backgroundShape: TaskCardScaffold.BgShape,
    feedback: EffectFeedback,
    modifier: Modifier,
) {
    val taskViewData = task.taskViewData
    val borderColor = lerp(
        start = MaterialTheme.colorScheme.primaryFixedDim,
        stop = MaterialTheme.colorScheme.outline,
        fraction = 0.5f
    )
    val backgroundColor = lerp(
        start = MaterialTheme.colorScheme.inversePrimary,
        stop = MaterialTheme.colorScheme.surfaceContainerLow,
        fraction = 0.95f
    )
    val titleState = TaskCardScaffold.TitleState(
        text = taskViewData.task.description,
        onOverflowedClick = {
            task.onInfoRequest(TaskList.ListType.Pinned)
            feedback.onClickEffect()
        },
        onLongClick = {
            task.onInfoRequest(TaskList.ListType.Pinned)
            feedback.onTapEffect()
        },
    ) {
        PinButton(
            onClick = task::onPinRequest,
            feedback = feedback,
        )
    }
    val subTitleState = TaskCardScaffold.SubTitleState(
        text = taskCardDefaultState.createDateText,
        subTitleEndRowButtons = taskCardDefaultState.subTitleEndRowButtons
    )
    TaskCardScaffold(
        titleState = titleState,
        subTitleState = subTitleState,
        backgroundColor = backgroundColor,
        backgroundBorder = BorderStroke((1.168).dp, borderColor),
        backgroundShape = backgroundShape,
        modifier = modifier,
    )
}

@Composable
private fun PinButton(
    onClick: () -> Unit,
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
        Icon(
            painter = painterResource(R.drawable.pinned),
            contentDescription = stringResource(R.string.pin)
        )
    }
}
