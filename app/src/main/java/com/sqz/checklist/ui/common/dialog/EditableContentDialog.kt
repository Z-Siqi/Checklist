package com.sqz.checklist.ui.common.dialog

import android.view.SoundEffectConstants
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.sqz.checklist.R
import com.sqz.checklist.ui.common.verticalColumnScrollbar
import com.sqz.checklist.ui.common.unit.pxToDpInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EditableContentDialog(
    onDismissRequest: () -> Unit,
    confirm: () -> Unit,
    title: String,
    confirmText: String,
    state: TextFieldState,
    modifier: Modifier = Modifier,
    contentProperties: EditableContentDialog = EditableContentDialog(),
    singleLine: Boolean = false,
    lineLimits: TextFieldLineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine(),
    doneImeAction: Boolean = singleLine,
    numberOnly: Boolean = false,
    disableConform: Boolean = false,
    onDisableConformClick: () -> Unit = {},
    onDismissClick: () -> Unit = onDismissRequest,
) {
    val view = LocalView.current
    var defData: String? by rememberSaveable { mutableStateOf(null) }
    if (defData == null) LaunchedEffect(Unit) {
        delay(200)
        defData = state.text.toString()
    }
    var clearFocus by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    fun releaseFocusAndDismiss(isClick: Boolean = false) = coroutineScope.launch {
        clearFocus = true
        delay(80)
       if (isClick) onDismissClick() else onDismissRequest()
    }

    val scrollState = rememberScrollState()
    val containerSize = LocalWindowInfo.current.containerSize
    AlertDialog(
        modifier = modifier
            .sizeIn(maxWidth = 720.dp)
            .width((containerSize.width.pxToDpInt() / 1.1).dp),
        onDismissRequest = { releaseFocusAndDismiss() },
        confirmButton = {
            Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                contentProperties.extraButtonBottom()
                Spacer(modifier = modifier.weight(1f))
                TextButton(onClick = {
                    releaseFocusAndDismiss(true)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }) {
                    Text(text = stringResource(R.string.cancel))
                }
                Spacer(modifier = modifier.width(8.dp))
                TextButton(
                    colors = if (disableConform) {
                        ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.outlineVariant)
                    } else ButtonDefaults.textButtonColors(), onClick = {
                        if (!disableConform) coroutineScope.launch {
                            clearFocus = true
                            delay(80)
                            confirm()
                        } else onDisableConformClick()
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                    }) {
                    Text(text = confirmText)
                }
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, fontSize = 22.sp)
                Spacer(modifier = modifier.weight(1f))
                contentProperties.extraButtonTop()
            }
        },
        text = {
            Column {
                contentProperties.extraContentTop()
                val screenHeightDp = containerSize.height.pxToDpInt()
                val height = when {
                    singleLine -> 100
                    screenHeightDp >= 700 -> (screenHeightDp / 5.8).toInt()
                    screenHeightDp < (containerSize.width / 1.1) -> (screenHeightDp / 3.2).toInt()
                    else -> (screenHeightDp / 5.1).toInt()
                }
                OutlinedCard(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(height.dp) then if (singleLine)
                        modifier.padding(top = 16.dp, bottom = 16.dp)
                    else modifier,
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
                    shape = if (singleLine) ShapeDefaults.Small else CardDefaults.outlinedShape
                ) {
                    val focus = LocalFocusManager.current
                    if (clearFocus) LaunchedEffect(true) {
                        focus.clearFocus()
                        clearFocus = false
                    }
                    val fontSize = if (!singleLine) 19.sp else 23.sp
                    val focusRequester = remember { FocusRequester() }
                    if (singleLine) Spacer(
                        modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) { detectTapGestures { focusRequester.requestFocus() } }
                            .weight(0.2f))
                    val showScrollBar =
                        scrollState.canScrollBackward || scrollState.canScrollForward
                    BasicTextField(
                        modifier = modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .fillMaxSize()
                            .padding(8.dp)
                            .verticalColumnScrollbar(
                                scrollState = scrollState, endPadding = 0f,
                                scrollBarCornerRadius = 12f,
                                scrollBarTrackColor = MaterialTheme.colorScheme.outlineVariant,
                                scrollBarColor = MaterialTheme.colorScheme.outline,
                                showScrollBar = showScrollBar && !singleLine
                            ),
                        state = state,
                        textStyle = TextStyle(
                            fontSize = fontSize, color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = if (doneImeAction) {
                                ImeAction.Done
                            } else ImeAction.Default,
                            keyboardType = if (numberOnly) KeyboardType.Number else KeyboardType.Unspecified
                        ),
                        onKeyboardAction = { if (doneImeAction) clearFocus = true },
                        lineLimits = lineLimits,
                        scrollState = scrollState
                    )
                }
                contentProperties.extraContentBottom()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = state.text.isEmpty() || state.text.toString() == defData
        )
    )
}

data class EditableContentDialog(
    val extraButtonTop: @Composable () -> Unit = {},
    val extraButtonBottom: @Composable () -> Unit = {},
    val extraContentTop: @Composable () -> Unit = {},
    val extraContentBottom: @Composable () -> Unit = {},
)

@Preview
@Composable
private fun EditableContentDialogPreview() {
    val state = rememberTextFieldState()
    EditableContentDialog(
        {}, {}, "TEST", "TEST", state, Modifier, EditableContentDialog(),
        singleLine = false
    )
}
