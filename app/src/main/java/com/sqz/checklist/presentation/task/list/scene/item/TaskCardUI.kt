package com.sqz.checklist.presentation.task.list.scene.item

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sqz.checklist.presentation.task.list.scene.SwipeDismissScaffold
import com.sqz.checklist.presentation.task.list.scene.TaskCardScaffold
import sqz.checklist.common.EffectFeedback
import sqz.checklist.task.api.list.TaskList
import sqz.checklist.task.api.list.model.TaskItemModel

@Composable
fun TaskCardUI(
    task: TaskItemModel,
    listType: TaskList.ListType,
    onFinished: () -> Unit,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
    fromList: List<TaskItemModel> = listOf(),
) {
    val taskCardDefaultState = TaskCardDefaultState(
        task = task,
        feedback = feedback,
    )
    SwipeDismissScaffold(
        swipeEnabled = true,
        frontContent = {
            when (listType) {
                TaskList.ListType.Primary, TaskList.ListType.Search -> DefaultTaskCard(
                    task = task,
                    taskCardDefaultState = taskCardDefaultState,
                    feedback = feedback,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                TaskList.ListType.Pinned -> PinnedTaskCard(
                    task = task,
                    taskCardDefaultState = taskCardDefaultState,
                    backgroundShape = fromList.toBgShape(task),
                    feedback = feedback,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                TaskList.ListType.Reminded -> RemindedTaskCard(
                    task = task,
                    taskCardDefaultState = taskCardDefaultState,
                    backgroundShape = fromList.toBgShape(task),
                    feedback = feedback,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        },
        backContent = { itemState ->
            val bgShape = listType.let { type ->
                when (type) {
                    TaskList.ListType.Primary,
                    TaskList.ListType.Search -> TaskCardScaffold.BgShape.Whole

                    else -> fromList.let {
                        if (it.isEmpty() || it.size == 1) TaskCardScaffold.BgShape.Whole
                        else it.toBgShape(task)
                    }
                }
            }
            TaskSwipeBgCard(
                state = itemState,
                horizontalPaddingValue = 14.dp,
                shape = bgShape.shape
            )
        },
        onSwipeToDismiss = onFinished,
        keepVisible = true,
        feedback = feedback,
        modifier = modifier
    )
}

/** This method expected to be called only within this package and its sub-packages. **/
@Immutable
internal data class TaskCardDefaultState(
    val createDateText: String,
    val reminderTimeText: String,
    val subTitleEndRowButtons: @Composable () -> Unit,
)

private fun List<TaskItemModel>.toBgShape(task: TaskItemModel): TaskCardScaffold.BgShape {
    if (this.size == 1) {
        return TaskCardScaffold.BgShape.Whole
    }
    fun TaskItemModel.toId(): Long {
        return this.taskViewData.task.id
    }
    if (this.first().toId() == task.toId()) {
        return TaskCardScaffold.BgShape.TopWhole
    }
    if (this.last().toId() == task.toId()) {
        return TaskCardScaffold.BgShape.BottomWhole
    }
    return TaskCardScaffold.BgShape.CenterWhole
}
