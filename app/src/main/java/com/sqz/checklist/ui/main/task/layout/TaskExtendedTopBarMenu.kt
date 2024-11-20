package com.sqz.checklist.ui.main.task.layout

import android.content.Context
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sqz.checklist.R
import com.sqz.checklist.ui.MainLayoutNav

enum class TopBarMenuClickType { History, Search, BackupRestore }

/**
 * Top bar menu content.
 * @return close menu request when return 0
 */
@Composable
fun topBarExtendedMenu(
    state: Boolean = false,
    navController: NavHostController,
    onClickType: (type: TopBarMenuClickType, context: Context) -> Unit,
    view: View,
    modifier: Modifier = Modifier,
): Int {
    var closeMenu by remember { mutableIntStateOf(-1) }
    LaunchedEffect(closeMenu) { if (state) closeMenu = 1 }
    val taskHistoryClick = {
        onClickType(TopBarMenuClickType.History, view.context)
        navController.navigate(MainLayoutNav.TaskHistory.name)
    }
    val searchClick = { onClickType(TopBarMenuClickType.Search, view.context) }
    val backupRestoreClick = {
        onClickType(TopBarMenuClickType.BackupRestore, view.context)
        navController.navigate(MainLayoutNav.BackupRestore.name)
    }
    val menuList = listOf(
        MenuItem(stringResource(R.string.task_history)) { taskHistoryClick() },
        MenuItem(stringResource(R.string.search)) { searchClick() },
        MenuItem("Backup & Restore") { backupRestoreClick() }
    )
    MenuLayout(
        expanded = state,
        onDismissRequest = { closeMenu = 0 },
        menuItem = menuList,
        view = view,
        modifier = modifier
    )
    return closeMenu
}

@Composable
private fun MenuLayout(
    expanded: Boolean, onDismissRequest: () -> Unit, menuItem: List<MenuItem>, view: View,
    modifier: Modifier = Modifier,
) = Row(
    modifier = modifier
        .fillMaxWidth()
        .padding(top = 15.dp, end = 8.dp),
    horizontalArrangement = Arrangement.End
) {
    Column(verticalArrangement = Arrangement.Top) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier
        ) {
            menuItem.forEach {
                DropdownMenuItem(
                    onClick = {
                        it.onClick()
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        onDismissRequest()
                    },
                    text = { Text(text = it.name) }
                )
            }
        }
    }
}

data class MenuItem(val name: String, val onClick: () -> Unit)
