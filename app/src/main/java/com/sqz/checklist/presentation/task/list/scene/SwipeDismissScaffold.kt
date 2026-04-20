package com.sqz.checklist.presentation.task.list.scene

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sqz.checklist.common.AndroidEffectFeedback
import kotlinx.coroutines.delay
import sqz.checklist.common.EffectFeedback
import kotlin.math.abs

/** This method expected to be called only within this package and its sub-packages. **/
internal data class SwipeBackContentState(
    val animationStartThresholdWeight: MutableFloatState,
    val swipeState: SwipeToDismissBoxState,
    val swipeDirection: SwipeToDismissBoxValue,
)

/**
 * **This method expected to be called only within this package and its sub-packages.**
 *
 * @param swipeEnabled whether the swipe is enabled.
 * @param frontContent the content in the front of the swipe scaffold UI.
 * @param backContent the content in the back of the swipe scaffold UI.
 * @param onSwipeToDismiss the callback when the swipe is dismissed.
 * @param keepVisible whether the content is kept visible; [SwipeDismissScaffold] will resize the
 *   UI to visible again after the swipe is dismissed if [keepVisible] is `true`.
 * @param feedback the [EffectFeedback] instance.
 * @param modifier the modifier.
 */
@Composable
internal fun SwipeDismissScaffold(
    swipeEnabled: Boolean,
    frontContent: @Composable (RowScope.() -> Unit),
    backContent: @Composable (RowScope.(SwipeBackContentState) -> Unit),
    onSwipeToDismiss: () -> Unit,
    keepVisible: Boolean,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
) {
    val screenWidthPx = LocalWindowInfo.current.containerSize.width * 0.35f
    val itemState = rememberSwipeToDismissBoxState(
        positionalThreshold = { screenWidthPx } // This seems like not work anymore in material3 1.4.0 ~ 1.5.0-alpha06 or maybe above
    )
    var positionalThreshold by remember { mutableStateOf(58.dp) }

    val density = LocalDensity.current
    var onValueChanged by rememberSaveable { mutableStateOf(false) }
    if (keepVisible && onValueChanged) LaunchedEffect(Unit) { // on reset
        itemState.reset()
        onValueChanged = false
    }
    Column(
        modifier = Modifier
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            .height(if (onValueChanged) 0.dp else Dp.Unspecified),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val weight = remember { mutableFloatStateOf(88f) }
        //var weight by remember { mutableFloatStateOf(88f) }
        SwipeToDismissBox2(
            state = itemState,
            onValueChange = { currentValue ->
                if (!onValueChanged && currentValue != SwipeToDismissBoxValue.Settled) {
                    onValueChanged = true
                    onSwipeToDismiss()
                }
            },
            modifier = modifier.onGloballyPositioned { layoutCoordinates ->
                val widthPx = layoutCoordinates.size.width
                val width = with(density) { (widthPx * 0.35f).toDp() }
                positionalThreshold = width.let { if (it > 200.dp) 200.dp else it }
            },
            swipeEnabled = (itemState.dismissDirection != SwipeToDismissBoxValue.Settled).let {
                if (it) swipeEnabled else true
            },
            backgroundContent = {
                val swipeBackContentState = SwipeBackContentState(
                    animationStartThresholdWeight = weight,
                    swipeState = itemState,
                    swipeDirection = itemState.dismissDirection
                )
                backContent(swipeBackContentState)
            },
            positionalThreshold = positionalThreshold,
            animationStartThreshold = weight.floatValue * 1.5f,
            feedback = feedback,
            content = frontContent
        )
    }
}

@Composable
private fun SwipeToDismissBox2(
    state: SwipeToDismissBoxState,
    onValueChange: (SwipeToDismissBoxValue) -> Unit,
    swipeEnabled: Boolean,
    backgroundContent: @Composable (RowScope.(SwipeToDismissBoxValue) -> Unit),
    modifier: Modifier = Modifier,
    positionalThreshold: Dp = 38.dp, // google broke it in material3 1.4.0, so rewrite this function... fuck
    animationStartThreshold: Float = 80f,
    feedback: EffectFeedback,
    content: @Composable (RowScope.() -> Unit),
) {
    val positionalThresholdPx = with(LocalDensity.current) { positionalThreshold.toPx() }
    var swipeDirection by remember { mutableStateOf(SwipeToDismissBoxValue.Settled) }
    val pressed = remember { mutableStateOf(false) }
    var onDismissPreparer by remember { mutableStateOf(false) }
    val onDismiss = remember { mutableStateOf(false) }
    if (pressed.value) {
        var currentOffset by remember { mutableFloatStateOf(0f) }
        currentOffset = try {
            state.requireOffset()
        } catch (_: IllegalStateException) {
            0f
        }
        // to solve the state.progress not return value before arrive positionalThreshold anymore
        if (abs(currentOffset) > animationStartThreshold) when {
            currentOffset == 0f -> swipeDirection = SwipeToDismissBoxValue.Settled
            currentOffset > 0f -> swipeDirection = SwipeToDismissBoxValue.StartToEnd
            currentOffset < 0f -> swipeDirection = SwipeToDismissBoxValue.EndToStart
        } else {
            swipeDirection = SwipeToDismissBoxValue.Settled
        }
        // to fix the positionalThreshold not work correctly
        if (abs(currentOffset) > positionalThresholdPx) LaunchedEffect(Unit) {
            onDismissPreparer = true
        } else LaunchedEffect(Unit) {
            onDismissPreparer = false
            onDismiss.value = false
        }
    } else if (onDismissPreparer && !onDismiss.value) LaunchedEffect(Unit) {
        onValueChange(state.dismissDirection).also { onDismiss.value = true }
        onDismissPreparer = false
    }
    SwipeEffect(
        feedback = feedback,
        isInSwipeZone = onDismissPreparer || state.targetValue != SwipeToDismissBoxValue.Settled && state.progress != 1.0f,
        isSwipeOut = !onDismiss.value && pressed.value
    )
    SwipeToDismissBox(
        gesturesEnabled = swipeEnabled,
        state = state,
        backgroundContent = { backgroundContent(swipeDirection) },
        onDismiss = { onValueChange(it).also { onDismiss.value = true } },
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) { // detect for the new positionalThreshold
                    awaitFirstDown(pass = PointerEventPass.Initial)
                    pressed.value = true
                    var stillPressed = true
                    while (stillPressed) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        stillPressed = event.changes.any { it.pressed }
                    }
                    pressed.value = false
                }
            }
        },
        content = content
    )
}

@Composable
private fun SwipeEffect(feedback: EffectFeedback, isInSwipeZone: Boolean, isSwipeOut: Boolean) {
    val isVibrated = remember { mutableStateOf(false) }
    if (isInSwipeZone) LaunchedEffect(Unit) {
        feedback.onPressEffect()
        isVibrated.value = true
    } else if (isVibrated.value && isSwipeOut) LaunchedEffect(Unit) {
        feedback.onDragEffect()
        isVibrated.value = false
    }
}

@Preview
@Composable
private fun SwipeDismissScaffoldPreview() {
    val visible = remember { mutableStateOf(true) }
    visible.value.let {
        if (!it) LaunchedEffect(Unit) {
            delay(3168)
            visible.value = true
        }
    }
    SwipeDismissScaffold(
        frontContent = {
            Card(
                modifier = Modifier.size(200.dp, 100.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) { Text("A", Modifier.padding(16.dp)) }
        }, backContent = {
            Card(
                modifier = Modifier.size(200.dp, 100.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) { Text("B", Modifier.padding(16.dp)) }
        }, swipeEnabled = true, onSwipeToDismiss = {
            visible.value = false
        }, keepVisible = visible.value, feedback = AndroidEffectFeedback(
            LocalView.current
        )
    )
}
