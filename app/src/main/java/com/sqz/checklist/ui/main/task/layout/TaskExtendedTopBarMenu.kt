package com.sqz.checklist.ui.main.task.layout

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
import com.sqz.checklist.R

/**
 * Top bar menu content.
 * @return close menu request when return 0
 */
@Composable
fun topBarExtendedMenu(
    state: Boolean = false,
    onClickToTaskHistory: () -> Unit,
    onClickToSearch: () -> Unit,
    view: View,
    modifier: Modifier = Modifier,
) : Int {
    var closeMenu by remember { mutableIntStateOf(-1) }
    LaunchedEffect(closeMenu) { if (state) closeMenu = 1 }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 15.dp, end = 8.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Column(verticalArrangement = Arrangement.Top) {
            DropdownMenu(
                expanded = state,
                onDismissRequest = { closeMenu = 0 },
                modifier = modifier
            ) {
                DropdownMenuItem(
                    onClick = {
                        onClickToTaskHistory()
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        closeMenu = 0
                    },
                    text = { Text(text = stringResource(R.string.task_history)) }
                )
                DropdownMenuItem(
                    onClick = {
                        onClickToSearch()
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        closeMenu = 0
                    },
                    text = { Text(text = stringResource(R.string.search)) }
                )
            }
        }
    }
    return closeMenu
}
