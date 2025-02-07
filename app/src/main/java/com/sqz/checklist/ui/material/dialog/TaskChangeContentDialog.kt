package com.sqz.checklist.ui.material.dialog

import android.view.SoundEffectConstants
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.sqz.checklist.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TaskChangeContentDialog(
    onDismissRequest: () -> Unit,
    confirm: () -> Unit,
    title: String,
    confirmText: String,
    state: TextFieldState,
    modifier: Modifier = Modifier,
    extraButtonTop: @Composable () -> Unit = {},
    extraButtonBottom: @Composable () -> Unit = {},
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.MultiLine(),
    doneImeAction: Boolean = false
) {
    val view = LocalView.current
    var clearFocus by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    fun releaseFocusAndDismiss() = coroutineScope.launch {
        clearFocus = true
        delay(80)
        onDismissRequest()
    }

    val scrollState = rememberScrollState()
    AlertDialog(
        modifier = modifier
            .sizeIn(maxWidth = 720.dp)
            .width((LocalConfiguration.current.screenWidthDp / 1.2).dp),
        onDismissRequest = { releaseFocusAndDismiss() },
        confirmButton = {
            Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                extraButtonBottom()
                Spacer(modifier = modifier.weight(1f))
                TextButton(onClick = {
                    releaseFocusAndDismiss()
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }) {
                    Text(text = stringResource(R.string.cancel))
                }
                Spacer(modifier = modifier.width(8.dp))
                TextButton(onClick = {
                    coroutineScope.launch {
                        clearFocus = true
                        delay(80)
                        confirm()
                    }
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
                extraButtonTop()
            }
        },
        text = {
            val screenHeightDp = LocalConfiguration.current.screenHeightDp
            val height = when {
                screenHeightDp >= 700 -> (screenHeightDp / 5.8).toInt()
                screenHeightDp < (LocalConfiguration.current.screenWidthDp / 1.2) -> (screenHeightDp / 3.2).toInt()
                else -> (screenHeightDp / 5.1).toInt()
            }
            OutlinedCard(
                modifier = modifier.fillMaxWidth() then modifier.height(height.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer)
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
                        fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = if (doneImeAction) {
                            ImeAction.Done
                        } else ImeAction.Default
                    ),
                    onKeyboardAction = { if (doneImeAction) clearFocus = true },
                    lineLimits = lineLimits,
                )
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

@Preview
@Composable
private fun TaskChangeContentCardPreview() {
    @Composable
    fun icon() = Icon(painter = painterResource(id = R.drawable.close), contentDescription = null)
    val state = rememberTextFieldState()
    TaskChangeContentDialog({}, {}, "TEST", "TEST", state, Modifier, { icon() }, { icon() })
}
