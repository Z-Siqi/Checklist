package com.sqz.checklist.ui.main.task.history

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sqz.checklist.R
import com.sqz.checklist.ui.material.WarningAlertDialog

@Composable
fun TaskHistoryNavBar(
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
            selected = selectState.onSelect,
            onClick = {
                if (selectState.onSelect) historyState.removeFromHistory(
                    TaskHistoryViewModel.DoTaskAction.Delete,
                    selectState.selectedId
                ) else {
                    deleteAllView = true
                }
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
        )
        val redoText = stringResource(R.string.redo)
        NavigationBarItem(
            colors = colors,
            icon = { Icon(imageVector = Icons.Filled.Refresh, contentDescription = redoText) },
            label = { Text(text = redoText) },
            selected = selectState.onSelect,
            onClick = {
                if (selectState.onSelect) historyState.removeFromHistory(
                    TaskHistoryViewModel.DoTaskAction.Redo,
                    selectState.selectedId
                ) else {
                    redoAllView = true
                }
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
        )
        Spacer(modifier = modifier.weight(0.5f))
    }
    LaunchedEffect(true) {
        if (selectState.onSelect) historyState.resetSelectState()
    }
}
