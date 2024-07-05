package com.sqz.checklist.ui.main

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqz.checklist.R

@Composable
fun NavTooltipContent(
    textRid: Int,
    onDismissRequest: () -> Unit,
    onClickToTaskHistory: () -> Unit,
    view: View,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
) {
    Row(
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
                DropdownMenuItem(
                    onClick = {
                        onClickToTaskHistory()
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                    },
                    text = { Text(text = stringResource(textRid)) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    NavTooltipContent(R.string.app_name, {}, {}, LocalView.current, expanded = true)
}
