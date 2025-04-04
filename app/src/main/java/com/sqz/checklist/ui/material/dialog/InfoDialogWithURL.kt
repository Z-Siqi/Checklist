package com.sqz.checklist.ui.material.dialog

import android.annotation.SuppressLint
import android.view.SoundEffectConstants
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.sqz.checklist.R
import com.sqz.checklist.ui.material.UrlText
import com.sqz.checklist.ui.material.verticalColumnScrollbar

@Composable
fun InfoDialogWithURL(
    onDismissRequest: () -> Unit,
    title: String? = null,
    url: String,
    urlTitle: String? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val scrollState = rememberScrollState()
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val height = when {
        screenHeightDp >= 700 -> (screenHeightDp / 5.8).toInt()
        screenHeightDp < (LocalConfiguration.current.screenWidthDp / 1.2) -> (screenHeightDp / 3.2).toInt()
        else -> (screenHeightDp / 5.1).toInt()
    }
    AlertDialog(
        modifier = modifier
            .width((LocalConfiguration.current.screenWidthDp / 1.2).dp)
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
            OutlinedCard(
                modifier = modifier.fillMaxWidth() then modifier
                    .height(height.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { focus.clearFocus() }
                    },
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier.padding(8.dp)) {
                    SelectionContainer(
                        modifier = modifier.verticalColumnScrollbar(
                            scrollState = scrollState, endPadding = 0f, scrollBarCornerRadius = 12f,
                            scrollBarTrackColor = MaterialTheme.colorScheme.outlineVariant,
                            scrollBarColor = MaterialTheme.colorScheme.outline,
                            showScrollBar = scrollState.canScrollBackward || scrollState.canScrollForward
                        ) then modifier.verticalScroll(scrollState)
                    ) {
                        UrlText(url, text = urlTitle ?: url, view = view, fontSize = 19.sp)
                    }
                }
            }
        },
        title = { if (title != null) Text(text = title) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

@Preview
@Composable
private fun PreviewOfInfoDialogWithURL() {
    InfoDialogWithURL(
        onDismissRequest = {},
        url = "TEST"
    )
}
