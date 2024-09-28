package com.sqz.checklist.ui.main.task.layout

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sqz.checklist.R
import com.sqz.checklist.database.Task
import com.sqz.checklist.ui.main.task.ListData
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import com.sqz.checklist.ui.main.task.layout.item.ItemMode
import com.sqz.checklist.ui.main.task.layout.item.TaskData
import com.sqz.checklist.ui.main.task.layout.item.TaskItem

/**
 * --- List of task ---
 **/
const val CardHeight = 120

@Composable
fun LazyList(
    listState: ListData,
    lazyState: LazyListState,
    reminderCard: (Int) -> Unit,
    setReminder: (Int) -> Unit,
    undoTask: (state: SwipeToDismissBoxState) -> Unit,
    isInSearch: Boolean,
    context: Context,
    modifier: Modifier = Modifier,
    taskState: TaskLayoutViewModel = viewModel()
) {
    val screenWidthPx = LocalConfiguration.current.screenWidthDp * LocalDensity.current.density
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = lazyState
    ) {
        if (!isInSearch) {
            if (listState.isRemindedItem.isNotEmpty()) {
                item {
                    RemindedItem(
                        isRemindedItem = listState.isRemindedItem,
                        reminderCard = reminderCard,
                        setReminder = setReminder,
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
                        reminderCard = reminderCard,
                        setReminder = setReminder,
                        screenWidthPx = screenWidthPx,
                        context = context,
                        taskState = taskState
                    )
                }
            }
            item { Spacer(modifier = modifier.height(20.dp)) }
            items(listState.item, key = { it.id }) {
                MainListItem(
                    it = it,
                    reminderCard = reminderCard,
                    setReminder = setReminder,
                    screenWidthPx = screenWidthPx,
                    undoTask = undoTask,
                    context = context,
                    taskState = taskState
                )
            }
        } else {
            item { Spacer(modifier = modifier.height(72.dp)) }
            items(listState.inSearchItem, key = { it.id }) {
                MainListItem(
                    it = it,
                    reminderCard = reminderCard,
                    setReminder = setReminder,
                    screenWidthPx = screenWidthPx,
                    undoTask = undoTask,
                    context = context,
                    taskState = taskState
                )
            }
        }
        item { Spacer(modifier = modifier.height(10.dp)) }
    }
}

/**
 * Task list item
 **/
@Composable
private fun MainListItem(
    it: Task,
    reminderCard: (Int) -> Unit,
    setReminder: (Int) -> Unit,
    screenWidthPx: Float,
    undoTask: (state: SwipeToDismissBoxState) -> Unit,
    context: Context,
    taskState: TaskLayoutViewModel = viewModel()
) {
    val state = rememberSwipeToDismissBoxState(
        positionalThreshold = { screenWidthPx * 0.35f },
    )
    TaskItem(
        taskData = TaskData(it.id, it.description, it.createDate, it.reminder),
        reminderCardClick = { id -> reminderCard(id) },
        setReminderClick = { id -> setReminder(id) },
        isPin = it.isPin, context = context, itemState = state,
        mode = ItemMode.NormalTask, taskState = taskState
    )
    undoTask(state)
}

/** Reminded item list **/
@Composable
private fun RemindedItem(
    isRemindedItem: List<Task>,
    reminderCard: (Int) -> Unit, setReminder: (Int) -> Unit,
    screenWidthPx: Float, context: Context,
    modifier: Modifier = Modifier,
    taskState: TaskLayoutViewModel = viewModel()
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
            items(isRemindedItem, key = { it.id }) {
                val state = rememberSwipeToDismissBoxState(
                    positionalThreshold = { screenWidthPx * 0.35f },
                )
                TaskItem(
                    taskData = TaskData(
                        it.id, it.description, it.createDate, it.reminder
                    ),
                    reminderCardClick = { id -> reminderCard(id) },
                    setReminderClick = { id -> setReminder(id) },
                    isPin = it.isPin, context = context, itemState = state,
                    mode = ItemMode.RemindedTask, taskState = taskState
                )
            }
        }
    }
}

/** Pinned item list **/
@Composable
private fun PinnedItem(
    pinnedItem: List<Task>,
    reminderCard: (Int) -> Unit, setReminder: (Int) -> Unit,
    screenWidthPx: Float, context: Context,
    modifier: Modifier = Modifier,
    taskState: TaskLayoutViewModel = viewModel()
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
            items(pinnedItem, key = { it.id }) {
                val state = rememberSwipeToDismissBoxState(
                    positionalThreshold = { screenWidthPx * 0.35f },
                )
                TaskItem(
                    taskData = TaskData(
                        it.id, it.description, it.createDate, it.reminder
                    ),
                    reminderCardClick = { id -> reminderCard(id) },
                    setReminderClick = { id -> setReminder(id) },
                    isPin = it.isPin, context = context, itemState = state,
                    mode = ItemMode.PinnedTask, taskState = taskState
                )
            }
        }
    }
}
