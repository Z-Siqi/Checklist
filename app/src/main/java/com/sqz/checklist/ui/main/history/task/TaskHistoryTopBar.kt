package com.sqz.checklist.ui.main.history.task

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sqz.checklist.R
import com.sqz.checklist.ui.common.TextTooltipBox
import com.sqz.checklist.ui.common.unit.isLandscape
import com.sqz.checklist.ui.theme.Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTopBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Theme.color
    TopAppBar(
        colors = colors.topBarBgColors(isLandscape()),
        title = { Text(text = stringResource(R.string.task_history)) },
        modifier = modifier.shadow(
            elevation = 1.dp,
            ambientColor = MaterialTheme.colorScheme.primaryContainer
        ),
        navigationIcon = {
            TextTooltipBox(
                textRid = R.string.back,
                extraPadding = PaddingValues(top = 25.dp, bottom = 20.dp),
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
