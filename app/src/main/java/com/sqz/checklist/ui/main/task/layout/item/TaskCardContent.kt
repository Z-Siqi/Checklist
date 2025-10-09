package com.sqz.checklist.ui.main.task.layout.item

import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.sqz.checklist.R
import com.sqz.checklist.ui.common.TextTooltipBox
import com.sqz.checklist.ui.common.dialog.InfoAlertDialog
import com.sqz.checklist.ui.main.task.CardHeight
import com.sqz.checklist.ui.theme.Theme
import kotlin.math.min

enum class CardClickType { Reminder, Edit, Pin, Close, Detail }

enum class ItemMode {
    NormalTask, PinnedTask, RemindedTask
}

data class TaskIconState(
    val isPinned: Boolean = false,
    val isReminderSet: Boolean = false,
    val isDetailExist: Boolean = false,
)

data class TaskTextState(
    val description: String,
    val dateText: String,
    val reminderTooltip: String? = null
)

/**
 * The content of the task UI
 */
@Composable
fun TaskCardContent(
    textState: TaskTextState,
    onClick: (CardClickType) -> Unit,
    mode: ItemMode,
    iconState: TaskIconState,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val colors = Theme.color
    TaskBackground(colors = colors, modifier = modifier) {
        Row(modifier = Modifier, horizontalArrangement = Arrangement.Start) {
            TaskDescription(
                textState = textState,
                modifier = Modifier
            )
            Spacer(modifier = Modifier.weight(1f))
            PinOrCloseButton(
                mode = mode,
                onClick = onClick,
                iconState = iconState,
                modifier = Modifier.requiredSizeIn(minWidth = 28.dp),
                view = view,
            )
        }
        Row(
            modifier = Modifier.heightIn(min = CardHeight.dp * 0.38f),
            verticalAlignment = Alignment.Bottom
        ) {
            DateText(
                modifier = Modifier
                    .weight(1f),
                textState = textState,
            )
            ButtonsRow(
                textState = textState,
                onClick = onClick,
                iconState = iconState,
                view = view,
                modifier = Modifier
            )
            Spacer(modifier = Modifier.padding(end = 15.dp))
        }
    }
}

@Composable
private fun DateText(
    textState: TaskTextState,
    modifier: Modifier,
    density: Density = LocalDensity.current,
    twoLinesHeightDp: Dp = with(density) { ((14.sp).toPx() * 2.5f).toDp() }
) {
    BoxWithConstraints(
        modifier = modifier.heightIn(max = twoLinesHeightDp),
        contentAlignment = Alignment.BottomStart
    ) {
        val measurer = rememberTextMeasurer()
        val raw = textState.dateText.replace(
            Regex("(\\d{1,2}) (\\p{Alpha}+) (\\d{4})"), "$1\u00A0$2\u00A0$3"
        )
        val normalStyle = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        )
        val smallStyle = normalStyle.copy(
            fontSize = 12.sp / 1.8f,
            lineHeight = 14.sp / 1.8f
        )
        val maxW = constraints.maxWidth
        val maxH = min(constraints.maxHeight, with(density) { twoLinesHeightDp.roundToPx() })
        val fitsNormal = remember(raw, maxW, maxH) {
            val result = measurer.measure(
                text = AnnotatedString(raw),
                style = normalStyle,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                constraints = Constraints(maxWidth = maxW, maxHeight = maxH)
            )
            !result.hasVisualOverflow
        }
        val finalText = if (fitsNormal) raw else raw.replace("\n", "")
        val finalStyle = if (fitsNormal) normalStyle else smallStyle
        Text(
            text = finalText,
            style = finalStyle,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 5.dp, bottom = 3.dp)
        )
    }
}

@Composable
private fun ButtonsRow(
    textState: TaskTextState,
    onClick: (CardClickType) -> Unit,
    iconState: TaskIconState,
    view: View,
    modifier: Modifier
) = BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.BottomStart) {
    val buttonsArea = min(maxWidth * 0.38f, 200.dp)
    Row(
        modifier = Modifier.width(buttonsArea),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconState.isDetailExist) TextTooltipBox(textRid = R.string.detail) {
            IconButton(modifier = Modifier.requiredSize(30.dp), onClick = {
                onClick(CardClickType.Detail)
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }) {
                Icon(
                    modifier = Modifier.rotate(-5f),
                    painter = painterResource(id = R.drawable.attach),
                    contentDescription = stringResource(R.string.detail)
                )
            }
        } else Spacer(
            modifier = Modifier.requiredSize(30.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        val timerText = textState.reminderTooltip ?: stringResource(R.string.reminder)
        TextTooltipBox(text = timerText) {
            IconButton(modifier = Modifier.requiredSize(30.dp), onClick = {
                onClick(CardClickType.Reminder)
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }) {
                val timerIcon =
                    if (iconState.isReminderSet) R.drawable.timer_on else R.drawable.timer
                Icon(
                    painter = painterResource(id = timerIcon),
                    contentDescription = timerText
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        TextTooltipBox(textRid = R.string.edit) {
            IconButton(modifier = Modifier.requiredSize(30.dp), onClick = {
                onClick(CardClickType.Edit)
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.edit),
                    contentDescription = stringResource(R.string.edit)
                )
            }
        }
    }
}

@Composable
private fun TaskDescription(
    textState: TaskTextState,
    modifier: Modifier,
    density: Density = LocalDensity.current
) = Card(
    modifier = modifier
        .padding(top = 8.dp)
        .fillMaxWidth(0.75f)
        .height(IntrinsicSize.Min)
        .width(IntrinsicSize.Min),
    colors = CardDefaults.cardColors(Color.Transparent),
) {
    val twoLinesHeightDp = with(density) {
        ((21.sp).toPx() * 2.5f).toDp()
    }
    var overflowState by remember { mutableStateOf(false) }
    var overflowInfo by rememberSaveable { mutableStateOf(false) }
    val clickableModifier = if (overflowState) Modifier.combinedClickable(
        onLongClick = { overflowInfo = true },
        onClick = { overflowInfo = true }
    ) else Modifier.pointerInput(Unit) {
        detectTapGestures(onLongPress = { overflowInfo = true })
    }
    Box(modifier = clickableModifier.height(twoLinesHeightDp)) {
        Text(
            text = textState.description,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 5.dp),
            fontSize = 21.sp,
            lineHeight = 21.sp,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { overflowState = it.hasVisualOverflow },
            maxLines = 2,
            fontWeight = FontWeight.Normal,
        )
    }
    if (overflowInfo) InfoAlertDialog(
        onDismissRequest = { overflowInfo = false },
        text = textState.description
    )
}

@Composable
private fun PinOrCloseButton(
    mode: ItemMode,
    onClick: (CardClickType) -> Unit,
    iconState: TaskIconState,
    view: View,
    modifier: Modifier,
    iconTextId: Int = if (mode == ItemMode.RemindedTask) R.string.cancel_highlight else R.string.pin
) = TextTooltipBox(iconTextId, modifier = modifier.requiredSize(30.dp)) {
    IconButton(
        modifier = if (mode != ItemMode.RemindedTask) Modifier.rotate(40f) else Modifier,
        onClick = {
            if (mode == ItemMode.RemindedTask) onClick(CardClickType.Close) else {
                onClick(CardClickType.Pin)
            }
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
    ) {
        val iconId = if (mode == ItemMode.RemindedTask) R.drawable.close else {
            if (iconState.isPinned) R.drawable.pinned else R.drawable.pin
        }
        Icon(
            painter = painterResource(iconId),
            contentDescription = stringResource(iconTextId)
        )
    }
}

@Composable
private fun TaskBackground(
    colors: Theme,
    modifier: Modifier,
    content: @Composable (ColumnScope.() -> Unit),
) = OutlinedCard(
    modifier = modifier,
    colors = CardDefaults.cardColors(colors.taskBackgroundColor),
    border = BorderStroke(1.dp, colors.taskBorderColor),
    shape = ShapeDefaults.ExtraLarge
) {
    Column(
        modifier = Modifier.padding(bottom = 7.dp, top = 5.dp, start = 12.dp, end = 12.dp),
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

@Preview
@Composable
private fun Preview() {
    TaskCardContent(
        textState = TaskTextState("TEST", "DATE", null),
        onClick = {}, mode = ItemMode.NormalTask, iconState = TaskIconState(isDetailExist = true)
    )
}
