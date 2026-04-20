package com.sqz.checklist.presentation.task.list.scene.item

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sqz.checklist.R
import com.sqz.checklist.presentation.task.list.scene.SwipeBackContentState

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun TaskSwipeBgCard(
    state: SwipeBackContentState,
    horizontalPaddingValue: Dp,
    shape: Shape = ShapeDefaults.ExtraLarge
) {
    val isStartToEnd = state.swipeDirection == SwipeToDismissBoxValue.StartToEnd
    val isEndToStart = state.swipeDirection == SwipeToDismissBoxValue.EndToStart
    val isInProcess = state.swipeState.progress != 1.0f
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp, horizontal = horizontalPaddingValue),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondary),
        shape = shape
    ) {
        val weightModifier = Modifier.weight(0.05f)
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = weightModifier.onGloballyPositioned { layoutCoordinates ->
                val widthPx = layoutCoordinates.size.width
                state.animationStartThresholdWeight.floatValue = widthPx.toFloat()
            })
            AnimateInFinishedTask(isStartToEnd, Alignment.Start)
            if (isInProcess && !isStartToEnd && !isEndToStart) {
                AnimateInFinishedTask(true, Alignment.CenterHorizontally)
            } else {
                Spacer(modifier = Modifier.weight(0.7f))
            }
            AnimateInFinishedTask(isEndToStart, Alignment.End)
            Spacer(modifier = weightModifier)
        }
    }
}

@Composable
private fun AnimateInFinishedTask(visible: Boolean = false, alignment: Alignment.Horizontal) {
    AnimatedVisibility(
        visible = visible,
        enter = expandHorizontally(expandFrom = alignment) + fadeIn(initialAlpha = 0.5f),
        exit = fadeOut()
    ) {
        Icon(imageVector = Icons.Filled.Check, contentDescription = stringResource(R.string.check))
    }
}
