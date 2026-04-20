package com.sqz.checklist.presentation.task.info.type.task

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqz.checklist.R
import com.sqz.checklist.presentation.task.info.type.InfoDialogScaffold
import com.sqz.checklist.ui.common.AdaptiveTieredFlowLayout
import com.sqz.checklist.ui.common.TextTooltipBox
import com.sqz.checklist.ui.common.TieredFlowAlignment
import com.sqz.checklist.ui.common.unit.isSmallScreenSizeForDialog
import com.sqz.checklist.ui.common.unit.pxToDp
import com.sqz.checklist.ui.common.unit.pxToDpInt
import com.sqz.checklist.ui.common.verticalColumnScrollbar
import sqz.checklist.common.EffectFeedback

/** Read-Only Info Task Dialog **/
@Composable
fun InfoTaskDialogUI(
    description: String,
    onDismissRequest: () -> Unit,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
) {
    val isSmallScreenSize = isSmallScreenSizeForDialog()
    InfoTaskDialog(
        description = description,
        onDismissRequest = onDismissRequest,
        isSmallScreenSize = isSmallScreenSize,
        feedback = feedback,
        modifier = modifier
    )
}

/** Info Task Dialog With Detail Button **/
@Composable
fun InfoTaskDialogUI(
    onDetailClick: () -> Unit,
    description: String,
    onDismissRequest: () -> Unit,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
) {
    val isSmallScreenSize = isSmallScreenSizeForDialog()
    InfoTaskDialog(
        description = description,
        onDismissRequest = onDismissRequest,
        isSmallScreenSize = isSmallScreenSize,
        feedback = feedback,
        modifier = modifier,
        contentButton = { DetailButton(isSmallScreenSize, onDetailClick) }
    )
}

/** Info Task Dialog With Pin Changeable **/
@Composable
fun InfoTaskDialogUI(
    pinned: Boolean,
    onPinChange: (Boolean) -> Unit,
    description: String,
    onDismissRequest: () -> Unit,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
) {
    val isSmallScreenSize = isSmallScreenSizeForDialog()
    InfoTaskDialogWithPin(
        pinned = pinned,
        onPinChange = onPinChange,
        description = description,
        onDismissRequest = onDismissRequest,
        isSmallScreenSize = isSmallScreenSize,
        feedback = feedback,
        modifier = modifier,
    )
}

/** Info Task Dialog With Pin Changeable and Detail Button **/
@Composable
fun InfoTaskDialogUI(
    onDetailClick: () -> Unit,
    pinned: Boolean,
    onPinChange: (Boolean) -> Unit,
    description: String,
    onDismissRequest: () -> Unit,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
) {
    val isSmallScreenSize = isSmallScreenSizeForDialog()
    InfoTaskDialogWithPin(
        pinned = pinned,
        onPinChange = onPinChange,
        description = description,
        onDismissRequest = onDismissRequest,
        isSmallScreenSize = isSmallScreenSize,
        feedback = feedback,
        modifier = modifier,
    ) {
        DetailButton(isSmallScreenSize, onDetailClick)
    }
}

@Composable
private fun DetailButton(isSmallScreenSize: Boolean, onClick: () -> Unit) {
    val detailText = stringResource(R.string.detail)
    TextTooltipBox(text = detailText) {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            val iconModifier = Modifier.let {
                if (isSmallScreenSize) it else it.padding(vertical = 3.5.dp)
            }
            Icon(
                painter = painterResource(R.drawable.attach),
                contentDescription = detailText,
                modifier = iconModifier.let { if (isSmallScreenSize) it else it.size(20.dp) }
            )
            if (!isSmallScreenSize) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = detailText)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InfoTaskDialogWithPin(
    pinned: Boolean,
    onPinChange: (Boolean) -> Unit,
    description: String,
    onDismissRequest: () -> Unit,
    isSmallScreenSize: Boolean,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
    contentButton: @Composable (() -> Unit) = {},
) = InfoTaskDialog(
    description = description,
    onDismissRequest = onDismissRequest,
    isSmallScreenSize = isSmallScreenSize,
    feedback = feedback,
    modifier = modifier,
    titleButton = {
        ToggleButton(
            checked = pinned,
            onCheckedChange = { onPinChange(!pinned) },
            modifier = Modifier
                .requiredWidthIn(min = 40.dp)
                .requiredHeightIn(
                    max = (LocalWindowInfo.current.containerSize.height.pxToDp() / 5)
                ),
            enabled = true,
        ) {
            val iconInt = pinned.let {
                if (it) R.drawable.pinned else R.drawable.pin
            }
            val rotateModifier = Modifier.let {
                if (pinned) it else it.rotate(40f)
            }
            TextTooltipBox(textRid = R.string.create_as_pin) {
                Icon(
                    painter = painterResource(iconInt),
                    contentDescription = stringResource(R.string.create_as_pin),
                    modifier = rotateModifier
                )
            }
            if (!isSmallScreenSizeForDialog()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.pin_task),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 2.sp, maxFontSize = LocalTextStyle.current.fontSize
                    ),
                    modifier = Modifier.widthIn(max = 70.dp),
                )
            }
        }
    },
    contentButton = contentButton,
)

@Composable
private fun InfoTaskDialog(
    description: String,
    onDismissRequest: () -> Unit,
    isSmallScreenSize: Boolean,
    feedback: EffectFeedback,
    modifier: Modifier,
    titleButton: @Composable (() -> Unit) = {},
    contentButton: @Composable (() -> Unit)? = null,
) {
    val requestClearFocus = remember { mutableStateOf(false) }
    val windowSize = LocalWindowInfo.current.containerSize
    InfoDialogScaffold(
        onDismissRequest = onDismissRequest,
        onDialogBackgroundClick = { requestClearFocus.value = true },
        isSmallScreenSize = isSmallScreenSize,
        modifier = modifier
    ) {
        AdaptiveTieredFlowLayout(
            mergeWhenPossible = true,
            sectionGap = 3.dp,
            topContent = {
                ThisDialogTitle(
                    title = stringResource(R.string.task_info),
                    windowSize = windowSize,
                )
            },
            bottomContent = titleButton,
        )
        Spacer(modifier = Modifier.height(if (isSmallScreenSize) 8.dp else 20.dp))
        SelectionContainer {
            val focus = LocalFocusManager.current
            if (requestClearFocus.value) {
                focus.clearFocus()
                requestClearFocus.value = false
            }
            TaskTextCard(
                text = description,
                windowSize = windowSize,
            )
        }
        Spacer(modifier = Modifier.height(if (isSmallScreenSize) 10.dp else 24.dp))
        AdaptiveTieredFlowLayout(
            mergeWhenPossible = true,
            topAlignment = TieredFlowAlignment.Start,
            bottomAlignment = TieredFlowAlignment.End,
            sectionGap = 3.dp,
            topContent = { contentButton?.invoke() ?: Spacer(modifier = Modifier.size(1.dp)) },
            bottomContent = {
                TextButton(
                    onClick = { onDismissRequest().also { feedback.onClickEffect() } }
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        maxLines = 1,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ThisDialogTitle(title: String, windowSize: IntSize) {
    val titleStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
    Text(
        text = title,
        style = titleStyle,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .requiredWidthIn(max = (windowSize.width.pxToDpInt() / 2.1).dp)
            .requiredHeightIn(max = (windowSize.height.pxToDp() / 5)),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 5.sp, maxFontSize = titleStyle.fontSize)
    )
}

@Composable
private fun TaskTextCard(text: String, windowSize: IntSize) {
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
            .heightIn(min = (fontSizeDp * 2.5f), max = height)
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
        Text(
            text = text,
            style = textStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(10.dp),
        )
    }
}
