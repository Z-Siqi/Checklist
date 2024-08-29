package com.sqz.checklist.ui.main.task.layout.item

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ItemBox(
    description: String,
    dateText: String,
    reminderOnClick: () -> Unit,
    editOnClick: () -> Unit,
    timerIconState: Boolean,
    pinIconState: Boolean,
    topRightIconOnClick: () -> Unit,
    tooltipRemindText: String?,
    state: SwipeToDismissBoxState,
    mode: ItemMode,
    modifier: Modifier = Modifier,
    horizontalEdge: Int = 14
) { // Swipe-able card UI
    val bgStartEnd = horizontalEdge.dp
    val startEnd = bgStartEnd - 2.dp
    SwipeToDismissBox(
        state = state,
        backgroundContent = { // back of card
            Card(
                modifier = modifier
                    .fillMaxSize()
                    .padding(start = bgStartEnd, end = bgStartEnd, top = 4.dp, bottom = 4.dp),
                colors = cardBackgroundColor(true),
                shape = ShapeDefaults.ExtraLarge
            ) {
                val views = (state.progress in 0.1f..0.9f)
                val isStartToEnd = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
                val isEndToStart = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
                Row(
                    modifier = modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(modifier = modifier.weight(0.05f))
                    AnimateInFinishedTask((views && isStartToEnd), Alignment.Start)
                    Spacer(modifier = modifier.weight(0.7f))
                    AnimateInFinishedTask((views && isEndToStart), Alignment.End)
                    Spacer(modifier = modifier.weight(0.05f))
                }
            }
        },
    ) { // front of card
        OutlinedCard(
            modifier = modifier
                .fillMaxSize()
                .padding(start = startEnd, end = startEnd, top = 4.dp, bottom = 4.dp),
            colors = cardBackgroundColor(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceDim),
            shape = ShapeDefaults.ExtraLarge
        ) {
            Box(
                modifier = modifier.padding(bottom = 8.dp, top = 5.dp, start = 12.dp, end = 11.dp)
            ) {
                ItemContent(
                    description = description, createDate = dateText,
                    descriptionBgColor = cardBackgroundColor(),
                    reminderOnClick = reminderOnClick, editOnClick = editOnClick,
                    timerIconState = timerIconState, topRightIconState = pinIconState,
                    topRightIconOnClick = topRightIconOnClick,
                    tooltipRemindText = tooltipRemindText, mode = mode
                )
            }
        }
    }
}

@Composable
private fun cardBackgroundColor(onSlide: Boolean = false): CardColors {
    return if (!onSlide) {
        CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer)
    } else CardDefaults.cardColors(MaterialTheme.colorScheme.secondary)
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview() {
    Box(modifier = Modifier.size(500.dp, 120.dp)) {
        ItemBox(
            description = "description",
            dateText = "createDate",
            reminderOnClick = {},
            editOnClick = {},
            timerIconState = false,
            topRightIconOnClick = {},
            pinIconState = false,
            tooltipRemindText = null,
            state = rememberSwipeToDismissBoxState(),
            mode = ItemMode.NormalTask
        )
    }
}
