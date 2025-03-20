package com.sqz.checklist.ui.main.task.layout.item

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getSystemService
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.database.DatabaseRepository
import com.sqz.checklist.database.Task
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Swipe-able task item for list (Expected @LazyList call this)
 */
@Composable
fun SwipeAbleTaskCard(
    task: Task,
    onTaskItemClick: (
        task: Task, type: CardClickType, reminderState: Boolean, context: Context
    ) -> Unit,
    checked: (id: Long) -> Unit,
    getIsHistory: Boolean,
    context: Context,
    itemState: SwipeToDismissBoxState,
    mode: ItemMode,
    modifier: Modifier = Modifier,
    isPreview: Boolean = false
) { // Process card action
    val remindTime = @Composable { // The text of reminder time
        val getTimeInLong =
            if (task.reminder != null) getReminderTime(task.reminder) else 0L
        val fullDateShort = stringResource(R.string.full_date_short)
        if (getTimeInLong <= 1000L) null else SimpleDateFormat(
            fullDateShort,
            Locale.getDefault()
        ).format(getTimeInLong)
    }
    val formatter = DateTimeFormatter.ofPattern(
        stringResource(R.string.task_date_format),
        Locale.getDefault()
    )
    Column(
        modifier = Modifier
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            .height(swipeToDismissControl(itemState, { checked(task.id) }, getIsHistory, context)),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val horizontalEdge = if (mode == ItemMode.PinnedTask) 10 else 14
        val bgStartEnd = horizontalEdge.dp
        val startEnd = bgStartEnd - 2.dp
        val viewRange = (itemState.progress in 0.1f..0.9f)
        val isStartToEnd = itemState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
        val isEndToStart = itemState.dismissDirection == SwipeToDismissBoxValue.EndToStart
        SwipeToDismissBox(
            state = itemState,
            backgroundContent = { // back of card
                Card(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(start = bgStartEnd, end = bgStartEnd, top = 4.dp, bottom = 4.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondary),
                    shape = ShapeDefaults.ExtraLarge
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(modifier = modifier.weight(0.05f))
                        AnimateInFinishedTask((viewRange && isStartToEnd), Alignment.Start)
                        Spacer(modifier = modifier.weight(0.7f))
                        AnimateInFinishedTask((viewRange && isEndToStart), Alignment.End)
                        Spacer(modifier = modifier.weight(0.05f))
                    }
                }
            }
        ) { // front of card
            val reminderState = if (!isPreview) reminderState(task.reminder) else false
            TaskCardContent(
                description = task.description,
                dateText = if (mode == ItemMode.RemindedTask) {
                    stringResource(R.string.task_reminded_time, remindTime().toString())
                } else {
                    stringResource(
                        R.string.task_creation_time,
                        task.createDate.format(formatter)
                    )
                },
                onClick = { type -> onTaskItemClick(task, type, reminderState, context) },
                timerIconState = reminderState,
                pinIconState = task.isPin,
                tooltipRemindText = if (reminderState) remindTime() else null,
                mode = mode,
                isDetail = task.detail,
                modifier = modifier.padding(
                    start = startEnd,
                    end = startEnd,
                    top = 4.dp,
                    bottom = 4.dp
                )
            )
        }
    }
}

/** Control the action of swipe to dismiss, @return the height of card **/
@Composable
private fun swipeToDismissControl(
    itemState: SwipeToDismissBoxState,
    checked: () -> Unit, isHistory: Boolean, context: Context
): Dp {
    val dismissInEndToStart = itemState.currentValue == SwipeToDismissBoxValue.EndToStart
    val dismissInStartToEnd = itemState.currentValue == SwipeToDismissBoxValue.StartToEnd
    val isDismissed = dismissInEndToStart || dismissInStartToEnd
    if (isDismissed) {
        var isDismissedId by rememberSaveable { mutableIntStateOf(0) }
        LaunchedEffect(true) { isDismissedId++ }
        if (isDismissedId < 1) checked()
        if (!isHistory) LaunchedEffect(true) {
            itemState.reset()
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Vibrate(context, itemState)
    }
    return if (!isDismissed) 120.dp else 0.dp
}

/** check the reminder is set or not **/
@Composable
private fun reminderState(reminder: Int?): Boolean {
    val databaseRepository = DatabaseRepository(MainActivity.taskDatabase)
    var state by remember { mutableStateOf(false) }
    LaunchedEffect(databaseRepository.getIsRemindedNum(true)) {
        if (reminder != 0 && reminder != null) {
            try {
                state = !(databaseRepository.getReminderData(reminder)?.isReminded ?: true)
            } catch (e: Exception) {
                if (e.message != "The coroutine scope left the composition") Log.w(
                    "Exception: TaskItem", "$reminder, err: $e"
                )
            }
        } else state = false
    }
    return state
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
private fun Vibrate(
    context: Context,
    itemState: SwipeToDismissBoxState
) {
    var isOn by remember { mutableStateOf(false) }
    if (itemState.targetValue == SwipeToDismissBoxValue.StartToEnd ||
        itemState.targetValue == SwipeToDismissBoxValue.EndToStart
    ) {
        LaunchedEffect(true) {
            getSystemService(context, Vibrator::class.java)?.vibrate(
                VibrationEffect.createPredefined(
                    VibrationEffect.EFFECT_TICK
                )
            )
        }
        isOn = true
    } else if (isOn && itemState.targetValue == itemState.currentValue) {
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

@Composable
private fun getReminderTime(id: Int): Long {
    var data by rememberSaveable { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val databaseRepository = DatabaseRepository(MainActivity.taskDatabase)
        data = databaseRepository.getReminderData(id)?.reminderTime ?: 0L
    }
    return data
}

@Preview
@Composable
private fun Preview() {
    val screenWidthPx = LocalConfiguration.current.screenWidthDp * LocalDensity.current.density
    val state = rememberSwipeToDismissBoxState(
        positionalThreshold = { screenWidthPx * 0.38f },
    )
    SwipeAbleTaskCard(
        Task(0, "The quick brown fox jumps over the lazy dog.", LocalDate.now()),
        { _, _, _, _ -> }, {}, false, LocalContext.current, state, ItemMode.NormalTask,
        isPreview = true
    )
}
