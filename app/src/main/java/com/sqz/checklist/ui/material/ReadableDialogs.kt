package com.sqz.checklist.ui.material

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.SoundEffectConstants
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.sqz.checklist.R

@Composable
fun WarningAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirmButtonClick: () -> Unit,
    onDismissButtonClick: () -> Unit = onDismissRequest,
    textString: String = "",
    text: @Composable () -> Unit = {},
) {
    val view = LocalView.current
    AlertDialog(
        onDismissRequest = {
            onDismissRequest()
            view.playSoundEffect(SoundEffectConstants.CLICK)
        },
        confirmButton = {
            TextButton(onClick = onConfirmButtonClick) {
                Text(text = stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismissButtonClick()
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }) {
                Text(text = stringResource(R.string.dismiss))
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = stringResource(R.string.warning)
            )
        },
        title = { Text(text = stringResource(R.string.warning)) },
        text = {
            Column { if (textString == "") text() else Text(text = textString, fontSize = 17.sp) }
        }
    )
}

@Preview
@Composable
private fun WarningAlertDialogPreview() {
    WarningAlertDialog(
        onDismissRequest = {},
        onConfirmButtonClick = {},
        onDismissButtonClick = {},
        text = { Text(text = "TEST") }
    )
}

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
    val height = if (screenHeightDp >= 700) {
        (screenHeightDp / 5.8).toInt()
    } else if (screenHeightDp < (LocalConfiguration.current.screenWidthDp / 1.2)) {
        (screenHeightDp / 3.2).toInt()
    } else (screenHeightDp / 5.1).toInt()
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
                        modifier = modifier.verticalScroll(scrollState)
                    ) {
                        Text(
                            text = urlTitle ?: url,
                            fontSize = 19.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                view.context.startActivity(intent)
                            },
                            textDecoration = TextDecoration.Underline
                        )
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

@Composable
fun InfoAlertDialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    text: String,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val scrollState = rememberScrollState()
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val height = if (screenHeightDp >= 700) {
        (screenHeightDp / 5.8).toInt()
    } else if (screenHeightDp < (LocalConfiguration.current.screenWidthDp / 1.2)) {
        (screenHeightDp / 3.2).toInt()
    } else (screenHeightDp / 5.1).toInt()
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
                        modifier = modifier.verticalScroll(scrollState)
                    ) {
                        Text(
                            text = text,
                            fontSize = 19.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
private fun InfoAlertDialogPreview() {
    InfoAlertDialog(
        onDismissRequest = {},
        text = "TEST"
    )
}
