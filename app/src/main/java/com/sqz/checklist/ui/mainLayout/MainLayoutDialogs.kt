package com.sqz.checklist.ui.mainLayout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqz.checklist.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TaskChangeContentCard(
    onDismissRequest: () -> Unit,
    confirm: () -> Unit,
    title: String,
    confirmText: String,
    state: TextFieldState,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    doneImeAction: Boolean = false,
    modifier: Modifier = Modifier
) {
    var clearFocus by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    fun releaseFocusAndDismiss() = coroutineScope.launch {
        clearFocus = true
        delay(80)
        onDismissRequest()
    }

    val scrollState = rememberScrollState()
    AlertDialog(
        modifier = modifier.width((LocalConfiguration.current.screenWidthDp / 1.2).dp),
        onDismissRequest = { releaseFocusAndDismiss() },
        confirmButton = {
            TextButton(onClick = {
                coroutineScope.launch {
                    clearFocus = true
                    delay(80)
                    confirm()
                }
            }) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = { releaseFocusAndDismiss() }) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        title = {
            Text(
                text = title,
                fontSize = 22.sp
            )
        },
        text = {
            val height = if (LocalConfiguration.current.screenHeightDp >= 700) {
                (LocalConfiguration.current.screenHeightDp / 5.8).toInt()
            } else {
                (LocalConfiguration.current.screenHeightDp / 7.0).toInt()
            }
            OutlinedCard(
                modifier = modifier
                    .fillMaxWidth()
                    .height(height.dp)
            ) {
                val focus = LocalFocusManager.current
                if (clearFocus) LaunchedEffect(true) {
                    focus.clearFocus()
                    clearFocus = false
                }
                BasicTextField(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .verticalScroll(scrollState),
                    state = state,
                    textStyle = TextStyle(
                        fontSize = 19.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant),
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (doneImeAction) {
                            ImeAction.Done
                        } else ImeAction.Default
                    ),
                    onKeyboardAction = { if (doneImeAction) clearFocus = true },
                    lineLimits = lineLimits,
                )
            }
        }
    )
}

@Preview
@Composable
private fun TaskChangeContentCardPreview() {
    val state = rememberTextFieldState()
    TaskChangeContentCard({}, {}, "Create a task", "Add", state)
}


@Composable
fun WarningAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirmButtonClick: () -> Unit,
    onDismissButtonClick: () -> Unit,
    text: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirmButtonClick) {
                Text(text = stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissButtonClick) {
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
            Column { text() }
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
fun InfoAlertDialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    text: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    AlertDialog(
        modifier = modifier.width((LocalConfiguration.current.screenWidthDp / 1.2).dp),
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        text = {
            OutlinedCard(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
            ) {
                Column(
                    modifier.padding(8.dp)
                ) {
                    SelectionContainer(
                        modifier = modifier.verticalScroll(scrollState)
                    ) {
                        Text(
                            text = text,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        },
        title = { if (title != null) Text(text = title) }
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