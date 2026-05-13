package com.sqz.checklist.presentation.task.modify.dialog.detail.group

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import sqz.checklist.task.api.modify.TaskModify

/** This method expected to be called only within this package and its sub-packages. **/
internal data class ItemContentData(
    val index: Int,
    val isDragging: Boolean,
    val item: TaskModify.Detail.UIState,
    val itemModifier: Modifier,
)

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun DetailDraggableList(
    items: List<TaskModify.Detail.UIState>,
    draggable: Boolean,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    itemContent: @Composable (LazyItemScope.(ItemContentData) -> Unit)
) {
    var nextKey by remember { mutableIntStateOf(1000) }
    val localItems = remember {
        val initialNumbers = (1..items.size).toList().shuffled()
        mutableStateOf(items.mapIndexed { index, item ->
            val key = if (index < initialNumbers.size) initialNumbers[index] else nextKey++
            key to item
        })
    }

    LaunchedEffect(items) {
        localItems.value = items.toStableKeyedList(localItems.value) { nextKey++ }
    }

    val lazyListState = rememberLazyListState()
    val draggingIndex = remember { mutableStateOf<Int?>(null) }
    val fingerY = remember { mutableFloatStateOf(0f) }
    val clickOffsetInItem = remember { mutableFloatStateOf(0f) }

    if (draggable) DraggingIndexListener(
        localItems = localItems,
        draggingIndex = draggingIndex,
        lazyListState = lazyListState,
        fingerY = fingerY,
        clickOffsetInItem = clickOffsetInItem,
        density = LocalDensity.current,
    )

    var initDrag by remember { mutableStateOf(false) }
    if (draggable) {
        initDrag = true
    }
    var initialDraggingIndex by remember { mutableStateOf<Int?>(null) }

    val lazyColumnModifier = Modifier.pointerInput(Unit, initDrag) {
        if (draggable) detectDragGesturesAfterLongPress(
            onDragStart = { offset ->
                lazyListState.layoutInfo.visibleItemsInfo
                    .find { found ->
                        offset.y.toInt() in found.offset..(found.offset + found.size)
                    }
                    ?.let { item ->
                        draggingIndex.value = item.index
                        initialDraggingIndex = item.index
                        fingerY.floatValue = offset.y
                        clickOffsetInItem.floatValue = offset.y - item.offset
                    }
            },
            onDrag = { change, dragAmount ->
                change.consume()
                fingerY.floatValue += dragAmount.y
            },
            onDragEnd = {
                draggingIndex.value?.let { end ->
                    initialDraggingIndex?.let { start ->
                        if (start != end) onMove(start, end)
                    }
                }
                draggingIndex.value = null
            },
            onDragCancel = {
                localItems.value = items.toStableKeyedList(localItems.value) { nextKey++ }
                draggingIndex.value = null
            }
        )
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = lazyColumnModifier
    ) {
        itemsIndexed(
            items = localItems.value,
            key = { _, item -> item.first },
        ) { index, item ->
            val isDragging = index == draggingIndex.value
            val itemModifier = if (isDragging) {
                Modifier
                    .graphicsLayer {
                        val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                            it.key == item.first
                        }
                        if (itemInfo != null) {
                            translationY =
                                fingerY.floatValue - clickOffsetInItem.floatValue - itemInfo.offset
                        }
                        scaleX = 1.05f
                        scaleY = 1.05f
                        alpha = 0.9f
                    }
            } else {
                Modifier.animateItem()
            }

            val itemContentData = ItemContentData(
                index = index,
                isDragging = isDragging,
                item = item.second,
                itemModifier = itemModifier
            )
            itemContent(itemContentData)
        }
    }
}

private fun List<TaskModify.Detail.UIState>.toStableKeyedList(
    oldKeyedList: List<Pair<Int, TaskModify.Detail.UIState>>,
    getNextKey: () -> Int
): List<Pair<Int, TaskModify.Detail.UIState>> {
    val result = MutableList<Pair<Int, TaskModify.Detail.UIState>?>(this.size) { null }
    val available = oldKeyedList.mapIndexed { index, pair -> index to pair }.toMutableList()
    // Phase 1: Exact Match (Content unchanged, but position may have changed)
    for (i in this.indices) {
        val item = this[i]
        val foundIdx = available.indexOfFirst { it.second.second == item }
        if (foundIdx != -1) {
            val original = available.removeAt(foundIdx)
            result[i] = original.second.first to item
        }
    }
    // Phase 2: Matching by Position (Content has changed, but remains in the same position;
    //   typically involves editing a TextField)
    for (i in this.indices) {
        if (result[i] == null) {
            val foundIdx = available.indexOfFirst { it.first == i }
            if (foundIdx != -1) {
                val original = available.removeAt(foundIdx)
                result[i] = original.second.first to this[i]
            }
        }
    }
    // Phase 3: Processing newly added items
    return result.mapIndexed { i, pair ->
        pair ?: (getNextKey() to this[i])
    }
}

@Composable
private fun DraggingIndexListener(
    localItems: MutableState<List<Pair<Int, TaskModify.Detail.UIState>>>,
    draggingIndex: MutableState<Int?>,
    lazyListState: LazyListState,
    fingerY: MutableFloatState,
    clickOffsetInItem: MutableFloatState,
    density: Density,
) = LaunchedEffect(draggingIndex.value) {

    if (draggingIndex.value == null) return@LaunchedEffect

    val itemHeightPx = with(density) { 64.dp.toPx() }

    while (true) {
        val currentIndex = draggingIndex.value ?: break
        val layoutInfo = lazyListState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo

        if (visibleItems.isNotEmpty()) {
            val draggedItemTop = fingerY.floatValue - clickOffsetInItem.floatValue
            val draggedItemBottom = draggedItemTop + itemHeightPx

            val targetItem = visibleItems.find { item ->
                if (item.index == currentIndex) return@find false
                val itemCenter = item.offset + item.size / 2f
                if (currentIndex < item.index) {
                    draggedItemBottom > itemCenter
                } else {
                    draggedItemTop < itemCenter
                }
            }

            if (targetItem != null) {
                val newItems = localItems.value.toMutableList()
                newItems.add(targetItem.index, newItems.removeAt(currentIndex))

                val firstVisibleIndex = lazyListState.firstVisibleItemIndex
                val firstVisibleOffset = lazyListState.firstVisibleItemScrollOffset

                localItems.value = newItems
                draggingIndex.value = targetItem.index

                lazyListState.scrollToItem(firstVisibleIndex, firstVisibleOffset)
            }

            val viewportHeight = layoutInfo.viewportSize.height.toFloat()
            if (viewportHeight > 0f) {
                val scrollEdge = viewportHeight * 0.15f
                val scrollAmount = when {
                    fingerY.floatValue < scrollEdge && lazyListState.canScrollBackward ->
                        (fingerY.floatValue - scrollEdge) / 5f

                    fingerY.floatValue > viewportHeight - scrollEdge && lazyListState.canScrollForward ->
                        (fingerY.floatValue - (viewportHeight - scrollEdge)) / 5f

                    else -> 0f
                }
                if (scrollAmount != 0f) {
                    lazyListState.scrollBy(scrollAmount)
                }
            }
        }
        delay(16)
    }
}
