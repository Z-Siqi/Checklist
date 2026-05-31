package com.sqz.checklist.presentation.history.task.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.sqz.checklist.R
import sqz.checklist.common.EffectFeedback

enum class WarnType {
    DeleteAll, RedoAll
}

@Composable
fun DoAllWarnDialogUI(
    warnType: WarnType,
    onDismissRequest: () -> Unit,
    onConfirmRequest: () -> Unit,
    feedback: EffectFeedback,
) {
    val description = when (warnType) {
        WarnType.DeleteAll -> stringResource(R.string.delete_all_history)
        WarnType.RedoAll -> stringResource(R.string.redo_all_history)
    }
    DoAllWarnDialogScaffold(
        description = description,
        onDismissRequest = { onDismissRequest().also { feedback.onClickEffect() } },
        onConfirmRequest = { onConfirmRequest().also { feedback.onClickEffect() } },
    )
}

@Composable
private fun DoAllWarnDialogScaffold(
    description: String,
    onDismissRequest: () -> Unit,
    onConfirmRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirmRequest) {
                Text(text = stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        icon = {
            Icon(
                painter = painterResource(R.drawable.warning),
                contentDescription = stringResource(R.string.warning)
            )
        },
        title = { Text(text = stringResource(R.string.warning)) },
        text = { Text(text = description) }
    )
}
