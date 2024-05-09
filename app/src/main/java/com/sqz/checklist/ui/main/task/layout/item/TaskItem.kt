package com.sqz.checklist.ui.main.task.layout.item

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkManager
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.ui.material.TaskChangeContentCard
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    id: Int,
    description: String,
    createDate: LocalDate,
    isPin: Boolean,
    context: Context,
    itemState: SwipeToDismissBoxState,
    pinnedTask: Boolean = false,
    modifier: Modifier = Modifier,
    taskState: TaskLayoutViewModel = viewModel()
) {
    val dismissInEndToStart = itemState.currentValue == SwipeToDismissBoxValue.EndToStart
    val dismissInStartToEnd = itemState.currentValue == SwipeToDismissBoxValue.StartToEnd
    val isDismissed = dismissInEndToStart || dismissInStartToEnd
    if (isDismissed) {
        var isDismissedId by rememberSaveable { mutableIntStateOf(0) }
        LaunchedEffect(true) { isDismissedId++ }
        if (isDismissedId < 1) {
            taskState.deleteTaskToHistory(id)
            taskState.checkTaskAction = true
            taskState.undoActionId = id
        }
        if (!taskState.getIsHistory(id)) LaunchedEffect(true) {
            itemState.reset()
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Vibrate(context, itemState)
    }
    val height = if (!isDismissed) 120.dp else 0.dp
    Column(
        modifier = modifier
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            .height(height),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var taskEditCard by rememberSaveable { mutableStateOf(false) }
        val formatter = DateTimeFormatter.ofPattern(
            stringResource(R.string.task_date_format),
            Locale.getDefault()
        )
        val reminderState = reminderState(id, context, taskState)
        ItemBox(
            description = description,
            createDate = stringResource(R.string.task_creation_time, createDate.format(formatter)),
            reminderOnClick = {
                if (reminderState) {
                    taskState.reminderCard = true
                    taskState.setReminderId = id
                } else {
                    taskState.setReminderState = true
                    taskState.setReminderId = id
                }
            },
            editOnClick = {
                taskEditCard = true
            },
            timerIconState = reminderState,
            pinOnClick = {
                if (isPin) {
                    taskState.pinState(id = id, set = 0)
                } else {
                    taskState.pinState(id = id, set = 1)
                }
            },
            pinIconState = isPin,
            state = itemState,
            horizontalEdge = if (pinnedTask) 10 else 14
        )
        val textState = rememberTextFieldState()
        if (taskEditCard) {
            LaunchedEffect(true) {
                textState.clearText()
                textState.edit { insert(0, description) }
            }
            val noChangeDoNothing = stringResource(R.string.no_change_do_nothing)
            TaskChangeContentCard(
                onDismissRequest = { taskEditCard = false },
                confirm = {
                    if (textState.text.toString() != "") {
                        taskState.editTask(id = id, edit = textState.text.toString())
                        taskEditCard = false
                    } else {
                        Toast.makeText(context, noChangeDoNothing, Toast.LENGTH_SHORT).show()
                    }
                },
                state = textState,
                title = stringResource(R.string.edit_task),
                confirmText = stringResource(R.string.edit),
                doneImeAction = true
            )
        } else LaunchedEffect(true) {
            textState.clearText()
            textState.edit { insert(0, description) }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
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
fun cardBackgoundColor(onSlide: Boolean = false): CardColors {
    return if (!onSlide) {
        CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer)
    } else CardDefaults.cardColors(MaterialTheme.colorScheme.secondary)
}

@Composable
private fun reminderState( // check the reminder is set or not
    id: Int,
    context: Context,
    taskState: TaskLayoutViewModel
): Boolean {
    var rememberState by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(taskState.setReminderState) {
        val uuidAndTime = MainActivity.taskDatabase.taskDao().getReminderInfo(id)
        uuidAndTime?.let {
            val parts = it.split(":")
            if (parts.size >= 2) {
                val uuid = parts[0]
                parts[1].toLong()
                val workManager = WorkManager.getInstance(context)
                if (uuid != "undefined") {
                    workManager.getWorkInfoByIdLiveData(UUID.fromString(uuid))
                        .observeForever { workInfo ->
                            rememberState = !(workInfo != null && workInfo.state.isFinished)
                        }
                } else {
                    rememberState = false
                }
            } else {
                rememberState = false
            }
        }
    }
    return rememberState
}

@Composable
fun AnimateInFinishedTask(
    visible: Boolean = false,
    alignment: Alignment.Horizontal
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandHorizontally(
            expandFrom = alignment
        ) + fadeIn(
            initialAlpha = 0.5f
        ),
        exit = fadeOut()
    ) {
        Icon(imageVector = Icons.Filled.Check, contentDescription = stringResource(R.string.check))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview() {
    val screenWidthPx = LocalConfiguration.current.screenWidthDp * LocalDensity.current.density
    val state = rememberSwipeToDismissBoxState(
        positionalThreshold = { screenWidthPx * 0.38f },
    )
    TaskItem(
        id = 0, description = "The quick brown fox jumps over the lazy dog.", isPin = false,
        createDate = LocalDate.now(), context = LocalContext.current, itemState = state
    )
}
