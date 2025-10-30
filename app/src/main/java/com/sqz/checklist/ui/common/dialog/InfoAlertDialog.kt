package com.sqz.checklist.ui.common.dialog

import android.view.SoundEffectConstants
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.sqz.checklist.R
import com.sqz.checklist.ui.common.unit.pxToDpInt
import com.sqz.checklist.ui.common.verticalColumnScrollbar

@Composable
fun InfoAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    titleRow: @Composable RowScope.() -> Unit = {},
    text: String?,
    textBackgroundColor: Boolean = true,
    content: @Composable () -> Unit = {},
) {
    val view = LocalView.current
    val scrollState = rememberScrollState()
    val containerSize = LocalWindowInfo.current.containerSize
    val screenHeightDp = containerSize.height.pxToDpInt()
    val height = when {
        screenHeightDp >= 700 -> (screenHeightDp / 5.8).toInt()
        screenHeightDp < (containerSize.width.pxToDpInt() / 1.2) -> (screenHeightDp / 3.2).toInt()
        else -> (screenHeightDp / 5.1).toInt()
    }
    AlertDialog(
        modifier = modifier
            .width((containerSize.width.pxToDpInt() / 1.2).dp)
            .sizeIn(maxWidth = 560.dp),
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onDismissRequest()
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        text = {
            val focus = LocalFocusManager.current
            val bgColor = if (!textBackgroundColor) Color.Transparent else {
                MaterialTheme.colorScheme.surfaceContainer
            }
            Column {
                Card(
                    modifier = Modifier.fillMaxWidth() then Modifier
                        .height(height.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { focus.clearFocus() }
                        },
                    colors = CardDefaults.cardColors(bgColor),
                    shape = if (title == null) ShapeDefaults.Large else ShapeDefaults.Medium
                ) {
                    if (text == null) content() else SelectionContainer(
                        modifier = Modifier.padding(8.dp) then Modifier.verticalColumnScrollbar(
                            scrollState = scrollState, endPadding = 0f, scrollBarCornerRadius = 12f,
                            scrollBarTrackColor = MaterialTheme.colorScheme.outlineVariant,
                            scrollBarColor = MaterialTheme.colorScheme.outline,
                            showScrollBar = scrollState.canScrollBackward || scrollState.canScrollForward
                        ) then Modifier.verticalScroll(scrollState)
                    ) {
                        Text(
                            text = text,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 19.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (title != null) {
                    Text(text = title)
                    Spacer(modifier = Modifier.weight(1f))
                }
                titleRow()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

@Preview
@Composable
private fun InfoAlertDialogPreview() {
    InfoAlertDialog(
        onDismissRequest = {},
        text = "TEST"
    )
}
