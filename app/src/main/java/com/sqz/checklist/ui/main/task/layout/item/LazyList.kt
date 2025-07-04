package com.sqz.checklist.ui.main.task.layout.item

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqz.checklist.R
import com.sqz.checklist.database.Task
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import com.sqz.checklist.ui.main.task.TaskLayoutViewModelPreview
import com.sqz.checklist.ui.main.task.cardHeight
import com.sqz.checklist.ui.main.task.handler.ReminderHandler
import com.sqz.checklist.ui.theme.Theme
import com.sqz.checklist.ui.theme.unit.navBarsBottomDp
import kotlinx.coroutines.delay
import java.time.LocalDate

/**
 * List of TaskLayout (Expected @TaskLayout call this)
 */
@Composable
fun LazyList(
    listState: ListData,
    lazyState: LazyListState,
    isInSearch: @Composable () -> Boolean,
    context: Context,
    modifier: Modifier = Modifier,
    taskState: TaskLayoutViewModel,
    searchBarSpace: Int = 72
) {
    var allowSwipe by remember { mutableStateOf(true) }
    var inSearch by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = lazyState
    ) {
        if (!inSearch) {
            if (listState.item.isNotEmpty()) {
                item {
                    RemindedItem(
                        isRemindedItem = listState.isRemindedItem,
                        context = context,
                        taskState = taskState,
                        allowSwipe = allowSwipe,
                        modifier = modifier
                    )
                }
            }
            if (listState.item.isNotEmpty()) {
                item {
                    PinnedItem(
                        pinnedItem = listState.pinnedItem,
                        context = context,
                        taskState = taskState,
                        allowSwipe = allowSwipe,
                        modifier = modifier
                    )
                }
            }
            item { Spacer(modifier = modifier.height(20.dp)) }
            items(listState.item, key = { it.id }) { task ->
                MainListItem(
                    task = task,
                    context = context,
                    taskState = taskState,
                    allowSwipe = allowSwipe,
                    modifier = modifier
                )
            }
        } else {
            item { Spacer(modifier = modifier.height(searchBarSpace.dp)) }
            items(listState.inSearchItem, key = { it.id }) { task ->
                MainListItem(
                    task = task,
                    context = context,
                    taskState = taskState,
                    allowSwipe = allowSwipe,
                    modifier = modifier
                )
            }
        }
        item { Spacer(modifier = modifier.height(2.dp + navBarsBottomDp())) }
    }
    inSearch = isInSearch() // Searching UI & search state
    AutoScrollList(
        lazyState = lazyState,
        listState = listState,
        reminderHandler = taskState.reminderHandler
    )
    LaunchedEffect(lazyState.isScrollInProgress) {
        // Fix the issue may cause accidental swipe task when scroll list
        allowSwipe = !lazyState.isScrollInProgress
    }
}

@Composable
private fun AutoScrollList(
    lazyState: LazyListState, listState: ListData, reminderHandler: ReminderHandler
) {
    LaunchedEffect(listState.pinnedItem) { // Auto scroll to pinned area when first pin is set
        if (listState.pinnedItem.size == 1 && lazyState.canScrollBackward && lazyState.firstVisibleItemIndex in 1..2) {
            delay(50)
            lazyState.animateScrollToItem(0)
        }
    }
    var rememberValue by rememberSaveable { mutableIntStateOf(0) }
    val value by reminderHandler.getIsRemindedNum()!!.collectAsState(initial = 0)
    var init by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(value, rememberValue) {
        if (value != rememberValue) {
            if (value >= 1 && init) {
                reminderHandler.requestUpdateList()
                // Auto scroll to reminded area when first notification is arrived and at top
                if (value == 1 && lazyState.canScrollBackward && lazyState.firstVisibleItemIndex in 1..2) {
                    delay(50)
                    lazyState.animateScrollToItem(0)
                    Log.d("LazyListScroll", "Scroll to top (item index 0)")
                }
            }
            rememberValue = value
        }
        init = true
    }
}

@Composable
private fun rememberSwipeState(): SwipeToDismissBoxState {
    val swipePosition = swipePosition()
    return rememberSwipeToDismissBoxState(positionalThreshold = { swipePosition })
}

@ReadOnlyComposable
@Composable
private fun swipePosition(): Float {
    val screenWidthPx = LocalWindowInfo.current.containerSize.width.toFloat()
    return screenWidthPx * 0.35f
}

/**
 * Task list item
 */
@Composable
private fun MainListItem(
    task: Task,
    context: Context,
    allowSwipe: Boolean,
    taskState: TaskLayoutViewModel,
    modifier: Modifier = Modifier,
) {
    SwipeAbleTaskCard(
        task = task,
        onTaskItemClick = taskState::onTaskItemClick,
        checked = { taskState.taskChecked(it) },
        getIsHistory = taskState.getIsHistory(task.id),
        context = context,
        itemState = rememberSwipeState(),
        mode = ItemMode.NormalTask,
        allowSwipe = allowSwipe,
        modifier = modifier,
        databaseRepository = taskState.database()
    )
}

private val colors: Theme @Composable get() = Theme.color

/** Reminded item list **/
@Composable
private fun RemindedItem(
    isRemindedItem: List<Task>,
    context: Context,
    allowSwipe: Boolean,
    modifier: Modifier = Modifier,
    taskState: TaskLayoutViewModel
) {
    val notEmpty = isRemindedItem.isNotEmpty()
    val density = LocalDensity.current
    var textHeight by remember { mutableIntStateOf(25) }
    val remindedHeight =
        if (notEmpty) (25 + textHeight + (cardHeight(context) * isRemindedItem.size)).dp else 0.dp
    val animatedRemindedHeight by animateDpAsState(
        targetValue = remindedHeight, label = "Reminded Height"
    )
    OutlinedCard(
        modifier = modifier
            .height(animatedRemindedHeight)
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 10.dp),
        shape = ShapeDefaults.Large,
        colors = CardDefaults.cardColors(colors.remindedBackgroundColor),
        border = BorderStroke(1.2.dp, colors.remindedBorderColor)
    ) {
        Text(
            text = stringResource(R.string.recently_reminded), fontSize = 15.sp,
            fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(start = 9.dp, top = 5.dp, bottom = 5.dp),
            onTextLayout = { textHeight = with(density) { it.size.height.toDp() }.value.toInt() }
        )
        LazyColumn(userScrollEnabled = false) {
            items(isRemindedItem, key = { it.id }) { task ->
                SwipeAbleTaskCard(
                    task = task,
                    onTaskItemClick = taskState::onTaskItemClick,
                    checked = { taskState.taskChecked(it) },
                    getIsHistory = taskState.getIsHistory(task.id),
                    context = context,
                    itemState = rememberSwipeState(),
                    mode = ItemMode.RemindedTask,
                    allowSwipe = allowSwipe,
                    databaseRepository = taskState.database()
                )
            }
        }
    }
}

/** Pinned item list **/
@Composable
private fun PinnedItem(
    pinnedItem: List<Task>,
    context: Context,
    allowSwipe: Boolean,
    modifier: Modifier = Modifier,
    taskState: TaskLayoutViewModel
) {
    val notEmpty = pinnedItem.isNotEmpty()
    val density = LocalDensity.current
    var textHeight by remember { mutableIntStateOf(25) }
    val pinnedHeight =
        if (notEmpty) (25 + textHeight + (cardHeight(context) * pinnedItem.size)).dp else 0.dp
    val animatedPinnedHeight by animateDpAsState(
        targetValue = pinnedHeight, label = "Pinned Height"
    )
    OutlinedCard(
        modifier = modifier
            .height(animatedPinnedHeight)
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 10.dp),
        shape = ShapeDefaults.Large,
        colors = CardDefaults.cardColors(colors.pinnedBackgroundColor)
    ) {
        Text(
            text = stringResource(R.string.pinned_task), fontSize = 15.sp,
            fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 9.dp, top = 5.dp, bottom = 5.dp),
            onTextLayout = { textHeight = with(density) { it.size.height.toDp() }.value.toInt() }
        )
        LazyColumn(userScrollEnabled = false) {
            items(pinnedItem, key = { it.id }) { task ->
                SwipeAbleTaskCard(
                    task = task,
                    onTaskItemClick = taskState::onTaskItemClick,
                    checked = { taskState.taskChecked(it) },
                    getIsHistory = taskState.getIsHistory(task.id),
                    context = context,
                    itemState = rememberSwipeState(),
                    mode = ItemMode.PinnedTask,
                    allowSwipe = allowSwipe,
                    databaseRepository = taskState.database()
                )
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
private fun Preview() {
    val item = listOf(Task(0, "The quick brown fox jumps over the lazy dog.", LocalDate.now()))
    val context = LocalContext.current
    LazyList(
        ListData(false, item, item, item), rememberLazyListState(),
        { false }, context, Modifier, TaskLayoutViewModelPreview()
    )
}
