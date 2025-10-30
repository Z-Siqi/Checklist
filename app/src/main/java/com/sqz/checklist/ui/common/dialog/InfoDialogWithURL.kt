package com.sqz.checklist.ui.common.dialog

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqz.checklist.ui.common.UrlText
import com.sqz.checklist.ui.common.verticalColumnScrollbar

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
    InfoAlertDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        text = null,
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
}

@Preview
@Composable
private fun PreviewOfInfoDialogWithURL() {
    InfoDialogWithURL(
        onDismissRequest = {},
        url = "TEST"
    )
}
