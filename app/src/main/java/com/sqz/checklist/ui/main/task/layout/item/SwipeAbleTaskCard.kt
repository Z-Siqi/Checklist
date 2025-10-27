package com.sqz.checklist.ui.main.task.layout.item

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getSystemService
import com.sqz.checklist.R
import com.sqz.checklist.database.Task
import com.sqz.checklist.database.TaskViewData
import com.sqz.checklist.ui.common.unit.isApi29AndAbove
import com.sqz.checklist.ui.main.task.CardHeight
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Swipe-able task item for list (Expected @LazyList call this)
 */
@Composable
fun SwipeAbleTaskCard(
    taskView: TaskViewData,
    onTaskItemClick: (task: TaskViewData, type: CardClickType, context: Context) -> Unit,
    checked: (id: Long) -> Unit,
    getIsHistory: Boolean,
    context: Context,
    mode: ItemMode,
    allowSwipe: Boolean,
    modifier: Modifier = Modifier,
) { // Process card action
    val task = taskView.task
    val screenWidthPx = LocalWindowInfo.current.containerSize.width * 0.35f
    val itemState = rememberSwipeToDismissBoxState(
        positionalThreshold = { screenWidthPx } // This seems like not work anymore in material3 1.4.0 ~ 1.5.0-alpha06 or maybe above
    )
    val density = LocalDensity.current
    var onValueChange by rememberSaveable { mutableStateOf(false) }
    if (onValueChange && !getIsHistory) LaunchedEffect(Unit) { // on restore
        itemState.reset()
        onValueChange = false
    }
    Column(
        modifier = Modifier
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            .height(if (onValueChange) 0.dp else Dp.Unspecified),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val horizontalEdge =
            if (mode == ItemMode.PinnedTask || mode == ItemMode.RemindedTask) 10 else 14
        val bgStartEnd = horizontalEdge.dp
        val startEnd = bgStartEnd - 2.dp
        var weight by remember { mutableFloatStateOf(88f) }
        var positionalThreshold by remember { mutableStateOf(58.dp) }
        SwipeToDismissBox(
            state = itemState,
            onValueChange = { currentValue ->
                if (!onValueChange && currentValue != SwipeToDismissBoxValue.Settled) { // on checked
                    onValueChange = true
                    checked(task.id)
                }
            },
            modifier = modifier.onGloballyPositioned { layoutCoordinates ->
                val widthPx = layoutCoordinates.size.width
                val width = with(density) { (widthPx * 0.35f).toDp() }
                positionalThreshold = width.let { if (it > 200.dp) 200.dp else it }
            },
            swipeEnabled = if (itemState.dismissDirection != SwipeToDismissBoxValue.Settled) allowSwipe else true,
            backgroundContent = { swipeDirection -> // back of card
                val isStartToEnd = swipeDirection == SwipeToDismissBoxValue.StartToEnd
                val isEndToStart = swipeDirection == SwipeToDismissBoxValue.EndToStart
                val isInProcess = itemState.progress != 1.0f
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = bgStartEnd, end = bgStartEnd, top = 4.dp, bottom = 4.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondary),
                    shape = ShapeDefaults.ExtraLarge
                ) {
                    val weightModifier = Modifier.weight(0.05f)
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(modifier = weightModifier.onGloballyPositioned { layoutCoordinates ->
                            val widthPx = layoutCoordinates.size.width
                            weight = widthPx.toFloat()
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
            },
            positionalThreshold = positionalThreshold,
            animationStartThreshold = weight * 1.5f,
        ) { // front of card
            val remindTime = @Composable { // The text of reminder time
                val getTimeInLong = taskView.reminderTime ?: 0L
                val fullDateShort = stringResource(R.string.full_date_short)
                if (getTimeInLong <= 1000L) null else SimpleDateFormat(
                    fullDateShort,
                    Locale.getDefault()
                ).format(getTimeInLong)
            }
            val dateReminderText = stringResource(
                R.string.task_reminded_time, remindTime().toString()
            )
            val formatter = DateTimeFormatter.ofPattern(
                stringResource(R.string.task_date_format), Locale.getDefault()
            )
            val dateText = stringResource(
                R.string.task_creation_time, task.createDate.format(formatter)
            )
            //val reminderState = databaseRepository.reminderState(task.reminder)
            TaskCardContent(
                textState = TaskTextState(
                    description = task.description,
                    dateText = dateText,
                    dateReminderText = dateReminderText,
                    reminderTooltip = if (taskView.reminderTime != null) remindTime() else null
                ),
                onClick = { type -> onTaskItemClick(taskView, type, context) },
                iconState = TaskIconState(
                    isPinned = task.isPin,
                    isReminderSet = !taskView.isReminded && taskView.reminderTime != null,
                    isDetailExist = taskView.isDetailExist
                ),
                modifier = Modifier
                    .padding(start = startEnd, end = startEnd, top = 4.dp, bottom = 4.dp)
                    .heightIn(min = CardHeight.dp),
                mode = mode,
            )
        }
    }
}

@Composable
private fun SwipeToDismissBox(
    state: SwipeToDismissBoxState,
    onValueChange: (SwipeToDismissBoxValue) -> Unit,
    swipeEnabled: Boolean,
    backgroundContent: @Composable (RowScope.(SwipeToDismissBoxValue) -> Unit),
    modifier: Modifier = Modifier,
    positionalThreshold: Dp = 38.dp, // google broke it in material3 1.4.0, so rewrite this function... fuck
    animationStartThreshold: Float,
    content: @Composable (RowScope.() -> Unit),
) {
    val positionalThresholdPx = with(LocalDensity.current) { positionalThreshold.toPx() }
    var swipeDirection by remember { mutableStateOf(SwipeToDismissBoxValue.Settled) }
    var pressed by remember { mutableStateOf(false) }
    var onDismissPreparer by remember { mutableStateOf(false) }
    var onDismiss by remember { mutableStateOf(false) }
    if (pressed) {
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
            onDismiss = false
        }
    } else if (onDismissPreparer && !onDismiss) LaunchedEffect(Unit) {
        onValueChange(state.dismissDirection).also { onDismiss = true }
        onDismissPreparer = false
    }
    if (isApi29AndAbove) Vibrate(
        context = LocalContext.current,
        isInTarget = onDismissPreparer || state.targetValue != SwipeToDismissBoxValue.Settled && state.progress != 1.0f,
        isBackable = !onDismiss && pressed
    )
    SwipeToDismissBox(
        gesturesEnabled = swipeEnabled,
        state = state,
        backgroundContent = { backgroundContent(swipeDirection) },
        onDismiss = { onValueChange(it).also { onDismiss = true } },
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) { // detect for the new positionalThreshold
                    awaitFirstDown(pass = PointerEventPass.Initial)
                    pressed = true
                    var stillPressed = true
                    while (stillPressed) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        stillPressed = event.changes.any { it.pressed }
                    }
                    pressed = false
                }
            }
        },
        content = content
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
private fun Vibrate(context: Context, isInTarget: Boolean, isBackable: Boolean) {
    var isOn by remember { mutableStateOf(false) }
    if (isInTarget) LaunchedEffect(Unit) {
        getSystemService(context, Vibrator::class.java)?.vibrate(
            VibrationEffect.createPredefined(
                VibrationEffect.EFFECT_TICK
            )
        )
        isOn = true
    } else if (isOn && isBackable) LaunchedEffect(Unit) {
        getSystemService(context, Vibrator::class.java)?.vibrate(
            VibrationEffect.createOneShot(12L, 58)
        )
        isOn = false
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

@Preview
@Composable
private fun Preview() {
    val task = Task(0, "The quick brown fox jumps over the lazy dog.", LocalDate.now())
    SwipeAbleTaskCard(
        TaskViewData(task, isDetailExist = false, false, null),
        { _, _, _ -> }, {}, false, LocalContext.current, ItemMode.NormalTask,
        true,
    )
}
