package com.sqz.checklist.presentation.history.task.list

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sqz.checklist.presentation.history.task.TaskHistoryViewModel
import com.sqz.checklist.ui.common.unit.navBarsBottomDp
import sqz.checklist.common.EffectFeedback
import sqz.checklist.history.api.task.TaskHistory

@Composable
fun TaskHistoryListUI(
    viewModel: TaskHistoryViewModel,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
    onLongPress: (taskId: Long) -> Unit,
) {
    val historyInventory by viewModel.historyInventory.collectAsState().also {
        if (it.value !is TaskHistory.Inventory.Default) {
            throw IllegalStateException("Current state is not allowed in TaskHistoryListUI!")
        }
    }
    val defaultInventory = historyInventory as TaskHistory.Inventory.Default
    val taskList by defaultInventory.historyList.collectAsState(listOf())
    LazyColumn(modifier = modifier) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(taskList, key = { it.isHistoryId }) { task ->
            TaskHistoryCard(
                historyLong = task.isHistoryId,
                taskDescription = task.description,
                createDate = task.createDate,
                onLongClick = { onLongPress(task.id).also { feedback.onTapEffect() } },
                onClick = {
                    viewModel.singleSelectTask(task.id).also { feedback.onClickEffect() }
                },
                isSelected = task.id == defaultInventory.selectedTaskId,
                modifier = Modifier.animateItem()
            )
        }

        item { Spacer(modifier = Modifier.height(2.dp + navBarsBottomDp())) }
    }
}
