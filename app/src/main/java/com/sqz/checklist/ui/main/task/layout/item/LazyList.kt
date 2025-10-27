package com.sqz.checklist.ui.main.task.layout.item

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.sqz.checklist.R
import com.sqz.checklist.database.Task
import com.sqz.checklist.database.TaskViewData
import com.sqz.checklist.ui.common.unit.navBarsBottomDp
import com.sqz.checklist.ui.main.task.CardHeight
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import com.sqz.checklist.ui.main.task.TaskLayoutViewModelPreview
import com.sqz.checklist.ui.main.task.handler.ReminderHandler
import com.sqz.checklist.ui.theme.Theme
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
            items(listState.item, key = { it.task.id }) { taskView ->
                MainListItem(
                    taskView = taskView,
                    context = context,
                    taskState = taskState,
                    allowSwipe = allowSwipe,
                    modifier = modifier
                )
            }
        } else {
            item { Spacer(modifier = modifier.height(searchBarSpace.dp)) }
            items(listState.inSearchItem, key = { it.task.id }) { taskView ->
                MainListItem(
                    taskView = taskView,
                    context = context,
                    taskState = taskState,
                    allowSwipe = allowSwipe,
                    modifier = modifier
                )
            }
        }
        item { Spacer(modifier = modifier.height(2.dp + navBarsBottomDp())) }
    }
    @Suppress("AssignedValueIsNeverRead")
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


/**
 * Task list item
 */
@Composable
private fun MainListItem(
    taskView: TaskViewData,
    context: Context,
    allowSwipe: Boolean,
    taskState: TaskLayoutViewModel,
    modifier: Modifier = Modifier,
) {
    SwipeAbleTaskCard(
        taskView = taskView,
        onTaskItemClick = taskState::onTaskItemClick,
        checked = { taskState.taskChecked(it) },
        getIsHistory = taskState.getIsHistory(taskView.task.id),
        context = context,
        mode = ItemMode.NormalTask,
        allowSwipe = allowSwipe,
        modifier = modifier,
    )
}

private val colors: Theme @Composable get() = Theme.color

/** Reminded item list **/
@Composable
private fun RemindedItem(
    isRemindedItem: List<TaskViewData>,
    context: Context,
    allowSwipe: Boolean,
    modifier: Modifier = Modifier,
    taskState: TaskLayoutViewModel
) {
    HighlightItems(
        items = isRemindedItem,
        title = stringResource(R.string.recently_reminded),
        titleColor = MaterialTheme.colorScheme.onTertiaryContainer,
        containerColor = colors.remindedBackgroundColor,
        modifier = modifier,
        border = BorderStroke((1.2).dp, colors.remindedBorderColor)
    ) { entity ->
        val taskView = entity as TaskViewData
        SwipeAbleTaskCard(
            taskView = taskView,
            onTaskItemClick = taskState::onTaskItemClick,
            checked = { taskState.taskChecked(it) },
            getIsHistory = taskState.getIsHistory(taskView.task.id),
            context = context,
            mode = ItemMode.RemindedTask,
            allowSwipe = allowSwipe,
        )
    }
}

/** Pinned item list **/
@Composable
private fun PinnedItem(
    pinnedItem: List<TaskViewData>,
    context: Context,
    allowSwipe: Boolean,
    modifier: Modifier = Modifier,
    taskState: TaskLayoutViewModel
) {
    HighlightItems(
        items = pinnedItem,
        title = stringResource(R.string.pinned_task),
        titleColor = MaterialTheme.colorScheme.outline,
        containerColor = colors.pinnedBackgroundColor,
        modifier = modifier
    ) { entity ->
        val taskView = entity as TaskViewData
        SwipeAbleTaskCard(
            taskView = taskView,
            onTaskItemClick = taskState::onTaskItemClick,
            checked = { taskState.taskChecked(it) },
            getIsHistory = taskState.getIsHistory(taskView.task.id),
            context = context,
            mode = ItemMode.PinnedTask,
            allowSwipe = allowSwipe,
        )
    }
}

/** Highlight items layout **/
@Composable
private fun HighlightItems(
    items: List<Any>,
    title: String,
    titleColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    border: BorderStroke = CardDefaults.outlinedCardBorder(),
    itemContent: @Composable (item: Any) -> Unit
) {
    val notEmpty = items.isNotEmpty()
    val density = LocalDensity.current
    val textHeight = with(density) { 15.sp.toDp() }.value
    val height = if (notEmpty) (textHeight + (CardHeight * items.size)).dp else 0.dp
    val animatedHeight by animateDpAsState(targetValue = height, label = "Height")
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = animatedHeight, max = if (notEmpty) Dp.Unspecified else animatedHeight)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            .padding(start = 8.dp, end = 8.dp, top = 10.dp),
        shape = ShapeDefaults.Large,
        colors = CardDefaults.cardColors(containerColor),
        border = border
    ) {
        Text(
            text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = titleColor,
            modifier = Modifier.padding(start = 9.dp, top = 5.dp, bottom = 5.dp),
        )
        var cacheList by remember { mutableIntStateOf(items.size) }
        if (cacheList != items.size) { // refresh list
            items.fastForEach { item ->
                itemContent(item)
            }
            LaunchedEffect(Unit) { cacheList = items.size }
        } else {
            items.fastForEach { item ->
                itemContent(item)
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
private fun Preview() {
    val task = Task(0, "The quick brown fox jumps over the lazy dog.", LocalDate.now())
    val item = listOf(TaskViewData(task, isDetailExist = false, false, null))
    val context = LocalContext.current
    LazyList(
        ListData(false, item, item, item), rememberLazyListState(),
        { false }, context, Modifier, TaskLayoutViewModelPreview()
    )
}
