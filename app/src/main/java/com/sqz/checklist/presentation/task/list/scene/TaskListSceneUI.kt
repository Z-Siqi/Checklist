package com.sqz.checklist.presentation.task.list.scene

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqz.checklist.R
import com.sqz.checklist.presentation.task.list.TaskListViewModel
import com.sqz.checklist.presentation.task.list.scene.item.TaskCardUI
import com.sqz.checklist.ui.common.unit.navBarsBottomDp
import kotlinx.coroutines.delay
import sqz.checklist.common.EffectFeedback
import sqz.checklist.task.api.list.TaskList
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun BoxScope.TaskListSceneUI(
    viewModel: TaskListViewModel,
    onSearchCancel: () -> Unit,
    view: android.view.View,
    feedback: EffectFeedback,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    val listInventory by viewModel.listInventory.collectAsState().also {
        if (it.value is TaskList.Inventory.Loading) {
            throw IllegalAccessException("Should not be loading state")
        }
    }

    Box(modifier = Modifier.align(Alignment.TopCenter)) {
        if (listInventory is TaskList.Inventory.Default) {
            DefaultList(
                scrollState = lazyListState,
                listData = listInventory as TaskList.Inventory.Default,
                view = view,
                feedback = feedback,
                viewModel = viewModel
            )
        }
        if (listInventory is TaskList.Inventory.Search) {
            SearchList(
                scrollState = lazyListState,
                listData = listInventory as TaskList.Inventory.Search,
                onSearchCancel = onSearchCancel,
                view = view,
                feedback = feedback,
                viewModel = viewModel
            )
        }
    }

    if (viewModel.undoState.collectAsState().value) {
        // close undo listener
        LocalWindowInfo.current.isWindowFocused.let {
            if (!it) {
                viewModel.setUndoBreakFactor(null)
            } else {
                LaunchedEffect(Unit) {
                    var maxLoop = 50
                    while (maxLoop > 1) {
                        delay(168.milliseconds)
                        viewModel.setUndoBreakFactor(lazyListState)
                        maxLoop--
                    }
                }
            }
        }
        // undo button
        UndoButton(
            view = view,
            onClick = viewModel::onUndoClick,
            feedback = feedback,
        )
    } else LaunchedEffect(Unit) {
        viewModel.resetUndoReminder()

        // run this once undo state false, then loop 1 min a time
        viewModel.removeRemindedInfoByTime(androidContext = view.context)
    }
}

@Composable
private fun DefaultList(
    scrollState: LazyListState,
    listData: TaskList.Inventory.Default,
    view: android.view.View,
    feedback: EffectFeedback,
    viewModel: TaskListViewModel,
) {
    val remindedList by listData.remindedList.collectAsState(initial = listOf())
    val pinnedList by listData.pinnedList.collectAsState(initial = listOf())
    val primaryList by listData.primaryList.collectAsState(initial = listOf())

    LazyColumn(
        state = scrollState,
    ) {
        if (remindedList.isNotEmpty()) defaultListTitleItem(
            text = { stringResource(R.string.recently_reminded) },
            textColor = {
                lerp(
                    start = MaterialTheme.colorScheme.onTertiaryContainer,
                    stop = MaterialTheme.colorScheme.outline,
                    fraction = 0.7f
                )
            }
        )
        items(
            items = remindedList,
            key = { "${TaskList.ListType.Reminded}_${it.taskViewData.task.id}" }
        ) {
            Row(Modifier.animateItem()) {
                TaskCardUI(
                    task = viewModel.safeTaskItemModel(it),
                    listType = TaskList.ListType.Reminded,
                    onFinished = {
                        viewModel.onFinished(it, scrollState, view.context)
                    },
                    feedback = feedback,
                    fromList = remindedList,
                )
            }
        }
        if (pinnedList.isNotEmpty()) defaultListTitleItem(
            text = { stringResource(R.string.pinned_task) },
            textColor = {
                lerp(
                    start = MaterialTheme.colorScheme.onPrimaryContainer,
                    stop = MaterialTheme.colorScheme.outline,
                    fraction = 0.78f
                )
            }
        )
        items(
            items = pinnedList,
            key = { "${TaskList.ListType.Pinned}_${it.taskViewData.task.id}" }
        ) {
            Row(Modifier.animateItem()) {
                TaskCardUI(
                    task = viewModel.safeTaskItemModel(it),
                    listType = TaskList.ListType.Pinned,
                    onFinished = {
                        viewModel.onFinished(it, scrollState, view.context)
                    },
                    feedback = feedback,
                    fromList = pinnedList,
                )
            }
        }
        if (remindedList.isEmpty() && pinnedList.isEmpty()) item {
            Spacer(modifier = Modifier.height(15.dp))
        } else defaultListTitleItem(
            text = { stringResource(R.string.todo_tasks) },
            textColor = { MaterialTheme.colorScheme.outline }
        )
        items(
            items = primaryList,
            key = { "${TaskList.ListType.Primary}_${it.taskViewData.task.id}" }
        ) {
            Row(Modifier.animateItem()) {
                TaskCardUI(
                    task = viewModel.safeTaskItemModel(it),
                    listType = TaskList.ListType.Primary,
                    onFinished = {
                        viewModel.onFinished(it, scrollState, view.context)
                    },
                    feedback = feedback,
                )
            }
        }
        item { Spacer(modifier = Modifier.height(2.dp + navBarsBottomDp())) }
    }
}

private fun LazyListScope.defaultListTitleItem(
    text: @Composable () -> String, textColor: @Composable () -> Color
) = item {
    val backgroundModifier = Modifier
        .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape)
        .padding(vertical = 3.dp, horizontal = 12.dp)
    Column(Modifier.animateItem()) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = text(),
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor(),
            modifier = Modifier.padding(start = 18.dp) then backgroundModifier,
        )
    }
}

@Composable
private fun SearchList(
    scrollState: LazyListState,
    listData: TaskList.Inventory.Search,
    onSearchCancel: () -> Unit,
    view: android.view.View,
    feedback: EffectFeedback,
    viewModel: TaskListViewModel,
) {
    val searchQuery = listData.searchQuery
    val searchList by listData.inSearchList.collectAsState(initial = listOf())

    var foldHideProgress by remember { mutableFloatStateOf(0f) }

    PredictiveBackHandler { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                // Use backEvent.progress (0.0 to 1.0) to update your UI/animations
                foldHideProgress = backEvent.progress
            }
            // Gesture completed successfully (progress reaches 1.0)
            viewModel.setSearchState(false)
            onSearchCancel()
        } catch (_: CancellationException) {
            // Gesture was canceled by the user reversing the swipe
        }
    }

    LazyColumn(state = scrollState) {
        item {
            val searchBarHeight: Dp = LocalDensity.current.let { density ->
                val paddingHeight = 42.dp
                val textHeightDp = with(density) { ((24.sp).toPx() * 1.5f).toDp() }
                paddingHeight + textHeightDp
            }
            Spacer(
                modifier = Modifier.height(searchBarHeight - (searchBarHeight * foldHideProgress))
            )
        }
        items(
            items = searchList,
            key = { it.taskViewData.task.id }
        ) {
            Row(Modifier.animateItem()) {
                TaskCardUI(
                    task = viewModel.safeTaskItemModel(it),
                    listType = TaskList.ListType.Search,
                    onFinished = {
                        viewModel.onFinished(it, scrollState, view.context)
                    },
                    feedback = feedback,
                )
            }
        }
    }

    SlideUpDisappear(progress = foldHideProgress) {
        TaskSearchBar(
            searchQuery = searchQuery,
            onSearchQueryChange = viewModel::onSearchQueryChange,
        )
    }
}

@Composable
private fun SlideUpDisappear(
    progress: Float, modifier: Modifier = Modifier, content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.graphicsLayer {
            val p = progress.coerceIn(0f, 1f)
            translationY = -size.height * p
            alpha = 1f - p
        }
    ) { content() }
}
