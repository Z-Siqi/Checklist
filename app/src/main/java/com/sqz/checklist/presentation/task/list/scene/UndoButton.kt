package com.sqz.checklist.presentation.task.list.scene

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqz.checklist.R
import com.sqz.checklist.common.AndroidEffectFeedback
import com.sqz.checklist.ui.common.unit.isApi35AndAbove
import com.sqz.checklist.ui.common.unit.isLandscape
import sqz.checklist.common.EffectFeedback

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun UndoButton(
    onClick: () -> Unit,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
) {
    val bottomPadding = modifier.let {
        if (isApi35AndAbove && isLandscape()) {
            it.windowInsetsPadding(WindowInsets.navigationBars)
        } else it
    }
    Box(modifier = modifier.fillMaxSize() then bottomPadding) {
        FloatingActionButton(
            modifier = modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            onClick = onClick.also { feedback.onClickEffect() },
            containerColor = MaterialTheme.colorScheme.secondary
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(R.drawable.undo),
                    contentDescription = stringResource(R.string.undo),
                    tint = MaterialTheme.colorScheme.onSecondary
                )
                Text(
                    text = stringResource(R.string.undo),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondaryContainer
                )
            }
        }
    }
}

@Preview
@Composable
private fun UndoButtonPreview() {
    val feedback = AndroidEffectFeedback(LocalView.current)
    UndoButton({}, feedback)
}
