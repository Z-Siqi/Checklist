package com.sqz.checklist.ui.main.task.layout.item

import android.content.Context
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqz.checklist.R
import com.sqz.checklist.database.Task
import com.sqz.checklist.ui.main.task.CardHeight
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel

/**
 * List of TaskLayout (Expected @TaskLayout call this)
 */
@Composable
fun LazyList(
    listState: ListData,
    lazyState: LazyListState,
    undoTask: (state: SwipeToDismissBoxState) -> Unit,
    isInSearch: @Composable () -> Boolean,
    context: Context,
    modifier: Modifier = Modifier,
    taskState: TaskLayoutViewModel
) {
    var inSearch by rememberSaveable { mutableStateOf(false) }
    val screenWidthPx = LocalConfiguration.current.screenWidthDp * LocalDensity.current.density
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = lazyState
    ) {
        if (!inSearch) {
            if (listState.isRemindedItem.isNotEmpty()) {
                item {
                    RemindedItem(
                        isRemindedItem = listState.isRemindedItem,
                        screenWidthPx = screenWidthPx,
                        context = context,
                        taskState = taskState
                    )
                }
            }
            if (listState.pinnedItem.isNotEmpty()) {
                item {
                    PinnedItem(
                        pinnedItem = listState.pinnedItem,
                        screenWidthPx = screenWidthPx,
                        context = context,
                        taskState = taskState
                    )
                }
            }
            item { Spacer(modifier = modifier.height(20.dp)) }
            items(listState.item, key = { it.id }) { task ->
                MainListItem(
                    task = task,
                    screenWidthPx = screenWidthPx,
                    undoTask = undoTask,
                    context = context,
                    taskState = taskState
                )
            }
        } else {
            item { Spacer(modifier = modifier.height(72.dp)) }
            items(listState.inSearchItem, key = { it.id }) { task ->
                MainListItem(
                    task = task,
                    screenWidthPx = screenWidthPx,
                    undoTask = undoTask,
                    context = context,
                    taskState = taskState
                )
            }
        }
        item { Spacer(modifier = modifier.height(10.dp)) }
    }
    inSearch = isInSearch() // Searching UI & search state
}

/**
 * Task list item
 */
@Composable
private fun MainListItem(
    task: Task,
    screenWidthPx: Float,
    undoTask: (state: SwipeToDismissBoxState) -> Unit,
    context: Context,
    taskState: TaskLayoutViewModel
) {
    val state = rememberSwipeToDismissBoxState(
        positionalThreshold = { screenWidthPx * 0.35f },
    )
    TaskItem(
        task = task,
        onTaskItemClick = taskState::onTaskItemClick,
        checked = { taskState.taskChecked(it, context) },
        getIsHistory = taskState.getIsHistory(task.id),
        context = context, itemState = state,
        mode = ItemMode.NormalTask
    )
    undoTask(state)
}

/** Reminded item list **/
@Composable
private fun RemindedItem(
    isRemindedItem: List<Task>,
    screenWidthPx: Float, context: Context,
    modifier: Modifier = Modifier,
    taskState: TaskLayoutViewModel
) {
    val remindedHeight = (39 + (CardHeight * isRemindedItem.size)).dp
    val animatedRemindedHeight by animateDpAsState(
        targetValue = remindedHeight, label = "Reminded Height"
    )
    Spacer(modifier = modifier.height(10.dp))
    OutlinedCard(
        modifier = modifier
            .height(animatedRemindedHeight)
            .padding(start = 8.dp, end = 8.dp),
        shape = ShapeDefaults.Large,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Text(
            text = stringResource(R.string.just_reminded),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = modifier.padding(start = 9.dp, top = 5.dp, bottom = 5.dp)
        )
        LazyColumn {
            items(isRemindedItem, key = { it.id }) { task ->
                val state = rememberSwipeToDismissBoxState(
                    positionalThreshold = { screenWidthPx * 0.35f },
                )
                TaskItem(
                    task = task,
                    onTaskItemClick = taskState::onTaskItemClick,
                    checked = { taskState.taskChecked(it, context) },
                    getIsHistory = taskState.getIsHistory(task.id),
                    context = context,
                    itemState = state,
                    mode = ItemMode.RemindedTask
                )
            }
        }
    }
}

/** Pinned item list **/
@Composable
private fun PinnedItem(
    pinnedItem: List<Task>,
    screenWidthPx: Float, context: Context,
    modifier: Modifier = Modifier,
    taskState: TaskLayoutViewModel
) {
    val pinnedHeight = (39 + (CardHeight * pinnedItem.size)).dp
    val animatedPinnedHeight by animateDpAsState(
        targetValue = pinnedHeight, label = "Pinned Height"
    )
    Spacer(modifier = modifier.height(10.dp))
    OutlinedCard(
        modifier = modifier
            .height(animatedPinnedHeight)
            .padding(start = 8.dp, end = 8.dp),
        shape = ShapeDefaults.Large,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = stringResource(R.string.pinned_task),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.outline,
            modifier = modifier.padding(start = 9.dp, top = 5.dp, bottom = 5.dp)
        )
        LazyColumn {
            items(pinnedItem, key = { it.id }) { task ->
                val state = rememberSwipeToDismissBoxState(
                    positionalThreshold = { screenWidthPx * 0.35f },
                )
                TaskItem(
                    task = task,
                    onTaskItemClick = taskState::onTaskItemClick,
                    checked = { taskState.taskChecked(it, context) },
                    getIsHistory = taskState.getIsHistory(task.id),
                    context = context,
                    itemState = state,
                    mode = ItemMode.PinnedTask
                )
            }
        }
    }
}
