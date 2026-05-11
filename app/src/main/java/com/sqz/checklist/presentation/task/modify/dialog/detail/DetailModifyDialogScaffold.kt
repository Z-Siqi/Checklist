package com.sqz.checklist.presentation.task.modify.dialog.detail

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sqz.checklist.ui.common.verticalColumnScrollbar
import com.sqz.checklist.ui.theme.UISizeLimit

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun DetailModifyDialogScaffold(
    onDismissRequest: () -> Unit,
    onDialogBackgroundClick: () -> Unit,
    isSmallScreenSize: Boolean,
    isModified: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (ColumnScope.() -> Unit),
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = !isModified
        ),
    ) {
        val surfaceModifier = Modifier.let { modifier ->
            if (!isSmallScreenSize) {
                modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
            } else {
                modifier.fillMaxSize()
            }
        }
        val surfaceShape = MaterialTheme.shapes.let {
            if (isSmallScreenSize) it.extraSmall else it.extraLarge
        }
        UISizeLimit {
            val contentModifier = Modifier.let {
                val scrollState = rememberScrollState()
                it.verticalColumnScrollbar(
                    scrollState = scrollState, scrollBarCornerRadius = 12f,
                    scrollBarTrackColor = MaterialTheme.colorScheme.outlineVariant,
                    scrollBarColor = MaterialTheme.colorScheme.outline,
                    showScrollBar = scrollState.canScrollBackward || scrollState.canScrollForward
                ) then it.verticalScroll(scrollState)
            }.let {
                if (isSmallScreenSize) it.padding(1.dp) else it.padding(24.dp)
            }
            Surface(
                modifier = surfaceModifier then modifier,
                shape = surfaceShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = contentModifier.pointerInput(Unit) {
                        detectTapGestures { onDialogBackgroundClick() }
                    },
                    content = content,
                )
            }
        }
    }
}
