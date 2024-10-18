package com.sqz.checklist.ui.main.task.history

import android.util.Log
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.sqz.checklist.R
import com.sqz.checklist.ui.main.NavMode
import com.sqz.checklist.ui.material.WarningAlertDialog

@Composable
fun TaskHistoryNavBar(
    mode: NavMode,
    view: View,
    modifier: Modifier = Modifier,
    historyState: TaskHistoryViewModel,
) {
    val selectState = historyState.selectState.collectAsState().value

    var deleteAllView by rememberSaveable { mutableStateOf(false) }
    var redoAllView by rememberSaveable { mutableStateOf(false) }
    if (deleteAllView) {
        WarningAlertDialog(
            onDismissRequest = { deleteAllView = false },
            onConfirmButtonClick = {
                historyState.doAllTask(TaskHistoryViewModel.DoTaskAction.Delete)
                deleteAllView = false
            },
            textString = stringResource(R.string.delete_all_history)
        )
    }
    if (redoAllView) {
        WarningAlertDialog(
            onDismissRequest = { redoAllView = false },
            onConfirmButtonClick = {
                historyState.doAllTask(TaskHistoryViewModel.DoTaskAction.Redo)
                redoAllView = false
            },
            textString = stringResource(R.string.redo_all_history)
        )
    }

    NavigationSelector(
        mode = mode,
        selected = selectState.onSelect,
        deleteClick = {
            if (selectState.onSelect) historyState.removeFromHistory(
                TaskHistoryViewModel.DoTaskAction.Delete,
                selectState.selectedId
            ) else {
                deleteAllView = true
            }
        },
        redoClick = {
            if (selectState.onSelect) historyState.removeFromHistory(
                TaskHistoryViewModel.DoTaskAction.Redo,
                selectState.selectedId
            ) else {
                redoAllView = true
            }
        },
        view = view,
        modifier = modifier
    )

    LaunchedEffect(true) {
        if (selectState.onSelect) historyState.resetSelectState()
    }
}

private data class TaskHistoryColors(
    val containerColor: Color, val contentColor: Color,
    val indicatorColor: Color, val selectedIconColor: Color, val disabledIconColor: Color,
)

@Composable
private fun NavigationSelector(
    mode: NavMode,
    selected: Boolean, deleteClick: () -> Unit, redoClick: () -> Unit,
    view: View, modifier: Modifier = Modifier,
) {
    val colors = TaskHistoryColors (
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        indicatorColor = MaterialTheme.colorScheme.inversePrimary,
        selectedIconColor = MaterialTheme.colorScheme.inverseSurface,
        disabledIconColor = MaterialTheme.colorScheme.primary,
    )
    when (mode) {
        NavMode.NavBar -> NavBar(colors, selected, deleteClick, redoClick, view, modifier)
        NavMode.NavRail -> NavRailBar(colors, selected, deleteClick, redoClick, view, modifier)
        NavMode.Disable -> {
            val nulLog = { Log.d("NavBarLayout", "The task history navigation bar is disable") }
            Spacer(modifier = modifier).also { nulLog() }
        }
    }
}

@Composable
private fun NavBar(
    colors: TaskHistoryColors,
    selected: Boolean,
    deleteClick: () -> Unit,
    redoClick: () -> Unit,
    view: View,
    modifier: Modifier = Modifier,
) {
    val buttonColors = NavigationBarItemDefaults.colors(
        indicatorColor = colors.indicatorColor,
        selectedIconColor = colors.selectedIconColor,
        disabledIconColor = colors.disabledIconColor
    )
    NavigationBar(
        containerColor = colors.containerColor, contentColor = colors.contentColor,
        modifier = modifier,
    ) {
        Spacer(modifier = modifier.weight(0.5f))
        val deleteText = stringResource(R.string.delete)
        NavigationBarItem(
            colors = buttonColors,
            icon = { Icon(imageVector = Icons.Filled.Delete, contentDescription = deleteText) },
            label = { Text(text = deleteText) },
            selected = selected,
            onClick = {
                deleteClick()
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
        )
        val redoText = stringResource(R.string.redo)
        NavigationBarItem(
            colors = buttonColors,
            icon = { Icon(imageVector = Icons.Filled.Refresh, contentDescription = redoText) },
            label = { Text(text = redoText) },
            selected = selected,
            onClick = {
                redoClick()
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
        )
        Spacer(modifier = modifier.weight(0.5f))
    }
}

@Composable
private fun NavRailBar(
    colors: TaskHistoryColors,
    selected: Boolean,
    deleteClick: () -> Unit,
    redoClick: () -> Unit,
    view: View,
    modifier: Modifier = Modifier,
) {
    val buttonColors = NavigationRailItemDefaults.colors(
        indicatorColor = colors.indicatorColor,
        selectedIconColor = colors.selectedIconColor,
        disabledIconColor = colors.disabledIconColor
    )
    NavigationRail(
        containerColor = colors.containerColor, contentColor = colors.contentColor,
        modifier = modifier
    ) {
        Spacer(modifier = modifier.weight(0.58f))
        val deleteText = stringResource(R.string.delete)
        NavigationRailItem(
            colors = buttonColors,
            icon = { Icon(imageVector = Icons.Filled.Delete, contentDescription = deleteText) },
            label = { Text(text = deleteText) },
            selected = selected,
            onClick = {
                deleteClick()
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
        )
        Spacer(modifier = modifier.weight(0.5f))
        val redoText = stringResource(R.string.redo)
        NavigationRailItem(
            colors = buttonColors,
            icon = { Icon(imageVector = Icons.Filled.Refresh, contentDescription = redoText) },
            label = { Text(text = redoText) },
            selected = selected,
            onClick = {
                redoClick()
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
        )
        Spacer(modifier = modifier.weight(0.5f))
    }
}
