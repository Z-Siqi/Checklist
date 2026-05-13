package com.sqz.checklist.presentation.task.modify.dialog.detail.set

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import com.sqz.checklist.R
import com.sqz.checklist.ui.common.verticalColumnScrollbar
import sqz.checklist.task.api.modify.TaskModify

/** This method expected to be called only within this package and its sub-packages. **/
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TextTypeStateCard(
    clearFocusState: MutableState<Boolean>,
    textState: TaskModify.Detail.TypeState.Text,
    onStateChange: (TaskModify.Detail.TypeState.Text) -> Unit,
    isSmallScreenSize: Boolean,
) {
    val focus = LocalFocusManager.current
    val state = rememberTextFieldState(initialText = textState.description)
    LaunchedEffect(Unit) { // Update change (when recomposed)
        if (state.text.toString() != textState.description) {
            state.clearText()
            state.edit { insert(0, textState.description) }
        }
    }
    LaunchedEffect((state.text != textState.description)) { // Callback change
        onStateChange(textState.copy(description = state.text.toString()))
    }
    if (clearFocusState.value) {
        focus.clearFocus()
        clearFocusState.value = false
    }
    val fieldScrollState = rememberScrollState()
    TextField(
        state = state,
        placeholder = {
            if (!isSmallScreenSize) Text(text = "${stringResource(R.string.text)} ...")
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Default,
        ),
        onKeyboardAction = { focus.clearFocus() },
        modifier = Modifier
            .fillMaxWidth()
            .verticalColumnScrollbar(
                scrollState = fieldScrollState, scrollBarCornerRadius = 12f,
                endPadding = -8f, topBottomPadding = 6f,
                scrollBarTrackColor = MaterialTheme.colorScheme.outlineVariant,
                scrollBarColor = MaterialTheme.colorScheme.outline,
                showScrollBar = fieldScrollState.let { it.canScrollBackward || it.canScrollForward },
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        shape = MaterialTheme.shapes.large,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        lineLimits = TextFieldLineLimits.MultiLine(
            minHeightInLines = 2,
            maxHeightInLines = 4,
        ),
        scrollState = fieldScrollState
    )
    val softIme = remember { mutableStateOf(false) }
    if (WindowInsets.isImeVisible) {
        softIme.value = true
    }
    if (softIme.value && !WindowInsets.isImeVisible) LaunchedEffect(Unit) {
        focus.clearFocus()
        softIme.value = false
    }
}

@Preview
@Composable
private fun TextTypeStateCardPreview() {
    TextTypeStateCard(
        clearFocusState = remember { mutableStateOf(false) },
        textState = TaskModify.Detail.TypeState.Text("Preview Text"),
        onStateChange = {},
        isSmallScreenSize = false
    )
}
