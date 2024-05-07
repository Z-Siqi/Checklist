package com.sqz.checklist.ui.main.task.layout

import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.database.Task
import com.sqz.checklist.ui.main.WarningAlertDialog
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TaskHistory(
    navBack: () -> Unit,
    taskState: TaskLayoutViewModel = viewModel(),
    modifier: Modifier = Modifier,
    item: List<Task> = taskState.loadTaskHistoryData(MainActivity.taskDatabase.taskDao())
) {
    val view = LocalView.current
    var deleteAllView by rememberSaveable { mutableStateOf(false) }
    var redoAllView by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = { HistoryTopBar(onClick = {
            navBack()
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }) },
        bottomBar = {
            HistoryNavBar(
                deleteAllView = { deleteAllView = true },
                redoAllView = { redoAllView = true },
                view = view
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = modifier.padding(paddingValues),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            LazyColumn(
                modifier = modifier.fillMaxSize()
            ) {
                item {
                    Spacer(modifier = modifier.height(20.dp))
                }
                items(item, key = { it.id }) {
                    ItemBox(
                        id = it.id,
                        description = it.description,
                        createDate = it.createDate,
                        view = view
                    )
                }
                item {
                    Spacer(modifier = modifier.height(10.dp))
                }
            }
            var value by rememberSaveable { mutableIntStateOf(-1) }
            if (value == 0) {
                Column(
                    modifier = modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.nothing_here),
                        fontWeight = FontWeight.Medium,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            } else LaunchedEffect(item) {
                value = MainActivity.taskDatabase.taskDao().getIsHistorySum()
            }
            if (deleteAllView) {
                WarningAlertDialog(
                    onDismissRequest = { deleteAllView = false },
                    onConfirmButtonClick = {
                        taskState.deleteAll()
                        deleteAllView = false
                    },
                    onDismissButtonClick = { deleteAllView = false },
                    text = { Text(stringResource(R.string.delete_all_history)) }
                )
            }
            if (redoAllView) {
                WarningAlertDialog(
                    onDismissRequest = { redoAllView = false },
                    onConfirmButtonClick = {
                        taskState.redoAll()
                        redoAllView = false
                    },
                    onDismissButtonClick = { redoAllView = false },
                    text = { Text(stringResource(R.string.redo_all_history)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTopBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
            Text(text = "Task History")
        },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = { onClick() }) {
                Icon(
                    painter = painterResource(R.drawable.back),
                    contentDescription = stringResource(R.string.back)
                )
            }
        }
    )
}

@Composable
private fun HistoryNavBar(
    deleteAllView: () -> Unit,
    redoAllView: () -> Unit,
    taskState: TaskLayoutViewModel = viewModel(),
    view: View,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        val colors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.inversePrimary,
            selectedIconColor = MaterialTheme.colorScheme.inverseSurface,
            disabledIconColor = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = modifier.weight(0.5f))
        val deleteText = stringResource(R.string.delete)
        NavigationBarItem(
            colors = colors,
            icon = { Icon(imageVector = Icons.Filled.Delete, contentDescription = deleteText) },
            label = { Text(text = deleteText) },
            selected = taskState.onSelect,
            onClick = {
                if (taskState.onSelect) {
                    taskState.deleteTask(taskState.selectedId)
                    taskState.selectedId = -0
                    taskState.onSelect = false
                } else {
                    deleteAllView()
                }
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
        )
        val redoText = stringResource(R.string.redo)
        NavigationBarItem(
            colors = colors,
            icon = { Icon(imageVector = Icons.Filled.Refresh, contentDescription = redoText) },
            label = { Text(text = redoText) },
            selected = taskState.onSelect,
            onClick = {
                if (taskState.onSelect) {
                    taskState.undoTaskToHistory(taskState.selectedId)
                    taskState.selectedId = -0
                    taskState.onSelect = false
                } else {
                    redoAllView()
                }
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
        )
        Spacer(modifier = modifier.weight(0.5f))
    }
}

@Composable
private fun ItemBox(
    id: Int,
    description: String,
    createDate: LocalDate,
    taskState: TaskLayoutViewModel = viewModel(),
    view: View,
    modifier: Modifier = Modifier
) {
    val border = if (taskState.selectedId == id) {
        BorderStroke(3.dp, MaterialTheme.colorScheme.tertiary)
    } else BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceDim)
    Column(modifier = modifier.height(120.dp)) {
        OutlinedCard(
            modifier = modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer),
            border = border,
            shape = ShapeDefaults.ExtraLarge
        ) {
            val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
            val padding = modifier.padding(bottom = 8.dp, top = 12.dp, start = 12.dp, end = 11.dp)
            val onClick = modifier.clickable {
                taskState.selectTask(id)
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
            Box(
                modifier = modifier.fillMaxSize() then onClick
            ) {
                val time = stringResource(R.string.task_creation_time, createDate.format(formatter))
                Box(modifier = padding) {
                    Column {
                        Column(
                            modifier = modifier
                                .fillMaxWidth(0.75f)
                                .height(50.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = description,
                                modifier = modifier.padding(top = 0.dp),
                                fontSize = 21.sp,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = modifier.weight(1f))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = time,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    val item = listOf(
        Task(0, "The quick brown fox jumps over the lazy dog.", LocalDate.now()),
        Task(1, "The quick brown fox jumps over the lazy dog.", LocalDate.now())
    )
    TaskHistory({}, item = item)
}