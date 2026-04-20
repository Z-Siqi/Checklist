package com.sqz.checklist.presentation.task.list.scene.item

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sqz.checklist.R
import com.sqz.checklist.ui.common.TextTooltipBox
import sqz.checklist.common.EffectFeedback
import sqz.checklist.common.KmpLocalDatePatternFormatter
import sqz.checklist.data.database.Task
import sqz.checklist.data.database.model.TaskViewData
import sqz.checklist.task.api.list.model.TaskItemModel
import java.text.SimpleDateFormat

/** This method expected to be called only within this package and the filename end with `UI`. **/
@SuppressLint("ComposableNaming")
@Composable
internal fun TaskCardDefaultState(
    task: TaskItemModel,
    feedback: EffectFeedback,
): TaskCardDefaultState {
    val taskViewData = task.taskViewData
    val reminderTimeText = getReminderTimeText(taskViewData)
    val reminderTimeDescription = reminderTimeText?.let {
        stringResource(
            R.string.task_reminded_time, reminderTimeText
        )
    }
    return TaskCardDefaultState(
        createDateText = getTaskCreateDateText(task.taskViewData.task),
        reminderTimeText = reminderTimeDescription ?: "N/A"
    ) {
        DefaultButtonsRow(
            isDetailExist = taskViewData.isDetailExist,
            reminderTooltip = reminderTimeText,
            isReminderSet = !taskViewData.isReminded && taskViewData.reminderTime != null,
            onDetailClick = task::onDetailRequest,
            onReminderClick = task::onReminderRequest,
            onEditClick = task::onEditRequest,
            feedback = feedback,
        )
        Spacer(modifier = Modifier.padding(end = 15.dp))
    }
}

@Composable
private fun DefaultButtonsRow(
    isDetailExist: Boolean,
    reminderTooltip: String?,
    isReminderSet: Boolean,
    onDetailClick: () -> Unit,
    onReminderClick: () -> Unit,
    onEditClick: () -> Unit,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
) = BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.BottomStart) {
    val buttonsArea = androidx.compose.ui.unit.min(maxWidth * 0.38f, 200.dp)
    Row(
        modifier = Modifier
            .widthIn(min = 90.dp)
            .width(buttonsArea),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDetailExist) TextTooltipBox(textRid = R.string.detail) {
            IconButton(modifier = Modifier.requiredSize(32.dp), onClick = {
                onDetailClick()
                feedback.onClickEffect()
            }) {
                Icon(
                    modifier = Modifier.rotate(-5f),
                    painter = painterResource(id = R.drawable.attach),
                    contentDescription = stringResource(R.string.detail)
                )
            }
        } else Spacer(
            modifier = Modifier.requiredSize(32.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        val timerText = reminderTooltip ?: stringResource(R.string.reminder)
        TextTooltipBox(text = timerText) {
            IconButton(modifier = Modifier.requiredSize(32.dp), onClick = {
                onReminderClick()
                feedback.onClickEffect()
            }) {
                val timerIcon =
                    if (isReminderSet) R.drawable.timer_on else R.drawable.timer
                Icon(
                    painter = painterResource(id = timerIcon),
                    contentDescription = timerText
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        TextTooltipBox(textRid = R.string.edit) {
            IconButton(modifier = Modifier.requiredSize(32.dp), onClick = {
                onEditClick()
                feedback.onClickEffect()
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.edit),
                    contentDescription = stringResource(R.string.edit)
                )
            }
        }
    }
}

@Composable
@ReadOnlyComposable
private fun getTaskCreateDateText(task: Task): String {
    val dateFormat = KmpLocalDatePatternFormatter.format(
        task.createDate, stringResource(R.string.task_date_format),
    )
    return stringResource(R.string.task_creation_time, dateFormat)
}

@Composable
@ReadOnlyComposable
private fun getReminderTimeText(taskViewData: TaskViewData): String? {
    val fullDateShort = stringResource(R.string.full_date_short)
    val getTimeInLong = taskViewData.reminderTime.let { it ?: return null }
    if (getTimeInLong <= 1000L) return null
    val formatter = SimpleDateFormat(fullDateShort, LocalLocale.current.platformLocale)
    return formatter.format(getTimeInLong)
}
