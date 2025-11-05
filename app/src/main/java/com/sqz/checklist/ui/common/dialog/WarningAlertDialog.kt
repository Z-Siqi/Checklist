package com.sqz.checklist.ui.common.dialog

import android.view.SoundEffectConstants
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
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
    PrimaryDialog(
        onDismissRequest = {
            onDismissRequest()
            view.playSoundEffect(SoundEffectConstants.CLICK)
        },
        actionButton = {
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
                painter = painterResource(R.drawable.warning),
                contentDescription = stringResource(R.string.warning)
            )
        },
        title = { Text(text = stringResource(R.string.warning)) },
        content = {
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
