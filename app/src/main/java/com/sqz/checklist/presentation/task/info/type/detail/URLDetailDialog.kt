package com.sqz.checklist.presentation.task.info.type.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqz.checklist.R
import com.sqz.checklist.common.AndroidEffectFeedback
import com.sqz.checklist.presentation.task.info.type.InfoDialogScaffold
import com.sqz.checklist.ui.common.UrlText
import com.sqz.checklist.ui.common.unit.pxToDp
import com.sqz.checklist.ui.common.verticalColumnScrollbar
import sqz.checklist.common.EffectFeedback
import sqz.checklist.task.api.info.TaskInfo

/**
 * This method expected to be called only within this package and its sub-packages.
 *
 * @throws ClassCastException if the detail type is not [TaskInfo.DetailInfoState.DetailType.URL].
 */
@Composable
internal fun URLDetailDialog(
    detail: TaskInfo.DetailInfoState,
    onDismissRequest: () -> Unit,
    isSmallScreenSize: Boolean,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
    view: android.view.View = LocalView.current
) {
    val urlType = detail.detailType as TaskInfo.DetailInfoState.DetailType.URL
    val requestClearFocus = remember { mutableStateOf(false) }
    InfoDialogScaffold(
        onDismissRequest = onDismissRequest,
        onDialogBackgroundClick = { requestClearFocus.value = true },
        isSmallScreenSize = isSmallScreenSize,
        modifier = modifier
    ) {
        InfoDetailDialogTitle(
            detailTitle = stringResource(R.string.url),
            detailDescription = detail.detailDescription
        )
        Spacer(modifier = Modifier.height(if (isSmallScreenSize) 10.dp else 18.dp))
        SelectionContainer {
            val focus = LocalFocusManager.current
            if (requestClearFocus.value) {
                focus.clearFocus()
                requestClearFocus.value = false
            }
            URLTypeCard(url = urlType.url, view = view)
        }
        Spacer(modifier = Modifier.height(if (isSmallScreenSize) 10.dp else 20.dp))
        ThisDialogButton {
            onDismissRequest().also { feedback.onClickEffect() }
        }
    }
}

@Composable
private fun URLTypeCard(url: String, view: android.view.View) {
    val windowSize = LocalWindowInfo.current.containerSize
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 19.sp
    )
    val fontSizeDp = with(LocalDensity.current) {
        ((textStyle.fontSize).toPx() * 1.5f).toDp()
    }
    val screenHeightDp = windowSize.height.pxToDp()
    val height: Dp = when {
        screenHeightDp >= 700.dp -> (screenHeightDp / 5.8f)
        screenHeightDp < (windowSize.width.pxToDp() / 1.2f) -> (screenHeightDp / 3.2f)
        else -> (screenHeightDp / 5.1f)
    }
    val scrollState = rememberScrollState()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = (fontSizeDp * 2.618f), max = height)
            .verticalColumnScrollbar(
                scrollState = scrollState, endPadding = 0f, scrollBarCornerRadius = 12f,
                scrollBarTrackColor = MaterialTheme.colorScheme.outlineVariant,
                scrollBarColor = MaterialTheme.colorScheme.outline,
                showScrollBar = scrollState.canScrollBackward || scrollState.canScrollForward
            )
            .verticalScroll(scrollState),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        UrlText(
            url = url,
            style = textStyle,
            view = view,
            modifier = Modifier.padding(10.dp),
        )
    }
}

@Composable
private fun ThisDialogButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier,
        ) {
            Text(
                text = stringResource(R.string.cancel),
                maxLines = 1,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Preview
@Composable
private fun URLDetailDialogPreview() {
    URLDetailDialog(
        detail = TaskInfo.DetailInfoState(
            detailDescription = "To the MOON!",
            detailType = TaskInfo.DetailInfoState.DetailType.URL(
                url = "https://www.cho-kaguyahime.com"
            )
        ),
        onDismissRequest = {},
        isSmallScreenSize = false,
        feedback = AndroidEffectFeedback(LocalView.current)
    )
}
