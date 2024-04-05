package com.sqz.checklist.ui.mainLayout

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkManager
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaskItem(
    id: Int,
    description: String,
    createDate: LocalDate,
    context: Context,
    itemState: SwipeToDismissBoxState,
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
            taskState.deleteTaskToHistory(id, context)
            taskState.checkTaskAction = true
            taskState.undoActionId = id
        }
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
        TaskBox(
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
            iconId = if (reminderState) {
                R.drawable.timer_on
            } else R.drawable.timer,
            state = itemState
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

@Composable
private fun cardBackgoundColor(onSlide: Boolean = false): CardColors {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskBox(
    description: String,
    createDate: String,
    reminderOnClick: () -> Unit,
    editOnClick: () -> Unit,
    iconId: Int,
    state: SwipeToDismissBoxState,
    modifier: Modifier = Modifier
) {
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            Card(
                modifier = modifier
                    .fillMaxSize()
                    .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
                colors = cardBackgoundColor(true),
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
    ) {
        OutlinedCard(
            modifier = modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            colors = cardBackgoundColor(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceDim),
            shape = ShapeDefaults.ExtraLarge
        ) {
            Box(
                modifier = modifier.padding(bottom = 8.dp, top = 12.dp, start = 12.dp, end = 11.dp)
            ) {
                TaskContent(
                    description = description,
                    createDate = createDate,
                    reminderOnClick = reminderOnClick,
                    editOnClick = editOnClick,
                    iconId = iconId
                )
            }
        }
    }
}

@Composable
private fun TaskContent(
    description: String,
    createDate: String,
    reminderOnClick: () -> Unit,
    editOnClick: () -> Unit,
    iconId: Int,
    modifier: Modifier = Modifier
) {
    Column {
        Column(
            modifier = modifier
                .fillMaxWidth(0.75f)
                .height(50.dp),
            horizontalAlignment = Alignment.Start
        ) {
            var overflowState by rememberSaveable { mutableStateOf(false) }
            var overflowInfo by rememberSaveable { mutableStateOf(false) }
            Card(colors = cardBackgoundColor()) {
                Text(
                    text = description,
                    modifier = modifier
                        .fillMaxSize()
                        .clickable(overflowState) { overflowInfo = true },
                    fontSize = 21.sp,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textLayoutResult: TextLayoutResult ->
                        overflowState = textLayoutResult.hasVisualOverflow
                    },
                    fontWeight = FontWeight.Normal,
                )
            }
            if (overflowInfo) {
                InfoAlertDialog(
                    onDismissRequest = { overflowInfo = false },
                    text = description
                )
            }
        }
        Spacer(modifier = modifier.weight(1f))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = createDate,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier.padding(start = 2.dp)
            )
            Spacer(modifier = modifier.weight(1f))
            IconButton(modifier = modifier.size(30.dp), onClick = reminderOnClick) {
                Icon(
                    painter = painterResource(id = iconId),
                    contentDescription = stringResource(R.string.reminder)
                )
            }
            Spacer(modifier = modifier.weight(0.2f))
            IconButton(modifier = modifier.size(30.dp), onClick = editOnClick) {
                Icon(
                    painter = painterResource(id = R.drawable.edit),
                    contentDescription = stringResource(R.string.edit)
                )
            }
            Spacer(modifier = modifier.weight(0.32f))
        }
    }
}

@Composable
private fun AnimateInFinishedTask(
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
        positionalThreshold = {
            screenWidthPx * 0.38f
        },
    )
    TaskItem(
        id = 0,
        description = "The quick brown fox jumps over the lazy dog.",
        createDate = LocalDate.now(),
        context = LocalContext.current,
        itemState = state
    )
}
