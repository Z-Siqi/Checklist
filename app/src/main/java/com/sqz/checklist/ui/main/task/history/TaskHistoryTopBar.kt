package com.sqz.checklist.ui.main.task.history

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.sqz.checklist.R
import com.sqz.checklist.ui.material.TextTooltipBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTopBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = { Text(text = stringResource(R.string.task_history)) },
        modifier = modifier,
        navigationIcon = {
            TextTooltipBox(
                textRid = R.string.back,
                topLeftExtraPadding = true
            ) {
                IconButton(onClick = { onClick() }) {
                    Icon(
                        painter = painterResource(R.drawable.back),
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        }
    )
}
